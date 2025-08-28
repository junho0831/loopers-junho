package com.loopers.infrastructure.payment;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgClientErrorDecoder implements ErrorDecoder {
    private static final Logger log = LoggerFactory.getLogger(PgClientErrorDecoder.class);
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.warn("PG 요청 실패: method={}, status={}, reason={}", 
                methodKey, response.status(), response.reason());

        return switch (response.status()) {
            case 400 -> new IllegalArgumentException("잘못된 결제 요청: " + response.reason());
            case 401 -> new IllegalStateException("PG 인증 실패: " + response.reason());
            case 429 -> new IllegalStateException("PG 요청 한도 초과: " + response.reason());
            case 500 -> new RuntimeException("PG 서버 오류: " + response.reason());
            case 503 -> new RuntimeException("PG 서버 일시 정지: " + response.reason());
            default -> defaultErrorDecoder.decode(methodKey, response);
        };
    }
}