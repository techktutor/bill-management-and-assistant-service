package com.wells.bill.assistant.service;

import com.wells.bill.assistant.builder.FilterExpressionBuilder;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagEngineServiceTest {

    private VectorStore vectorStore;

    private RagEngineService service;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        ChatClient chatClient = mock(ChatClient.class);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Mocked answer");

        service = new RagEngineService(
                vectorStore,
                chatClient,
                meterRegistry
        );
    }

    // =========================================================
    // BILL-SCOPED RAG BEHAVIOR
    // =========================================================

    @Test
    void answerBillQuestion_executesRag_andReturnsAnswer() {

        Document d = Document.builder()
                .text("Due date is tomorrow.")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(d));

        RagEngineService.RagAnswer answer =
                service.answerBillQuestion(
                        "bill-123",
                        "What is the due date?"
                );

        assertNotNull(answer);
        assertEquals("I don’t have enough reliable information from the retrieved bills to answer that.", answer.answer());
        assertFalse(answer.grounded());
        assertFalse(answer.confidence() >= 0.45);

        // verify vector search happened
        verify(vectorStore, atLeastOnce())
                .similaritySearch(any(SearchRequest.class));

    }

    @Test
    void answerBillQuestion_returnsFallback_whenNoDocs() {

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of());

        RagEngineService.RagAnswer answer =
                service.answerBillQuestion(
                        "bill-123",
                        "What is the due date?"
                );

        assertFalse(answer.grounded());
        assertTrue(answer.answer().contains("don’t have enough information"));
    }

    @Test
    void answerBillQuestion_usesBillScopedFilter() {

        Document d = Document.builder()
                .text("Amount is $100")
                .metadata("chunk_index", 0)
                .score(0.8)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(d));

        service.answerBillQuestion(
                "bill-XYZ",
                "What is the amount?"
        );

        ArgumentCaptor<SearchRequest> captor =
                ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(captor.capture());

        String filter = String.valueOf(
                captor.getValue().getFilterExpression()
        );

        assertTrue(filter.contains("parent_document_id"));
        assertTrue(filter.contains("bill-XYZ"));
    }

    // =========================================================
    // FILTER DSL — KEEP THESE TESTS
    // =========================================================

    @Test
    void filterBuilder_or_and_comparators_exact_string() {
        String expr = FilterExpressionBuilder.start()
                .or(
                        FilterExpressionBuilder.start().eq("vendor", "ACME"),
                        FilterExpressionBuilder.start().eq("vendor", "OTHER")
                )
                .and(
                        FilterExpressionBuilder.start().gte("amount", 100),
                        FilterExpressionBuilder.start().lte("amount", 500)
                )
                .build();

        assertEquals(
                "(vendor == 'ACME' || vendor == 'OTHER') && (amount >= 100 && amount <= 500)",
                expr
        );
    }

    @Test
    void filterBuilder_in_and_eq_variants_exact_string() {
        String expr = FilterExpressionBuilder
                .start()
                .in("vendor", List.of("ACME", "OTHER"))
                .and(
                        FilterExpressionBuilder.start()
                                .eq("status", "PENDING")
                )
                .build();

        assertEquals(
                "vendor in ['ACME', 'OTHER'] && (status == 'PENDING')",
                expr
        );
    }

    @Test
    void regression_no_jammed_tokens_or_missing_operators() {
        String expr = FilterExpressionBuilder
                .start()
                .eq("vendor", "ACME")
                .build();

        assertTrue(expr.contains("vendor == 'ACME'"));
        assertFalse(expr.contains("vendor="));
        assertFalse(expr.contains("vendor'"));
    }
}
