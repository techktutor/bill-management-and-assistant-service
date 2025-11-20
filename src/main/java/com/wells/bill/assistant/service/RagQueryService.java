package com.wells.bill.assistant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RagQueryService:
 * - Exposes semantic retrieval from VectorStore
 * - Provides context-building helpers
 * - Provides advisor for LLM calls with automatic RAG augmentation
 * The component that takes the user’s question → finds relevant chunks in pgvector → gives them to the AI → so the AI answers using your data, not hallucinations
 * Embeddings → VectorStore → Semantic Search → LLM Answering
 */
@Service
@RequiredArgsConstructor
public class RagQueryService {

    private final VectorStoreDocumentRetriever retriever;
    private final RetrievalAugmentationAdvisor advisor;

    /**
     * Perform raw retrieval using the VectorStoreDocumentRetriever.
     * Returns top-k relevant documents.
     */
    public List<Document> retrieveDocuments(String userQuery) {
        Query query = new Query(userQuery);
        return retriever.retrieve(query);
    }

    /**
     * Build a readable context block for injection into traditional prompts
     * (if not using the advisor).
     */
    public String buildContextString(String query) {
        List<Document> docs = retrieveDocuments(query);

        if (docs.isEmpty()) {
            return "";
        }

        return docs.stream()
                .map(doc -> {
                    String src = doc.getMetadata().containsKey("source")
                            ? doc.getMetadata().get("source").toString()
                            : "unknown";

                    return """
                            Source: %s
                            %s
                            """.formatted(src, doc.getText());
                })
                .collect(Collectors.joining("\n---\n\n"));
    }

    /**
     * Convenience method:
     * Run a full RAG-augmented prompt in one line (optional).
     * This uses the ChatClient with the RetrievalAugmentationAdvisor attached.
     */
    public ChatResponse runRagQuery(ChatClient chatClient, Prompt prompt) {
        return chatClient
                .prompt(prompt)
                .advisors(advisor)
                .call()
                .chatResponse();
    }

    public List<Document> retrieveBillsForComparison(String utilityType) {
        String query = "compare " + utilityType + " bills";
        return retriever.retrieve(new Query(query));
    }

}
