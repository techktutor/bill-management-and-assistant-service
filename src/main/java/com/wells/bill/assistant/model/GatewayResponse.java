package com.wells.bill.assistant.model;

public record GatewayResponse(boolean success, String referenceId, String errorCode, String errorMessage) {

    public static GatewayResponse success(String referenceId) {
        return new GatewayResponse(true, referenceId, null, null);
    }

    public static GatewayResponse failure(String code, String message) {
        return new GatewayResponse(false, null, code, message);
    }
}
