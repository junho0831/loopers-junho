package com.loopers.application.order;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.*;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.payment.*;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
    import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderFacade {
    private static final Logger log = LoggerFactory.getLogger(OrderFacade.class);
    
    private final ProductService productService;
    private final PointService pointService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderFacade(ProductService productService,
                       PointService pointService, OrderService orderService, CouponService couponService,
                       ApplicationEventPublisher eventPublisher) {
        this.productService = productService;
        this.pointService = pointService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 쿠폰을 포함한 주문 생성 (새로운 메서드)
     */
    public Order createOrderWithCoupon(String userId, OrderRequest orderRequest) {
        return createOrderInternal(userId, orderRequest.getItems(), orderRequest.getCouponId(), 
            orderRequest.getCardCompany(), orderRequest.getCardNumber());
    }

    /**
     * 사용자 행동 추적을 포함한 주문 생성 (카드 정보 포함)
     */
    public Order createOrder(String userId, List<OrderItemRequest> itemRequests, 
                           String sessionId, String userAgent, String ipAddress,
                           String cardCompany, String cardNumber) {
        Order order = createOrderInternal(userId, itemRequests, null, cardCompany, cardNumber);
        
        // 사용자 행동 추적 이벤트 발행
        publishUserActionEvent(UserActionEvent.orderCreated(
            userId, order.getId(), sessionId, userAgent, ipAddress));
        
        return order;
    }

    /**
     * 사용자 행동 추적을 포함한 주문 생성 (기본 카드 정보)
     */
    public Order createOrder(String userId, List<OrderItemRequest> itemRequests, 
                           String sessionId, String userAgent, String ipAddress) {
        return createOrder(userId, itemRequests, sessionId, userAgent, ipAddress, "SAMSUNG", "1234-5678-9012-3456");
    }

    /**
     * 기존 호환성을 위한 메서드 - 기본 카드 정보 사용
     */
    public Order createOrder(String userId, List<OrderItemRequest> itemRequests) {
        return createOrderInternal(userId, itemRequests, null, "SAMSUNG", "1234-5678-9012-3456");
    }

    /**
     * 실제 주문 처리 로직 - 이벤트 기반으로 트랜잭션 분리
     */
    private Order createOrderInternal(String userId, List<OrderItemRequest> itemRequests, Long couponId, 
                                    String cardCompany, String cardNumber) {
        // 1. 상품 로드 및 재고 검증
        List<Product> products = productService.loadProductsWithLock(itemRequests);
        orderService.validateProductsStock(products, itemRequests);

        // 2. 도메인 서비스를 통한 총 주문 금액 계산
        Money originalAmount = orderService.calculateTotalAmount(products, itemRequests);
        Money finalAmount = originalAmount;

        // 3. 쿠폰 유효성 검증만 수행 (실제 사용은 이벤트에서)
        if (couponId != null) {
            UserCoupon userCoupon = couponService.loadAndValidateUserCoupon(couponId, userId, originalAmount);
            finalAmount = couponService.applyCouponDiscount(userCoupon.getCoupon(), originalAmount);
        }
        // 4. 사용자 포인트 로드 및 검증, 차감
        Point userPoints = pointService.loadUserPointsWithLock(userId);
        pointService.validateUserPoints(userPoints, finalAmount);
        pointService.deductPoints(userPoints, finalAmount);

        // 5. 재고 차감
        productService.decreaseStock(products, itemRequests);
        
        // 6. 주문 생성 및 저장 (핵심 트랜잭션)
        List<OrderItem> orderItems = orderService.createOrderItems(products, itemRequests);
        Order newOrder = new Order(userId, orderItems, finalAmount);
        Order savedOrder = orderService.saveOrder(newOrder);

        // 7. 주문 생성 이벤트 발행 (커밋 후 비동기 처리)
        OrderCreatedEvent event = OrderCreatedEvent.from(savedOrder, couponId, cardCompany, cardNumber);
        eventPublisher.publishEvent(event);
        
        log.info("주문 생성 완료 및 이벤트 발행 - orderId: {}, userId: {}", savedOrder.getId(), userId);

        return savedOrder;
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderService.getUserOrders(userId);
    }

    private void publishUserActionEvent(UserActionEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            // 사용자 행동 추적 실패해도 메인 비즈니스에는 영향 없도록
            log.warn("사용자 행동 추적 이벤트 발행 실패: {}", e.getMessage());
        }
    }
}
