package com.loopers.domain.ranking;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis ZSET 기반 랭킹 시스템의 키 관리 클래스
 * 일간 랭킹을 위한 키 생성 및 시간의 양자화 담당
 */
public class RankingKey {
    
    private static final String RANKING_PREFIX = "ranking";
    private static final String ALL_PRODUCTS = "all";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final String keyType;
    private final String date;
    
    public RankingKey(String keyType, LocalDate date) {
        this.keyType = keyType;
        this.date = date.format(DATE_FORMAT);
    }
    
    public RankingKey(String keyType, String date) {
        this.keyType = keyType;
        this.date = date;
    }
    
    /**
     * 전체 상품 일간 랭킹 키 생성
     */
    public static RankingKey dailyAll(LocalDate date) {
        return new RankingKey(ALL_PRODUCTS, date);
    }
    
    /**
     * 오늘 전체 상품 랭킹 키 생성
     */
    public static RankingKey todayAll() {
        return dailyAll(LocalDate.now());
    }
    
    /**
     * 어제 전체 상품 랭킹 키 생성
     */
    public static RankingKey yesterdayAll() {
        return dailyAll(LocalDate.now().minusDays(1));
    }
    
    /**
     * Redis 키 문자열 반환
     * 형식: ranking:{keyType}:{yyyyMMdd}
     */
    public String getKey() {
        return String.format("%s:%s:%s", RANKING_PREFIX, keyType, date);
    }
    
    /**
     * 다음 날 키 생성
     */
    public RankingKey nextDay() {
        LocalDate currentDate = LocalDate.parse(date, DATE_FORMAT);
        return new RankingKey(keyType, currentDate.plusDays(1));
    }
    
    /**
     * 이전 날 키 생성
     */
    public RankingKey previousDay() {
        LocalDate currentDate = LocalDate.parse(date, DATE_FORMAT);
        return new RankingKey(keyType, currentDate.minusDays(1));
    }
    
    public String getKeyType() {
        return keyType;
    }
    
    public String getDate() {
        return date;
    }
    
    public LocalDate getDateAsLocalDate() {
        return LocalDate.parse(date, DATE_FORMAT);
    }
    
    @Override
    public String toString() {
        return getKey();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RankingKey that = (RankingKey) o;
        return keyType.equals(that.keyType) && date.equals(that.date);
    }
    
    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}