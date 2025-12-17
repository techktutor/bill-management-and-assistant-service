package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillCreateResponse;
import com.wells.bill.assistant.model.BillDetails;
import com.wells.bill.assistant.service.BillParser;
import com.wells.bill.assistant.service.BillService;
import com.wells.bill.assistant.service.BillTextExtractionService;
import com.wells.bill.assistant.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final BillParser billParser;
    private final BillService billService;
    private final IngestionService etlService;
    private final BillTextExtractionService billTextExtractionService;

    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public ResponseEntity<?> ingest(@RequestParam("file") MultipartFile file) throws Exception {
        log.info("Ingest request received: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File cannot be empty"
            ));
        }

        String normalizedText = billTextExtractionService.extractText(file);

        BillDetails details = billParser.parse(normalizedText);

        if (details.getAmount() == null || details.getDueDate() == null) {
            details = billTextExtractionService.extractUsingLLM(normalizedText);
        }
        details.setFileName(file.getOriginalFilename());

        BillCreateResponse bill = billService.createBill(details);

        int chunks = etlService.ingestFile(bill.getId(), file);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "chunks", chunks,
                "filename", Objects.requireNonNull(file.getOriginalFilename())
        ));
    }
}