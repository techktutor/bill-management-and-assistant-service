package com.wells.bill.assistant.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;

import java.time.Duration;
import java.util.UUID;

public final class CookieGenerator {

    public static final String CONTEXT_COOKIE = "CTX_ID";
    public static final String USER_COOKIE = "USER_ID";

    public static String getOrCreateUserId(
            @CookieValue(value = USER_COOKIE, required = false) String userId,
            HttpServletResponse response
    ) {
        if (userId == null) {
            userId = UUID.randomUUID().toString();

            ResponseCookie cookie = ResponseCookie.from(USER_COOKIE, userId)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(90))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return userId;
    }

    public static String getContextKey(
            String contextKey,
            HttpServletResponse response
    ) {
        if (contextKey == null) {
            contextKey = UUID.randomUUID().toString();

            ResponseCookie cookie = ResponseCookie.from(CONTEXT_COOKIE, contextKey)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofMinutes(30))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return contextKey;
    }
}
