package com.wells.bill.assistant.util;

import java.util.Currency;
import java.util.Map;

public final class CurrencyUtil {

    private CurrencyUtil() {
    }

    private static final Map<String, Currency> SYMBOL_TO_CURRENCY = Map.ofEntries(
            Map.entry("$", Currency.getInstance("USD")),
            Map.entry("USD", Currency.getInstance("USD")),

            Map.entry("₹", Currency.getInstance("INR")),
            Map.entry("INR", Currency.getInstance("INR")),

            Map.entry("€", Currency.getInstance("EUR")),
            Map.entry("EUR", Currency.getInstance("EUR")),

            Map.entry("£", Currency.getInstance("GBP")),
            Map.entry("GBP", Currency.getInstance("GBP"))
    );

    /**
     * Converts a currency symbol or ISO code into java.util.Currency.
     *
     * @param raw symbol or code (e.g. "$", "USD", "₹", "INR")
     * @return resolved Currency (defaults to INR if null or unknown)
     */
    public static Currency fromSymbol(String raw) {

        if (raw == null || raw.isBlank()) {
            return defaultCurrency();
        }

        String normalized = raw.trim().toUpperCase();

        // Direct symbol / code match
        Currency currency = SYMBOL_TO_CURRENCY.get(normalized);
        if (currency != null) {
            return currency;
        }

        // Fallback: try ISO currency code
        try {
            return Currency.getInstance(normalized);
        } catch (IllegalArgumentException ignored) {
            return defaultCurrency();
        }
    }

    private static Currency defaultCurrency() {
        // India-first assumption (adjust if needed)
        return Currency.getInstance("INR");
    }
}

