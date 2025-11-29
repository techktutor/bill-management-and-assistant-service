package com.wells.bill.assistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagEngineServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient chatClient;

    private RagEngineService ragEngineService;

    @BeforeEach
    void setup() {
        ragEngineService = new RagEngineService(vectorStore, chatClient);
    }

    // -------------------------------------------------------------
    // TEST: hybridRetrieve
    // -------------------------------------------------------------
    @Test
    void testHybridRetrieve_basic() {
        // Prepare Documents and put _score into metadata (Spring AI stores score in metadata)
        Document d1 = new Document("This is a bill related to electricity usage");
        d1.getMetadata().put("_score", 0.8);

        Document d2 = new Document("Random text unrelated");
        d2.getMetadata().put("_score", 0.2);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d1, d2));

        List<Document> results = ragEngineService.hybridRetrieve("electricity", Map.of("parent_document_id", "123"), 5);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(d1, results.getFirst()); // higher score + lexical boost should rank d1 first
    }

    // -------------------------------------------------------------
    // TEST: stitchContext
    // -------------------------------------------------------------
    @Test
    void testStitchContext_orderingAndLimit() {
        Document d1 = new Document("chunk A");
        d1.getMetadata().put("chunk_index", 0);

        Document d2 = new Document("chunk B");
        d2.getMetadata().put("chunk_index", 1);

        String stitched = ragEngineService.stitchContext(List.of(d2, d1), 200);

        assertTrue(stitched.indexOf("chunk A") < stitched.indexOf("chunk B"));
        assertTrue(stitched.contains("---")); // separator present
    }

    @Test
    void testStitchContext_respectsMaxLength() {
        Document d1 = new Document("A".repeat(5000));
        d1.getMetadata().put("chunk_index", 0);

        Document d2 = new Document("B".repeat(5000));
        d2.getMetadata().put("chunk_index", 1);

        String stitched = ragEngineService.stitchContext(List.of(d1, d2), 6000);

        assertTrue(stitched.length() <= 6000);
    }

    // -------------------------------------------------------------
    // TEST: answerQuestionForBill
    // -------------------------------------------------------------
    @Test
    void testAnswerQuestionForBill_successful() {
        // Mock vector store returning a single chunk
        Document d1 = new Document("Bill amount is 200 USD\nDue date is Jan 5");
        d1.getMetadata().put("chunk_index", 0);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d1));

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn("Answer: Due date is Jan 5");

        String answer = ragEngineService.answerQuestionForBill("123", "When is it due?");

        assertNotNull(answer);
        assertTrue(answer.contains("Jan 5"));
    }

    @Test
    void testAnswerQuestionForBill_noChunks() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String answer = ragEngineService.answerQuestionForBill("123", "When is it due?");
        assertEquals("I don't have enough information from the retrieved bills.", answer);
    }

    @Test
    void testAnswerQuestionForBill_emptyInputs() {
        assertEquals("Bill ID is required.", ragEngineService.answerQuestionForBill("", "q"));
        assertEquals("Question is required.", ragEngineService.answerQuestionForBill("123", ""));
    }

    @Test
    void testHybridRetrieve_verifiesSearchRequest() {
        Document d = new Document("dummy");
        d.getMetadata().put("_score", 0.5);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d));

        ragEngineService.hybridRetrieve("query text", Map.of("parent_document_id", "42"), 3);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest used = captor.getValue();
        assertNotNull(used);
    }
}
