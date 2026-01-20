package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wells.bill.assistant.model.BillDetails;
import com.wells.bill.assistant.util.DateParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BillParser {

    private final ChatClient chatClient;

    public BillParser(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public BillDetails parse(String text) {
        BillDetails details = new BillDetails();
        details.setConsumerName(extractConsumerName(text));
        details.setConsumerNumber(extractConsumerNumber(text));
        details.setAmount(extractAmount(text));
        details.setDueDate(extractDate(text, "Due Date|Payment Due Date|Bill Due Date"));
        details.setLastDueDate(extractDate(text, "Last Due Date|Disconnection Date"));
        return details;
    }

    private String extractConsumerName(String text) {
        log.info("Extracting consumer name");
        Pattern pattern = Pattern.compile(
                "Consumer Name\\s*:?\\s*([A-Za-z .]+?)(?=\\s+Consumer Number|\\s+Service Connection|\\s+Billing Period)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            log.info("Consumer name matched: {}", name);
            return name;
        }
        log.warn("Consumer name not found");
        return null;
    }

    private String extractConsumerNumber(String text) {
        log.info("Extracting consumer number");
        Pattern pattern = Pattern.compile(
                "Consumer Number\\s*:?\\s*(\\d{6,15})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String number = matcher.group(1);
            log.info("Consumer number matched: {}", number);
            return number;
        }
        log.warn("Consumer number not found");
        return null;
    }

    private BigDecimal extractAmount(String text) {
        log.info("Extracting amount...");
        Pattern pattern = Pattern.compile("(?:Amount Due|Total Amount Due|Net Payable|Payable Amount|Bill Amount|Current Charges)"
                        + "\\s*:?\\s*"
                        + "(?:₹\\s*|Rs\\.?\\s*|INR\\s*)?"
                        + "([0-9,]+(?:\\.\\d{1,2})?)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            log.info("Amount matched: {}", matcher.group(1));
            return new BigDecimal(matcher.group(1).replace(",", ""));
        }
        log.warn("Amount not found");
        return null;
    }

    private LocalDate extractDate(String text, String labelRegex) {
        log.info("Extracting date for labels: {}", labelRegex);

        Pattern pattern = Pattern.compile("(?:" + labelRegex + ")"
                        + "\\s*:?\\s*"
                        + "([0-9]{1,2}-[A-Za-z]{3}-[0-9]{4})",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            log.info("Date matched: {}", matcher.group(1));
            return DateParser.parseDate(matcher.group(1));
        }

        log.warn("Date not found for {}", labelRegex);
        return null;
    }

    public BillDetails parseUsingLLM(String billText) {
        log.info("Parsing text using LLM =>>>");
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
                - amount (number only)
                - dueDate (yyyy-MM-dd)
                - lastDueDate (yyyy-MM-dd or null)
                - consumerName
                - consumerNumber
                
                Return ONLY valid JSON.
                
                Bill Text:
                %s
                """.formatted(billText);
    }

    private BillDetails parseJsonToBillDetails(String json) {
        log.info("LLM response: {}", json);

        String sanitizedJson = sanitizeJson(json);
        log.info("Sanitized JSON: {}", sanitizedJson);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            BillDetails billDetails = mapper.readValue(sanitizedJson, BillDetails.class);
            log.info("Extracted bill details using LLM");
            return billDetails;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    private String sanitizeJson(String llmResponse) {
        String cleaned = llmResponse
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();

        // Optional: remove leading text before first '{'
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned;
    }
}
