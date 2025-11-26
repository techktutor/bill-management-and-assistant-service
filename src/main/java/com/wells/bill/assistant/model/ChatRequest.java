package com.wells.bill.assistant.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "conversationId is required and cannot be blank")
    private String conversationId;

    @NotBlank(message = "message is required and cannot be blank")
    private String message;
}