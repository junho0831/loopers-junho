package com.loopers.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.loopers.infrastructure.payment")
public class FeignConfig {
}