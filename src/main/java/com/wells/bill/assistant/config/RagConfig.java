package com.wells.bill.assistant.config;

import com.wells.bill.assistant.util.CustomPromptTemple;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    /**
     * Global RetrievalAugmentationAdvisor used by RagQueryService.runRagQuery().
     * <p>
     * It will:
     * - augment the user's query with retrieved context ("ContextualQueryAugmenter")
     * - use a bill-focused answer template for the final response
     */
    //@Bean("ragAdvisor")
    public Advisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .build())
                .build();
    }
}
