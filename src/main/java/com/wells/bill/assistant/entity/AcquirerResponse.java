package com.wells.bill.assistant.entity;

import lombok.Data;

@Data
public class AcquirerResponse {
    private boolean success;
    private String remoteAuthId;

    public AcquirerResponse(boolean success, String remoteAuthId) {
        this.success = success;
        this.remoteAuthId = remoteAuthId;
    }
}
