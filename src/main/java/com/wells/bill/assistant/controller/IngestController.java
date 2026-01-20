package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillDetails;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.IngestionService;
import com.wells.bill.assistant.util.TextExtractor;
import com.wells.bill.assistant.service.BillParser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final BillParser billParser;
    private final BillService billService;
    private final IngestionService etlService;

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingest(@RequestPart("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No files provided"
            ));
        }

        log.info("Ingest request received for files: {}", files.size());
        List<Map<String, Object>> results = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            log.info("Processing file: {}", file.getOriginalFilename());

            String normalizedText = TextExtractor.extractText(file);
            BillDetails details = billParser.parse(normalizedText);

            if (details.getAmount() == null || details.getDueDate() == null) {
                details = billParser.parseUsingLLM(normalizedText);
            }
            details.setFileName(file.getOriginalFilename());
            details.setCustomerId(UUID.randomUUID());

            UUID billId = billService.createBill(details);

            int chunks = etlService.ingestFile(billId, file);

            results.add(Map.of(
                    "fileName", Objects.requireNonNull(file.getOriginalFilename()),
                    "size", file.getSize(),
                    "contentType", Objects.requireNonNull(file.getContentType()),
                    "success", true,
                    "chunks", chunks,
                    "billId", billId.toString()
            ));
        }
        return ResponseEntity.ok(results);
    }
}