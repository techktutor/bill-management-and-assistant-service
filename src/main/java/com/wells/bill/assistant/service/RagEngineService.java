package com.wells.bill.assistant.service;

import com.wells.bill.assistant.builder.FilterExpressionBuilder;
import com.wells.bill.assistant.model.RagAnswer;
import com.wells.bill.assistant.store.RagAnswerCache;
import com.wells.bill.assistant.util.CustomPromptTemple;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagEngineService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final MeterRegistry meterRegistry;
    private final RagAnswerCache ragAnswerCache;

    private static final int TOP_K = 12;
    private static final int HYBRID_FETCH_MULTIPLIER = 4;
    private static final int MAX_CONTEXT_CHARS = 8000;

    // -------------------------
    // Guardrail thresholds
    // -------------------------
    private static final double BLOCK_THRESHOLD = 0.45;
    private static final double WARN_THRESHOLD = 0.65;
    private static final double MIN_LEXICAL_GROUNDING = 0.20;

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    // ============================================================
    // PUBLIC ENTRY — Bill-scoped RAG (single authoritative entry)
    // ============================================================
    public RagAnswer answerBillQuestion(String conversationId, String billId, String question) {
        log.info("RAG Engine: conversationId={} billId={} question={}", conversationId, billId, question);

        meterRegistry.counter("rag.requests.total").increment();
        meterRegistry.counter("rag.requests.bill").increment();

        Timer.Sample totalTimer = Timer.start(meterRegistry);
        String normalizedQuestion = normalizeQuestion(question);
        try {
            if (billId == null || billId.isBlank()) {
                return RagAnswer.blocked("Bill ID is required to answer this question.");
            }

            if (question == null || question.isBlank()) {
                return RagAnswer.blocked("Please ask a question about the bill.");
            }

            // =====================================================
            // ✅ CACHE LOOKUP
            // =====================================================
            //Optional<RagAnswer> cached = ragAnswerCache.get(conversationId, billId, normalizedQuestion);
            Optional<RagAnswer> cached = Optional.ofNullable(ragAnswerCache.get(conversationId, billId, normalizedQuestion))
                    .orElse(Optional.empty());

            if (cached.isPresent()) {
                meterRegistry.counter("rag.cache.hit").increment();
                log.info("RAG cache hit: conversationId={} billId={}", conversationId, billId);
                log.info("RAG cached answer: {}", cached.get().answer());
                return cached.get();
            }
            meterRegistry.counter("rag.cache.miss").increment();

            List<Document> chunks = retrieveBillChunksHybrid(billId, question);
            meterRegistry.summary("rag.retrieval.chunks").record(chunks.size());

            if (chunks.isEmpty()) {
                meterRegistry.counter("rag.retrieval.empty").increment();
                return RagAnswer.blocked("I don’t have enough information from the retrieved bills.");
            }

            String context = stitchContext(chunks);
            if (context.isBlank()) {
                meterRegistry.counter("rag.context.empty").increment();
                return RagAnswer.blocked("I don’t have enough information from the retrieved bills.");
            }

            String rawAnswer = generateAnswer(question, context);
            RagAnswer evaluated = evaluateAnswer(rawAnswer, chunks);

            meterRegistry.summary("rag.answer.confidence").record(evaluated.confidence());

            RagAnswer finalAnswer;

            if (evaluated.confidence() < BLOCK_THRESHOLD) {
                meterRegistry.counter("rag.answer.blocked").increment();
                finalAnswer = RagAnswer.blocked("I don’t have enough reliable information from the retrieved bills to answer that.");
            } else if (evaluated.confidence() < WARN_THRESHOLD) {
                meterRegistry.counter("rag.answer.warned").increment();
                finalAnswer = RagAnswer.warned(
                        "⚠️ This answer may be incomplete.\n\n" + evaluated.answer(),
                        evaluated.confidence(),
                        evaluated.chunksUsed()
                );
            } else {
                meterRegistry.counter("rag.answer.accepted").increment();
                finalAnswer = evaluated;
            }

            // =====================================================
            // ✅ CACHE STORE (never null now)
            // =====================================================
            ragAnswerCache.put(
                    conversationId,
                    billId,
                    normalizedQuestion,
                    finalAnswer,
                    CACHE_TTL
            );
            log.info("RAG final answer: {}", finalAnswer.answer());
            return Objects.requireNonNull(finalAnswer, "RagAnswer must never be null");
        } finally {
            totalTimer.stop(meterRegistry.timer("rag.latency.total"));
        }
    }

    private Set<String> inferChunkTypes(String query) {
        String q = query.toLowerCase();

        if (q.contains("amount") || q.contains("due") || q.contains("balance")) {
            return Set.of("AMOUNT", "SUMMARY");
        }

        if (q.contains("date")) {
            return Set.of("DATES");
        }

        if (q.contains("charge") || q.contains("breakdown")) {
            return Set.of("LINE_ITEMS");
        }

        return Set.of(); // no preference
    }

    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return query.toLowerCase()
                // remove UUIDs (handled via metadata filters)
                .replaceAll("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", " ")
                // normalize currency symbols
                .replaceAll("\\$", " usd ")
                .replaceAll("₹", " inr ")
                // remove punctuation (keep numbers and words)
                .replaceAll("[^a-z0-9. ]", " ")
                // collapse whitespace
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ============================================================
    // Retrieval (private)
    // ============================================================
    private List<Document> retrieveBillChunksHybrid(String billId, String query) {

        int fetch = Math.max(TOP_K * HYBRID_FETCH_MULTIPLIER, TOP_K);

        String sanitizedQuery = sanitizeQuery(normalizeQuestion(query));

        Set<String> preferredTypes = inferChunkTypes(sanitizedQuery);

        // ------------------------------------------------------------
        // Build base bill filter
        // ------------------------------------------------------------
        FilterExpressionBuilder baseFilter =
                FilterExpressionBuilder.start()
                        .eq("bill_id", billId);

        // ------------------------------------------------------------
        // Optional chunk_type OR-filter (vector-safe)
        // ------------------------------------------------------------
        FilterExpressionBuilder finalFilter = baseFilter;

        if (!preferredTypes.isEmpty()) {
            FilterExpressionBuilder[] typeFilters =
                    preferredTypes.stream()
                            .map(t -> FilterExpressionBuilder.start().eq("chunk_type", t))
                            .toArray(FilterExpressionBuilder[]::new);

            finalFilter = FilterExpressionBuilder.start()
                    .and(
                            baseFilter,
                            FilterExpressionBuilder.start().or(typeFilters)
                    );
        }

        // ------------------------------------------------------------
        // Primary retrieval
        // ------------------------------------------------------------
        SearchRequest request = SearchRequest.builder()
                .query(sanitizedQuery)
                .topK(fetch)
                .filterExpression(finalFilter.build())
                .build();

        List<Document> candidates = safeSimilaritySearch(request);

        // ------------------------------------------------------------
        // Fallback: remove chunk_type constraint if nothing found
        // ------------------------------------------------------------
        if (candidates.isEmpty() && !preferredTypes.isEmpty()) {

            SearchRequest fallback = SearchRequest.builder()
                    .query(sanitizedQuery)
                    .topK(fetch)
                    .filterExpression(
                            FilterExpressionBuilder.start()
                                    .eq("bill_id", billId)
                                    .build()
                    )
                    .build();

            candidates = safeSimilaritySearch(fallback);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        // ------------------------------------------------------------
        // Hybrid re-ranking (unchanged)
        // ------------------------------------------------------------
        return rerankHybrid(query, candidates)
                .stream()
                .limit(TOP_K)
                .toList();
    }

    private List<Document> safeSimilaritySearch(SearchRequest request) {
        Timer.Sample timer = Timer.start(meterRegistry);
        try {
            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            meterRegistry.counter("rag.retrieval.errors").increment();
            return List.of();
        } finally {
            timer.stop(meterRegistry.timer("rag.latency.retrieval"));
        }
    }

    // ============================================================
    // Hybrid re-ranking
    // ============================================================
    private List<Document> rerankHybrid(String query, List<Document> candidates) {
        List<String> tokens = tokenize(query);
        List<ScoredDoc> scored = new ArrayList<>(candidates.size());

        for (Document d : candidates) {
            double lexical = lexicalScore(tokens, d.getText());
            Double semantic = d.getScore();
            double combined = semantic != null
                    ? (0.75 * semantic + 0.25 * lexical)
                    : lexical;
            scored.add(new ScoredDoc(d, combined));
        }

        scored.sort(Comparator.comparingDouble(ScoredDoc::score).reversed());
        return scored.stream().map(ScoredDoc::doc).toList();
    }

    // ============================================================
    // Context stitching
    // ============================================================
    private String stitchContext(List<Document> chunks) {

        List<Document> ordered = chunks.stream()
                .sorted(Comparator.comparingInt(this::chunkIndex))
                .toList();

        StringBuilder sb = new StringBuilder();

        for (Document d : ordered) {
            String text = d.getText();
            if (text == null || text.isBlank()) continue;

            if (sb.length() + text.length() > MAX_CONTEXT_CHARS) {
                int remaining = MAX_CONTEXT_CHARS - sb.length();
                if (remaining > 20) {
                    sb.append(text, 0, remaining - 3).append("...");
                }
                break;
            }

            sb.append(text).append("\n\n---\n\n");
        }

        return sb.toString().trim();
    }

    private int chunkIndex(Document d) {
        Object v = d.getMetadata().get("chunk_index");
        try {
            return v instanceof Number ? ((Number) v).intValue() :
                    v instanceof String ? Integer.parseInt((String) v) :
                            Integer.MAX_VALUE;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    // ============================================================
    // Answer generation
    // ============================================================
    private String generateAnswer(String question, String context) {
        String prompt = CustomPromptTemple.prompt().formatted(question, context);
        Timer.Sample timer = Timer.start(meterRegistry);
        try {
            return Optional.ofNullable(
                    chatClient.prompt()
                            .system("Use only the provided bill context.")
                            .user(prompt)
                            .call()
                            .content()
            ).orElseGet(() -> {
                meterRegistry.counter("rag.answer.fallback").increment();
                return "I don’t have enough information from the retrieved bills.";
            });
        } catch (Exception e) {
            meterRegistry.counter("rag.llm.errors").increment();
            log.error("LLM call failed", e);
            return "Failed to generate answer due to an internal error.";
        } finally {
            timer.stop(meterRegistry.timer("rag.latency.llm"));
        }
    }

    // ============================================================
    // Utilities
    // ============================================================
    private List<String> tokenize(String s) {
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

    private record ScoredDoc(Document doc, double score) {
    }

    // ============================================================
    // Confidence evaluation
    // ============================================================
    private RagAnswer evaluateAnswer(String answer, List<Document> chunks) {
        double topScore = chunks.stream()
                .map(Document::getScore)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0.0);

        boolean retrievalStrong = topScore >= 0.75 && chunks.size() >= 2;

        long coverage = chunks.stream()
                .map(d -> d.getMetadata().get("chunk_index"))
                .filter(Objects::nonNull)
                .distinct()
                .count();

        boolean coverageStrong = coverage >= 2;

        String stitchedContext = stitchContext(chunks);
        double lexical = lexicalScoreImproved(answer, stitchedContext);

        double confidence =
                (retrievalStrong ? 0.5 : 0.0) +
                        (coverageStrong ? 0.3 : 0.0) +
                        (lexical >= MIN_LEXICAL_GROUNDING ? 0.2 : 0.0);
        log.info("""
                        RAG CONFIDENCE TRACE
                        --------------------
                        topScore        = {}
                        chunksUsed      = {}
                        coverage        = {}
                        retrievalStrong = {}
                        coverageStrong  = {}
                        lexicalScore    = {}
                        confidence      = {}
                        """,
                topScore,
                chunks.size(),
                coverage,
                retrievalStrong,
                coverageStrong,
                lexical,
                confidence
        );

        return new RagAnswer(
                answer,
                confidence,
                confidence >= BLOCK_THRESHOLD,
                chunks.size()
        );
    }

    public static String normalizeQuestion(String q) {
        return q == null ? "" :
                q.toLowerCase()
                        .replaceAll("[^a-z0-9 ]", "")
                        .replaceAll("\\s+", " ")
                        .replaceAll("[?!.]+$", "")
                        .trim();
    }

    private static final Map<String, String> CANONICAL_TERMS = Map.of(
            "total amount due", "amount_due",
            "amount due", "amount_due",
            "balance due", "amount_due",
            "payable amount", "amount_due",
            "due amount", "amount_due"
    );

    private String normalizeForLexical(String s) {
        if (s == null) return "";

        return s.toLowerCase()
                .replaceAll("\\$", " usd ")
                .replaceAll("₹", " inr ")
                .replaceAll("(\\d+),(\\d+)", "$1$2")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String canonicalizeBillTerms(String s) {
        String normalized = s;
        for (var e : CANONICAL_TERMS.entrySet()) {
            normalized = normalized.replace(e.getKey(), e.getValue());
        }
        return normalized;
    }

    private List<String> tokenizeForLexical(String s) {
        return Arrays.stream(s.split("[^a-z0-9_.]+"))
                .filter(t -> t.length() > 2)
                .toList();
    }

    private double lexicalScoreImproved(String answer, String context) {
        if (answer == null || context == null) {
            return 0.0;
        }

        String normAnswer = canonicalizeBillTerms(normalizeForLexical(answer));
        String normContext = canonicalizeBillTerms(normalizeForLexical(context));

        List<String> answerTokens = tokenizeForLexical(normAnswer);
        List<String> contextTokens = tokenizeForLexical(normContext);

        if (answerTokens.isEmpty() || contextTokens.isEmpty()) {
            return 0.0;
        }

        long matches = answerTokens.stream()
                .filter(contextTokens::contains)
                .count();

        long numericMatches = answerTokens.stream()
                .filter(t -> t.matches("\\d+(\\.\\d+)?"))
                .filter(contextTokens::contains)
                .count();

        double base = (double) matches / answerTokens.size();

        // 🔐 SAFE NUMERIC BOOST:
        // If the numeric value exists verbatim in retrieved context,
        // treat this as sufficient lexical grounding.
        if (numericMatches > 0) {
            return Math.max(base, MIN_LEXICAL_GROUNDING);
        }

        log.debug("Lexical grounding: base={}, numericMatch={}", base, numericMatches);
        return base;
    }
}
