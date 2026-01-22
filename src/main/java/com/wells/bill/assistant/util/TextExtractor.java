package com.wells.bill.assistant.util;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
public class TextExtractor {

    public static String extractTextUsingTika(MultipartFile file) {
        Tika tika = new Tika();
        try (InputStream is = file.getInputStream()) {
            return tika.parseToString(is);
        } catch (TikaException | IOException e) {
            throw new InvalidUserInputException("Failed to extract text from bill", e);
        }
    }

    public static List<Document> extractTextDocuments(MultipartFile file) {
        log.info("Extracting text from bill: {}", file.getOriginalFilename());
        List<Document> documents;
        try (InputStream is = file.getInputStream()) {
            Resource resource = new InputStreamResource(is);
            documents = new TikaDocumentReader(resource).get();
        } catch (IOException e) {
            throw new InvalidUserInputException("Failed to extract text from bill", e);
        }
        log.info("Text extraction completed for bill: {}", file.getOriginalFilename());
        return documents;
    }
}
