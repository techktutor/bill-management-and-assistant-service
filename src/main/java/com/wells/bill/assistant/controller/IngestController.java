package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.service.BillParser;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.IngestionService;
import com.wells.bill.assistant.store.ContextStoreInMemory;
import com.wells.bill.assistant.util.TextExtractor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.wells.bill.assistant.util.CookieGenerator.CONTEXT_COOKIE;
import static com.wells.bill.assistant.util.CookieGenerator.getContextKey;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final BillParser billParser;
    private final BillService billService;
    private final IngestionService etlService;
    private final ContextStoreInMemory contextStore;

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingest(@RequestPart("files") List<MultipartFile> files,
                                    @CookieValue(value = CONTEXT_COOKIE, required = false) String contextKey,
                                    HttpServletResponse response) {

        // 1️⃣ Resolve key
        contextKey = getContextKey(contextKey, response);

        // 2️⃣ Load context (expires automatically after 10 min idle)
        Context context = contextStore.get(contextKey);

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No files provided"
            ));
        }

        log.info("Ingest request received for files: {}", files.size());
        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty() || file.getSize() > (20 * 1024 * 1024)) {
                log.info("File size not valid: {}", file.getOriginalFilename());
                continue;
            }

            log.info("Processing file: {}", file.getOriginalFilename());

            String normalizedText = TextExtractor.extractTextUsingTika(file);
            if (normalizedText == null || normalizedText.isBlank()) {
                throw new IllegalArgumentException("Unable to extract readable text from file.");
            }

            BillDetail details = billParser.parse(normalizedText, context.userId());

            if (details.amountDue() == null || details.dueDate() == null) {
                details = billParser.parseUsingLLM(normalizedText);
            }

            BillDetail billDetail = billService.createBill(details);

            int chunks = etlService.ingestFile(billDetail.id(), file);

            results.add(Map.of(
                    "fileName", Objects.requireNonNull(file.getOriginalFilename()),
                    "size", file.getSize(),
                    "contentType", Objects.requireNonNull(file.getContentType()),
                    "success", true,
                    "chunks", chunks,
                    "billDetail", billDetail.id()
            ));
        }
        return ResponseEntity.ok(results);
    }
}