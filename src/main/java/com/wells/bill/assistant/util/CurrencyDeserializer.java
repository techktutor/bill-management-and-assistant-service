package com.wells.bill.assistant.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.util.Currency;
import java.util.Locale;
import java.util.Map;

public class CurrencyDeserializer extends JsonDeserializer<Currency> {

    private static final Map<String, String> SYMBOL_TO_CODE = Map.of(
            "$", "USD",
            "₹", "INR",
            "€", "EUR",
            "£", "GBP"
    );

    @Override
    public Currency deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        // Symbol → ISO code
        if (SYMBOL_TO_CODE.containsKey(normalized)) {
            return Currency.getInstance(SYMBOL_TO_CODE.get(normalized));
        }

        // ISO code already
        try {
            return Currency.getInstance(normalized);
        } catch (IllegalArgumentException e) {
            throw InvalidFormatException.from(
                    p,
                    "Unrecognized currency: " + value,
                    value,
                    Currency.class
            );
        }
    }
}



