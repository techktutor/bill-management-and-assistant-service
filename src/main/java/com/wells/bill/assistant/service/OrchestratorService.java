package com.wells.bill.assistant.service;

import com.wells.bill.assistant.model.*;
import com.wells.bill.assistant.store.ConversationStateStore;
import com.wells.bill.assistant.tools.BillAssistantTool;
import com.wells.bill.assistant.tools.PaymentAssistantTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wells.bill.assistant.model.ConversationState.PAYMENT_INTENT_ALLOWED;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private static final String DEFAULT_RESPONSE = "I’m sorry, I couldn’t process your request safely. Please try again.";
    private static final String SYSTEM_PROMPT = """
                    You are an AI Bill Assistant.
                    Your task is to analyze uploaded bills or invoices and return structured, accurate information.
                    If any thing else apart from bill/invoice related, politely decline saying ask any thing related to your bills only.
            """;

    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private final BillAssistantTool billAssistantTool;
    private final PaymentAssistantTool paymentAssistantTool;

    private final RagEngineService ragEngineService;
    //private final RetrievalAugmentationAdvisor ragAdvisor;

    private final IntentResolver intentResolver;
    private final ConversationStateStore stateStore;
    private final PaymentGuardService paymentGuardService;

    // ---------------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------------
    public String processMessage(ChatRequest request) {
        String conversationId = request.getConversationId();
        String userId = request.getUserId();
        String merchantId = request.getMerchantId();
        String userMessage = request.getMessage();
        try {
            log.info("Processing ChatRequest Message: conversationId= {} message= {}", conversationId, userMessage);

            // 1️⃣ Load conversation state
            ConversationContext context = stateStore.load(conversationId, userId);

            // 2️⃣ Resolve intent (deterministic)
            Intent intent = intentResolver.resolve(request, context.state());

            // 3️⃣ Evaluate payment guard (state machine authority)
            GuardResult guardResult = paymentGuardService.evaluate(context, intent);

            // 4️⃣ Apply state transition
            ConversationContext updatedContext = context.apply(guardResult);

            stateStore.save(updatedContext, Duration.ofMinutes(10));

            // 5️⃣ Block / ask-for-confirmation short-circuit
            if (!guardResult.isAllowed()) {
                log.info("Guard blocked or requires confirmation: conversationId={} message={}", conversationId, userMessage);
                return guardResult.userMessage();
            }

            // =============================================================
            // 6️⃣ Deterministic RAG (bill-scoped, advisory only)
            // =============================================================
            UUID billId = extractBillId(userMessage);
            if (billId != null && intent instanceof QueryBillsIntent) {
                log.info("Invoking Custom RAG Engine for billId= {} conversationId= {}", billId, conversationId);
                RagAnswer ragAnswer = ragEngineService.answerBillQuestion(
                        conversationId,
                        billId.toString(),
                        userMessage
                );

                if (!ragAnswer.grounded()) {
                    log.info("RAG Engine answer blocked (confidence= {} billId= {})", ragAnswer.confidence(), billId);
                    return ragAnswer.answer();
                }
                log.info("RAG Engine answer provided (confidence= {} billId= {})", ragAnswer.confidence(), billId);
                return ragAnswer.answer();
            }

            // =============================================================
            // 7️⃣ DETERMINISTIC PAYMENT TOOL EXECUTION (NO LLM)
            // =============================================================
            if (intent instanceof ConfirmPaymentIntent && updatedContext.state() == PAYMENT_INTENT_ALLOWED) {
                log.info("Executing confirmed payment intent for conversationId= {}", conversationId);

                PendingPayment pending = updatedContext.pendingPayment();
                if (pending == null) {
                    throw new IllegalStateException("Confirmed payment without PendingPayment in context");
                }
            }

            // =============================================================
            // 8️⃣ LLM FALLBACK (conversation only, no critical actions)
            // =============================================================
            Object[] tools = new Object[]{billAssistantTool, paymentAssistantTool};

            log.info("Invoking LLM fallback for conversationId= {}", conversationId);

            String response = chatClient
                    .prompt()
                    .tools(tools)
                    .user(userMessage)
                    .system(SYSTEM_PROMPT)
                    .call()
                    .content();

            log.info("LLM response for conversationId= {} is: {}", conversationId, response);

            if (response == null || response.isBlank()) {
                log.info("Empty LLM response for conversationId={}", conversationId);
                return DEFAULT_RESPONSE;
            }

            return response;
        } catch (Exception e) {
            log.error("Error processing message for conversationId={}", conversationId, e);
            return DEFAULT_RESPONSE;
        }
    }

    // ---------------------------------------------------------------------
    // Deterministic billId extraction (UUID only)
    // ---------------------------------------------------------------------
    private UUID extractBillId(String msg) {
        if (msg == null) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-" +
                        "[0-9a-fA-F]{12}"
        );

        Matcher matcher = pattern.matcher(msg);
        return matcher.find() ? UUID.fromString(matcher.group()) : null;
    }
}
