package com.loopers.utils;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseCleanUp implements InitializingBean {

    @PersistenceContext
    private EntityManager entityManager;

    private final List<String> tableNames = new ArrayList<>();

    @Override
    public void afterPropertiesSet() {
        entityManager.getMetamodel().getEntities().stream()
            .filter(entity -> entity.getJavaType().getAnnotation(Entity.class) != null)
            .map(entity -> {
                Table table = entity.getJavaType().getAnnotation(Table.class);
                return table != null ? table.name() : entity.getName().toLowerCase();
            })
            .forEach(tableNames::add);
    }

    @Transactional
    public void truncateAllTables() {
        entityManager.flush();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        for (String table : tableNames) {
            try {
                // MySQL 예약어 테이블명을 올바르게 처리
                String tableName = table.equals("user") ? "`user`" : table;
                entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
            } catch (Exception e) {
                // 테이블이 존재하지 않으면 무시
                System.out.println("Table " + table + " does not exist, skipping truncate");
            }
        }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }
}
