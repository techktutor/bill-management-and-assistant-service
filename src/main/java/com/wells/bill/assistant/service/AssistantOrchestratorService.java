package com.wells.bill.assistant.service;

import com.wells.bill.assistant.tools.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssistantOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AssistantOrchestratorService.class);

    private static final String DEFAULT_RESPONSE = "I apologize, but I encountered an error processing your request. Please try again.";
    private static final String SYSTEM_PROMPT = """
            You are an AI Bill Advisor assistant. Your responsibilities:
            - Answer questions about user bills using retrieved billing data.
            - Help users schedule and make payments using available tools.
            - Provide helpful suggestions for bill management.
            - Understanding bill details
            - Managing payments
            - Retrieving relevant bill information
            
            Guidelines:
            - If you don't have enough billing data to answer, say: "I do not have sufficient information from your bills to answer this question."
            - Use payment tools only when explicitly requested by the user.
            - Always confirm payment actions before executing.
            - Never execute payments without explicit user confirmation.
            - Be concise, factual, and safe.
            """;

    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final BillAssistantTool billAssistantTool;
    private final PaymentAssistantTool paymentAssistantTool;
    private final RetrievalAugmentationAdvisor ragAdvisor;

    public String processMessage(String conversationId, String userMessage) {
        try {
            log.info("Processing message: conversationId={} message={}", conversationId, userMessage);

            var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(conversationId)
                    .build();

            // Call model with proper advisor chain
            String response = chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .advisors(memoryAdvisor)  // Memory advisor manages conversation history
                    .advisors(ragAdvisor)  // RAG advisor retrieves and augments context
                    .tools(paymentAssistantTool, billAssistantTool)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("Empty response from LLM for conversationId={}", conversationId);
                return DEFAULT_RESPONSE;
            }
            log.info("Message processed successfully for conversationId={}", conversationId);
            return response;
        } catch (IllegalArgumentException e) {
            log.error("Validation error in processMessage: {}", e.getMessage());
            return "Invalid input: " + e.getMessage();
        } catch (Exception e) {
            log.error("Error processing message for conversationId={}: {}", conversationId, e.getMessage(), e);
            return DEFAULT_RESPONSE;
        }
    }
}
