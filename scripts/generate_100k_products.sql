-- 10만개 상품 데이터 생성 스크립트
-- 각 컬럼의 값을 다양하게 분포시켜 현실적인 테스트 데이터 생성

USE loopers;

-- 안전장치 해제
SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- 기존 데이터 삭제
DELETE FROM product_like;
DELETE FROM product;  
DELETE FROM brand;

-- 브랜드 데이터 생성 (50개)
INSERT INTO brand (name, created_at, updated_at) VALUES
('Nike', NOW(), NOW()),
('Adidas', NOW(), NOW()),
('Samsung', NOW(), NOW()),
('Apple', NOW(), NOW()),
('Sony', NOW(), NOW()),
('LG', NOW(), NOW()),
('Canon', NOW(), NOW()),
('Nikon', NOW(), NOW()),
('HP', NOW(), NOW()),
('Dell', NOW(), NOW()),
('Toyota', NOW(), NOW()),
('Honda', NOW(), NOW()),
('BMW', NOW(), NOW()),
('Mercedes', NOW(), NOW()),
('Audi', NOW(), NOW()),
('Zara', NOW(), NOW()),
('H&M', NOW(), NOW()),
('Uniqlo', NOW(), NOW()),
('Starbucks', NOW(), NOW()),
('McDonalds', NOW(), NOW()),
('Tesla', NOW(), NOW()),
('Google', NOW(), NOW()),
('Microsoft', NOW(), NOW()),
('Amazon', NOW(), NOW()),
('Netflix', NOW(), NOW()),
('Spotify', NOW(), NOW()),
('Uber', NOW(), NOW()),
('Airbnb', NOW(), NOW()),
('Coca Cola', NOW(), NOW()),
('Pepsi', NOW(), NOW()),
('KFC', NOW(), NOW()),
('Pizza Hut', NOW(), NOW()),
('Dominos', NOW(), NOW()),
('Subway', NOW(), NOW()),
('Burger King', NOW(), NOW()),
('Dunkin', NOW(), NOW()),
('Baskin Robbins', NOW(), NOW()),
('Taco Bell', NOW(), NOW()),
('Ford', NOW(), NOW()),
('Chevrolet', NOW(), NOW()),
('Nissan', NOW(), NOW()),
('Hyundai', NOW(), NOW()),
('Kia', NOW(), NOW()),
('Puma', NOW(), NOW()),
('Reebok', NOW(), NOW()),
('Under Armour', NOW(), NOW()),
('New Balance', NOW(), NOW()),
('Asics', NOW(), NOW()),
('Converse', NOW(), NOW()),
('Vans', NOW(), NOW());

-- 상품 데이터 생성 (100,000개)
-- 브랜드별로 균등 분배하여 다양성 확보

-- 1-20,000번 상품 (브랜드 1-50, 각 400개)
INSERT INTO product (name, value, quantity, likes_count, brand_id, created_at, updated_at)
SELECT 
    CONCAT(
        b.name, ' ',
        ELT(FLOOR(1 + RAND() * 15), 
            '프리미엄', '스탠다드', '베이직', '에센셜', '클래식', 
            '모던', '빈티지', '트렌디', '심플', '럭셔리', 
            '컴팩트', '무선', '스마트', '에코', '오가닉'), ' ',
        ELT(FLOOR(1 + RAND() * 12),
            '전자제품', '의류', '신발', '스포츠용품', '생활용품',
            '화장품', '도서', '음식', '가전제품', '자동차용품', '게임', '악세사리'), ' ',
        FLOOR(1 + RAND() * 999)
    ) as name,
    -- 가격 분포: 현실적인 분포
    CASE 
        WHEN RAND() < 0.4 THEN 5000 + FLOOR(RAND() * 45000)      -- 40%: 5k-50k (저가)
        WHEN RAND() < 0.7 THEN 50000 + FLOOR(RAND() * 150000)    -- 30%: 50k-200k (중가)
        WHEN RAND() < 0.9 THEN 200000 + FLOOR(RAND() * 300000)   -- 20%: 200k-500k (고가)
        ELSE 500000 + FLOOR(RAND() * 1500000)                    -- 10%: 500k-2M (프리미엄)
    END as value,
    -- 재고 분포
    CASE 
        WHEN RAND() < 0.1 THEN 0                                 -- 10%: 품절
        WHEN RAND() < 0.5 THEN 1 + FLOOR(RAND() * 50)           -- 40%: 1-50개 (소량)
        WHEN RAND() < 0.8 THEN 51 + FLOOR(RAND() * 149)         -- 30%: 51-200개 (중간)
        ELSE 201 + FLOOR(RAND() * 799)                          -- 20%: 201-1000개 (대량)
    END as quantity,
    -- 좋아요 수 분포: 파레토 분포 (80-20 법칙)
    CASE 
        WHEN RAND() < 0.6 THEN FLOOR(RAND() * 10)               -- 60%: 0-9개 (대부분)
        WHEN RAND() < 0.8 THEN 10 + FLOOR(RAND() * 40)          -- 20%: 10-49개 
        WHEN RAND() < 0.95 THEN 50 + FLOOR(RAND() * 450)        -- 15%: 50-499개 
        ELSE 500 + FLOOR(RAND() * 4500)                         -- 5%: 500-4999개 (인기)
    END as likes_count,
    ((ROW_NUMBER() OVER() - 1) % 50) + 1 as brand_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 1095) DAY) as created_at,  -- 최근 3년
    NOW() as updated_at
FROM (
    SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
    SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION
    SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION
    SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
) t1
CROSS JOIN (
    SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
    SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION
    SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION
    SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION
    SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION
    SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION
    SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION
    SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION
    SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION
    SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) t2
CROSS JOIN (
    SELECT 1 as n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION
    SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION
    SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION
    SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION
    SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25 UNION
    SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30 UNION
    SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35 UNION
    SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION
    SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION
    SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
) t3
CROSS JOIN brand b
LIMIT 100000;

-- 성능 인덱스 생성
CREATE INDEX idx_brand_likes ON product(brand_id, likes_count DESC);
CREATE INDEX idx_likes_count ON product(likes_count DESC);
CREATE INDEX idx_price ON product(value);
CREATE INDEX idx_created_at ON product(created_at);

-- 안전장치 복구
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
COMMIT;

-- 데이터 검증
SELECT '=== 데이터 생성 완료 ===' as status;

SELECT 
    COUNT(*) as total_products,
    MIN(value) as min_price,
    MAX(value) as max_price,
    ROUND(AVG(value)) as avg_price,
    MIN(likes_count) as min_likes,
    MAX(likes_count) as max_likes,
    ROUND(AVG(likes_count)) as avg_likes,
    MIN(quantity) as min_stock,
    MAX(quantity) as max_stock,
    ROUND(AVG(quantity)) as avg_stock
FROM product;

-- 브랜드별 상품 분포 확인
SELECT 
    b.name as brand_name,
    COUNT(p.id) as product_count,
    ROUND(AVG(p.value)) as avg_price,
    ROUND(AVG(p.likes_count)) as avg_likes
FROM brand b
LEFT JOIN product p ON b.id = p.brand_id
GROUP BY b.id, b.name
ORDER BY product_count DESC
LIMIT 10;

-- 가격대별 분포
SELECT 
    CASE 
        WHEN value < 50000 THEN '5만원 미만'
        WHEN value < 200000 THEN '5-20만원'
        WHEN value < 500000 THEN '20-50만원'
        ELSE '50만원 이상'
    END as price_range,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM product), 1) as percentage
FROM product
GROUP BY 
    CASE 
        WHEN value < 50000 THEN '5만원 미만'
        WHEN value < 200000 THEN '5-20만원'
        WHEN value < 500000 THEN '20-50만원'
        ELSE '50만원 이상'
    END
ORDER BY MIN(value);

-- 좋아요 수 분포
SELECT 
    CASE 
        WHEN likes_count < 10 THEN '10개 미만'
        WHEN likes_count < 50 THEN '10-49개'
        WHEN likes_count < 500 THEN '50-499개'
        ELSE '500개 이상'
    END as likes_range,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM product), 1) as percentage
FROM product
GROUP BY 
    CASE 
        WHEN likes_count < 10 THEN '10개 미만'
        WHEN likes_count < 50 THEN '10-49개'
        WHEN likes_count < 500 THEN '50-499개'
        ELSE '500개 이상'
    END
ORDER BY MIN(likes_count);

SELECT '=== 10만개 상품 데이터 생성 및 검증 완료 ===' as final_status;