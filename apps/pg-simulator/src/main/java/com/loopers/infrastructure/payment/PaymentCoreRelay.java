package com.loopers.infrastructure.payment;

import com.loopers.application.payment.TransactionInfo;
import com.loopers.domain.payment.PaymentRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentCoreRelay implements PaymentRelay {
    private static final Logger logger = LoggerFactory.getLogger(PaymentCoreRelay.class);
    private static final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void notify(String callbackUrl, TransactionInfo transactionInfo) {
        try {
            restTemplate.postForEntity(callbackUrl, transactionInfo, Object.class);
        } catch (Exception e) {
            logger.error("콜백 호출을 실패했습니다. {}", e.getMessage(), e);
        }
    }
}

