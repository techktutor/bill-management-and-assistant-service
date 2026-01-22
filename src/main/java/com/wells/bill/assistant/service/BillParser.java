package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.model.BillDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BillParser {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    private final ChatClient chatClient;

    public BillParser(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public BillDetail parse(String rawText, UUID uuid) {
        log.info("Parsing bill text using rule-based parser =>>>");
        String text = normalize(rawText);
        return BillDetail.builder()
                .consumerName(extractConsumerName(text))
                .consumerId(extractConsumerNumber(text))
                .amountDue(extractAmountDue(text))
                .dueDate(extractDueDate(text))
                .providerName(extractProviderName(text))
                .billCategory(detectBillCategory(text))
                .userId(uuid)
                .build();
    }

    /* -------------------- NORMALIZATION -------------------- */
    private static String normalize(String text) {
        return text
                .replaceAll("[₹,]", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    /* -------------------- AMOUNT -------------------- */
    private static BigDecimal extractAmountDue(String text) {
        Pattern pattern = Pattern.compile(
                "(TOTAL AMOUNT DUE|AMOUNT PAYABLE|NET PAYABLE|AMOUNT DUE)\\s*:?\\s*(\\d+(\\.\\d{1,2})?)"
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(2));
        }
        return null;
    }

    /* -------------------- DATE -------------------- */
    private static LocalDate extractDueDate(String text) {
        Pattern pattern = Pattern.compile(
                "(DUE DATE|PAY BY|LAST DATE)\\s*:?\\s*(\\d{2}[-/.]\\d{2}[-/.]\\d{4}|\\d{4}-\\d{2}-\\d{2})"
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return parseDate(matcher.group(2));
        }
        return null;
    }

    private static LocalDate parseDate(String date) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(date, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /* -------------------- CONSUMER NUMBER -------------------- */
    private static String extractConsumerNumber(String text) {
        Pattern pattern = Pattern.compile(
                "(CONSUMER (NO|NUMBER|ID)|ACCOUNT NO|CA NO)\\s*:?\\s*(\\d{6,20})"
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(3);
        }
        return null;
    }

    /* -------------------- CONSUMER NAME -------------------- */
    private static String extractConsumerName(String text) {
        Pattern pattern = Pattern.compile(
                "(NAME|CONSUMER NAME|CUSTOMER NAME)\\s*:?\\s*([A-Z ]{3,})"
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return null;
    }

    /* -------------------- PROVIDER -------------------- */
    private static String extractProviderName(String text) {
        if (text.contains("BSES")) return "BSES";
        if (text.contains("TATA POWER")) return "TATA POWER";
        if (text.contains("MAHADISCOM")) return "MAHADISCOM";
        if (text.contains("DELHI JAL BOARD")) return "DELHI JAL BOARD";
        if (text.contains("INDANE")) return "INDANE";
        if (text.contains("BHARAT GAS")) return "BHARAT GAS";

        return null;
    }

    /* -------------------- CATEGORY -------------------- */
    private static BillCategory detectBillCategory(String text) {
        if (text.contains("ELECTRICITY") || text.contains("KWH")) {
            return BillCategory.ELECTRICITY;
        }
        if (text.contains("WATER") || text.contains("METER READING")) {
            return BillCategory.WATER;
        }
        if (text.contains("GAS") || text.contains("SCM")) {
            return BillCategory.GAS;
        }
        if (text.contains("MOBILE") || text.contains("TELECOM")) {
            return BillCategory.MOBILE;
        }
        return BillCategory.OTHER;
    }

    public BillDetail parseUsingLLM(String billText) {
        log.info("Parsing bill text using LLM =>>>");
        String prompt = buildPrompt(billText);

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        return parseJsonToBillDetails(response);
    }

    private String buildPrompt(String billText) {
        return """
                You are extracting information from an Indian utility bill.
                
                Extract the following fields:
                - amountDue (number only)
                - dueDate (yyyy-MM-dd)
                - consumerName
                - consumerNumber
                - providerName
                - billCategory
                
                Return ONLY valid JSON.
                
                Bill Text:
                %s
                """.formatted(billText);
    }

    private BillDetail parseJsonToBillDetails(String json) {
        String sanitizedJson = sanitizeJson(json);
        log.info("Sanitized JSON: {}", sanitizedJson);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            BillDetail billDetail = mapper.readValue(sanitizedJson, BillDetail.class);
            log.info("Extracted bill details using LLM");
            return billDetail;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    private String sanitizeJson(String llmResponse) {
        String cleaned = llmResponse
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned;
    }
}
