package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.service.AssistantOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AssistantOrchestratorService orchestrator;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String convId = body.getOrDefault("conversationId", "default");
        String message = body.get("message");
        String reply = orchestrator.processMessage(convId, message);
        return Map.of("reply", reply);
    }
}
