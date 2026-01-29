package com.wells.bill.assistant.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.UUID;

public final class CookieGenerator {

    public static final String CONTEXT_COOKIE = "CTX_ID";

    public static String getContextKey(String contextKey, HttpServletResponse response) {
        if (contextKey == null) {
            contextKey = UUID.randomUUID().toString();

            /*Cookie cookie = new Cookie(CONTEXT_COOKIE, contextKey);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);*/

            // ✅ Production-Ready Cookie Code
            addContextCookie(response, contextKey);
        }
        return contextKey;
    }

    // ✅ Production-Ready Cookie Code
    private static void addContextCookie(HttpServletResponse response, String contextKey) {

        boolean isProd = false; // TODO: drive from env / profile

        ResponseCookie cookie = ResponseCookie.from(CONTEXT_COOKIE, contextKey)
                .httpOnly(true)
                .secure(isProd) // true only in HTTPS
                .sameSite(isProd ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
