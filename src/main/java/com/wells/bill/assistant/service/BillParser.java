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
        details.setAmount(extractAmount(text));
        details.setDueDate(extractDate(text, "Due Date|Payment Due Date|Bill Due Date"));
        details.setLastDueDate(extractDate(text, "Last Due Date|Disconnection Date"));
        return details;
    }

    private BigDecimal extractAmount(String text) {
        log.info("Extracting amount from text: {}", text);
        Pattern pattern = Pattern.compile(
                "(Amount Due|Total Amount Due|Net Payable|Payable Amount|Bill Amount|Current Charges)" +
                        "\\s*[:₹Rs.INR]*\\s*" +
                        "([0-9,]+(?:\\.\\d{1,2})?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(2).replace(",", ""));
        }
        return null;
    }

    private LocalDate extractDate(String text, String label) {
        log.info("Extracting date for label >>> {}", label);
        Pattern pattern = Pattern.compile(
                label +
                        "\\s*:?\\s*" +
                        "([0-9]{1,2}[\\-/ ]?[A-Za-z]{3}[\\-/ ]?[0-9]{4}" +
                        "|[0-9]{1,2}[\\-/][0-9]{1,2}[\\-/][0-9]{4}" +
                        "|[A-Za-z]{3}\\s[0-9]{1,2},\\s[0-9]{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return DateParserUtil.parseDate(matcher.group(1));
        }
        return null;
    }
}
