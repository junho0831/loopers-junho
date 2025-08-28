package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentRequest;
import com.loopers.domain.payment.PaymentResponse;
import com.loopers.domain.payment.PaymentStatusResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeignPaymentGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(FeignPaymentGateway.class);
    
    private final PgClient pgClient;

    public FeignPaymentGateway(PgClient pgClient) {
        this.pgClient = pgClient;
    }

    @Override
    @CircuitBreaker(name = "pgClient", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgClient", fallbackMethod = "requestPaymentFallback")
    public PaymentResponse requestPayment(PaymentRequest request) {
        try {
            log.info("PG 결제 요청 시작: orderId={}", request.getOrderId());
            
            PgClient.PgPaymentRequest pgRequest = new PgClient.PgPaymentRequest(
                    request.getOrderId(),
                    request.getCardType(),
                    request.getCardNo(),
                    request.getAmount(),
                    request.getCallbackUrl()
            );

            PgClient.PgSimulatorResponse response = pgClient.requestPayment("135135", pgRequest);
            
            if (response.success() && response.data() != null) {
                log.info("PG 결제 요청 성공: orderId={}, transactionId={}", 
                        request.getOrderId(), response.data().transactionId());
                return new PaymentResponse(response.message(), true, response.data().transactionId());
            } else {
                log.warn("PG 결제 요청 실패: orderId={}, message={}", request.getOrderId(), response.message());
                return new PaymentResponse(response.message(), false);
            }
        } catch (Exception e) {
            log.error("PG 결제 요청 중 예외 발생: orderId={}", request.getOrderId(), e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "pgClient", fallbackMethod = "checkPaymentStatusFallback")
    @Retry(name = "pgClient", fallbackMethod = "checkPaymentStatusFallback")
    public PaymentStatusResponse checkPaymentStatus(String transactionId) {
        try {
            log.info("PG 결제 상태 확인 시작: transactionId={}", transactionId);
            
            PgClient.PgStatusResponse response = pgClient.getPaymentStatus("135135", transactionId);
            
            if (response.success() && response.data() != null) {
                PgClient.PgStatusResponse.StatusData data = response.data();
                log.info("PG 결제 상태 확인 성공: transactionId={}, status={}", 
                        transactionId, data.status());
                return new PaymentStatusResponse(data.transactionId(), data.status(), response.message(), data.orderId());
            } else {
                log.warn("PG 결제 상태 확인 실패: transactionId={}, message={}", transactionId, response.message());
                return new PaymentStatusResponse(transactionId, "UNKNOWN", response.message(), null);
            }
        } catch (Exception e) {
            log.error("PG 결제 상태 확인 중 예외 발생: transactionId={}", transactionId, e);
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "pgClient", fallbackMethod = "checkPaymentByOrderIdFallback")
    @Retry(name = "pgClient", fallbackMethod = "checkPaymentByOrderIdFallback")
    public PaymentStatusResponse checkPaymentByOrderId(String orderId) {
        try {
            log.info("PG 주문별 결제 상태 확인 시작: orderId={}", orderId);
            
            PgClient.PgStatusResponse response = pgClient.getPaymentByOrderId("135135", orderId);
            
            if (response.success() && response.data() != null) {
                PgClient.PgStatusResponse.StatusData data = response.data();
                log.info("PG 주문별 결제 상태 확인 성공: orderId={}, status={}", 
                        orderId, data.status());
                return new PaymentStatusResponse(data.transactionId(), data.status(), response.message(), data.orderId());
            } else {
                log.warn("PG 주문별 결제 상태 확인 실패: orderId={}, message={}", orderId, response.message());
                return new PaymentStatusResponse(null, "UNKNOWN", response.message(), orderId);
            }
        } catch (Exception e) {
            log.error("PG 주문별 결제 상태 확인 중 예외 발생: orderId={}", orderId, e);
            throw e;
        }
    }

    // Fallback methods
    public PaymentResponse requestPaymentFallback(PaymentRequest request, Throwable throwable) {
        log.error("PG 결제 요청 Fallback 실행: orderId={}, error={}", request.getOrderId(), throwable.getMessage());
        return new PaymentResponse("결제 서비스가 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해주세요.", false);
    }

    public PaymentStatusResponse checkPaymentStatusFallback(String transactionId, Throwable throwable) {
        log.error("PG 결제 상태 확인 Fallback 실행: transactionId={}, error={}", transactionId, throwable.getMessage());
        return new PaymentStatusResponse(transactionId, "PENDING", "결제 상태 확인이 일시적으로 불가능합니다.", null);
    }

    public PaymentStatusResponse checkPaymentByOrderIdFallback(String orderId, Throwable throwable) {
        log.error("PG 주문별 결제 상태 확인 Fallback 실행: orderId={}, error={}", orderId, throwable.getMessage());
        return new PaymentStatusResponse(null, "PENDING", "결제 상태 확인이 일시적으로 불가능합니다.", orderId);
    }
}