-- 🏪 루프팩 BE L2 - 테스트 데이터 생성 스크립트
-- 결제시스템 및 ApplicationEvent 테스트를 위한 기본 데이터 생성
-- 
-- ⚠️  사용법: 
-- docker exec -i docker-mysql-1 mysql -u application -papplication -D loopers < test-data-setup.sql
--
-- 또는 MySQL 클라이언트에서:
-- source /path/to/test-data-setup.sql;

-- 데이터 삭제 (초기화 - 필요시 주석 해제)
-- DELETE FROM user_coupon;
-- DELETE FROM coupon;  
-- DELETE FROM product_like;
-- DELETE FROM point;
-- DELETE FROM payment;
-- DELETE FROM order_item;
-- DELETE FROM `order`;
-- DELETE FROM product;
-- DELETE FROM brand;
-- DELETE FROM user;

-- 1. 브랜드 데이터
INSERT INTO brand (id, name, created_at, updated_at) 
VALUES 
    (1, '삼성', NOW(), NOW()),
    (2, '애플', NOW(), NOW()),
    (3, 'LG', NOW(), NOW()),
    (4, '소니', NOW(), NOW()),
    (5, '나이키', NOW(), NOW()),
    (6, '닌텐도', NOW(), NOW()),
    (7, '델', NOW(), NOW()),
    (8, 'HP', NOW(), NOW()),
    (9, '캐논', NOW(), NOW()),
    (10, '아디다스', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    name = VALUES(name), 
    updated_at = VALUES(updated_at);

-- 2. 상품 데이터 (더 다양하게 추가)
INSERT INTO product (id, name, `value`, quantity, brand_id, likes_count, created_at, updated_at)
VALUES 
    (1, '갤럭시 S24', 1200000, 100, 1, 0, NOW(), NOW()),
    (2, '갤럭시 S24 Ultra', 1500000, 50, 1, 5, NOW(), NOW()),
    (3, '갤럭시 Z플립5', 1100000, 30, 1, 8, NOW(), NOW()),
    (4, '아이폰 15', 1300000, 80, 2, 12, NOW(), NOW()),
    (5, '아이폰 15 Pro', 1600000, 30, 2, 8, NOW(), NOW()),
    (6, '아이폰 15 Pro Max', 1800000, 20, 2, 15, NOW(), NOW()),
    (7, 'LG 그램', 800000, 60, 3, 3, NOW(), NOW()),
    (8, 'LG OLED TV', 2500000, 25, 3, 7, NOW(), NOW()),
    (9, '소니 헤드폰', 300000, 200, 4, 15, NOW(), NOW()),
    (10, '소니 카메라', 1800000, 40, 4, 6, NOW(), NOW()),
    (11, '에어맥스', 150000, 150, 5, 20, NOW(), NOW()),
    (12, '나이키 조던', 200000, 80, 5, 25, NOW(), NOW()),
    (13, '닌텐도 스위치', 350000, 100, 6, 30, NOW(), NOW()),
    (14, '델 모니터', 400000, 75, 7, 4, NOW(), NOW()),
    (15, 'HP 프린터', 250000, 60, 8, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    name = VALUES(name), 
    `value` = VALUES(`value`), 
    quantity = VALUES(quantity),
    updated_at = VALUES(updated_at);

-- 3. 테스트 사용자 데이터
INSERT INTO user (user_id, gender, `value`, amount, birth_date, created_at, updated_at)
VALUES 
    ('test-user-1', 'MALE', 'testuser1@example.com', 1000, '1990-01-01', NOW(), NOW()),
    ('test-user-2', 'FEMALE', 'testuser2@example.com', 2000, '1995-05-15', NOW(), NOW()),
    ('test-user-3', 'MALE', 'testuser3@example.com', 1500, '1988-12-25', NOW(), NOW()),
    ('test-user-4', 'FEMALE', 'testuser4@example.com', 3000, '1992-08-30', NOW(), NOW()),
    ('test-user-5', 'MALE', 'testuser5@example.com', 2500, '1987-11-12', NOW(), NOW()),
    ('user1', 'MALE', 'user1@example.com', 1000, '1992-03-10', NOW(), NOW()),
    ('admin', 'MALE', 'admin@example.com', 5000, '1985-07-20', NOW(), NOW()),
    ('guest', 'FEMALE', 'guest@example.com', 500, '1998-02-14', NOW(), NOW()),
    ('power-user', 'MALE', 'poweruser@example.com', 10000, '1980-06-25', NOW(), NOW()),
    ('vip', 'FEMALE', 'vip@example.com', 20000, '1975-09-15', NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    `value` = VALUES(`value`), 
    amount = VALUES(amount),
    updated_at = VALUES(updated_at);

-- 4. 포인트 계정 데이터 (모든 사용자에게 포인트 부여)
INSERT INTO point (user_id, point_balance)
VALUES 
    ('test-user-1', 5000000.00),    -- 5백만 포인트
    ('test-user-2', 3000000.00),    -- 3백만 포인트  
    ('test-user-3', 1000000.00),    -- 1백만 포인트
    ('test-user-4', 2000000.00),    -- 2백만 포인트
    ('test-user-5', 1500000.00),    -- 1.5백만 포인트
    ('user1', 500000.00),           -- 50만 포인트
    ('admin', 10000000.00),         -- 1천만 포인트
    ('guest', 100000.00),           -- 10만 포인트
    ('power-user', 8000000.00),     -- 8백만 포인트
    ('vip', 15000000.00)            -- 1.5천만 포인트
ON DUPLICATE KEY UPDATE 
    point_balance = VALUES(point_balance);

-- 5. 쿠폰 마스터 데이터
INSERT INTO coupon (id, name, coupon_type, discount_value, min_order_amount, max_discount_amount, created_at, updated_at)
VALUES 
    (1, '신규가입 10만원 할인', 'FIXED_AMOUNT', 100000.00, 0.00, 100000.00, NOW(), NOW()),
    (2, '생일축하 5만원 할인', 'FIXED_AMOUNT', 50000.00, 0.00, 50000.00, NOW(), NOW()),
    (3, '특별할인 20만원', 'FIXED_AMOUNT', 200000.00, 500000.00, 200000.00, NOW(), NOW()),
    (4, '관리자 특별할인', 'FIXED_AMOUNT', 500000.00, 0.00, 500000.00, NOW(), NOW()),
    (5, '10% 할인쿠폰', 'PERCENTAGE', 10.00, 100000.00, 50000.00, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    coupon_type = VALUES(coupon_type),
    discount_value = VALUES(discount_value),
    updated_at = VALUES(updated_at);

-- 6. 사용자 쿠폰 데이터 (UserCoupon) - 실제 테이블 구조에 맞게 수정
INSERT INTO user_coupon (id, user_id, coupon_id, used, order_id, created_at, updated_at)
VALUES 
    (1, 'test-user-1', 1, 0, null, NOW(), NOW()),
    (2, 'test-user-1', 2, 0, null, NOW(), NOW()),
    (3, 'test-user-2', 1, 0, null, NOW(), NOW()),
    (4, 'test-user-3', 3, 0, null, NOW(), NOW()),
    (5, 'admin', 4, 0, null, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    coupon_id = VALUES(coupon_id),
    updated_at = VALUES(updated_at);

-- 7. 좋아요 테스트 데이터 (더 다양하게 추가)
INSERT INTO product_like (id, user_id, product_id, created_at, updated_at)
VALUES 
    (1, 'test-user-1', 1, NOW(), NOW()),
    (2, 'test-user-1', 4, NOW(), NOW()),
    (3, 'test-user-1', 9, NOW(), NOW()),
    (4, 'test-user-2', 2, NOW(), NOW()),
    (5, 'test-user-2', 6, NOW(), NOW()),
    (6, 'test-user-2', 11, NOW(), NOW()),
    (7, 'test-user-3', 7, NOW(), NOW()),
    (8, 'test-user-3', 13, NOW(), NOW()),
    (9, 'test-user-4', 5, NOW(), NOW()),
    (10, 'test-user-4', 10, NOW(), NOW()),
    (11, 'test-user-5', 8, NOW(), NOW()),
    (12, 'test-user-5', 12, NOW(), NOW()),
    (13, 'power-user', 3, NOW(), NOW()),
    (14, 'power-user', 14, NOW(), NOW()),
    (15, 'vip', 15, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    updated_at = VALUES(updated_at);

-- 8. 테스트용 샘플 주문 데이터 (선택사항 - 필요시 주석 해제)
-- INSERT INTO `order` (id, user_id, status, total_amount, created_at, updated_at)
-- VALUES 
--     (100, 'test-user-1', 'COMPLETED', 1200000, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
--     (101, 'test-user-2', 'PENDING', 1500000, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
--     (102, 'test-user-3', 'CANCELLED', 800000, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
-- ON DUPLICATE KEY UPDATE 
--     status = VALUES(status),
--     updated_at = VALUES(updated_at);

-- 실행 완료 메시지
SELECT '🎉 테스트 데이터 생성 완료!' AS message;
SELECT '============================================' AS `separator`;

-- 데이터 확인 쿼리들
SELECT '=== 📊 생성된 데이터 요약 ===' AS info;
SELECT 
    (SELECT COUNT(*) FROM brand) as brands,
    (SELECT COUNT(*) FROM product) as products,
    (SELECT COUNT(*) FROM user) as users,
    (SELECT COUNT(*) FROM point) as point_accounts,
    (SELECT COUNT(*) FROM coupon) as coupons,
    (SELECT COUNT(*) FROM user_coupon) as user_coupons,
    (SELECT COUNT(*) FROM product_like) as likes;

SELECT '=== 🏷️ 브랜드 데이터 ===' AS info;
SELECT id, name, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') as created FROM brand ORDER BY id LIMIT 10;

SELECT '=== 📱 상품 데이터 (TOP 10) ===' AS info;
SELECT id, name, FORMAT(`value`, 0) as price, quantity, brand_id, likes_count 
FROM product ORDER BY id LIMIT 10;

SELECT '=== 👥 사용자 데이터 ===' AS info;  
SELECT id, user_id, gender, `value` as email, amount as embedded_point 
FROM user ORDER BY id LIMIT 10;

SELECT '=== 💰 포인트 계정 ===' AS info;
SELECT user_id, FORMAT(point_balance, 0) as balance FROM point ORDER BY point_balance DESC LIMIT 10;

SELECT '=== 🎫 쿠폰 마스터 ===' AS info;
SELECT id, name, coupon_type, FORMAT(discount_value, 0) as discount, FORMAT(min_order_amount, 0) as min_order 
FROM coupon ORDER BY id LIMIT 10;

SELECT '=== 🎟️ 사용자 쿠폰 ===' AS info;
SELECT id, user_id, coupon_id, used, order_id FROM user_coupon ORDER BY id LIMIT 10;

SELECT '=== ❤️ 좋아요 통계 ===' AS info;
SELECT 
    p.name as product_name,
    COUNT(*) as like_count
FROM product_like pl 
JOIN product p ON pl.product_id = p.id 
GROUP BY pl.product_id, p.name 
ORDER BY like_count DESC 
LIMIT 5;

SELECT '=== ✅ 테스트 준비 완료 ===' AS status;
SELECT 'API 테스트를 시작할 수 있습니다!' AS message;
SELECT '예시: POST /api/v1/orders (X-USER-ID: test-user-1)' AS example;