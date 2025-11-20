package com.wells.bill.assistant.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    /**
     * PgVectorStore is autoconfigured by Spring Boot:
     * - A PgVectorStore bean already exists
     * - No need to build one manually
     */
    @Bean
    public VectorStoreDocumentRetriever vectorStoreDocumentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(6)
                .similarityThreshold(0.0)
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(vectorStoreDocumentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .promptTemplate(PromptTemplate.builder().template("""
                                Use the following extracted document context to answer the user query.
                                
                                Query:
                                {query}
                                
                                Context:
                                {context}
                                
                                Answer:
                                """
                        ).build())
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
