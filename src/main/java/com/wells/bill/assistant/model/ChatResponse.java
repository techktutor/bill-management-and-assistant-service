package com.wells.bill.assistant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String conversationId;
    private String reply;
    private boolean success;
}