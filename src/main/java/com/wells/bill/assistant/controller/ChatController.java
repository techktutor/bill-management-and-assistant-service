// ============================
// Optimized ChatController
// ============================
package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.ChatRequest;
import com.wells.bill.assistant.model.ChatResponse;
import com.wells.bill.assistant.service.OrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final OrchestratorService orchestrator;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: conversationId={}", request.getConversationId());

        // Basic manual validation for nulls (since @Valid only checks annotated fields)
        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(null, "conversationId cannot be empty", false));
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(request.getConversationId(), "message cannot be empty", false));
        }

        String reply = orchestrator.processMessage(request.getConversationId(), request.getMessage());
        return ResponseEntity.ok(new ChatResponse(request.getConversationId(), reply, true));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "Chat service is healthy"));
    }
}
