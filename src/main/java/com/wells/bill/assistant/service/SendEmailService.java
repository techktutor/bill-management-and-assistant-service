package com.wells.bill.assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailService {

    private final JavaMailSender mailSender;

    public void sendPaymentConfirmationTokenEmail(
            String providerName,
            BigDecimal amount,
            LocalDate scheduledDate,
            String token,
            String userEmail
    ) {

        String subject = "Your Payment Confirmation Code";

        String body = scheduledDate == null
                ? """
                Hello,
                
                You requested a payment for:
                
                Provider: %s
                Amount: %s
                
                Confirmation Code: %s
                
                This code expires in 5 minutes.
                If you did not request this, ignore this email.
                """
                .formatted(providerName, amount, token)
                : """
                Hello,
                
                You requested a scheduled payment for:
                
                Provider: %s
                Amount: %s
                Scheduled Date: %s
                
                Confirmation Code: %s
                
                This code expires in 5 minutes.
                If you did not request this, ignore this email.
                """
                .formatted(providerName, amount, scheduledDate, token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setFrom("no-reply@billassistant.com");
        message.setSubject(subject);
        message.setText(body);

        try {
            log.info("Sending payment confirmation email to {}", userEmail);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send confirmation email", ex);
            throw new IllegalStateException("Unable to send confirmation email");
        }
    }
}

