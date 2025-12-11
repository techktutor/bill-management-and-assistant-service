package com.wells.bill.assistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RagEngineServiceTest {

    private VectorStore vectorStore;
    private RagEngineService service;
    private ChatClient.CallResponseSpec mockCallResponse;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        ChatClient chatClient = mock(ChatClient.class);

        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        mockCallResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.system(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallResponse);
        when(mockCallResponse.content()).thenReturn("Mocked answer");

        service = new RagEngineService(vectorStore, chatClient);
    }

    @Test
    void retrieveByBillId_buildsProperFilter() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.retrieveByBillId("bill-123", 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        String filter = String.valueOf(captor.getValue().getFilterExpression());
        assertNotNull(filter);
        assertTrue(filter.contains("parent_document_id"));
        assertTrue(filter.contains("bill-123"));
    }

    @Test
    void retrieveByVendor_buildsProperFilter() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.retrieveByVendor("ACME", 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        String filter = String.valueOf(captor.getValue().getFilterExpression());
        assertNotNull(filter);
        assertTrue(filter.contains("vendor"));
        assertTrue(filter.contains("EQ"));
        assertTrue(filter.contains("ACME"));
    }

    @Test
    void retrieveByMetadata_acceptsRawFilter() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // Raw filter in CORRECT Spring AI DSL
        Map<String, Object> input = Map.of("rawFilter", "(vendor == 'ACME' || vendor == 'ACME2')");

        service.retrieveByMetadata(input, 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        String filter = String.valueOf(captor.getValue().getFilterExpression());
        assertNotNull(filter);
        assertTrue(filter.contains("vendor"));
        assertTrue(filter.contains("Value[value=ACME]"));
        assertTrue(filter.contains("OR"));
    }

    @Test
    void hybridRetrieve_buildsInExpressionAndRanks() {
        Document d1 = Document.builder()
                .text("Invoice from ACME")
                .metadata("vendor", "ACME")
                .score(0.9)
                .build();

        Document d2 = Document.builder()
                .text("Other vendor note")
                .metadata("vendor", "OTHER")
                .score(0.5)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d1, d2));

        Map<String, Object> filter = Map.of("vendor", List.of("ACME", "OTHER"));

        List<Document> out = service.hybridRetrieve("invoice", filter, 1);

        assertFalse(out.isEmpty());
        assertEquals("Invoice from ACME", out.getFirst().getText());

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        String filterExpr = String.valueOf(captor.getValue().getFilterExpression());
        assertNotNull(filterExpr);
        assertTrue(filterExpr.contains("vendor"));
        assertTrue(filterExpr.contains("type=IN"));
        assertTrue(filterExpr.contains("ACME"));
        assertTrue(filterExpr.contains("OTHER"));
    }

    @Test
    void answerQuestionForBill_usesChatClient_and_buildsCorrectFilter() {
        Document d = Document.builder()
                .text("Due date is tomorrow.")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d));
        when(mockCallResponse.content()).thenReturn("Answer");

        String out = service.answerQuestionForBill("bill1", "What is due?");

        assertEquals("Answer", out);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, atLeastOnce()).similaritySearch(captor.capture());

        String filterExpr = String.valueOf(captor.getValue().getFilterExpression());
        assertNotNull(filterExpr);
        assertTrue(filterExpr.contains("parent_document_id"));
        assertTrue(filterExpr.contains("EQ"));
        assertTrue(filterExpr.contains("bill1"));
    }

    // Keep the builder exact-format tests separate and targeted at the DSL

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

        assertEquals("(vendor == 'ACME' || vendor == 'OTHER') && (amount >= 100 && amount <= 500)", expr);
    }

    @Test
    void filterBuilder_in_and_eq_variants_exact_string() {
        String expr = FilterExpressionBuilder
                .start()
                .in("vendor", List.of("ACME", "OTHER"))
                .and(FilterExpressionBuilder
                        .start()
                        .eq("status", "PENDING"))
                .build();

        assertEquals("vendor in ['ACME', 'OTHER'] && (status == 'PENDING')", expr);
    }

    @Test
    void stitchContext_truncates_and_orders() {
        Document d1 = Document.builder().text("A".repeat(100)).metadata("chunk_index", 0).build();
        Document d2 = Document.builder().text("B".repeat(9000)).metadata("chunk_index", 1).build();

        String out = service.stitchContext(List.of(d1, d2), 200);
        assertTrue(out.length() <= 205);
        assertTrue(out.contains("A"));

        Document x1 = Document.builder().text("first").metadata("chunk_index", 2).build();
        Document x2 = Document.builder().text("second").metadata("chunk_index", 1).build();
        String out2 = service.stitchContext(List.of(x1, x2), 5000);
        assertTrue(out2.indexOf("second") < out2.indexOf("first"));
    }

    @Test
    void regression_no_operator_missing_or_jammed_tokens() {
        String e1 = FilterExpressionBuilder.start().eq("a", "x").build();
        String e2 = FilterExpressionBuilder.start().in("b", List.of("x", "y")).build();
        String expr = FilterExpressionBuilder.start().eq("vendor", "ACME").build();

        assertTrue(e1.contains("a == 'x'"), e1);
        assertTrue(e2.startsWith("b in ["));
        assertTrue(e2.contains(", "));
        assertFalse(expr.contains("vendor'"));
        assertFalse(expr.contains("vendor="));
        assertTrue(expr.contains("vendor == 'ACME'"));
    }
}
