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

    /**
     * Global RetrievalAugmentationAdvisor used by RagQueryService.runRagQuery().
     * <p>
     * It will:
     * - augment the user's query with retrieved context ("ContextualQueryAugmenter")
     * - use a bill-focused answer template for the final response
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(vectorStoreDocumentRetriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .promptTemplate(PromptTemplate.builder()
                                .template("""
                                        You are an AI assistant helping a user manage and understand their bills.
                                        
                                        Use ONLY the context provided below to answer the question.
                                        If the context is missing something, say so clearly.
                                        
                                        Question:
                                        {query}
                                        
                                        Context:
                                        {context}
                                        
                                        Answer (be concise, structured, and mention specific bill details when relevant):
                                        """)
                                .build())
                        .allowEmptyContext(true)
                        .build())
                .build();
    }
}
