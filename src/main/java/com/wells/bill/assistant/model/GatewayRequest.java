package com.wells.bill.assistant.model;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class GatewayRequest {
    private final BigDecimal amount;
    private final String currency;
    private final String idempotencyKey;

    private GatewayRequest(Builder builder) {
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.idempotencyKey = builder.idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal amount;
        private String currency;
        private String idempotencyKey;

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public GatewayRequest build() {
            if (amount == null || currency == null || idempotencyKey == null) {
                throw new IllegalStateException("amount, currency and idempotencyKey are required");
            }
            return new GatewayRequest(this);
        }
    }
}

