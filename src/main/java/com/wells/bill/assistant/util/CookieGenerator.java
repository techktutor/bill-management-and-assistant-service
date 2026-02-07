package com.wells.bill.assistant.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.UUID;

public final class CookieGenerator {

    public static final String CONTEXT_COOKIE = "CTX_ID";
    public static final String USER_COOKIE = "USER_ID";

    private CookieGenerator() {}

    public static UUID getOrCreateUserId(String raw, HttpServletResponse response) {
        UUID userId = parseUuid(raw);
        if (userId == null) {
            userId = UUID.randomUUID();
            addCookie(response, USER_COOKIE, userId.toString(), Duration.ofDays(90));
        }
        return userId;
    }

    public static UUID getOrCreateContextId(String raw, HttpServletResponse response) {
        UUID contextId = parseUuid(raw);
        if (contextId == null) {
            contextId = UUID.randomUUID();
            addCookie(response, CONTEXT_COOKIE, contextId.toString(), Duration.ofMinutes(30));
        }
        return contextId;
    }

    private static void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ✅ Delete cookie (force browser removal)
    public static void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static UUID parseUuid(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
