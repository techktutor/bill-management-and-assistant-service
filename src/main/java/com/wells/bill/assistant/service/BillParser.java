package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wells.bill.assistant.model.BillCategory;
import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BillParser {

    /* ==================== CONSTANTS ==================== */

    private static final int LLM_FALLBACK_THRESHOLD = 55;

    /* ==================== DATE FORMATTERS ==================== */

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,

            formatter("d-MMM-uuuu"),
            formatter("dd-MMM-uuuu"),   // ✅ FIX
            formatter("dd MMM uuuu"),   // ✅ FIX
            formatter("MMM d uuuu"),
            formatter("MMMM d uuuu"),
            formatter("MMMM d, uuuu")
    };

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH);
    }

    /* ==================== MONEY ==================== */
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(USD|INR|EUR|GBP|₹|\\$|€|£)?\\s*" +
                    "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AMOUNT_LABEL_PATTERN = Pattern.compile(
            "(TOTAL\\s+AMOUNT\\s+DUE|AMOUNT\\s+DUE|TOTAL\\s+DUE|PAYABLE)[^0-9]{0,20}" +
                    "(USD|INR|EUR|GBP|₹|\\$|€|£)?\\s*" +
                    "([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, Currency> CURRENCY_MAP = Map.ofEntries(
            Map.entry("$", Currency.getInstance("USD")),
            Map.entry("USD", Currency.getInstance("USD")),
            Map.entry("₹", Currency.getInstance("INR")),
            Map.entry("INR", Currency.getInstance("INR")),
            Map.entry("€", Currency.getInstance("EUR")),
            Map.entry("EUR", Currency.getInstance("EUR")),
            Map.entry("£", Currency.getInstance("GBP")),
            Map.entry("GBP", Currency.getInstance("GBP"))
    );

    /* ==================== OTHER PATTERNS ==================== */
    private static final String DATE_TOKEN =
            "(" +
                    "\\d{1,2}[-\\s][A-Z]{3}[-\\s]\\d{4}" +        // 14-FEB-2026 / 14 FEB 2026
                    "|" +
                    "\\d{4}-\\d{2}-\\d{2}" +                     // 2026-04-20
                    "|" +
                    "[A-Z]{3,9}\\s+\\d{1,2},\\s*\\d{4}" +        // APRIL 20, 2026
                    ")";

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(DUE DATE|PAYMENT DUE BY|PAY BY|LAST DATE|DUE BY)\\s*:?\\s*" +
                    DATE_TOKEN,
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONSUMER_NUMBER_PATTERN =
            Pattern.compile("(CONSUMER NUMBER|ACCOUNT NUMBER|ACCOUNT NO|CA NO)\\s*:?\\s*([A-Z0-9\\-]{5,30})");

    private static final Pattern CONSUMER_NAME_PATTERN = Pattern.compile(
            "(CONSUMER NAME|CUSTOMER NAME|NAME)\\s*:?\\s*" +
                    "([A-Z ]{3,}?)\\s*(?=CONSUMER NUMBER|ACCOUNT NUMBER|ACCOUNT NO|CA NO|BILLING PERIOD|DUE DATE|PAY BY|$)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BILLING_PERIOD_PATTERN = Pattern.compile(
            "(BILLING PERIOD|BILL PERIOD|PERIOD)\\s*:?\\s*" +
                    DATE_TOKEN +
                    "\\s*[-–—]\\s*" +
                    DATE_TOKEN,
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MONTH_PERIOD_PATTERN = Pattern.compile(
            "(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROVIDER_LABEL_PATTERN = Pattern.compile(
            "(VENDOR|PROVIDER|ISSUER|SERVICE PROVIDER)\\s*:?\\s*" +
                    "([A-Z0-9 .&()\\-]{3,}?)\\s*(?=CATEGORY|DUE DATE|BILLING PERIOD|AMOUNT|TOTAL|$)",
            Pattern.CASE_INSENSITIVE
    );

    /* ==================== PROVIDERS ==================== */
    private static final Map<Pattern, String> KNOWN_PROVIDER_PATTERNS = Map.of(
            Pattern.compile("\\bTATA POWER\\b"), "TATA POWER",
            Pattern.compile("\\bBSES\\b"), "BSES",
            Pattern.compile("\\bMAHADISCOM\\b"), "MAHADISCOM",
            Pattern.compile("\\bDELHI JAL BOARD\\b"), "DELHI JAL BOARD",
            Pattern.compile("\\bINDANE\\b"), "INDANE",
            Pattern.compile("\\bBHARAT GAS\\b"), "BHARAT GAS",
            Pattern.compile("\\bAIRTEL\\b"), "AIRTEL",
            Pattern.compile("\\bJIO\\b"), "JIO",
            Pattern.compile("\\bVODAFONE\\b"), "VODAFONE IDEA"
    );

    private final ChatClient chatClient;

    public BillParser(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /* ==================== ENTRY POINT ==================== */
    public BillParseResult parse(String rawText) {

        String text = normalize(rawText);

        var amount = extractAmount(text);
        var dueDate = extractDueDate(text);
        var billingPeriod = extractBillingPeriod(text);
        var consumerName = extractConsumerName(text);
        var consumerNumber = extractConsumerNumber(text);
        var provider = extractProvider(text);
        var category = extractCategory(text, provider.value());

        int ruleConfidence = ruleBasedConfidence(
                amount,
                dueDate,
                billingPeriod,
                consumerName,
                consumerNumber,
                provider,
                category
        );

        BillDetail ruleBill = BillDetail.builder()
                .amountDue(amount.value())
                .dueDate(dueDate.value())
                .billingPeriod(billingPeriod.value())
                .consumerName(consumerName.value())
                .consumerNumber(consumerNumber.value())
                .providerName(provider.value())
                .billCategory(
                        category.value() != null
                                ? BillCategory.valueOf(category.value())
                                : BillCategory.OTHER
                )
                .build();

        if (ruleConfidence < LLM_FALLBACK_THRESHOLD) {
            log.info("Low confidence ({}) → falling back to LLM", ruleConfidence);

            BillDetail llmBill = parseUsingLLM(rawText);

            BillDetail merged = mergeRuleAndLLM(ruleBill, llmBill);

            int calculateLLMConfidence = calculateLLMConfidence(merged);
            int finalConfidence = Math.max(
                    ruleConfidence,
                    calculateLLMConfidence
            );

            log.info(
                    "RuleConfidence={}, LLMConfidence={}, FinalConfidence={}",
                    ruleConfidence,
                    calculateLLMConfidence,
                    finalConfidence
            );

            return BillParseResult.builder()
                    .bill(merged)
                    .overallConfidence(finalConfidence)
                    .build();
        }

        log.info("RuleConfidence={}", ruleConfidence);
        return BillParseResult.builder()
                .bill(ruleBill)
                .overallConfidence(ruleConfidence)
                .build();
    }

    private BillDetail mergeRuleAndLLM(BillDetail rule, BillDetail llm) {

        if (llm == null) return rule;
        if (rule == null) return llm;

        return BillDetail.builder()
                // -------- MUST-HAVE (RULE WINS) --------
                .amountDue(rule.amountDue() != null
                        ? rule.amountDue()
                        : llm.amountDue())

                .dueDate(rule.dueDate() != null
                        ? rule.dueDate()
                        : llm.dueDate())

                // -------- OPTIONAL --------
                .billingPeriod(rule.billingPeriod() != null
                        ? rule.billingPeriod()
                        : llm.billingPeriod())

                .consumerNumber(rule.consumerNumber() != null
                        ? rule.consumerNumber()
                        : llm.consumerNumber())

                .consumerName(rule.consumerName() != null
                        ? rule.consumerName()
                        : llm.consumerName())

                .providerName(rule.providerName() != null
                        ? rule.providerName()
                        : llm.providerName())

                // -------- CATEGORY --------
                .billCategory(
                        rule.billCategory() != null &&
                                rule.billCategory() != BillCategory.OTHER
                                ? rule.billCategory()
                                : llm.billCategory()
                )
                .build();
    }

    /* ==================== EXTRACTION ==================== */
    private static FieldExtraction<Money> extractAmount(String text) {

        Matcher totalMatcher = AMOUNT_LABEL_PATTERN.matcher(text);
        if (totalMatcher.find()) {
            return FieldExtraction.<Money>builder()
                    .value(toMoney(totalMatcher.group(2), totalMatcher.group(3)))
                    .confidence(98)
                    .reasons(List.of(ReasonCode.AMOUNT_LABEL_MATCHED))
                    .build();
        }

        // 2️⃣ SECONDARY: Largest numeric value (fallback)
        Matcher m = MONEY_PATTERN.matcher(text);
        BigDecimal max = BigDecimal.ZERO;
        Money selected = null;

        while (m.find()) {
            BigDecimal val = new BigDecimal(m.group(2).replace(",", ""));
            if (val.compareTo(max) > 0) {
                max = val;
                selected = toMoney(m.group(1), m.group(2));
            }
        }

        return selected != null
                ? FieldExtraction.<Money>builder()
                .value(selected)
                .confidence(80)
                .reasons(List.of(ReasonCode.AMOUNT_NUMERIC_PARSED))
                .build()
                : notFound();
    }

    private static Money toMoney(String currencyRaw, String amountRaw) {
        if (amountRaw == null) return null;

        BigDecimal amount = new BigDecimal(amountRaw.replace(",", ""));

        Currency currency;
        if (currencyRaw == null || currencyRaw.isBlank()) {
            currency = Currency.getInstance("INR"); // default
        } else {
            String key = currencyRaw.trim().toUpperCase(Locale.ROOT);
            currency = CURRENCY_MAP.getOrDefault(
                    key,
                    Currency.getInstance("INR")
            );
        }

        return new Money(amount, currency);
    }

    private static FieldExtraction<LocalDate> extractDueDate(String text) {
        Matcher m = DATE_PATTERN.matcher(text);
        if (m.find()) {
            LocalDate date = parseDate(m.group(2));
            if (date != null) {
                return FieldExtraction.<LocalDate>builder()
                        .value(date)
                        .confidence(95)
                        .reasons(List.of(ReasonCode.DATE_LABEL_MATCHED))
                        .build();
            }
        }
        return notFound();
    }

    private static FieldExtraction<DateRange> extractBillingPeriod(String text) {
        Matcher m = BILLING_PERIOD_PATTERN.matcher(text);
        if (m.find()) {
            LocalDate start = parseDate(m.group(2));
            LocalDate end = parseDate(m.group(3));
            if (start != null && end != null && !end.isBefore(start)) {
                return FieldExtraction.<DateRange>builder()
                        .value(new DateRange(start, end))
                        .confidence(95)
                        .reasons(List.of(ReasonCode.DATE_RANGE_PARSED))
                        .build();
            }
        }

        Matcher month = MONTH_PERIOD_PATTERN.matcher(text);
        if (month.find()) {
            Month mth = Month.valueOf(month.group(1).substring(0, 3).toUpperCase());
            int year = Integer.parseInt(month.group(2));
            LocalDate start = LocalDate.of(year, mth, 1);
            return FieldExtraction.<DateRange>builder()
                    .value(new DateRange(start, start.withDayOfMonth(start.lengthOfMonth())))
                    .confidence(80)
                    .reasons(List.of(ReasonCode.MONTH_YEAR_INFERRED))
                    .build();
        }

        return notFound();
    }

    private static FieldExtraction<String> extractConsumerName(String text) {
        Matcher m = CONSUMER_NAME_PATTERN.matcher(text);
        return m.find()
                ? FieldExtraction.<String>builder()
                .value(m.group(2).trim())
                .confidence(70)
                .reasons(List.of(ReasonCode.STRONG_LABEL_MATCH))
                .build()
                : notFound();
    }

    private static FieldExtraction<String> extractConsumerNumber(String text) {
        Matcher m = CONSUMER_NUMBER_PATTERN.matcher(text);
        return m.find()
                ? FieldExtraction.<String>builder()
                .value(m.group(2).trim())
                .confidence(90)
                .reasons(List.of(ReasonCode.NUMERIC_PATTERN_MATCH))
                .build()
                : notFound();
    }

    private static FieldExtraction<String> extractProvider(String text) {

        // 1️⃣ Explicit label extraction (BEST)
        Matcher labeled = PROVIDER_LABEL_PATTERN.matcher(text);
        if (labeled.find()) {
            return FieldExtraction.<String>builder()
                    .value(labeled.group(2).trim())
                    .confidence(95)
                    .reasons(List.of(ReasonCode.STRONG_LABEL_MATCH))
                    .build();
        }

        // 2️⃣ Known providers fallback
        for (var e : KNOWN_PROVIDER_PATTERNS.entrySet()) {
            if (e.getKey().matcher(text).find()) {
                return FieldExtraction.<String>builder()
                        .value(e.getValue())
                        .confidence(85)
                        .reasons(List.of(ReasonCode.EXACT_KEYWORD_MATCH))
                        .build();
            }
        }

        return notFound();
    }

    private static FieldExtraction<String> extractCategory(String text, String provider) {
        BillCategory category;
        int confidence;

        if (text.contains("KWH") && provider != null) {
            category = BillCategory.ELECTRICITY;
            confidence = 90;
        } else if (text.contains("WATER")) {
            category = BillCategory.WATER;
            confidence = 80;
        } else if (text.contains("PNG") || text.contains("SCM")) {
            category = BillCategory.GAS;
            confidence = 75;
        } else if (text.contains("LPG")) {
            category = BillCategory.LPG;
            confidence = 75;
        } else {
            category = BillCategory.OTHER;
            confidence = 40;
        }

        return FieldExtraction.<String>builder()
                .value(category.name())
                .confidence(confidence)
                .reasons(List.of(ReasonCode.CATEGORY_KEYWORD_MATCHED))
                .build();
    }

    /* ==================== HELPERS ==================== */
    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;

        // 🔥 Normalize date-specific noise
        String date = raw
                .replace(",", "") // APRIL 20, 2026 → APRIL 20 2026
                .replaceAll("[–—−]", "-") // normalize Unicode dashes
                .trim()
                .replaceAll("\\s+", " ");

        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(date, f);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static int ruleBasedConfidence(
            FieldExtraction<?> amount,
            FieldExtraction<?> dueDate,
            FieldExtraction<?> billingPeriod,
            FieldExtraction<?> consumerName,
            FieldExtraction<?> consumerNumber,
            FieldExtraction<?> provider,
            FieldExtraction<?> category
    ) {
        // ---------- HARD GATE ----------
        if (amount.confidence() == 0 || dueDate.confidence() == 0) {
            return 0;
        }

        // ---------- BASE CONFIDENCE ----------
        int score = 40; // amount + dueDate satisfied

        // ---------- OPTIONAL SIGNALS ----------
        if (billingPeriod.confidence() > 0) score += 15;
        if (consumerNumber.confidence() > 0) score += 15;
        if (provider.confidence() > 0) score += 15;
        if (consumerName.confidence() > 0) score += 10;
        if (category.confidence() > 0 &&
                !"OTHER".equals(category.value())) score += 5;

        return Math.min(score, 100);
    }

    private int calculateLLMConfidence(BillDetail bill) {
        // ---------- HARD VALIDATION ----------
        if (bill == null) return 0;

        if (bill.amountDue() == null ||
                bill.amountDue().amount() == null ||
                bill.amountDue().currency() == null) {
            return 0; // ❌ Mandatory fields missing
        }

        // ---------- BASE CONFIDENCE ----------
        int score = 35; // amountDue + dueDate satisfied

        // ---------- OPTIONAL QUALITY SIGNALS ----------
        if (bill.billingPeriod() != null &&
                bill.billingPeriod().start() != null &&
                bill.billingPeriod().end() != null) {
            score += 15;
        }

        if (bill.consumerNumber() != null && !bill.consumerNumber().isBlank()) {
            score += 15;
        }

        if (bill.providerName() != null && !bill.providerName().isBlank()) {
            score += 15;
        }

        if (bill.consumerName() != null && !bill.consumerName().isBlank()) {
            score += 10;
        }

        if (bill.billCategory() != null && bill.billCategory() != BillCategory.OTHER) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private static <T> FieldExtraction<T> notFound() {
        return FieldExtraction.<T>builder()
                .value(null)
                .confidence(0)
                .reasons(List.of(ReasonCode.NOT_FOUND))
                .build();
    }

    private static String normalize(String text) {
        return text == null ? "" :
                text.trim()
                        .replaceAll("\\s+", " ")
                        .toUpperCase(Locale.ROOT);
    }

    /* ==================== LLM ==================== */
    public BillDetail parseUsingLLM(String billText) {
        String response = chatClient
                .prompt(buildPrompt(billText))
                .call()
                .content();
        return parseJsonToBillDetails(response);
    }

    private String buildPrompt(String billText) {
        return """
                Extract bill details and return ONLY valid JSON.
                Fields:
                - amountDue { amount, currency }
                - dueDate (yyyy-MM-dd)
                - billingPeriod { startDate, endDate }
                - consumerName
                - consumerNumber
                - providerName
                - billCategory
                Bill Text:
                %s
                """.formatted(billText);
    }

    private BillDetail parseJsonToBillDetails(String json) {
        String sanitizeJson = sanitizeJson(json);
        log.info("Parsing Sanitized JSON =>>>");
        try {
            ObjectMapper mapper = JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                    .build();

            return mapper.readValue(sanitizeJson, BillDetail.class);
        } catch (Exception e) {
            throw new InvalidUserInputException("Unable to parse bill via LLM", e);
        }
    }

    private String sanitizeJson(String response) {
        log.info("Sanitizing LLM response =>>>");
        String cleaned = response.replaceAll("(?s)```json|```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new InvalidUserInputException("Invalid JSON from LLM");
        }
        String finalJson = cleaned.substring(start, end + 1);
        log.info("Sanitized JSON: {}", finalJson);
        return finalJson;
    }
}
