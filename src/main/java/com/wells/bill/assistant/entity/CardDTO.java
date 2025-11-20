package com.wells.bill.assistant.entity;

import lombok.Data;

@Data
public class CardDTO {
    private String cardNumber;
    private String expMonth;
    private String expYear;
    private String cvv;
}
