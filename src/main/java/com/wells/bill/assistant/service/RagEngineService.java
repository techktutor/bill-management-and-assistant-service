package com.wells.bill.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RagEngineService
 * <p>
 * Merges VectorRetrievalService + RagQueryService responsibilities:
 * - metadata-filtered vector search (retrieveByBillId, retrieveByVendor, retrieveByMetadata)
 * - hybrid retrieval (vector + lexical re-ranking)
 * - context stitching for multi-chunk documents
 * - bill-aware RAG prompt + LLM call
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagEngineService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    // Defaults can be tuned
    private static final int DEFAULT_TOP_K = 12;
    private static final int HYBRID_FETCH_MULTIPLIER = 4; // fetch more candidates for re-ranking
    private static final int DEFAULT_STITCH_MAX_CHARS = 8000;

    // ----------------------- Retrieval helpers -----------------------

    /**
     * Retrieve chunks for a specific bill using metadata filtering. Safe input handling.
     */
    public List<Document> retrieveByBillId(String billId, int topK) {
        if (billId == null || billId.isBlank()) return List.of();
        SearchRequest request = SearchRequest.builder()
                .query("bill details for " + billId)
                .topK(Math.max(1, topK))
                .filterExpression(Map.of("parent_document_id", billId).toString())
                .build();
        return safeSimilaritySearch(request);
    }

    public List<Document> retrieveByVendor(String vendor, int topK) {
        if (vendor == null || vendor.isBlank()) return List.of();
        SearchRequest request = SearchRequest.builder()
                .query("vendor information " + vendor)
                .topK(Math.max(1, topK))
                .filterExpression(Map.of("vendor", vendor).toString())
                .build();
        return safeSimilaritySearch(request);
    }

    public List<Document> retrieveByMetadata(Map<String, Object> filter, int topK) {
        if (filter == null || filter.isEmpty()) return List.of();
        SearchRequest request = SearchRequest.builder()
                .query("metadata filtered search")
                .topK(Math.max(1, topK))
                .filterExpression(filter.toString())
                .build();
        return safeSimilaritySearch(request);
    }

    private List<Document> safeSimilaritySearch(SearchRequest request) {
        try {
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Vector similarity search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ----------------------- Hybrid retrieval -----------------------

    /**
     * Hybrid retrieval: run a vector search restricted by metadata filters (if provided)
     * then re-rank the results by a simple lexical overlap score and return top-K.
     *
     * @param query          free-text query (used for vector similarity)
     * @param metadataFilter optional metadata filter map (e.g. parent_document_id, vendor)
     * @param topK           number of candidates to return
     */
    public List<Document> hybridRetrieve(String query, Map<String, Object> metadataFilter, int topK) {
        if (query == null) query = "";
        int fetch = Math.max(topK * HYBRID_FETCH_MULTIPLIER, Math.max(topK, DEFAULT_TOP_K));

        SearchRequest.Builder b = SearchRequest.builder()
                .query(query)
                .topK(fetch);

        if (metadataFilter != null && !metadataFilter.isEmpty()) {
            b.filterExpression(metadataFilter.toString());
        }

        SearchRequest req = b.build();
        List<Document> candidates = safeSimilaritySearch(req);
        if (candidates.isEmpty()) return List.of();

        List<String> queryTokens = tokenize(query);

        List<ScoredDoc> scored = new ArrayList<>(candidates.size());
        for (Document d : candidates) {
            double lexical = lexicalScore(queryTokens, d.getText());
            Double simScore = d.getScore();
            double combined = (simScore != null) ? (0.75 * simScore + 0.25 * lexical) : lexical;
            scored.add(new ScoredDoc(d, combined));
        }

        scored.sort(Comparator.comparingDouble(ScoredDoc::score).reversed());

        return scored.stream()
                .limit(Math.max(1, topK))
                .map(s -> s.doc)
                .collect(Collectors.toList());
    }

    /**
     * Convenience wrapper: hybrid retrieve restricted to billId's chunks
     */
    public List<Document> retrieveByBillIdHybrid(String billId, String query, int topK) {
        if (billId == null || billId.isBlank()) return List.of();
        Map<String, Object> filter = Map.of("parent_document_id", billId);
        return hybridRetrieve(query, filter, topK);
    }

    // ----------------------- Context stitching -----------------------

    /**
     * Stitch chunks into a readable context. Sorts by metadata chunk_index when available.
     * Limits output to maxChars (roughly mapping to token window).
     */
    public String stitchContext(List<Document> chunks, int maxChars) {
        if (chunks == null || chunks.isEmpty()) return "";
        List<Document> sorted = chunks.stream()
                .sorted(Comparator.comparingInt(this::extractChunkIndex))
                .toList();

        StringBuilder sb = new StringBuilder();
        for (Document d : sorted) {
            String t = d.getText();
            if (t == null || t.isBlank()) continue;
            if (sb.length() + t.length() > maxChars) {
                int remaining = maxChars - sb.length();
                if (remaining > 20) {
                    sb.append(t, 0, Math.max(remaining - 3, 0)).append("...");
                }
                break;
            }
            sb.append(t).append("\n\n---\n\n");
        }
        return sb.toString().trim();
    }

    public String stitchContext(List<Document> chunks) {
        return stitchContext(chunks, DEFAULT_STITCH_MAX_CHARS);
    }

    private int extractChunkIndex(Document d) {
        try {
            Object v = d.getMetadata().get("chunk_index");
            if (v instanceof Number) return ((Number) v).intValue();
            if (v instanceof String) return Integer.parseInt((String) v);
        } catch (Exception ignored) {
        }
        return Integer.MAX_VALUE;
    }

    // ----------------------- RAG: prompt + LLM call -----------------------

    /**
     * Answer a question about a single bill using RAG:
     * - hybrid retrieve
     * - stitch context
     * - build a constrained prompt
     * - call ChatClient
     */
    public String answerQuestionForBill(String billId, String userQuery) {
        if (billId == null || billId.isBlank()) return "Bill ID is required.";
        if (userQuery == null || userQuery.isBlank()) return "Question is required.";

        List<Document> chunks = retrieveByBillIdHybrid(billId, userQuery, DEFAULT_TOP_K);
        if (chunks.isEmpty()) {
            return "I don't have enough information from the retrieved bills.";
        }

        String context = stitchContext(chunks, DEFAULT_STITCH_MAX_CHARS);
        if (context.isBlank()) {
            return "I don't have enough information from the retrieved bills.";
        }

        String prompt = buildBillPrompt(userQuery, context);

        try {
            String response = chatClient
                    .prompt()
                    .system("You are an AI Bill Assistant. Use only the provided context to answer, be concise and do not hallucinate.")
                    .user(prompt)
                    .call()
                    .content();
            return response == null ? "I couldn't generate an answer." : response.trim();
        } catch (Exception e) {
            log.error("LLM call failed for billId={}: {}", billId, e.getMessage(), e);
            return "Failed to generate answer due to an internal error.";
        }
    }

    private String buildBillPrompt(String userQuery, String context) {
        return "You are an AI Bill Management Assistant.\n" +
                "Use ONLY the context below to answer the question. Do NOT hallucinate.\n\n" +
                "Question:\n" +
                userQuery +
                "\n\nRetrieved Bill Context:\n" +
                context +
                "\n\nInstructions:\n" +
                "- If context is insufficient, reply: \"I don’t have enough information from the retrieved bills.\"\n" +
                "- If context contains amounts/dates/usage/vendor info, extract them accurately and consistently.\n" +
                "- If multiple chunks belong to the same bill, combine their meaning.\n" +
                "- Answer concisely and only using the context.\n";
    }

    // ----------------------- Utilities -----------------------

    private List<String> tokenize(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private double lexicalScore(List<String> tokens, String text) {
        if (tokens == null || tokens.isEmpty() || text == null) return 0.0;
        List<String> words = tokenize(text);
        long matches = words.stream().filter(tokens::contains).count();
        return (double) matches / Math.max(words.size(), 1);
    }

    private record ScoredDoc(Document doc, double score) {
        public double score() {
            return score;
        }
    }
}
