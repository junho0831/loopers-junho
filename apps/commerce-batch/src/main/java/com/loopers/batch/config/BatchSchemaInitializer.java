package com.loopers.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 로컬 실행 시 Spring Batch 메타테이블(BATCH_*)이 없을 경우 자동 생성한다.
 * schema-mysql.sql을 실행하며, 이미 존재해도 continueOnError로 무시한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BatchSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BatchSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public BatchSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'BATCH_JOB_INSTANCE'",
                    Integer.class
            );
            if (exists != null && exists > 0) {
                log.debug("Spring Batch meta tables found.");
                return;
            }
            log.info("Spring Batch meta tables not found. Initializing schema...");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new org.springframework.core.io.ClassPathResource("org/springframework/batch/core/schema-mysql.sql")
            );
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("Spring Batch meta schema initialized.");
        } catch (Exception e) {
            log.warn("Batch meta schema initialization encountered an error (will continue): {}", e.getMessage());
        }
    }
}
