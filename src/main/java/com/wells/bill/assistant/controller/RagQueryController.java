// ============================
// Optimized RagQueryController
// ============================
package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.service.RagEngineService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagQueryController {

    private static final Logger log = LoggerFactory.getLogger(RagQueryController.class);

    private final RagEngineService ragEngineService;

    @GetMapping("/answerBillQuery")
    public ResponseEntity<?> answer(@RequestParam String billId, @RequestParam String question) {

        log.info("RAG query request: billId={}", billId);

        if (billId == null || billId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "billId is required"
            ));
        }
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "question is required"
            ));
        }

        String response = ragEngineService.answerQuestionForBill(billId, question);

        return ResponseEntity.ok(Map.of(
                "billId", billId,
                "question", question,
                "answer", response
        ));
    }
}
