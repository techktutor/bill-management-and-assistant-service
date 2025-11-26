package com.wells.bill.assistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final VectorStore vectorStore;

    /**
     * Retrieve chunks for a specific bill using metadata filtering.
     * This pulls only chunks whose metadata has: parent_document_id = billId
     */
    public List<Document> retrieveByBillId(String billId, int topK) {
        if (billId == null || billId.isBlank()) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query("bill details for " + billId) // query still required
                .topK(topK)
                .filterExpression(Map.of("parent_document_id", billId).toString())
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * Retrieve chunks filtered by vendor metadata
     */
    public List<Document> retrieveByVendor(String vendor, int topK) {
        if (vendor == null || vendor.isBlank()) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query("vendor information " + vendor)
                .topK(topK)
                .filterExpression(Map.of("vendor", vendor).toString())
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * Generic metadata filter
     */
    public List<Document> retrieveByMetadata(Map<String, Object> filter, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query("metadata filtered search")
                .topK(topK)
                .filterExpression(filter.toString())
                .build();

        return vectorStore.similaritySearch(request);
    }
}
