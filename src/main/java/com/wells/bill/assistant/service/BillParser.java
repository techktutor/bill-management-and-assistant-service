package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wells.bill.assistant.entity.BillCategory;
import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BillParser {

    /* -------------------- DATE FORMATS -------------------- */
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
    };

    /* -------------------- REGEX -------------------- */
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(TOTAL AMOUNT DUE|AMOUNT PAYABLE|NET PAYABLE|TOTAL DUE)\\s*:?\\s*(\\d+(\\.\\d{1,2})?)"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(DUE DATE|PAYMENT DUE BY|PAY BY|LAST DATE)\\s*:?\\s*" +
                    "([A-Z]+\\s+\\d{1,2},?\\s+\\d{4}|\\d{2}[-/.][A-Z]{3}[-/.]\\d{4}|\\d{2}[-/.]\\d{2}[-/.]\\d{4})"
    );

    private static final Pattern CONSUMER_NUMBER_PATTERN = Pattern.compile(
            "(CONSUMER NUMBER|ACCOUNT NUMBER|ACCOUNT NO|CA NO)\\s*:?\\s*([A-Z0-9\\-]{6,25})"
    );

    private static final Pattern CONSUMER_NAME_PATTERN = Pattern.compile(
            "(CONSUMER NAME|CUSTOMER NAME|NAME)\\s*:?\\s*([A-Z ]{3,})"
    );

    private static final Pattern VENDOR_PATTERN =
            Pattern.compile("(VENDOR|PROVIDER)\\s*:?\\s*([A-Z ]{3,})");

    /* -------------------- PROVIDER KEYWORDS -------------------- */
    private static final Map<String, String> PROVIDER_KEYWORDS;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("TATA POWER", "TATA POWER");
        map.put("BSES", "BSES");
        map.put("MAHADISCOM", "MAHADISCOM");
        map.put("DELHI JAL BOARD", "DELHI JAL BOARD");
        map.put("INDANE", "INDANE");
        map.put("BHARAT GAS", "BHARAT GAS");
        map.put("AIRTEL", "AIRTEL");
        map.put("JIO", "JIO");
        map.put("VODAFONE", "VODAFONE IDEA");
        map.put("VI ", "VODAFONE IDEA");
        PROVIDER_KEYWORDS = Collections.unmodifiableMap(map);
    }

    private final ChatClient chatClient;

    public BillParser(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /* -------------------- ENTRY POINT -------------------- */
    public BillParseResult parse(String rawText) {

        String text = normalize(rawText);

        FieldExtraction<BigDecimal> amount = extractAmount(text);
        FieldExtraction<LocalDate> dueDate = extractDueDate(text);
        FieldExtraction<String> consumerName = extractConsumerName(text);
        FieldExtraction<String> consumerNumber = extractConsumerNumber(text);
        FieldExtraction<String> provider = extractProvider(text);
        FieldExtraction<String> category = extractCategory(text);

        BillDetail bill = BillDetail.builder()
                .amountDue(amount.getValue())
                .dueDate(dueDate.getValue())
                .consumerName(consumerName.getValue())
                .consumerNumber(consumerNumber.getValue())
                .providerName(provider.getValue())
                .billCategory(category.getValue() != null
                        ? BillCategory.valueOf(category.getValue())
                        : BillCategory.OTHER)
                .build();

        ParsedFields parsedFields = ParsedFields.builder()
                .amountDue(amount)
                .dueDate(dueDate)
                .consumerName(consumerName)
                .consumerNumber(consumerNumber)
                .providerName(provider)
                .billCategory(category)
                .build();

        int overallConfidence = average(
                amount.getConfidence(),
                dueDate.getConfidence(),
                consumerName.getConfidence(),
                consumerNumber.getConfidence(),
                provider.getConfidence(),
                category.getConfidence()
        );

        return BillParseResult.builder()
                .bill(bill)
                .parsedFields(parsedFields)
                .overallConfidence(overallConfidence)
                .build();
    }

    /* -------------------- NORMALIZATION -------------------- */
    private static String normalize(String text) {
        return text
                .replaceAll("[₹$,]", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    /* -------------------- EXTRACTION -------------------- */

    private static FieldExtraction<BigDecimal> extractAmount(String text) {
        Matcher m = AMOUNT_PATTERN.matcher(text);
        if (m.find()) {
            return FieldExtraction.<BigDecimal>builder()
                    .value(new BigDecimal(m.group(2)))
                    .confidence(100)
                    .reasons(List.of(
                            ReasonCode.AMOUNT_LABEL_MATCHED,
                            ReasonCode.AMOUNT_NUMERIC_PARSED))
                    .build();
        }
        return notFound();
    }

    private static FieldExtraction<LocalDate> extractDueDate(String text) {
        Matcher m = DATE_PATTERN.matcher(text);
        if (m.find()) {
            LocalDate date = parseDate(m.group(2));
            if (date != null) {
                return FieldExtraction.<LocalDate>builder()
                        .value(date)
                        .confidence(95)
                        .reasons(List.of(
                                ReasonCode.DATE_LABEL_MATCHED,
                                ReasonCode.DATE_FORMAT_PARSED))
                        .build();
            }
        }
        return notFound();
    }

    private static FieldExtraction<String> extractConsumerName(String text) {
        Matcher m = CONSUMER_NAME_PATTERN.matcher(text);
        if (m.find()) {
            return FieldExtraction.<String>builder()
                    .value(m.group(2).trim())
                    .confidence(70)
                    .reasons(List.of(ReasonCode.STRONG_LABEL_MATCH))
                    .build();
        }
        return notFound();
    }

    private static FieldExtraction<String> extractConsumerNumber(String text) {
        Matcher m = CONSUMER_NUMBER_PATTERN.matcher(text);
        if (m.find()) {
            String normalized = m.group(2).replaceAll("[^0-9]", "");
            return FieldExtraction.<String>builder()
                    .value(normalized)
                    .confidence(90)
                    .reasons(List.of(
                            ReasonCode.STRONG_LABEL_MATCH,
                            ReasonCode.NUMERIC_PATTERN_MATCH))
                    .build();
        }
        return notFound();
    }

    private static FieldExtraction<String> extractProvider(String text) {

        Matcher vendor = VENDOR_PATTERN.matcher(text);
        if (vendor.find()) {
            return FieldExtraction.<String>builder()
                    .value(vendor.group(2).trim())
                    .confidence(95)
                    .reasons(List.of(ReasonCode.STRONG_LABEL_MATCH))
                    .build();
        }

        for (var e : PROVIDER_KEYWORDS.entrySet()) {
            if (text.contains(e.getKey())) {
                return FieldExtraction.<String>builder()
                        .value(e.getValue())
                        .confidence(100)
                        .reasons(List.of(ReasonCode.EXACT_KEYWORD_MATCH))
                        .build();
            }
        }

        String firstLine = text.split("\n")[0];
        if (firstLine.contains("COMPANY") || firstLine.contains("POWER")) {
            return FieldExtraction.<String>builder()
                    .value(firstLine.trim())
                    .confidence(70)
                    .reasons(List.of(ReasonCode.INFERRED))
                    .build();
        }

        return notFound();
    }

    private static FieldExtraction<String> extractCategory(String text) {
        BillCategory category;

        if (text.contains("KWH") || text.contains("ELECTRICITY")) category = BillCategory.ELECTRICITY;
        else if (text.contains("WATER")) category = BillCategory.WATER;
        else if (text.contains("PNG") || text.contains("SCM")) category = BillCategory.GAS;
        else if (text.contains("LPG")) category = BillCategory.LPG;
        else category = BillCategory.OTHER;

        return FieldExtraction.<String>builder()
                .value(category.name())
                .confidence(category == BillCategory.OTHER ? 40 : 80)
                .reasons(List.of(ReasonCode.CATEGORY_KEYWORD_MATCHED))
                .build();
    }

    /* -------------------- HELPERS -------------------- */

    private static LocalDate parseDate(String date) {
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(date, f);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static <T> FieldExtraction<T> notFound() {
        return FieldExtraction.<T>builder()
                .value(null)
                .confidence(0)
                .reasons(List.of(ReasonCode.NOT_FOUND))
                .build();
    }

    private static int average(int... values) {
        return Arrays.stream(values).sum() / values.length;
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

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build();
        try {
            return mapper.readValue(sanitizedJson, BillDetail.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON to BillDetail: {}", e.getMessage(), e);
            throw new InvalidUserInputException("No readable text found in the bill.", e);
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
