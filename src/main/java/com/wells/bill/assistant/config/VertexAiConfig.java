package com.wells.bill.assistant.config;

import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexAiConfig {

    private final VertexAiProperties properties;

    public VertexAiConfig(VertexAiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public VertexAiEmbeddingConnectionDetails vertexAiEmbeddingConnectionDetails() {
        return VertexAiEmbeddingConnectionDetails.builder()
                .projectId(properties.getProjectId())
                .location(properties.getLocation()).build();
    }
}
