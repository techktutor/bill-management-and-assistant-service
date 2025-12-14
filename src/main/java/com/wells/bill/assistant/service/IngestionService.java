package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final BillRepository billRepository;

    private final TokenTextSplitter splitter = new TokenTextSplitter();
    private final Tika tika = new Tika();

    /**
     * Ingests a bill document and links vector chunks to BillEntity.
     */
    @Transactional
    public int ingestFile(UUID billId, MultipartFile file) {

        BillEntity bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() != BillStatus.UPLOADED) {
            throw new IllegalStateException("Bill not in UPLOADED state");
        }

        try (InputStream is = file.getInputStream()) {

            if (file.getSize() > (20 * 1024 * 1024)) {
                throw new IllegalArgumentException("File too large. Max allowed: 20MB");
            }

            bill.setStatus(BillStatus.INGESTING);

            String filename = (file.getOriginalFilename() != null)
                    ? file.getOriginalFilename()
                    : "unknown";

            // Step 1: Extract text
            String text = tika.parseToString(is);

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Unable to extract readable text from file.");
            }

            // Step 2: Wrap & chunk
            Document mainDoc = new Document(text);
            List<Document> chunks = splitter.split(mainDoc);

            Instant now = Instant.now();

            int idx = 0;
            for (Document chunk : chunks) {
                Map<String, Object> metadata = chunk.getMetadata();
                metadata.put("bill_id", bill.getId().toString());
                metadata.put("customer_id", bill.getCustomerId().toString());
                metadata.put("chunk_index", idx++);
                metadata.put("ingested_at", now.toString());
                metadata.put("source_type", file.getContentType());
                metadata.put("original_filename", filename);
            }

            // Step 3: Store in vector DB
            vectorStore.add(chunks);

            // Step 4: Update bill ingestion metadata
            bill.setChunkCount(chunks.size());
            bill.setIngestedAt(now);
            bill.setStatus(BillStatus.INGESTED);

            log.info("Successfully ingested bill {} into {} chunks", billId, chunks.size());
            return chunks.size();

        } catch (Exception e) {
            bill.setStatus(BillStatus.FAILED);
            log.error("ETL ingestion failed for bill {}: {}", billId, e.getMessage(), e);
            throw new RuntimeException("ETL ingestion failed.", e);
        }
    }
}

