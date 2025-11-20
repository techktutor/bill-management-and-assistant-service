package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssistantOrchestratorService {

    private final ChatClient chatClient;
    private final RagQueryService ragQueryService;
    private final MessageWindowChatMemory chatMemory;
    private final PaymentsToolAdapter paymentsToolAdapter;
    private final BillAnalysisService billAnalysisService;
    private final RetrievalAugmentationAdvisor ragAdvisor;

    public String processMessage(String conversationId, String userMessage) {
        // 1) add user message to memory
        chatMemory.add(conversationId, new UserMessage(userMessage));
        List<Message> history = chatMemory.get(conversationId);

        // 2) Retrieve relevant chunks (this is where RagQueryService is ACTUALLY used)
        String retrievedContext = ragQueryService.buildContextString(userMessage);

        // 3) Build final prompt
        Prompt prompt = getPrompt(userMessage, history, retrievedContext);

        // 4) Call model with RAG advisor + tools
        ChatResponse response = chatClient
                .prompt(prompt)
                .advisors(ragAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .tools(paymentsToolAdapter, billAnalysisService)
                .call()
                .chatResponse();

        // 5) Extract generation & assistant message
        assert response != null;
        Generation gen = response.getResults().getFirst();
        AssistantMessage assistantMessage = gen.getOutput();

        // 6) First, default textual response
        String aiText = assistantMessage.getText();

        // 7) Check tool calls (AssistantMessage.ToolCall) and execute the first one if present
        List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
        if (!toolCalls.isEmpty()) {
            var call = toolCalls.getFirst();
            String toolName = call.name();
            String rawArgs = call.arguments();
            try {
                Map<String, Object> args = (Map<String, Object>) safeParseArgs(rawArgs);
                // ROUTE TOOL CALL
                aiText = switch (toolName) {
                    case "payBill", "schedulePayment" -> paymentsToolAdapter.executeTool(toolName, args);

                    case "classifyBill", "extractBillFields", "compareBills",
                         "detectAnomaly", "trendAnalysis",
                         "summarizeBill", "billSuggestion" -> billAnalysisService.executeAnalysisTool(toolName, args);

                    default -> "Unknown tool: " + toolName;
                };
            } catch (Exception e) {
                // Try a forgiving parser (e.g., clean JSON-ish string) before failing
                aiText = "Error parsing tool arguments: " + e.getMessage();
            }
        }
        // 7) Save memory
        chatMemory.add(conversationId, assistantMessage);
        return aiText;
    }

    private static Prompt getPrompt(String userMessage, List<Message> history, String retrievedContext) {
        String finalPromptText = """
                You are an AI Bill Advisor. Use retrieved billing data to answer questions.
                
                ===================
                Conversation History:
                %s
                ===================
                
                Retrieved Billing Data:
                %s
                ===================
                
                User Query:
                %s
                
                If retrieved data does not contain the answer, say:
                "I do not have enough info from your bill data."
                """.formatted(history.stream().map(Message::getText).reduce("", (a,b)->a+"\n"+b),
                retrievedContext, userMessage);

        return new Prompt(new UserMessage(finalPromptText));
    }

    private Map<?, ?> safeParseArgs(String rawArgs) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (rawArgs == null || rawArgs.isBlank()) {
            return Map.of();
        }
        try {
            // Try direct JSON
            return objectMapper.readValue(rawArgs, Map.class);
        } catch (Exception e1) {
            try {
                // Try to unescape and parse again
                String cleaned = rawArgs.replace("\\", "");
                return objectMapper.readValue(cleaned, Map.class);
            } catch (Exception e2) {
                try {
                    // Fix missing quotes around field names
                    String fixed = rawArgs
                            .replace("=", ":")
                            .replaceAll("([a-zA-Z0-9_]+):", "\"$1\":");
                    return objectMapper.readValue(fixed, Map.class);
                } catch (Exception e3) {
                    return Map.of("raw", rawArgs); // fallback
                }
            }
        }
    }
}
