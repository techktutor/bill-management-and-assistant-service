package com.wells.bill.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wells.bill.assistant.model.BillDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@Slf4j
@Service
public class TextExtractorService {

    private final ChatClient chatClient;

    public TextExtractorService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String extractText(MultipartFile file) {
        log.info("Extracting text from bill: {}", file.getOriginalFilename());
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // IMPORTANT
                }
            };

            TikaDocumentReader reader = new TikaDocumentReader(resource);

            String rawText = reader.get().stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));

            String normalizedText = normalize(rawText);
            if (normalizedText.isBlank()) {
                throw new IllegalArgumentException("Unable to extract text from bill");
            }
            log.info("Extracted text length: {}", normalizedText.length());
            log.info("Extracted text preview: {}", normalizedText);
            return normalizedText;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from bill", e);
        }
    }

    public String normalize(String text) {
        return text
                .replaceAll("\\r", " ")
                .replaceAll("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public BillDetails extractUsingLLM(String billText) {
        log.info("Extracting bill details using LLM");
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
