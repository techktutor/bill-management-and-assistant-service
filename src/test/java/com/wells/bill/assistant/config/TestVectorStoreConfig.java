package com.wells.bill.assistant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@TestConfiguration
public class TestVectorStoreConfig {

    // Marks this bean as the primary one when the 'test' profile is active,
    // overriding any other configuration that might try to create a VectorStore.
    @Primary
    @Bean(name = "vectorStore")
    public VectorStore vectorStore(EmbeddingModel embeddingClient) {
        // Use an in-memory SimpleVectorStore for testing with H2
        return SimpleVectorStore.builder(embeddingClient)
                .build();
    }
}