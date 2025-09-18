package com.loopers.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 로컬 개발 편의를 위한 샘플 데이터 시더.
 * - 이미 데이터가 있으면 건너뜀(멱등)
 * - brand(1), product(101,102,103), product_metrics(2025-09-01..10 일부) upsert
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class SeedDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public SeedDataInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // brand upsert
            jdbcTemplate.update("""
                INSERT INTO brand (id, name, created_at, updated_at)
                VALUES (1, 'DemoBrand', NOW(), NOW())
                ON DUPLICATE KEY UPDATE name=VALUES(name), updated_at=VALUES(updated_at)
            """);
        } catch (Exception e) {
            log.warn("Brand seeding skipped (table may not exist yet).", e);
        }

        try {
            // product upsert
            jdbcTemplate.update("""
                INSERT INTO product (id, name, value, quantity, likes_count, brand_id, created_at, updated_at)
                VALUES (101, 'Demo Product 101', 10000, 100, 0, 1, NOW(), NOW()),
                       (102, 'Demo Product 102', 20000, 200, 0, 1, NOW(), NOW()),
                       (103, 'Demo Product 103', 30000, 300, 0, 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE name=VALUES(name), value=VALUES(value), quantity=VALUES(quantity), brand_id=VALUES(brand_id), updated_at=VALUES(updated_at)
            """);
        } catch (Exception e) {
            log.warn("Product seeding skipped (table may not exist yet).", e);
        }
        try {
            // product_metrics upsert 샘플
            // - 월간: 9/1~9/30 일부
            // - 주간(2025-09-08~2025-09-14) 데이터 추가로 주간 집계도 항상 생성되도록 보강
            jdbcTemplate.update("""
                INSERT INTO product_metrics (product_id, metric_date, likes_count, likes_change, sales_count, sales_change, views_count, views_change, created_at, updated_at)
                VALUES
                  (101, '2025-09-01', 10, 10,  3, 3,  100, 100, NOW(), NOW()),
                  (101, '2025-09-02', 20, 10,  8, 5,  250, 150, NOW(), NOW()),
                  (101, '2025-09-05', 35, 15, 20, 12, 800, 550, NOW(), NOW()),
                  -- 주간 범위(9/8~9/14) 추가
                  (101, '2025-09-09', 42,  7, 25, 5, 1000, 200, NOW(), NOW()),
                  (101, '2025-09-13', 55, 13, 32, 7, 1300, 300, NOW(), NOW()),
                  (102, '2025-09-01',  5,  5,  1, 1,   50,  50, NOW(), NOW()),
                  (102, '2025-09-03', 15, 10,  4, 3,  150, 100, NOW(), NOW()),
                  (102, '2025-09-07', 28, 13, 12, 8,  500, 350, NOW(), NOW()),
                  -- 주간 범위(9/8~9/14) 추가
                  (102, '2025-09-11', 35,  7, 18, 6,  700, 200, NOW(), NOW()),
                  (102, '2025-09-12', 45, 10, 24, 6,  900, 200, NOW(), NOW()),
                  (103, '2025-09-02',  8,  8,  2, 2,  120, 120, NOW(), NOW()),
                  (103, '2025-09-06', 18, 10,  6, 4,  300, 180, NOW(), NOW()),
                  (103, '2025-09-10', 30, 12, 10, 4,  650, 350, NOW(), NOW()),
                  -- 월말 샘플(월간 검증 보강)
                  (103, '2025-09-30', 40, 10, 15, 5,  900, 250, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                  likes_count=VALUES(likes_count),
                  likes_change=VALUES(likes_change),
                  sales_count=VALUES(sales_count),
                  sales_change=VALUES(sales_change),
                  views_count=VALUES(views_count),
                  views_change=VALUES(views_change),
                  updated_at=VALUES(updated_at)
            """);

            log.info("Seed data inserted/updated successfully.");
        } catch (Exception e) {
            log.warn("product_metrics seeding failed (table may not exist yet).", e);
        }
    }
}
