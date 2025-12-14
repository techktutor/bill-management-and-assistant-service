package com.wells.bill.assistant.service;

import com.wells.bill.assistant.tools.BillAssistantTool;
import com.wells.bill.assistant.tools.PaymentAssistantTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantOrchestratorService {

    private static final String DEFAULT_RESPONSE =
            "I apologize, but I encountered an error processing your request. Please try again.";

    private static final String SYSTEM_PROMPT = """
            You are an AI Bill Advisor assistant.
            Responsibilities:
            - Answer questions about user bills using retrieved billing data
            - Help users create and manage payment intents (NOT execute payments)
            - Provide helpful suggestions for bill management
            
            Safety rules (MANDATORY):
            - You MUST NOT execute payments
            - You MUST NOT mark bills as PAID
            - Payment execution happens only after explicit confirmation
            - Use payment tools only when user intent is clearly payment-related
            - If confirmation is missing, ask for it
            - If you do not have enough information, say so clearly
            """;

    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final BillAssistantTool billAssistantTool;
    private final PaymentAssistantTool paymentAssistantTool;
    private final RetrievalAugmentationAdvisor ragAdvisor;
    private final PaymentConfirmationGuard paymentConfirmationGuard;

    public String processMessage(String conversationId, String userMessage) {
        try {
            log.info("Processing message: conversationId={} message={}", conversationId, userMessage);

            var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(conversationId)
                    .build();

            // ------------------------------------------------------------
            // Payment confirmation & intent guard (deterministic)
            // ------------------------------------------------------------
            PaymentConfirmationGuard.GuardResult guard = paymentConfirmationGuard.evaluate(conversationId, userMessage);

            // If guard needs to respond directly (missing bill / confirmation)
            if (guard.userMessage() != null) {
                return guard.userMessage();
            }

            // ------------------------------------------------------------
            // Tool gating based on guard result
            // ------------------------------------------------------------
            Object[] tools = (guard.paymentIntent() && guard.confirmed())
                    ? new Object[]{paymentAssistantTool, billAssistantTool}
                    : new Object[]{billAssistantTool};

            // ------------------------------------------------------------
            // LLM invocation
            // ------------------------------------------------------------
            String response = chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .advisors(memoryAdvisor, ragAdvisor)
                    .tools(tools)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("Empty response from LLM for conversationId={}", conversationId);
                return DEFAULT_RESPONSE;
            }
            return response;
        } catch (Exception e) {
            log.error("Error processing message for conversationId={}", conversationId, e);
            return DEFAULT_RESPONSE;
        }
    }
}
