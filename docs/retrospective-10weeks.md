# Loopers 백엔드 부트캠프 10주 회고 (코드 기반)

## 목차
- [TL;DR](#tldr)
- [전체 여정 요약: 흐름이 연결되며 쌓인 경험들](#전체-여정-요약-흐름이-연결되며-쌓인-경험들)
- [가장 큰 전환점: "설계를 위한 설계"의 가치 발견](#가장-큰-전환점-설계를-위한-설계의-가치-발견)
- [나의 Trade-off 판단: 현실과 이상 사이의 균형](#나의-trade-off-판단-현실과-이상-사이의-균형)
  - [1) Service vs Facade, 어디서 검증할 것인가?](#1-service-vs-facade-어디서-검증할-것인가)
  - [2) 비관적 락 vs 낙관적 락](#2-비관적-락-vs-낙관적-락)
  - [3) Circuit Breaker 적용 범위](#3-circuit-breaker-적용-범위)
- [실전과의 연결: 실무에서 써먹을 포인트](#실전과의-연결-실무에서-써먹을-포인트)
- [앞으로의 학습 방향: 더 깊이 있는 고민을 위해](#앞으로의-학습-방향-더-깊이-있는-고민을-위해)
- [마무리: 완벽하진 않지만 성장한 10주](#마무리-완벽하진-않지만-성장한-10주)

---

## TL;DR
- 멀티 앱/모듈 분리로 역할 명확화: `apps/commerce-api`(동기 API/캐시/도메인), `apps/commerce-streamer`(Kafka 컨슘/랭킹 집계/멱등성), `apps/commerce-batch`(주간·월간 배치/머티리얼라이즈드 뷰). 공통 인프라는 `modules/kafka`, `modules/jpa`로 재사용.
- 이벤트-카프카-레디스 파이프라인: Spring ApplicationEvent → `@TransactionalEventListener(AFTER_COMMIT)` → Kafka → Streamer에서 배치 컨슘+멱등 처리 → Redis ZSET 실시간 랭킹.
- 운영 내성: Resilience4j CircuitBreaker/Retry를 외부 PG 경계에만 적용(`FeignPaymentGateway`), PENDING 결제 복구 스케줄러 운영.
- 일관성과 동시성: 민감 도메인에는 비관적 락(`PESSIMISTIC_WRITE`) 적용, 컨슈머 측 `EventHandled` 테이블로 멱등성 보장.
- 배치 처리: `weeklyMonthlyRankingJob`으로 주간/월간 집계 → `mv_product_rank_*` 업데이트(머티리얼라이즈드 뷰).
- 테스트 흐름: 도메인 단위 테스트, 통합/경합 테스트, HTTP·K6 부하 테스트로 E2E 감각 확보.

---

## 전체 여정 요약: 흐름이 연결되며 쌓인 경험들
10주를 돌아보면 각 주차가 단절된 기술 습득이 아니라 하나의 설계 흐름으로 이어졌습니다.

- 1–3주차: 테스트 전략과 설계 감각
  - 테스트 종류와 책임 경계(E2E/통합/단위), 테스트가 설계를 이끄는 루프 구축
  - 도메인 모델링(값 객체, 불변성, 예외 정책)과 피드백 속도 최적화
  - 관측 가능한 품질지표(SLA/성능 기준) 수립과 도구화
- 4–5주차: 데이터 접근 성능과 캐싱 설계
  - 읽기/쓰기 경로 분리, 캐시 적중 전략과 키·TTL 정책 수립
  - 인덱스/조인/페이지네이션 설계, 조회 비용 예측과 측정
- 6–7주차: 이벤트 중심 설계와 경계 정의
  - 트랜잭션 경계 뒤 이벤트 발행, 강정합 vs 최종일관성 판단 기준 정립
  - 이벤트 품질 속성(순서, 중복, 재시도)과 설계 원칙 수립
- 8–9주차: 메시징 운영과 내결함성
  - 소비자 스루풋/안전성 균형(배치 수신, 수동 ack, 동시성) 설계
  - 정확히 한 번처럼 보이게 만들기(멱등키, 재시도, DLQ, 재처리 흐름)
  - 파티션 키와 키스페이스 설계, 백프레셔와 컨슈머 래그 대응
- 10주차: 오프라인 집계와 실시간 조합
  - 실시간 스트림 집계와 배치 집계의 역할 구분과 결합 전략
  - 콜드 스타트/TTL 전략, 머티리얼라이즈드 뷰 설계와 갱신 정책

---

## 가장 큰 전환점: "설계를 위한 설계"의 가치 발견
처음엔 “이벤트가 좋다면 전부 이벤트로”라고 생각했지만, 실제 적용에서 중복/순서/부분 실패를 만났습니다. 본 코드에서는 다음처럼 현실적 설계를 채택했습니다.

- 트랜잭션 뒤에서만 발행: API에서 도메인 이벤트 발행 → 커밋 이후 카프카 전송
  ```java
  // apps/commerce-api/.../KafkaEventHandler.java
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleProductLikeEvent(ProductLikeEvent event) {
      kafkaEventPublisher.publishCatalogEvent(catalogEvent);
  }
  ```
- 컨슈머 책임 강화: 배치 수신·수동 ack·재시도·DLQ·멱등 레코드로 운영 이슈 흡수
  ```java
  // apps/commerce-streamer/.../RankingConsumer.java
  @KafkaListener(topics = {"catalog-events", "order-events"},
                 containerFactory = KafkaConfig.BATCH_LISTENER,
                 groupId = "ranking-consumer-group")
  @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000L, multiplier = 2.0))
  @Transactional
  public void handleRankingEvents(List<ConsumerRecord<String, String>> messages,
                                  Acknowledgment ack) {
      boolean processed = idempotentEventService.processEventIdempotent(
          eventId, EventHandled.ConsumerType.RANKING, version, () -> { /* ... */ }
      );
      // ... 배치 업데이트 후 ack.acknowledge()
  }
  ```
- 모듈 분리로 의도 표현: API(동기/경계/캐시) ↔ Streamer(비동기/집계) ↔ Batch(오프라인 집계) 분리로 설계 목적이 코드에 드러남.

---

## 나의 Trade-off 판단: 현실과 이상 사이의 균형

### 1) Service vs Facade, 어디서 검증할 것인가?
- 선택: 검증·조회는 Service, 흐름 조합·추적/캐시/이벤트 발행은 Facade.
  ```java
  // apps/commerce-api/.../ProductService.java
  public void validateProductCreation(String name, long price, int stock) { /* ... */ }

  // apps/commerce-api/.../ProductFacade.java
  public Product createProduct(String name, long price, int stock, Long brandId) {
      productService.validateProductCreation(name, price, stock);
      // ... 조합 + 이벤트 발행
  }
  ```
- 이유: 재사용성과 테스트 용이성, Facade 단순화, “검증된 도메인” 상위 전달.
- 회고: 팀 컨벤션에 따라 달라질 여지가 있으나 현재 분리가 의도를 잘 담음.

### 2) 비관적 락 vs 낙관적 락
- 선택: 비관적 락(`PESSIMISTIC_WRITE`).
  ```java
  // apps/commerce-api/.../JpaProductRepository.java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id = :id")
  Optional<Product> findByIdWithLock(@Param("id") Long id);
  ```
- 이유: 재고/포인트/집계의 강한 정합성 우선, @Version 도입보다 단순.
- 회고: 트래픽 급증 시 낙관적 락 + 재시도 전환 고려. 현재는 경합 테스트로 안전성 확인.

### 3) Circuit Breaker 적용 범위
- 선택: 외부 경계(Feign PG)에서만 적용.
  ```java
  // apps/commerce-api/.../FeignPaymentGateway.java
  @CircuitBreaker(name = "pgClient", fallbackMethod = "requestPaymentFallback")
  @Retry(name = "pgClient", fallbackMethod = "requestPaymentFallback")
  public PaymentResponse requestPayment(PaymentRequest request) { /* ... */ }
  ```
- 보완: `PaymentRecoveryScheduler`가 5분마다 PENDING 결제 자동 복구.
- 이유: 장애 지점 명확화, 비즈니스 레이어 단순화, 디버깅 용이성.

---

## 실전과의 연결: 실무에서 써먹을 포인트
- “간소화된 아웃박스” 접근
  - 도메인 이벤트 → `@TransactionalEventListener(AFTER_COMMIT)` → `KafkaTemplate` 발행.
  - 컨슈머 측에서 멱등/재시도/DLQ로 최소 일관성 달성. 요구 시 RDB Outbox로 확장 여지.
  ```java
  // apps/commerce-api/.../KafkaEventPublisher.java
  kafkaTemplate.send(ORDER_EVENTS_TOPIC, partitionKey, eventJson)
               .whenComplete((result, ex) -> { /* 로깅/관찰 */ });
  ```
- 멱등성 템플릿
  - unique(eventId, consumerType) 제약의 `EventHandled` + 서비스 래퍼.
  ```java
  // apps/commerce-streamer/.../IdempotentEventService.java
  public boolean processEventIdempotent(String eventId, ConsumerType type,
                                        Long version, Runnable processor) {
      if (!markEventAsHandled(eventId, type, version)) return false;
      processor.run(); return true;
  }
  ```
- Kafka 컨슈머 운영 팁
  - 배치 리스너/수동 ack/동시성 설정으로 처리량·재처리 균형(`modules/kafka/KafkaConfig.kt`).
  - `@RetryableTopic`로 단계적 재시도, 최종 실패는 `@DltHandler`에 위임.
- Redis ZSET 랭킹 운영
  - 실시간 업데이트는 Streamer 배치 처리(파이프라인), 조회는 API에서 `reverseRangeWithScores`/`reverseRank`.
  - 콜드 스타트는 ZUNIONSTORE 가중치 이월 + TTL로 완화.
  ```java
  // apps/commerce-api/.../RankingService.java
  redisTemplate.execute((RedisCallback<Object>) conn -> {
      conn.execute("ZUNIONSTORE", dest, "1", src, "WEIGHTS", weight);
      conn.expire(dest, TTL.getSeconds());
      return null;
  });
  ```
- Spring Batch 집계
  - `weeklyMonthlyRankingJob`: JDBC Cursor Reader → 순위 산정 Processor → JDBC Batch Writer.
  - 실행 전 대상 파티션 삭제(listener)로 “치환” 보장. 조회는 MV 인덱스 활용.

---

## 주차별 핵심 코드

### 1–3주차: 기초·테스트 루틴 확립
도메인 단위 테스트, 통합/경합 테스트, E2E/성능 시나리오로 테스트 전략을 정립했습니다.

```java
// apps/commerce-api/src/test/java/com/loopers/domain/product/StockTest.java
@Test
@DisplayName("재고를 정상적으로 차감할 수 있다")
void decreaseStock() {
    Stock stock = new Stock(10);
    Stock decreased = stock.decrease(3);
    assertThat(decreased.getQuantity()).isEqualTo(7);
    assertThat(stock.getQuantity()).isEqualTo(10);
}
```

```java
// apps/commerce-api/src/test/java/com/loopers/application/order/OrderServiceIntegrationTest.java
@Test
@DisplayName("정상적인 주문 생성에 성공할 경우, Order 객체를 반환한다")
void createOrder_WithValidRequest_ReturnsOrder() {
    when(productService.loadProductsWithLock(itemRequests)).thenReturn(List.of(p1, p2));
    when(pointService.loadUserPointsWithLock(userId)).thenReturn(userPoint);
    when(orderService.calculateTotalAmount(any(), any())).thenReturn(new Money(40000));
    when(orderService.saveOrder(any(Order.class))).thenReturn(mockOrder);
    Order result = orderFacade.createOrder(userId, itemRequests);
    assertThat(result).isNotNull();
}
```

```http
## payment-system-test.http (발췌)
POST {{commerce_base_url}}/api/v1/orders
Content-Type: application/json
X-USER-ID: {{user_id}}
{
  "items": [{ "productId": 1, "quantity": 1 }]
}
```

```javascript
// k6-tests/product-performance-test.js (발췌)
export const options = {
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
    'product_list_duration': ['p(90)<200'],
  },
};
```

### 4–5주차: 성능과 데이터 설계
캐시 경계와 키/TTL/무효화 정책을 일원화하고, 조회 경로를 최적화했습니다.

```java
// apps/commerce-api/src/main/java/com/loopers/application/product/ProductCacheService.java
public void cacheProductDetail(Long productId, ProductDetailResponse detail) {
    redisTemplate.opsForValue().set("product:detail:" + productId, detail, Duration.ofMinutes(10));
}
public void evictProductCaches(Long productId) {
    redisTemplate.delete("product:detail:" + productId);
    redisTemplate.delete("product:popular");
    redisTemplate.delete(redisTemplate.keys("product:list:*"));
}
```

```java
// apps/commerce-api/src/main/java/com/loopers/infrastructure/product/JpaProductRepository.java
@Query("SELECT p FROM Product p JOIN FETCH p.brand WHERE p.id = :id")
Optional<Product> findByIdWithBrand(@Param("id") Long id);
```

### 6–7주차: 이벤트 기반 전환
트랜잭션 뒤 이벤트 발행, 동기 처리 구간(좋아요/캐시 무효화)과 비동기 분리를 명확히 했습니다.

```java
// apps/commerce-api/src/main/java/com/loopers/application/product/ProductFacade.java
private void publishUserActionEvent(UserActionEvent event) {
    try { eventPublisher.publishEvent(event); } catch (Exception ignored) {}
}
```

```java
// apps/commerce-api/src/main/java/com/loopers/application/like/LikeEventHandler.java
@EventListener
public void handleProductLikeEvent(ProductLikeEvent event) {
    updateProductLikeCount(event); // PESSIMISTIC_WRITE로 안전 갱신
    invalidateProductCaches(event.getProductId()); // 비동기 캐시 무효화
}
```

```java
// apps/commerce-api/src/main/java/com/loopers/application/event/KafkaEventHandler.java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handlePaymentResultEvent(PaymentResultEvent event) {
    kafkaEventPublisher.publishOrderEvent(OrderEvent.paymentProcessed(/*...*/));
}
```

### 8–9주차: 메시징/내결함성
컨슈머 배치 수신/수동 ack/동시성, 멱등 처리, 재시도와 DLQ를 구성했습니다.

```kotlin
// modules/kafka/src/main/kotlin/com/loopers/config/kafka/KafkaConfig.kt
@Bean(BATCH_LISTENER)
fun defaultBatchListenerContainerFactory(...): ConcurrentKafkaListenerContainerFactory<*, *> =
  ConcurrentKafkaListenerContainerFactory<Any, Any>().apply {
    consumerFactory = DefaultKafkaConsumerFactory(consumerConfig)
    containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
    setConcurrency(3)
    isBatchListener = true
  }
```

```java
// apps/commerce-streamer/src/main/java/com/loopers/consumer/RankingConsumer.java
@KafkaListener(topics = {"catalog-events", "order-events"}, containerFactory = KafkaConfig.BATCH_LISTENER)
@RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000L, multiplier = 2.0))
public void handleRankingEvents(List<ConsumerRecord<String, String>> messages, Acknowledgment ack) { /* ... */ }
```

```java
// apps/commerce-streamer/src/main/java/com/loopers/service/IdempotentEventService.java
@Transactional
public boolean processEventIdempotent(String eventId, ConsumerType type, Long version, Runnable work) { /* ... */ }
```

### 10주차: 대용량 배치/집계
주간/월간 집계 잡과 MV 저장소를 구성했습니다.

```java
// apps/commerce-batch/src/main/java/com/loopers/batch/ranking/RankingAggregationJobConfig.java
@Bean
public Job weeklyMonthlyRankingJob(JobRepository repo, Step weekly, Step monthly) {
  return new JobBuilder("weeklyMonthlyRankingJob", repo).start(weekly).next(monthly).build();
}
```

```java
// modules/jpa/src/main/java/com/loopers/domain/ranking/MvProductRankWeekly.java
@Table(name = "mv_product_rank_weekly",
  indexes = {@Index(name = "idx_mvrank_week", columnList = "`year_week`"),
             @Index(name = "idx_mvrank_week_rank", columnList = "`year_week`, `rank_no`")})
class MvProductRankWeekly { /* ... */ }
```


## 앞으로의 학습 방향: 더 깊이 있는 고민을 위해
1) 개인화/추천
- 현재는 전 사용자 공용 랭킹. 사용자/세그먼트별 키 전략, 피처 스토어, 오프라인 학습–온라인 서빙 파이프라인 설계가 다음 과제.

2) 복잡한 배치/재시작/튜닝
- 청크/커밋 간격/Failover/재시작 포인트, 대규모 데이터 파티셔닝(Job Parameter) 실전 튜닝 필요.

3) 운영 관찰성
- Kafka lag/처리량, DLQ 비율, Resilience4j 지표, Redis 히트율, 배치 성공률·처리시간을 Micrometer/Prometheus/Grafana로 대시보드화.
- 비즈니스 지표(전환/품질)와 시스템 지표 연계를 통한 빠른 피드백 루프.

---

## 마무리: 완벽하진 않지만 성장한 10주
가장 큰 변화는 “어떻게”에서 “왜”로의 관점 전환이었습니다.

- 왜 Service에서 검증하고 Facade는 조합에 집중했는가? → 재사용성과 책임 분리.
- 왜 비관적 락을 선택했는가? → 도메인 단순성과 강한 정합성 우선.
- 왜 이벤트 기반으로 분리했는가? → 확장성과 장애 격리를 고려.
- 왜(완전한) 아웃박스 대신 간소 브리지를 썼는가? → 현재 요구에 맞춘 현실적 일관성, 추후 확장 여지.

남은 과제(개인화, 대규모 트래픽 실측, 완전한 Outbox/Saga, 운영 지표 체계화)도 분명하지만, “왜 그렇게 했는가”를 설명할 수 있는 근거는 생겼습니다. 앞으로도 실제 사용자 가치에 닿는 설계를 계속 실험하고 개선하겠습니다.

---

부록: 폴더/핵심 파일 맵 (요약)
- API: `apps/commerce-api`
  - 이벤트 브리지: `application/event/KafkaEventHandler.java`, `infrastructure/event/KafkaEventPublisher.java`
  - 도메인/락: `infrastructure/product/JpaProductRepository.java`, `domain/*`
  - 캐시: `application/product/ProductCacheService.java`, `config/RedisConfig.java`
  - 회복: `infrastructure/payment/PaymentRecoveryScheduler.java`, `infrastructure/payment/FeignPaymentGateway.java`
- Streamer: `apps/commerce-streamer`
  - 컨슈머: `consumer/*`(배치 리스너/Retry/DLT)
  - 랭킹 집계: `service/RankingService.java`, `service/RankingCarryOverScheduler.java`
  - 멱등성: `domain/event/EventHandled.java`, `service/IdempotentEventService.java`
- Batch: `apps/commerce-batch`
  - 잡 구성: `batch/ranking/RankingAggregationJobConfig.java`
  - MV 엔티티: `modules/jpa/domain/ranking/MvProductRankWeekly.java`, `MvProductRankMonthly.java`
- 공통 Kafka: `modules/kafka/config/kafka/KafkaConfig.kt` (배치 리스너/수동 ack/동시성)
