package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.store.ContextStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.wells.bill.assistant.util.CookieGenerator.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContextController {

    private final ContextStore contextStore;

    @GetMapping("/context")
    public Map<String, String> resolveContext(
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        Context context = contextStore.resolveContext(contextId, userId);

        response.setHeader("Cache-Control", "no-store");

        return Map.of(
                "contextId", context.contextId().toString(),
                "conversationId", context.conversationId().toString()
        );
    }
}
