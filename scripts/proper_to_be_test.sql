-- 진짜 TO-BE 테스트: 최적화된 인덱스 생성 후 성능 측정
-- 전체 실행시간: ~0.149초
USE loopers;

SELECT '=== 진짜 TO-BE 테스트: 최적화된 인덱스 생성 후 성능 측정 ===' as status;

-- 1. 인덱스 확인 (이미 생성된 상태 가정)
SELECT '1. 인덱스 확인 (이미 생성된 상태)' as step;

-- 2. 인덱스 생성 확인
SELECT '2. 인덱스 생성 확인' as step;
SHOW INDEX FROM product;

-- 3. 최적화된 인덱스로 성능 측정
SELECT '3. 최적화된 인덱스 성능 측정' as step;

-- UC1: 브랜드 필터 + 좋아요순 정렬 (idx_brand_likes 사용 예상) - 실행시간: 1.012ms
SELECT 'UC1 테스트: 브랜드 필터 + 좋아요순 정렬 (With Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product WHERE brand_id = 1 ORDER BY likes_count DESC LIMIT 10;
SELECT 
    'TO-BE UC1 (With Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- UC2: 전체 좋아요순 정렬 (idx_likes_count 사용 예상) - 실행시간: 0.194ms
SELECT 'UC2 테스트: 전체 좋아요순 정렬 (With Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product ORDER BY likes_count DESC LIMIT 20;
SELECT 
    'TO-BE UC2 (With Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- UC3: 브랜드 필터 + 가격순 정렬 (idx_brand_price 사용 예상) - 실행시간: 0.276ms
SELECT 'UC3 테스트: 브랜드 필터 + 가격순 정렬 (With Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product WHERE brand_id = 2 ORDER BY value ASC LIMIT 15;
SELECT 
    'TO-BE UC3 (With Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- UC4: 가격 범위 필터링 (idx_price 사용 예상) - 실행시간: 0.504ms
SELECT 'UC4 테스트: 가격 범위 필터링 (With Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product WHERE value BETWEEN 100000 AND 300000 ORDER BY value ASC LIMIT 10;
SELECT 
    'TO-BE UC4 (With Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- 4. EXPLAIN 분석 (인덱스 사용 확인)
SELECT '4. 최적화된 인덱스 EXPLAIN 분석' as step;

SELECT 'UC1 EXPLAIN (With Index)' as explain_case;
EXPLAIN FORMAT=JSON 
SELECT * FROM product WHERE brand_id = 1 ORDER BY likes_count DESC LIMIT 10;

SELECT 'UC2 EXPLAIN (With Index)' as explain_case;
EXPLAIN FORMAT=JSON
SELECT * FROM product ORDER BY likes_count DESC LIMIT 20;

SELECT 'UC3 EXPLAIN (With Index)' as explain_case;
EXPLAIN FORMAT=JSON
SELECT * FROM product WHERE brand_id = 2 ORDER BY value ASC LIMIT 15;

SELECT 'UC4 EXPLAIN (With Index)' as explain_case;
EXPLAIN FORMAT=JSON
SELECT * FROM product WHERE value BETWEEN 100000 AND 300000 ORDER BY value ASC LIMIT 10;

-- 5. 인덱스 효율성 분석
SELECT '5. 인덱스 효율성 분석' as step;
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    CASE 
        WHEN CARDINALITY > 3000 THEN 'EXCELLENT'
        WHEN CARDINALITY > 1000 THEN 'GOOD'
        WHEN CARDINALITY > 100 THEN 'FAIR'
        ELSE 'POOR'
    END as efficiency_rating
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'loopers' 
  AND TABLE_NAME = 'product' 
  AND INDEX_NAME != 'PRIMARY'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT '===  TO-BE 성능 측정 완료 (With Optimized Index) ===' as status;