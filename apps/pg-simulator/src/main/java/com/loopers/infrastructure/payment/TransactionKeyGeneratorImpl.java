package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.TransactionKeyGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class TransactionKeyGeneratorImpl implements TransactionKeyGenerator {
    private static final String KEY_TRANSACTION = "TR";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String generate() {
        LocalDateTime now = LocalDateTime.now();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return String.format("%s:%s:%s", DATETIME_FORMATTER.format(now), KEY_TRANSACTION, uuid);
    }
}

