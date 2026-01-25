package com.wells.bill.assistant.util;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentResolver {

    private static final Pattern BILL_ID_PATTERN = Pattern.compile("\\b(bill[-_ ]?\\d+)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(₹|rs\\.?|inr)?\\s*(\\d+(\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);

    private static final Set<String> CONFIRMATION_KEYWORDS = Set.of("yes", "confirm", "proceed", "go ahead", "ok", "okay");

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");

    public static Intent resolve(ChatRequest request, ConversationState state) {

        String message = request.getUserMessage().trim().toLowerCase();

        // 1️⃣ Confirmation intent (only when awaiting confirmation)
        if (state == ConversationState.AWAITING_CONFIRMATION && isConfirmation(message)) {
            return new ConfirmPaymentIntent(request.getConversationId());
        }

        // 3️⃣ Initiate payment intent (only from IDLE)
        if (state == ConversationState.IDLE && containsPaymentKeyword(message)) {
            return new InitiatePaymentIntent(
                    request.getUserId(),
                    extractBillId(message).orElse(null),
                    extractAmount(message)
            );
        }

        // 4️⃣ Bill query
        if (containsBillKeyword(message)) {
            return new QueryBillsIntent(request.getUserId());
        }

        return new UnknownIntent();
    }

    // --------------------------------------------------
    // Helper methods
    // --------------------------------------------------

    private static boolean containsPaymentKeyword(String message) {
        return message.contains("pay") || message.contains("payment") || message.contains("schedule payment") || message.contains("schedule later");
    }

    private static boolean isConfirmation(String message) {
        return CONFIRMATION_KEYWORDS.contains(message);
    }

    private static boolean containsBillKeyword(String msg) {
        return msg.contains("bill")
                || msg.contains("due")
                || msg.contains("amount");
    }

    private static Optional<String> extractBillId(String message) {
        Matcher matcher = BILL_ID_PATTERN.matcher(message);
        return matcher.find()
                ? Optional.of(matcher.group(1).toUpperCase())
                : Optional.empty();
    }

    private LocalDate extractDate(String msg) {
        Matcher m = DATE_PATTERN.matcher(msg);
        if (m.find()) return LocalDate.parse(m.group(1));
        throw new InvalidUserInputException("Please specify a payment date (yyyy-MM-dd).");
    }

    private static BigDecimal extractAmount(String message) {
        Matcher matcher = AMOUNT_PATTERN.matcher(message);
        return matcher.find()
                ? new BigDecimal(matcher.group(2))
                : null;
    }

    private boolean containsDate(String msg) {
        return DATE_PATTERN.matcher(msg).find();
    }
}
