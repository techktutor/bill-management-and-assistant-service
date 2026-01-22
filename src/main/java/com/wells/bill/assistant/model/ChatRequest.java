package com.wells.bill.assistant.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "ConversationId is required and cannot be blank")
    private UUID conversationId;

    @NotBlank(message = "Message is required and cannot be blank")
    private String userMessage;

    @NotBlank(message = "Message is required and cannot be blank")
    private UUID userId;

    public ChatRequest(UUID conversationId, String userMessage) {
        this.conversationId = conversationId;
        this.userMessage = userMessage;
    }
}