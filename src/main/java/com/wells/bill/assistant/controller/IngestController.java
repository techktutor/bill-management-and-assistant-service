package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.service.RagEtlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final RagEtlService etlService;

    @PostMapping("/file")
    public ResponseEntity<String> ingest(@RequestParam("file") MultipartFile file, @RequestParam(value = "source", required = false) String source) {
        try {
            etlService.ingestFile(file, source == null ? file.getOriginalFilename() : source);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error: " + e.getMessage());
        }
    }
}
