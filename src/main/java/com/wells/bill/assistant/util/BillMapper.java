package com.wells.bill.assistant.util;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.model.BillDetail;

public final class BillMapper {

    private BillMapper() {
    }

    public static BillDetail toDetail(BillEntity b) {
        return new BillDetail(
                b.getId(),
                b.getUserId(),
                b.getConsumerNumber(),
                b.getConsumerName(),
                b.getProviderName(),
                b.getServiceNumber(),
                b.getBillCategory(),
                b.getBillingStartDate(),
                b.getBillingEndDate(),
                b.getDueDate(),
                b.getAmountDue(),
                b.getCurrency(),
                b.getStatus(),
                b.getPaymentId(),
                b.getIngestedAt(),
                b.getChunkCount(),
                b.getCreatedAt(),
                b.getUpdatedAt(),
                b.getConfidenceScore(),
                b.getConfidenceDecision()
        );
    }

    public static BillEntity toEntity(BillDetail d) {
        BillEntity b = new BillEntity();

        b.setUserId(d.userId());
        b.setConsumerNumber(d.consumerNumber());
        b.setConsumerName(d.consumerName());
        b.setProviderName(d.providerName());
        b.setServiceNumber(d.serviceNumber());
        b.setBillCategory(d.billCategory());
        b.setBillingStartDate(d.billingStartDate());
        b.setBillingEndDate(d.billingEndDate());
        b.setDueDate(d.dueDate());
        b.setAmountDue(d.amountDue());
        b.setCurrency(d.currency());
        b.setConfidenceScore(d.confidenceScore());
        b.setConfidenceDecision(d.confidenceDecision());

        return b;
    }
}
