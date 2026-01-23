package com.wells.bill.assistant.model;

import lombok.Builder;

@Builder
public record BillParseResult(BillDetail bill, ParsedFields parsedFields, int overallConfidence) {
}
