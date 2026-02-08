package com.wells.bill.assistant.service;

import com.wells.bill.assistant.exception.ContextMismatchException;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.store.ContextStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.wells.bill.assistant.util.CookieGenerator.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextFacade {

    private final ContextStore contextStore;
    private final CustomerService customerService;

    public Context resolveContext(String rawContextId, String rawUserId, HttpServletResponse response) {

        UUID userId = getOrCreateUserId(rawUserId, response);
        UUID contextId = getOrCreateContextId(rawContextId, response);

        try {
            Context context = contextStore.resolveContext(contextId, userId);
            ensureCustomer(context);

            response.setHeader("Cache-Control", "no-store");
            return context;
        } catch (ContextMismatchException ex) {

            deleteCookie(response, CONTEXT_COOKIE);

            UUID freshContextId = getOrCreateContextId(null, response);
            Context freshContext = contextStore.resolveContext(freshContextId, userId);

            ensureCustomer(freshContext);

            response.setHeader("Cache-Control", "no-store");
            return freshContext;
        }
    }

    private void ensureCustomer(Context context) {
        UUID customerId = customerService.createCustomerIfNotExists(context.userId());

        log.info("Resolved context: contextId={}, userId={}, customerId={}",
                context.contextId(),
                context.userId(),
                customerId);
    }
}

