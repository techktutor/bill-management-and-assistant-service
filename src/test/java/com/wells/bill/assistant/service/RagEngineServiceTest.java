package com.wells.bill.assistant.service;

import com.wells.bill.assistant.builder.FilterExpressionBuilder;
import com.wells.bill.assistant.model.RagAnswer;
import com.wells.bill.assistant.store.RagAnswerCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RagEngineServiceTest {

    private VectorStore vectorStore;
    private RagEngineService service;
    private RagAnswerCache ragAnswerCache;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        ChatClient chatClient = mock(ChatClient.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ragAnswerCache = mock(RagAnswerCache.class);

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Mocked answer");

        service = new RagEngineService(vectorStore, chatClient, meterRegistry, ragAnswerCache);
    }

    // =========================================================
    // BILL-SCOPED RAG BEHAVIOR
    // =========================================================

    @Test
    void answerBillQuestion_executesRag_andReturnsBlockedAnswer_whenConfidenceLow() {
        Document d = Document.builder()
                .text("Due date is tomorrow.")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d));

        RagAnswer answer = service.answerBillQuestion("conversation-4", "bill-123", "What is the due date?");

        assertNotNull(answer);
        assertFalse(answer.grounded());
        assertTrue(answer.confidence() < 0.45);
        assertTrue(answer.answer().contains("don’t have enough"));

        verify(vectorStore, atLeastOnce()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void answerBillQuestion_returnsBlocked_whenNoDocuments() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        RagAnswer answer = service.answerBillQuestion("conversation-5", "bill-123", "What is the due date?");

        assertFalse(answer.grounded());
        assertEquals(0.0, answer.confidence());
        assertTrue(answer.answer().contains("don’t have enough"));
    }

    @Test
    void answerBillQuestion_usesBillScopedFilter() {
        Document d = Document.builder()
                .text("Amount is $100")
                .metadata("chunk_index", 0)
                .score(0.8)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d));

        service.answerBillQuestion("conversation-6", "bill-XYZ", "What is the amount?");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(captor.capture());

        String filter = String.valueOf(captor.getValue().getFilterExpression());

        assertTrue(filter.contains("parent_document_id"));
        assertTrue(filter.contains("bill-XYZ"));
    }

    // =========================================================
    // FILTER DSL — KEEP THESE TESTS
    // =========================================================
    @Test
    void filterBuilder_or_and_comparators_exact_string() {
        String expr = FilterExpressionBuilder.start().or(FilterExpressionBuilder.start().eq("vendor", "ACME"),
                FilterExpressionBuilder.start().eq("vendor", "OTHER")).and(FilterExpressionBuilder.start().gte("amount", 100),
                FilterExpressionBuilder.start().lte("amount", 500)).build();

        assertEquals("(vendor == 'ACME' || vendor == 'OTHER') && (amount >= 100 && amount <= 500)", expr);
    }

    @Test
    void filterBuilder_in_and_eq_variants_exact_string() {
        String expr = FilterExpressionBuilder.start().in("vendor", List.of("ACME", "OTHER"))
                .and(FilterExpressionBuilder.start().eq("status", "PENDING")).build();

        assertEquals("vendor in ['ACME', 'OTHER'] && (status == 'PENDING')", expr);
    }

    @Test
    void regression_no_jammed_tokens_or_missing_operators() {
        String expr = FilterExpressionBuilder.start().eq("vendor", "ACME").build();

        assertTrue(expr.contains("vendor == 'ACME'"));
        assertFalse(expr.contains("vendor="));
        assertFalse(expr.contains("vendor'"));
    }

    @Test
    void answerBillQuestion_usesCache_andSkipsRetrieval() {
        RagAnswer cached =
                new RagAnswer(
                        "Cached answer",
                        0.9,
                        true,
                        3
                );

        when(ragAnswerCache.get(
                anyString(),
                eq("bill-123"),
                anyString()
        )).thenReturn(java.util.Optional.of(cached));

        RagAnswer result =
                service.answerBillQuestion(
                        "conversation-1",
                        "bill-123",
                        "What is the due date?"
                );

        assertEquals(cached, result);

        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
        verify(ragAnswerCache).get(anyString(), eq("bill-123"), anyString());
    }

    @Test
    void answerBillQuestion_returnsBlockedAnswer_whenConfidenceLow() {
        Document d1 = Document.builder()
                .text("Invoice created on March 10.")
                .metadata("chunk_index", 0)
                .score(0.4) // ❌ low similarity
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(d1));

        when(ragAnswerCache.get(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        RagAnswer answer = service.answerBillQuestion(
                "conversation-1",
                "bill-111",
                "When should I pay?"
        );

        assertNotNull(answer);
        assertFalse(answer.grounded());
        assertEquals(0.0, answer.confidence());
        assertTrue(answer.answer().contains("don’t have enough reliable information"));

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void answerBillQuestion_returnsWarnedAnswer_whenConfidenceMedium() {
        Document d1 = Document.builder()
                .text("Invoice generated on March 10.")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        Document d2 = Document.builder()
                .text("Payment instructions are provided on the invoice.")
                .metadata("chunk_index", 1)
                .score(0.8)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(d1, d2));

        when(ragAnswerCache.get(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        RagAnswer answer = service.answerBillQuestion(
                "conversation-2",
                "bill-456",
                "When should I pay?"
        );

        assertNotNull(answer);
        assertTrue(answer.grounded()); // ✅ WARNED is grounded
        assertTrue(answer.confidence() >= 0.45);
        assertFalse(answer.confidence() < 0.65);
        assertFalse(answer.answer().contains("⚠️"));

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void answerBillQuestion_returnsWarnedAnswer_whenConfidenceMedium1() {
        Document d1 = Document.builder()
                .text("The due date is March 10.")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        Document d2 = Document.builder()
                .text("Payment must be completed before the due date.")
                .metadata("chunk_index", 0) // 👈 SAME index → coverageStrong = false
                .score(0.8)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(d1, d2));

        when(ragAnswerCache.get(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        RagAnswer answer =
                service.answerBillQuestion(
                        "conversation-2",
                        "bill-456",
                        "When should I pay?"
                );

        assertNotNull(answer);
        assertTrue(answer.grounded()); // ✅ now true
        assertTrue(answer.confidence() >= 0.45);
        assertTrue(answer.confidence() < 0.65);
        assertTrue(answer.answer().contains("⚠️"));

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void answerBillQuestion_returnsAcceptedAnswer_whenConfidenceHigh() {
        Document d1 = Document.builder()
                .text("The due date is March 10.")
                .metadata("chunk_index", 0)
                .score(0.85)
                .build();

        Document d2 = Document.builder()
                .text("Payment must be completed before the due date.")
                .metadata("chunk_index", 1)
                .score(0.8)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(d1, d2));

        when(ragAnswerCache.get(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        RagAnswer answer = service.answerBillQuestion(
                "conversation-3",
                "bill-789",
                "When is the due date?"
        );

        assertNotNull(answer);
        assertTrue(answer.grounded());
        assertTrue(answer.confidence() >= 0.65);
        assertFalse(answer.answer().contains("⚠️"));

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void answerBillQuestion_returnsAcceptedAnswer_whenConfidenceHigh1() {
        Document d1 = Document.builder()
                .text("The due date is April 5.")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        Document d2 = Document.builder()
                .text("The total amount due is $120.")
                .metadata("chunk_index", 1)
                .score(0.88)
                .build();

        Document d3 = Document.builder()
                .text("Payment should be made before April 5.")
                .metadata("chunk_index", 2)
                .score(0.85)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d1, d2, d3));
        when(ragAnswerCache.get(anyString(), anyString(), anyString())).thenReturn(Optional.empty());

        RagAnswer answer =
                service.answerBillQuestion(
                        "conversation-3",
                        "bill-789",
                        "What is the due date and amount?"
                );

        assertNotNull(answer);
        assertTrue(answer.grounded());
        assertTrue(answer.confidence() >= 0.65);
        assertFalse(answer.answer().contains("⚠️"));

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
}
