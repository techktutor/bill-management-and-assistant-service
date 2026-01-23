package com.wells.bill.assistant.entity;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum BillCategory {
    ELECTRICITY,
    WATER,
    GAS,
    MOBILE,
    BROADBAND,
    INTERNET,
    DTH,
    LPG,
    MUNICIPAL,
    @JsonEnumDefaultValue
    OTHER
}
