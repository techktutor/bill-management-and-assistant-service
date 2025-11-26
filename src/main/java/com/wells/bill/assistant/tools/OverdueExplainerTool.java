package com.wells.bill.assistant.tools;

import com.wells.bill.assistant.service.VectorRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OverdueExplainerTool {

    private static final Logger log = LoggerFactory.getLogger(OverdueExplainerTool.class);
    private final VectorRetrievalService retrievalService;
    private final ChatClient chatClient;

    public OverdueExplainerTool(VectorRetrievalService retrievalService, ChatClient chatClient) {
        this.retrievalService = retrievalService;
        this.chatClient = chatClient;
    }

    @Tool(name = "explainWhyOverdue", description = "Explain why a bill is overdue using retrieved bill context chunks.")
    public String explainWhyOverdue(@ToolParam(description = "Bill ID") String billId) {
        try {
            List<Document> chunks = retrievalService.retrieveByBillId(billId, 10);
            if (chunks == null || chunks.isEmpty()) {
                return "I don't have enough information from the retrieved bills to explain why this bill is overdue.";
            }

            String context = chunks.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

            String prompt = "You are an assistant. Using ONLY the context below, explain why this bill might be overdue. If the context doesn't contain a clear reason, say you don't have enough information.\n\nContext:\n" + context;

            String response = chatClient
                    .prompt()
                    .system("You are an objective assistant that uses retrieved bill context to explain overdue reasons.")
                    .user(prompt)
                    .call()
                    .content();

            return response == null ? "I couldn't generate an explanation." : response;

        } catch (Exception e) {
            log.error("explainWhyOverdue error for billId={}: {}", billId, e.getMessage(), e);
            return "Failed to explain overdue reason due to an internal error.";
        }
    }
}
