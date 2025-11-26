package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.service.ETLPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingest")
public class IngestController {

    private final ETLPipelineService etlService;

    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public ResponseEntity<?> ingest(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty.");
        }
        try {
            int chunks = etlService.ingestFile(file);
            return ResponseEntity.ok("File ingested successfully. Chunks created: " + chunks);
        } catch (IllegalArgumentException bad) {
            return ResponseEntity.badRequest().body(bad.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ingestion failed. Please try again.");
        }
    }
}

