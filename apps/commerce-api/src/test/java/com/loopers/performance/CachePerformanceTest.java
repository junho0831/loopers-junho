package com.loopers.performance;

import org.junit.jupiter.api.Test;

/**
 * 캐시 성능 측정 방법 데모
 * 실제 성능 측정 방법을 보여주는 단순한 테스트
 */
public class CachePerformanceTest {

    @Test
    public void demonstratePerformanceMeasurement() {
        System.out.println("=== 성능 측정 방법 데모 ===");
        
        // 방법 1: System.currentTimeMillis() 사용 (밀리초 단위)
        long startTime = System.currentTimeMillis();
        simulateWork(100); // 100ms 작업 시뮬레이션
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        
        System.out.println("방법 1 - currentTimeMillis():");
        System.out.println("  실행시간: " + durationMs + "ms");
        
        // 방법 2: System.nanoTime() 사용 (나노초 → 밀리초 변환)
        long startNano = System.nanoTime();
        simulateWork(50); // 50ms 작업 시뮬레이션
        long endNano = System.nanoTime();
        long durationNano = (endNano - startNano) / 1_000_000; // 나노초 → 밀리초
        
        System.out.println("방법 2 - nanoTime():");
        System.out.println("  실행시간: " + durationNano + "ms");
        
        // 반복 측정을 통한 평균 계산
        int iterations = 10;
        long totalTime = 0;
        
        System.out.println("반복 측정 (10회):");
        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            simulateWork(20); // 20ms 작업
            totalTime += (System.currentTimeMillis() - start);
        }
        
        double averageTime = totalTime / (double) iterations;
        System.out.println("  총 시간: " + totalTime + "ms");
        System.out.println("  평균 시간: " + averageTime + "ms");
        
        System.out.println("성능 측정 방법 데모 완료!");
        System.out.println("실제 Redis 캐시 테스트는 통합 테스트에서 확인하세요.");
    }
    
    private void simulateWork(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
