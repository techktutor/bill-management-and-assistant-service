package com.wells.bill.assistant.entity;

public record AuthorizePayload(String cardToken, Long amount, String currency, String paymentId) {}
