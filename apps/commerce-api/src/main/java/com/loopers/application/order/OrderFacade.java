package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.order.JpaOrderRepository;
import com.loopers.infrastructure.point.JpaPointRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderFacade {
    private final JpaOrderRepository orderRepository;
    private final JpaProductRepository productRepository;
    private final JpaPointRepository pointRepository;
    private final OrderService orderService;

    public OrderFacade(JpaOrderRepository orderRepository, JpaProductRepository productRepository,
            JpaPointRepository pointRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.pointRepository = pointRepository;
        this.orderService = orderService;
    }

    public Order createOrder(String userId, List<OrderItemRequest> itemRequests) {
        // 1. 상품 존재 여부 확인
        List<Product> products = loadProducts(itemRequests);

        // 2. 도메인 서비스를 통한 재고 검증
        orderService.validateProductsStock(products, itemRequests);

        // 3. 도메인 서비스를 통한 총 주문 금액 계산
        Money totalAmount = orderService.calculateTotalAmount(products, itemRequests);

        // 4. 사용자 포인트 로드 및 검증
        Point userPoints = loadUserPoints(userId);
        orderService.validateUserPoints(userPoints, totalAmount);

        // 5. 도메인 서비스를 통한 재고 차감 및 포인트 차감
        orderService.processStockAndPointsDeduction(products, itemRequests, userPoints, totalAmount);
        saveProductsAndPoints(products, userPoints);

        // 6. 도메인 서비스를 통한 주문 아이템 생성
        List<OrderItem> orderItems = orderService.createOrderItems(products, itemRequests);
        Order newOrder = new Order(userId, orderItems, totalAmount);

        return orderRepository.save(newOrder);
    }

    private List<Product> loadProducts(List<OrderItemRequest> itemRequests) {
        List<Product> products = new ArrayList<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));
            products.add(product);
        }

        return products;
    }


    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }

    private Point loadUserPoints(String userId) {
        return pointRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User has no points account."));
    }

    private void saveProductsAndPoints(List<Product> products, Point userPoints) {
        // 재고 변경된 상품들 저장
        for (Product product : products) {
            productRepository.save(product);
        }
        
        // 포인트 변경 저장
        pointRepository.save(userPoints);
    }
}