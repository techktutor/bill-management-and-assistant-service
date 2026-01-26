package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.model.BillStatus;
import com.wells.bill.assistant.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.*;

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
    public int ingestFile(UUID billId, List<Document> documents) {
        log.info("Starting ETL ingestion for bill: {}", billId);

        BillEntity bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() != BillStatus.UPLOADED) {
            throw new IllegalStateException("Bill not in UPLOADED state");
        }

        bill.setStatus(BillStatus.INGESTING);

        try {
            Instant now = Instant.now();

            List<Document> mutableDocuments = new ArrayList<>(documents);

            for (int i = 0; i < mutableDocuments.size(); i++) {
                Document chunk = mutableDocuments.get(i);

                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());

                putIfNotNull(metadata, "billId", bill.getId().toString());
                putIfNotNull(metadata, "userId", bill.getUserId().toString());
                putIfNotNull(metadata, "chunkIndex", i);
                putIfNotNull(metadata, "ingestedAt", now.toString());
                putIfNotNull(metadata, "ingestionVersion", "v1");
                putIfNotNull(metadata, "billStatus", bill.getStatus().toString());
                putIfNotNull(metadata, "billCategory", null != bill.getBillCategory() ? bill.getBillCategory().name() : "Unknown");
                putIfNotNull(metadata, "amountDue", bill.getAmountDue().toString());
                putIfNotNull(metadata, "dueDate", bill.getDueDate().toString());
                putIfNotNull(metadata, "consumerName", null != bill.getConsumerName() ? bill.getConsumerName() : "Unknown");
                putIfNotNull(metadata, "consumerNumber", null != bill.getConsumerNumber() ? bill.getConsumerNumber() : "Unknown");
                putIfNotNull(metadata, "providerName", null != bill.getProviderName() ? bill.getProviderName() : "Unknown");
                putIfNotNull(metadata, "confidenceScore", bill.getConfidenceScore());
                putIfNotNull(metadata, "confidenceDecision", bill.getConfidenceDecision().name());

                Document enriched = Document.builder()
                        .id(chunk.getId())
                        .text(chunk.getText())
                        .metadata(metadata)
                        .build();

                mutableDocuments.set(i, enriched);
            }

            mutableDocuments.forEach(d ->
                    Assert.isTrue(
                            d.getMetadata().values().stream().noneMatch(Objects::isNull),
                            "Document metadata contains null values"
                    )
            );

            vectorStore.add(mutableDocuments);

            bill.setChunkCount(mutableDocuments.size());
            bill.setIngestedAt(now);
            bill.setStatus(BillStatus.INGESTED);

            log.info("Successfully ingested bill: {} into: {} chunks", billId, mutableDocuments.size());
            return mutableDocuments.size();
        } catch (Exception e) {
            bill.setStatus(BillStatus.FAILED);
            log.error("ETL ingestion failed for bill {}: {}", billId, e.getMessage(), e);
            throw new RuntimeException("ETL ingestion failed.", e);
        }
    }

    private void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
