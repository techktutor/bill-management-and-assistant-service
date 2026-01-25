package com.wells.bill.assistant.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.wells.bill.assistant.util.CurrencyDeserializer;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(
        BigDecimal amount,

        @JsonDeserialize(using = CurrencyDeserializer.class)
        Currency currency
) {
}
