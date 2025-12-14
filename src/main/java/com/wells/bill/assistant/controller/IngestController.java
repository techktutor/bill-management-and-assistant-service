// ============================
// Optimized IngestController
// ============================
package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.model.BillCreateResponse;
import com.wells.bill.assistant.service.BillService;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final BillService billService;
    private final IngestionService etlService;

    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public ResponseEntity<?> ingest(@RequestParam("file") MultipartFile file, @RequestParam UUID customerId) {

        log.info("Ingest request received: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File cannot be empty"
            ));
        }

        BillCreateResponse bill = billService.createBill(
                customerId,
                file.getOriginalFilename()
        );
        // Exceptions handled by GlobalExceptionHandler
        int chunks = etlService.ingestFile(bill.getId(), file);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "chunks", chunks,
                "filename", Objects.requireNonNull(file.getOriginalFilename())
        ));
    }
}