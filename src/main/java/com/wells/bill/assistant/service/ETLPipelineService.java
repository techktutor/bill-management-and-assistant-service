package com.wells.bill.assistant.service;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ETLPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ETLPipelineService.class);

    private final VectorStore vectorStore;

    // Recommended: 256–512 tokens, 30–40 token overlap
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public int ingestFile(MultipartFile file) {

        try (InputStream is = file.getInputStream()) {

            if (file.getSize() > (20 * 1024 * 1024)) {
                throw new IllegalArgumentException("File too large. Max allowed: 20MB");
            }

            String filename = (file.getOriginalFilename() != null)
                    ? file.getOriginalFilename()
                    : "unknown";

            // Step 1: Extract text using Tika
            Tika tika = new Tika();
            String text = tika.parseToString(is);

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Unable to extract readable text from file.");
            }

            // Step 2: Wrap raw text into a Document first
            Document mainDoc = new Document(text);

            // Step 3: Token-based chunking
            List<Document> chunks = splitter.split(mainDoc);

            Instant now = Instant.now();

            int idx = 0;
            for (Document chunk : chunks) {
                chunk.getMetadata().put("chunk_index", idx++);
                chunk.getMetadata().put("ingested_at", now.toString());
                chunk.getMetadata().put("ingested_by", "etl-service");
                chunk.getMetadata().put("source_type", file.getContentType());
                chunk.getMetadata().put("original_filename", filename);
            }
            vectorStore.add(chunks);
            log.info("Successfully ingested file '{}' into {} chunks.", filename, chunks.size());
            return chunks.size();
        } catch (Exception e) {
            log.error("ETL ingestion failed: {}", e.getMessage(), e);
            throw new RuntimeException("ETL ingestion failed.", e);
        }
    }
}
