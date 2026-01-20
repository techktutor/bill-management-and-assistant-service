package com.wells.bill.assistant.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@Slf4j
public class TextExtractor {

    public static String extractText(MultipartFile file) {
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

    public static String normalize(String text) {
        return text
                .replaceAll("\\r", " ")
                .replaceAll("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
