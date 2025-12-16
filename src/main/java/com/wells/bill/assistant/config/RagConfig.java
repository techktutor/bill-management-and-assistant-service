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
                .topK(10)
                .similarityThreshold(0.32)
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
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever retriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .promptTemplate(buildPromptTemplate())
                        .allowEmptyContext(false)
                        .build())
                .build();
    }

    private PromptTemplate buildPromptTemplate() {
        String template = """
                You are an AI Bill Management Assistant.
                You must answer ONLY using the retrieved bill context below.
                If the answer cannot be derived explicitly from the context, do NOT guess.
                
                Question:
                {query}
                
                Retrieved Bill Context (each chunk may include metadata like filename, chunk_index, ingested_at):
                {context}
                
                Rules:
                - Use information strictly from the retrieved context (amounts, due dates, usage, billing periods, vendor names, etc.).
                - If multiple chunks belong to the same bill, combine their meaning.
                - If the context is empty or does not contain enough information to answer, reply with:
                  "I don’t have enough information from the retrieved bills."
                - Never fabricate bill amounts, dates, or usage not present in the context.
                - Never rely on prior knowledge for bill data—always prioritize context tokens.
                - Keep answers concise, factual, and grounded strictly in the retrieved text.
                """;
        return new PromptTemplate(template);
    }
}
