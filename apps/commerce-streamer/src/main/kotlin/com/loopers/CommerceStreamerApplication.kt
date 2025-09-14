package com.loopers

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.TimeZone

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.loopers.repository"])
@EnableJpaAuditing
@EnableScheduling
open class CommerceStreamerApplication {

    @PostConstruct
    fun started() {
        // 타임존 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }
}

fun main(args: Array<String>) {
    runApplication<CommerceStreamerApplication>(*args)
}
