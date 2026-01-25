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
            int idx = 0;

            for (Document chunk : documents) {
                Map<String, Object> metadata = chunk.getMetadata();

                metadata.put("billId", bill.getId().toString());
                metadata.put("userId", bill.getUserId().toString());
                metadata.put("chunkIndex", idx++);
                metadata.put("ingestedAt", now.toString());
                metadata.put("ingestionVersion", "v1");

                metadata.put("billStatus", bill.getStatus().toString());
                metadata.put("billCategory", bill.getBillCategory().name());

                metadata.put("amountDue", bill.getAmountDue() != null ? bill.getAmountDue().toString() : null);

                metadata.put("dueDate", bill.getDueDate() != null ? bill.getDueDate().toString() : null);

                metadata.put("consumerName", bill.getConsumerName());
                metadata.put("consumerNumber", bill.getConsumerNumber());
                metadata.put("providerName", bill.getProviderName());
                metadata.put("confidenceScore", bill.getConfidenceScore());
                metadata.put("confidenceDecision", bill.getConfidenceDecision().name());
            }

            vectorStore.add(documents);

            bill.setChunkCount(documents.size());
            bill.setIngestedAt(now);
            bill.setStatus(BillStatus.INGESTED);
            bill.setChunkCount(documents.size());

            log.info("Successfully ingested bill: {} into: {} chunks", billId, documents.size());
            return documents.size();
        } catch (Exception e) {
            bill.setStatus(BillStatus.FAILED);
            log.error("ETL ingestion failed for bill {}: {}", billId, e.getMessage(), e);
            throw new RuntimeException("ETL ingestion failed.", e);
        }
    }
}
