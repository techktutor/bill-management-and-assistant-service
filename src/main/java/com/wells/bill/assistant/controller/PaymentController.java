package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.CaptureRequest;
import com.wells.bill.assistant.entity.CaptureResponse;
import com.wells.bill.assistant.entity.CreatePaymentRequest;
import com.wells.bill.assistant.entity.CreatePaymentResponse;
import com.wells.bill.assistant.service.PaymentsToolAdapter;
import com.wells.bill.assistant.service.PaymentServiceIntegration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentsToolAdapter paymentsToolAdapter;
    private final PaymentServiceIntegration paymentServiceIntegration;

    @PostMapping("/pay")
    public String pay(@RequestParam String billId, @RequestParam double amount) {
        return paymentsToolAdapter.payBill(billId, amount, "", "", "");
    }

    @PostMapping("/schedule")
    public String schedule(@RequestParam String billId, @RequestParam double amount, @RequestParam String date) {
        return paymentsToolAdapter.schedulePayment(billId, amount, date, "", "", "");
    }

    @PostMapping
    public ResponseEntity<CreatePaymentResponse> create(@RequestBody CreatePaymentRequest req,
                                                        @RequestHeader(value = "Idempotency-Key", required = false) String idemp) {
        var resp = paymentServiceIntegration.createPayment(req, idemp);
        return ResponseEntity.ok(resp);
    }


    @PostMapping("/{id}/capture")
    public ResponseEntity<CaptureResponse> capture(@PathVariable("id") UUID id,
                                                   @RequestBody CaptureRequest req,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idemp) {
        var resp = paymentServiceIntegration.capture(id, req.getAmount(), idemp);
        return ResponseEntity.accepted().body(resp);
    }
}