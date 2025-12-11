package com.wells.bill.assistant.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RagEngineService {

    private static final Logger log = LoggerFactory.getLogger(RagEngineService.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private static final int DEFAULT_TOP_K = 12;
    private static final int HYBRID_FETCH_MULTIPLIER = 4;
    private static final int DEFAULT_STITCH_MAX_CHARS = 8000;

    // ----------------------------------------------------------------------------
    // Safe invocation wrapper
    // ----------------------------------------------------------------------------
    private List<Document> safeSimilaritySearch(SearchRequest request) {
        log.info("safeSimilaritySearch filter={}", request.getFilterExpression());
        try {
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Vector similarity search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ----------------------------------------------------------------------------
    // Defensive filter setter (because string filters are parsed by ANTLR)
    // ----------------------------------------------------------------------------
    private SearchRequest.Builder applyFilterSafely(SearchRequest.Builder builder, String filterExpression) {
        if (filterExpression == null || filterExpression.isBlank()) {
            return builder;
        }

        log.info("Candidate filter expression: [{}]", filterExpression);

        try {
            // This will invoke FilterExpressionTextParser under the hood
            builder.filterExpression(filterExpression);
            return builder;
        } catch (Exception ex) {
            log.warn("Filter expression rejected. Falling back. expr=[{}] cause={}",
                    filterExpression, ex.getMessage());
            return builder; // leave unfiltered
        }
    }

    // ----------------------------------------------------------------------------
    // Retrieve by Bill ID
    // ----------------------------------------------------------------------------
    public void retrieveByBillId(String billId, int topK) {
        log.info("retrieveByBillId: {}", billId);
        if (billId == null || billId.isBlank()) return;

        String filter = FilterExpressionBuilder.start()
                .eq("parent_document_id", billId)
                .build();

        SearchRequest.Builder req = SearchRequest.builder()
                .query("bill details for " + billId)
                .topK(Math.max(1, topK));

        req = applyFilterSafely(req, filter);
        safeSimilaritySearch(req.build());
    }

    // ----------------------------------------------------------------------------
    // Retrieve by Vendor
    // ----------------------------------------------------------------------------
    public void retrieveByVendor(String vendor, int topK) {
        log.info("retrieveByVendor: {}", vendor);
        if (vendor == null || vendor.isBlank()) return;

        String filter = FilterExpressionBuilder.start()
                .eq("vendor", vendor)
                .build();

        SearchRequest.Builder builder = SearchRequest.builder()
                .query("vendor information " + vendor)
                .topK(Math.max(1, topK));

        builder = applyFilterSafely(builder, filter);
        safeSimilaritySearch(builder.build());
    }

    // ----------------------------------------------------------------------------
    // Metadata Filtering
    // ----------------------------------------------------------------------------
    public void retrieveByMetadata(Map<String, Object> metadataFilter, int topK) {
        log.info("retrieveByMetadata: keys={}", metadataFilter == null ? 0 : metadataFilter.size());
        if (metadataFilter == null || metadataFilter.isEmpty()) return;

        String filterExpression;

        Object raw = metadataFilter.get("rawFilter");
        if (raw instanceof String rawExpr) {
            filterExpression = rawExpr;
        } else {
            FilterExpressionBuilder b = FilterExpressionBuilder.start();
            metadataFilter.forEach((k, v) -> {
                if ("rawFilter".equals(k)) return;
                if (v instanceof Collection<?> col) {
                    b.in(k, col);
                } else {
                    b.eq(k, v);
                }
            });
            filterExpression = b.build();
        }

        SearchRequest.Builder req = SearchRequest.builder()
                .query("metadata filtered search")
                .topK(Math.max(1, topK));

        req = applyFilterSafely(req, filterExpression);
        safeSimilaritySearch(req.build());
    }

    // ----------------------------------------------------------------------------
    // Hybrid Retrieve
    // ----------------------------------------------------------------------------
    public List<Document> hybridRetrieve(String query,
                                         Map<String, Object> metadataFilter,
                                         int topK) {

        log.info("hybridRetrieve: query='{}' filterKeys={}",
                query, metadataFilter == null ? 0 : metadataFilter.size());

        if (query == null) {
            query = "";
        }

        int fetch = Math.max(topK * HYBRID_FETCH_MULTIPLIER,
                Math.max(topK, DEFAULT_TOP_K));

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(fetch);

        if (metadataFilter != null && !metadataFilter.isEmpty()) {
            Object raw = metadataFilter.get("rawFilter");

            if (raw instanceof String rawExpr && !rawExpr.isBlank()) {
                log.info("Using raw filter: {}", rawExpr);
                builder = applyFilterSafely(builder, rawExpr);
            } else {
                FilterExpressionBuilder fb = FilterExpressionBuilder.start();
                metadataFilter.forEach((k, v) -> {
                    if ("rawFilter".equals(k)) return;
                    if (v instanceof Collection<?> col) {
                        fb.in(k, col);
                    } else {
                        fb.eq(k, v);
                    }
                });
                String built = fb.build();
                builder = applyFilterSafely(builder, built);
            }
        }

        SearchRequest request = builder.build();
        List<Document> candidates = safeSimilaritySearch(request);
        if (candidates.isEmpty()) return List.of();

        List<String> queryTokens = tokenize(query);
        List<ScoredDoc> scored = new ArrayList<>(candidates.size());

        for (Document d : candidates) {
            double lexical = lexicalScore(queryTokens, d.getText());
            Double sim = d.getScore();
            double combined = (sim != null) ? (0.75 * sim + 0.25 * lexical) : lexical;
            scored.add(new ScoredDoc(d, combined));
        }

        scored.sort(Comparator.comparingDouble(ScoredDoc::score).reversed());

        return scored.stream()
                .limit(Math.max(1, topK))
                .map(ScoredDoc::doc)
                .toList();
    }

    public List<Document> retrieveByBillIdHybrid(String billId, String query, int topK) {
        log.info("retrieveByBillIdHybrid: {}", billId);
        if (billId == null || billId.isBlank()) return List.of();

        Map<String, Object> filter = Map.of("parent_document_id", billId);
        return hybridRetrieve(query, filter, topK);
    }

    // ----------------------------------------------------------------------------
    // Stitch context
    // ----------------------------------------------------------------------------
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
                    sb.append(t, 0, remaining - 3).append("...");
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

    // ----------------------------------------------------------------------------
    // Prompt + Answer
    // ----------------------------------------------------------------------------
    public String answerQuestionForBill(String billId, String userQuery) {
        if (billId == null || billId.isBlank()) return "Bill ID is required.";
        if (userQuery == null || userQuery.isBlank()) return "Question is required.";

        List<Document> chunks = retrieveByBillIdHybrid(billId, userQuery, DEFAULT_TOP_K);
        if (chunks.isEmpty()) return "I don't have enough information from the retrieved bills.";

        String context = stitchContext(chunks);
        if (context.isBlank()) return "I don't have enough information from the retrieved bills.";

        String prompt = buildBillPrompt(userQuery, context);

        try {
            return Optional.ofNullable(
                    chatClient
                            .prompt()
                            .system("You are an AI Bill Assistant. Use only provided context.")
                            .user(prompt)
                            .call()
                            .content()
            ).orElse("I couldn't generate an answer.").trim();
        } catch (Exception e) {
            log.error("LLM call failed for billId={}: {}", billId, e.getMessage());
            return "Failed to generate answer due to an internal error.";
        }
    }

    private String buildBillPrompt(String userQuery, String context) {
        return "You are an AI Bill Management Assistant.\n" +
                "Question:\n" + userQuery +
                "\n\nRetrieved Bill Context:\n" + context;
    }

    // ----------------------------------------------------------------------------
    // Utility scoring
    // ----------------------------------------------------------------------------
    private List<String> tokenize(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.toLowerCase().split("\\W+"))
                .filter(t -> !t.isBlank())
                .toList();
    }

    private double lexicalScore(List<String> tokens, String text) {
        if (tokens.isEmpty() || text == null) return 0;
        List<String> words = tokenize(text);
        long matches = words.stream().filter(tokens::contains).count();
        return (double) matches / Math.max(1, words.size());
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

    private record ScoredDoc(Document doc, double score) {
    }
}
