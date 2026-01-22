package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.PaymentEntity;
import com.wells.bill.assistant.gateway.PaymentGatewayClient;
import com.wells.bill.assistant.model.GatewayRequest;
import com.wells.bill.assistant.model.GatewayResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExecutorService {

    private final PaymentGatewayClient gatewayClient;

    @Transactional
    public GatewayResponse executeSinglePayment(PaymentEntity payment) {
        GatewayRequest request = GatewayRequest.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .idempotencyKey(payment.getIdempotencyKey())
                .build();

        GatewayResponse response = gatewayClient.charge(request);

        log.info("Executed payment id={}, amount={}, status={}",
                payment.getId(),
                payment.getAmount(),
                response.success() ? "SUCCESS" : "FAILURE"
        );
        return response;
    }
}
