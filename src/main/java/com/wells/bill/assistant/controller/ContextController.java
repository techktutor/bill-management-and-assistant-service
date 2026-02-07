package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.service.ContextFacade;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.wells.bill.assistant.util.CookieGenerator.CONTEXT_COOKIE;
import static com.wells.bill.assistant.util.CookieGenerator.USER_COOKIE;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContextController {

    private final ContextFacade contextFacade;

    @GetMapping("/context")
    public Map<String, String> resolveContext(
            @CookieValue(value = CONTEXT_COOKIE, required = false) String rawContextId,
            @CookieValue(value = USER_COOKIE, required = false) String rawUserId,
            HttpServletResponse response
    ) {
        Context context = contextFacade.resolveContext(rawContextId, rawUserId, response);

        return Map.of(
                "contextId", context.contextId().toString(),
                "conversationId", context.conversationId().toString()
        );
    }
}
