package com.wells.bill.assistant.service;

import com.wells.bill.assistant.model.BillDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BillParser {

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
            return DateParserUtil.parseDate(matcher.group(1));
        }

        log.warn("Date not found for {}", labelRegex);
        return null;
    }
}
