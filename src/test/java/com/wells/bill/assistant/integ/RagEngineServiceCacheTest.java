package com.wells.bill.assistant.integ;

import com.wells.bill.assistant.service.RagEngineService;
import com.wells.bill.assistant.store.RagAnswerCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagEngineServiceCacheTest {

    @Mock
    VectorStore vectorStore;

    @Mock
    ChatClient chatClient;

    @Mock
    RagAnswerCache ragAnswerCache;

    MeterRegistry meterRegistry;
    RagEngineService service;

    @BeforeEach
    void setup() {
        meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        service = new RagEngineService(
                vectorStore,
                chatClient,
                meterRegistry,
                ragAnswerCache
        );
    }

    @Test
    void cacheHit_shouldBypassVectorSearch() {
        String conversationId = "c1";
        String billId = "b1";
        String question = "What is the due date?";

        RagEngineService.RagAnswer cached = new RagEngineService.RagAnswer("Cached answer", 0.9, true, 3);

        when(ragAnswerCache.get(eq(conversationId), eq(billId), any()))
                .thenReturn(Optional.of(cached));

        RagEngineService.RagAnswer out = service.answerBillQuestion(conversationId, billId, question);

        assertEquals("Cached answer", out.answer());

        verifyNoInteractions(vectorStore);
        verify(ragAnswerCache, never()).put(any(), any(), any(), any(), any());
    }

    @Test
    void cacheMiss_shouldRetrieveAndStore() {
        String conversationId = "c1";
        String billId = "b1";
        String question = "What is the amount?";

        Document doc = Document.builder()
                .text("Amount due is $100")
                .metadata("chunk_index", 0)
                .score(0.9)
                .build();

        when(ragAnswerCache.get(eq(conversationId), eq(billId), any()))
                .thenReturn(Optional.empty());

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc));

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("The amount is $100");

        RagEngineService.RagAnswer out = service.answerBillQuestion(conversationId, billId, question);

        assertFalse(out.grounded());
        assertEquals(0.0, out.confidence());

        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(ragAnswerCache).put(
                eq(conversationId),
                eq(billId),
                any(),
                eq(out),
                any()
        );
    }

    @Test
    void sameQuestionDifferentConversation_shouldNotHitCache() {
        String billId = "b1";
        String question = "What is due date?";

        when(ragAnswerCache.get(any(), eq(billId), any())).thenReturn(Optional.empty());

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(
                        Document.builder()
                                .text("Due tomorrow")
                                .metadata("chunk_index", 0)
                                .score(0.9)
                                .build()
                ));

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Due tomorrow");

        service.answerBillQuestion("c1", billId, question);
        service.answerBillQuestion("c2", billId, question);

        verify(ragAnswerCache, times(2)).get(any(), eq(billId), any());
    }
}
