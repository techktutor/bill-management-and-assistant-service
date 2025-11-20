package com.wells.bill.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexAiConfig {

    @Bean
    public VertexAiEmbeddingConnectionDetails vertexAiEmbeddingConnectionDetails() {
        return VertexAiEmbeddingConnectionDetails.builder()
                .projectId("gc-vertex-spring-ai-project") // Replace with your GCP project ID
                .location("us-central1") // Replace with your desired GCP region
                .build();
    }

    @Bean
    public ChatClient chatClient(VertexAiGeminiChatModel model) {
        return ChatClient.builder(model).build();
    }
}

