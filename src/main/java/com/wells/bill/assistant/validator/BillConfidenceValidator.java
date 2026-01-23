package com.wells.bill.assistant.validator;

import com.wells.bill.assistant.model.*;

import java.util.ArrayList;
import java.util.List;

public final class BillConfidenceValidator {

    private BillConfidenceValidator() {
    }

    /* -------------------- THRESHOLDS -------------------- */
    private static final int MIN_AMOUNT_CONFIDENCE = 90;
    private static final int MIN_DUE_DATE_CONFIDENCE = 80;
    private static final int MIN_OVERALL_CONFIDENCE = 75;

    /* -------------------- ENTRY POINT -------------------- */
    public static ConfidenceValidationResult validate(BillParseResult result) {

        List<String> reasons = new ArrayList<>();
        ParsedFields fields = result.parsedFields();

        /* ---------- HARD BLOCKERS (MANDATORY FIELDS) ---------- */

        // amountDue must exist & be trustworthy
        if (fields.amountDue().getValue() == null) {
            reasons.add("Amount due not found");
            return reject(reasons);
        }

        if (fields.amountDue().getConfidence() < MIN_AMOUNT_CONFIDENCE) {
            reasons.add("Amount due confidence too low");
            return reject(reasons);
        }

        if (fields.amountDue().getReasons().contains(ReasonCode.NOT_FOUND) ||
                fields.amountDue().getReasons().contains(ReasonCode.INFERRED)) {

            reasons.add("Amount due inferred or not explicitly found");
            return reject(reasons);
        }

        // dueDate must exist & be trustworthy
        if (fields.dueDate().getValue() == null) {
            reasons.add("Due date not found");
            return reject(reasons);
        }

        if (fields.dueDate().getConfidence() < MIN_DUE_DATE_CONFIDENCE) {
            reasons.add("Due date confidence too low");
            return reject(reasons);
        }

        if (fields.dueDate().getReasons().contains(ReasonCode.NOT_FOUND) ||
                fields.dueDate().getReasons().contains(ReasonCode.INFERRED)) {

            reasons.add("Due date inferred or not explicitly found");
            return reject(reasons);
        }

        /* ---------- SOFT CHECKS (NON-BLOCKING) ---------- */

        if (result.overallConfidence() < MIN_OVERALL_CONFIDENCE) {
            reasons.add("Overall confidence below preferred threshold");
            return needsConfirmation(reasons);
        }

        if (fields.consumerName().getConfidence() < 60) {
            reasons.add("Consumer name confidence is low");
        }

        if (fields.consumerNumber().getConfidence() < 60) {
            reasons.add("Consumer number confidence is low");
        }

        if (fields.providerName().getConfidence() < 60) {
            reasons.add("Provider name confidence is low");
        }

        if (!reasons.isEmpty()) {
            return needsConfirmation(reasons);
        }

        /* ---------- ALL GOOD ---------- */
        reasons.add("All mandatory fields extracted with high confidence");
        return accept(reasons);
    }

    /* -------------------- HELPERS -------------------- */

    private static ConfidenceValidationResult reject(List<String> reasons) {
        return ConfidenceValidationResult.builder()
                .decision(DataQualityDecision.NOT_CONFIDENT)
                .reasons(reasons)
                .build();
    }

    private static ConfidenceValidationResult needsConfirmation(List<String> reasons) {
        return ConfidenceValidationResult.builder()
                .decision(DataQualityDecision.NEEDS_CONFIRMATION)
                .reasons(reasons)
                .build();
    }

    private static ConfidenceValidationResult accept(List<String> reasons) {
        return ConfidenceValidationResult.builder()
                .decision(DataQualityDecision.HIGH_CONFIDENCE)
                .reasons(reasons)
                .build();
    }
}

