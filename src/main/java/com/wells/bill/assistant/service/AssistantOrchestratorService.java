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

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            - Provide helpful suggestions and guidance for bill management
            
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

    private final RagEngineService ragEngineService;
    private final RetrievalAugmentationAdvisor ragAdvisor;

    private final PaymentConfirmationGuard paymentConfirmationGuard;

    public String processMessage(String conversationId, String userMessage) {
        try {
            log.info("Processing message: conversationId={} message={}", conversationId, userMessage);

            var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(conversationId)
                    .build();

            // ------------------------------------------------------------
            // 1️⃣ Payment confirmation & intent guard (deterministic)
            // ------------------------------------------------------------
            PaymentConfirmationGuard.GuardResult guard = paymentConfirmationGuard.evaluate(conversationId, userMessage);

            if (guard.userMessage() != null) {
                return guard.userMessage();
            }

            // ------------------------------------------------------------
            // 2️⃣ Bill-scoped RAG (authoritative, confidence-aware)
            // ------------------------------------------------------------
            if (!guard.paymentIntent()) {
                UUID billId = extractBillId(userMessage);
                if (billId != null) {
                    RagEngineService.RagAnswer ragAnswer = ragEngineService.answerBillQuestion(billId.toString(), userMessage);

                    // 🔒 Hard block — unsafe or ungrounded
                    if (!ragAnswer.grounded()) {
                        log.warn("RAG answer blocked (low confidence={} billId={})", ragAnswer.confidence(), billId);
                        return ragAnswer.answer();
                    }

                    // ⚠️ Soft warning — allow but mark uncertainty
                    if (ragAnswer.confidence() < 0.65) {
                        log.info("RAG answer warning (confidence={} billId={})", ragAnswer.confidence(), billId);
                        return ragAnswer.answer();
                    }

                    // ✅ High confidence — return directly
                    return ragAnswer.answer();
                }
            }

            boolean allowPaymentTools = guard.paymentIntent() && guard.confirmed();

            // ------------------------------------------------------------
            // 3️⃣ Tool gating
            // ------------------------------------------------------------
            Object[] tools = allowPaymentTools
                    ? new Object[]{paymentAssistantTool, billAssistantTool}
                    : new Object[]{billAssistantTool};

            // ------------------------------------------------------------
            // 4️⃣ LLM invocation (fallback / general queries)
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

    // ------------------------------------------------------------
    // Deterministic billId extraction
    // ------------------------------------------------------------
    private UUID extractBillId(String msg) {
        if (msg == null) return null;

        Pattern p = Pattern.compile(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{12}"
        );
        Matcher m = p.matcher(msg);
        return m.find() ? UUID.fromString(m.group()) : null;
    }
}
