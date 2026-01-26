package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.exception.InvalidUserInputException;
import com.wells.bill.assistant.model.BillDetail;
import com.wells.bill.assistant.model.BillParseResult;
import com.wells.bill.assistant.model.Context;
import com.wells.bill.assistant.model.DataQualityDecision;
import com.wells.bill.assistant.service.BillParser;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.IngestionService;
import com.wells.bill.assistant.store.ContextStoreInMemory;
import com.wells.bill.assistant.util.TextExtractor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

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
        Context context = contextStore.getOrCreate(contextKey);

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No files provided"
            ));
        }

        log.info("Ingest request received for: {} files", files.size());
        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty() || file.getSize() > (20 * 1024 * 1024)) {
                log.info("File size not valid: {}", file.getOriginalFilename());
                continue;
            }

            log.info("Processing file: {}", file.getOriginalFilename());

            String text = TextExtractor.extractTextUsingTika(file);
            if (text == null || text.isBlank()) {
                throw new InvalidUserInputException("No readable text found in the bill.");
            }
            log.info("Text extraction completed for bill using Tika: {}", file.getOriginalFilename());

            List<Document> documents = TextExtractor.extractTextDocuments(file);
            if (null == documents || documents.isEmpty()) {
                throw new IllegalStateException("No text extracted from the bill using TikaDocumentReader");
            }
            log.info("Text extraction completed for bill using TikaDocumentReader: {}", file.getOriginalFilename());

            BillDetail billDetail = extractEssentialDetailsAndIngest(text, documents, context.userId());

            results.add(Map.of(
                    "fileName", Objects.requireNonNull(file.getOriginalFilename()),
                    "size", file.getSize(),
                    "contentType", Objects.requireNonNull(file.getContentType()),
                    "success", true,
                    "billDetail", billDetail.toString()
            ));
        }
        return ResponseEntity.ok(results);
    }

    private BillDetail extractEssentialDetailsAndIngest(String rawText, List<Document> documents, UUID userId) {
        log.info("Extracting essential bill details using rule based parsing =>>>");
        BillParseResult parseResult = billParser.parse(rawText);

        BillDetail resultBill = parseResult.bill();

        int confidenceScore = parseResult.overallConfidence();
        DataQualityDecision decision = DataQualityDecision.fromScore(confidenceScore);
        log.info("{} confidence ({}) in extracted bill details: {}",
                decision, confidenceScore, resultBill.id());

        resultBill = BillDetail.builder()
                .amountDue(resultBill.amountDue())
                .dueDate(resultBill.dueDate())
                .billingPeriod(resultBill.billingPeriod())
                .consumerName(resultBill.consumerName())
                .consumerNumber(resultBill.consumerNumber())
                .providerName(resultBill.providerName())
                .billCategory(resultBill.billCategory())
                .userId(userId)
                .confidenceScore(confidenceScore)
                .confidenceDecision(decision)
                .build();

        BillDetail savedBill = billService.createBill(resultBill);
        etlService.ingestFile(savedBill.id(), documents);

        return savedBill;
    }
}
