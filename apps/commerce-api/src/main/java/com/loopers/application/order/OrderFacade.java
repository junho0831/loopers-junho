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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    public OrderFacade(ProductService productService,
                       PointService pointService, OrderService orderService, CouponService couponService,
                       PaymentService paymentService, PaymentGateway paymentGateway) {
        this.productService = productService;
        this.pointService = pointService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.paymentService = paymentService;
        this.paymentGateway = paymentGateway;
    }

    /**
     * 쿠폰을 포함한 주문 생성 (새로운 메서드)
     */
    public Order createOrderWithCoupon(String userId, OrderRequest orderRequest) {
        return createOrderInternal(userId, orderRequest.getItems(), orderRequest.getCouponId());
    }

    /**
     * 기존 호환성을 위한 메서드
     */
    public Order createOrder(String userId, List<OrderItemRequest> itemRequests) {
        return createOrderInternal(userId, itemRequests, null);
    }

    /**
     * 실제 주문 처리 로직 - 쿠폰 포함
     */
    private Order createOrderInternal(String userId, List<OrderItemRequest> itemRequests, Long couponId) {
        UserCoupon userCoupon = null;
        
        try {
            // 1. 상품 로드 및 재고 검증
            List<Product> products = productService.loadProductsWithLock(itemRequests);
            orderService.validateProductsStock(products, itemRequests);

            // 2. 도메인 서비스를 통한 총 주문 금액 계산
            Money originalAmount = orderService.calculateTotalAmount(products, itemRequests);
            Money finalAmount = originalAmount;

            // 3. 쿠폰 처리 (있는 경우)
            if (couponId != null) {
                userCoupon = couponService.loadAndValidateUserCoupon(couponId, userId, originalAmount);
                finalAmount = couponService.applyCouponDiscount(userCoupon.getCoupon(), originalAmount);
            }

            // 4. 사용자 포인트 로드 및 검증 (할인 적용된 금액으로)
            Point userPoints = pointService.loadUserPointsWithLock(userId);
            pointService.validateUserPoints(userPoints, finalAmount);

            // 5. 재고 차감 및 포인트 차감
            productService.decreaseStock(products, itemRequests);
            pointService.deductPoints(userPoints, finalAmount);
            
            // 6. 주문 생성 및 저장
            List<OrderItem> orderItems = orderService.createOrderItems(products, itemRequests);
            Order newOrder = new Order(userId, orderItems, finalAmount);
            Order savedOrder = orderService.saveOrder(newOrder);

            // 7. 결제 생성 및 PG 호출
            try {
                Payment payment = paymentService.createPayment(
                    savedOrder.getId().toString(), 
                    Long.parseLong(userId), 
                    BigDecimal.valueOf(finalAmount.getValue()), 
                    "SAMSUNG", // 기본 카드타입 (실제로는 요청에서 받아야 함)
                    "1234-5678-9012-3456" // 기본 카드번호 (실제로는 요청에서 받아야 함)
                );
                paymentService.savePayment(payment);
                
                // PG 결제 요청
                PaymentRequest paymentRequest = new PaymentRequest(
                    savedOrder.getId().toString(),
                    "SAMSUNG",
                    "1234-5678-9012-3456", 
                    BigDecimal.valueOf(finalAmount.getValue()),
                    "http://localhost:8080/api/v1/payments/callback"
                );
                
                PaymentResponse pgResponse = paymentGateway.requestPayment(paymentRequest);
                log.info("PG 결제 요청 완료 - orderId: {}, success: {}", savedOrder.getId(), pgResponse.isSuccess());
                
                if (pgResponse.isSuccess() && pgResponse.getTransactionId() != null) {
                    payment.updateTransactionId(pgResponse.getTransactionId());
                    paymentService.savePayment(payment);
                }
                
            } catch (Exception e) {
                log.error("PG 결제 요청 실패 - orderId: {}", savedOrder.getId(), e);
                // PG 호출 실패해도 주문은 PENDING 상태로 유지 (스케줄러가 나중에 처리)
            }

            // 8. 쿠폰 사용 처리 (주문 성공 후)
            if (userCoupon != null) {
                couponService.useCoupon(userCoupon, savedOrder.getId());
            }

            return savedOrder;
            
        } catch (Exception e) {
            // 주문 실패 시 쿠폰 사용 취소
            if (userCoupon != null) {
                couponService.cancelCouponUsage(userCoupon);
            }
            throw e;
        }
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderService.getUserOrders(userId);
    }


}
