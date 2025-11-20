package com.wells.bill.assistant.entity;

import lombok.Data;

@Data
public class TokenResponse {
    private String token;
    private String last4;
    private String brand;
    private int expMonth;
    private int expYear;

    public TokenResponse(String token, String last4, String brand, int expMonth, int expYear) {
        this.token = token;
        this.last4 = last4;
        this.brand = brand;
        this.expMonth = expMonth;
        this.expYear = expYear;
    }
}
