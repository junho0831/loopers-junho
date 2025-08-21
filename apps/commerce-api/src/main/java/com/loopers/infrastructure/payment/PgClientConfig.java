package com.loopers.infrastructure.payment;

import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PgClientConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                2000,  // connectTimeout (2초)
                5000   // readTimeout (5초)
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new PgClientErrorDecoder();
    }
}