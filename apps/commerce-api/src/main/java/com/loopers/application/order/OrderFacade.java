package com.loopers.application.order;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderRequest;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.infrastructure.order.JpaOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderFacade {
    private final JpaOrderRepository orderRepository;
    private final ProductService productService;
    private final PointService pointService;
    private final OrderService orderService;
    private final CouponService couponService;

    public OrderFacade(JpaOrderRepository orderRepository, ProductService productService,
            PointService pointService, OrderService orderService, CouponService couponService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.pointService = pointService;
        this.orderService = orderService;
        this.couponService = couponService;
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
            Order savedOrder = orderRepository.save(newOrder);

            // 7. 쿠폰 사용 처리 (주문 성공 후)
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
        return orderRepository.findById(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }


}
