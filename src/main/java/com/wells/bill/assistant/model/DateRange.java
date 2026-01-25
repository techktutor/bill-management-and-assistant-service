package com.wells.bill.assistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record DateRange(
        @JsonProperty("startDate") LocalDate start,
        @JsonProperty("endDate") LocalDate end
) {
}
