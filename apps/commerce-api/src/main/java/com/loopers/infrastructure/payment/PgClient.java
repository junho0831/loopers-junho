package com.loopers.infrastructure.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "pgClient", url = "${pg.client.url:http://localhost:8082}", configuration = PgClientConfig.class)
public interface PgClient {

    @PostMapping("/api/v1/payments")
    PgSimulatorResponse requestPayment(@RequestHeader("X-USER-ID") String userId,
                                       @RequestBody PgPaymentRequest request);

    @GetMapping("/api/v1/payments/{transactionId}")
    PgStatusResponse getPaymentStatus(@RequestHeader("X-USER-ID") String userId,
                                      @PathVariable String transactionId);

    @GetMapping("/api/v1/payments")
    PgStatusResponse getPaymentByOrderId(@RequestHeader("X-USER-ID") String userId,
                                         @RequestParam String orderId);

    record PgPaymentRequest(
            String orderId,
            String cardType,
            String cardNo,
            BigDecimal amount,
            String callbackUrl
    ) {}

    record PgSimulatorResponse(
            boolean success,
            String message,
            Data data
    ) {
        record Data(String transactionId) {}
    }

    record PgStatusResponse(
            boolean success,
            String message,
            StatusData data
    ) {
        record StatusData(
                String transactionId,
                String orderId,
                String status,
                BigDecimal amount,
                String cardType
        ) {}
    }
}