package com.wells.bill.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexAiConfig {

    @Bean
    public VertexAiEmbeddingConnectionDetails vertexAiEmbeddingConnectionDetails() {
        return VertexAiEmbeddingConnectionDetails.builder()
                .projectId("gc-vertex-spring-ai-project")
                .location("us-central1").build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // Builder already has:
        //  - ChatModel (Vertex AI Gemini)
        //  - Memory advisor (from chat-memory starter)
        //  - RAG advisor (from rag + advisors-vector-store starters)
        //  - Tool pipeline (from @Tool methods)
        return builder.build();
    }
}

