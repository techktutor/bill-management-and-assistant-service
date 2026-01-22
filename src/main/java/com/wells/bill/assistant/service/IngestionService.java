package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.entity.BillStatus;
import com.wells.bill.assistant.repository.BillRepository;
import com.wells.bill.assistant.util.TextExtractor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Ingests a bill document and links vector chunks to BillEntity.
     */
    @Transactional
    public int ingestFile(UUID billId, MultipartFile file) {
        log.info("Starting ETL ingestion for bill: {}", billId);

        BillEntity bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() != BillStatus.UPLOADED) {
            throw new IllegalStateException("Bill not in UPLOADED state");
        }

        try {
            List<Document> documents = TextExtractor.extractTextDocuments(file);

            Instant now = Instant.now();

            int idx = 0;
            for (Document chunk : documents) {
                Map<String, Object> metadata = chunk.getMetadata();
                metadata.put("bill_id", bill.getId().toString());
                metadata.put("customer_id", bill.getUserId().toString());
                metadata.put("chunk_index", idx++);
                metadata.put("ingested_at", now.toString());
                metadata.put("source_type", file.getContentType());
                metadata.put("original_filename", file.getOriginalFilename());
            }

            vectorStore.add(documents);

            bill.setChunkCount(documents.size());
            bill.setIngestedAt(now);
            bill.setStatus(BillStatus.INGESTED);

            log.info("Successfully ingested bill: {} into: {} chunks", billId, documents.size());
            return documents.size();
        } catch (Exception e) {
            bill.setStatus(BillStatus.FAILED);
            log.error("ETL ingestion failed for bill {}: {}", billId, e.getMessage(), e);
            throw new RuntimeException("ETL ingestion failed.", e);
        }
    }
}
