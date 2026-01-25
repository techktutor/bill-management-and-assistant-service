package com.wells.bill.assistant.util;

import com.wells.bill.assistant.entity.BillEntity;
import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.DateRange;
import com.wells.bill.assistant.model.Money;

public final class BillMapper {

    private BillMapper() {
    }

    public static BillDetail toDetail(BillEntity b) {
        DateRange billingPeriod = new DateRange(
                b.getBillingStartDate(),
                b.getBillingEndDate()
        );
        Money amountDue = new Money(
                b.getAmountDue(),
                CurrencyUtil.fromSymbol(b.getCurrency())
        );
        return new BillDetail(
                b.getId(),
                b.getUserId(),
                b.getConsumerNumber(),
                b.getConsumerName(),
                b.getProviderName(),
                b.getServiceNumber(),
                b.getBillCategory(),
                b.getDueDate(),
                billingPeriod,
                amountDue,
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
        b.setBillingStartDate(null != d.billingPeriod() ? d.billingPeriod().start() : null);
        b.setBillingEndDate(null != d.billingPeriod() ? d.billingPeriod().end() : null);
        b.setDueDate(d.dueDate());
        b.setAmountDue(d.amountDue().amount());
        b.setCurrency(d.amountDue().currency().getSymbol());
        b.setConfidenceScore(d.confidenceScore());
        b.setConfidenceDecision(d.confidenceDecision());

        return b;
    }
}
