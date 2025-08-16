-- 진짜 AS-IS 테스트: 인덱스 없는 상태에서 측정
-- 전체 실행시간: ~0.196초
USE loopers;

SELECT '=== 진짜 AS-IS 테스트: 인덱스 제거 후 성능 측정 ===' as status;

-- 1. 모든 인덱스 제거 (PRIMARY 제외)
SELECT '1. 인덱스 제거' as step;
-- Skip index dropping - assume clean state or ignore errors
-- TO-BE에서 생성한 인덱스들도 제거
-- Try to drop TO-BE indexes if they exist (ignore errors)
-- DROP INDEX idx_brand_price ON product;
-- DROP INDEX idx_price_created ON product;
-- DROP INDEX idx_brand_price_likes ON product;

-- 2. 인덱스 제거 확인
SELECT '2. 인덱스 제거 확인' as step;
SHOW INDEX FROM product WHERE Key_name != 'PRIMARY';

-- 3. 인덱스 없는 상태에서 성능 측정
SELECT '3. 인덱스 없는 상태 성능 측정' as step;

-- UC1: 브랜드 필터 + 좋아요순 정렬 (Table Scan 예상) - 실행시간: 0.952ms
SELECT 'UC1 테스트: 브랜드 필터 + 좋아요순 정렬 (No Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product WHERE brand_id = 1 ORDER BY likes_count DESC LIMIT 10;
SELECT 
    'AS-IS UC1 (No Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- UC2: 전체 좋아요순 정렬 (Full Table Scan + Sort 예상) - 실행시간: 1.298ms
SELECT 'UC2 테스트: 전체 좋아요순 정렬 (No Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product ORDER BY likes_count DESC LIMIT 20;
SELECT 
    'AS-IS UC2 (No Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- UC3: 브랜드 필터 + 가격순 정렬 (Table Scan + Sort 예상) - 실행시간: 0.226ms
SELECT 'UC3 테스트: 브랜드 필터 + 가격순 정렬 (No Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product WHERE brand_id = 2 ORDER BY value ASC LIMIT 15;
SELECT 
    'AS-IS UC3 (No Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- UC4: 가격 범위 필터링 (Table Scan 예상) - 실행시간: 1.342ms
SELECT 'UC4 테스트: 가격 범위 필터링 (No Index)' as test_case;
SET @start_time = NOW(6);
SELECT * FROM product WHERE value BETWEEN 100000 AND 300000 ORDER BY value ASC LIMIT 10;
SELECT 
    'AS-IS UC4 (No Index)' as test_type,
    TIMESTAMPDIFF(MICROSECOND, @start_time, NOW(6))/1000 as execution_time_ms;

-- 4. EXPLAIN 분석 (Table Scan 확인)
SELECT '4. 인덱스 없는 상태 EXPLAIN 분석' as step;

SELECT 'UC1 EXPLAIN (No Index)' as explain_case;
EXPLAIN FORMAT=JSON 
SELECT * FROM product WHERE brand_id = 1 ORDER BY likes_count DESC LIMIT 10;

SELECT 'UC2 EXPLAIN (No Index)' as explain_case;  
EXPLAIN FORMAT=JSON
SELECT * FROM product ORDER BY likes_count DESC LIMIT 20;

SELECT 'UC3 EXPLAIN (No Index)' as explain_case;
EXPLAIN FORMAT=JSON
SELECT * FROM product WHERE brand_id = 2 ORDER BY value ASC LIMIT 15;

SELECT 'UC4 EXPLAIN (No Index)' as explain_case;
EXPLAIN FORMAT=JSON
SELECT * FROM product WHERE value BETWEEN 100000 AND 300000 ORDER BY value ASC LIMIT 10;

SELECT '=== AS-IS 성능 측정 완료 (No Index) ===' as status;