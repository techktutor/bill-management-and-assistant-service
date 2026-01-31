package com.wells.bill.assistant.service;

import com.wells.bill.assistant.model.ChatRequest;
import com.wells.bill.assistant.tools.BillAssistantTool;
import com.wells.bill.assistant.tools.PaymentAssistantTool;
import com.wells.bill.assistant.util.ConversationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import static com.wells.bill.assistant.util.CustomPromptTemple.systemPrompt;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private static final String DEFAULT_RESPONSE = "I’m sorry, I couldn’t process your request safely. Please try again.";

    private final ChatClient chatClient;
    private final BillAssistantTool billAssistantTool;
    private final PaymentAssistantTool paymentAssistantTool;

    public String processMessage(ChatRequest request) {
        String conversationId = String.valueOf(request.getConversationId());
        String userId = String.valueOf(request.getUserId());
        String userMessage = request.getUserMessage();
        try {
            log.info("Processing request for conversationId= {}, message= {}", conversationId, userMessage);

            ConversationContextHolder.set(
                    request.getUserId(),
                    request.getConversationId());

            Object[] tools = new Object[]{billAssistantTool, paymentAssistantTool};

            String response = chatClient
                    .prompt()
                    .system(system -> system
                            .text(systemPrompt(userId))
                    )
                    .user(userMessage)
                    .tools(tools)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
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
        } finally {
            // 🧹 ALWAYS clear to avoid leaks
            ConversationContextHolder.clear();
        }
    }
}
