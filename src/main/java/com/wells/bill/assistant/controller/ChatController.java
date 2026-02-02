// ============================
// Optimized ChatController
// ============================
package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.ChatRequest;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.service.OrchestratorService;
import com.wells.bill.assistant.store.ContextStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.wells.bill.assistant.util.CookieGenerator.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/process")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final OrchestratorService orchestrator;
    private final ContextStore contextStore;

    @PostMapping
    public ResponseEntity<String> chat(
            @RequestBody String userMessage,
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

        log.info("Received chat request from User= {}, conversationId= {}", context.userId(), context.conversationId());

        if (context.conversationId() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("ConversationId cannot be empty");
        }

        if (context.userId() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("UserId cannot be empty");
        }

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("Message cannot be empty");
        }

        ChatRequest request = new ChatRequest(
                context.conversationId(),
                userMessage.trim(),
                context.userId()
        );

        // 3️⃣ Call Orchestrator Service
        String reply = orchestrator.processMessage(request);
        return ResponseEntity.ok(reply);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "Chat service is healthy"));
    }
}
