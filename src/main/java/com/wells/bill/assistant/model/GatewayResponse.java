package com.wells.bill.assistant.model;

public class GatewayResponse {

    private final boolean success;
    private final String referenceId;
    private final String errorCode;
    private final String errorMessage;

    private GatewayResponse(
            boolean success,
            String referenceId,
            String errorCode,
            String errorMessage
    ) {
        this.success = success;
        this.referenceId = referenceId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static GatewayResponse success(String referenceId) {
        return new GatewayResponse(true, referenceId, null, null);
    }

    public static GatewayResponse failure(String code, String message) {
        return new GatewayResponse(false, null, code, message);
    }

    public boolean success() {
        return success;
    }

    public String referenceId() {
        return referenceId;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }
}
