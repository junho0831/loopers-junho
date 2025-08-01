package com.loopers.domain.order;

import com.loopers.domain.point.Point;
import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.order.JpaOrderRepository;
import com.loopers.infrastructure.point.JpaPointRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class OrderService {
    private final JpaOrderRepository orderRepository;
    private final JpaProductRepository productRepository;
    private final JpaPointRepository pointRepository;

    public OrderService(JpaOrderRepository orderRepository, JpaProductRepository productRepository,
            JpaPointRepository pointRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.pointRepository = pointRepository;
    }

    public Order createOrder(String userId, List<OrderItemRequest> itemRequests) {
        // 1. 상품 존재 여부 및 재고 확인
        List<Product> products = validateProductsAndStock(itemRequests);

        // 2. 총 주문 금액 계산
        Money totalAmount = calculateTotalAmount(products, itemRequests);

        // 3. 사용자 포인트 확인
        Point userPoints = validateUserPoints(userId, totalAmount);

        // 4. 재고 차감 및 포인트 차감
        decreaseStockAndPoints(products, itemRequests, userPoints, totalAmount);

        // 5. 주문 생성
        List<OrderItem> orderItems = createOrderItems(products, itemRequests);
        Order newOrder = new Order(userId, orderItems, totalAmount);

        return orderRepository.save(newOrder);
    }

    private List<Product> validateProductsAndStock(List<OrderItemRequest> itemRequests) {
        List<Product> products = new ArrayList<>();

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

            if (!product.hasEnoughStock(itemRequest.getQuantity())) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            products.add(product);
        }

        return products;
    }

    private Money calculateTotalAmount(List<Product> products, List<OrderItemRequest> itemRequests) {
        Money totalAmount = Money.ZERO;

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            totalAmount = totalAmount.add(product.calculateTotalPrice(request.getQuantity()));
        }

        return totalAmount;
    }

    private Point validateUserPoints(String userId, Money totalAmount) {
        Point userPoints = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User has no points account."));

        if (!userPoints.hasEnoughPoints(totalAmount)) {
            throw new IllegalArgumentException("Insufficient points to place order.");
        }

        return userPoints;
    }

    private void decreaseStockAndPoints(List<Product> products, List<OrderItemRequest> itemRequests, Point userPoints,
            Money totalAmount) {
        // 재고 차감
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            product.decreaseStock(request.getQuantity());
            productRepository.save(product);
        }

        // 포인트 차감
        userPoints.use(totalAmount);
        pointRepository.save(userPoints);
    }

    private List<OrderItem> createOrderItems(List<Product> products, List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            orderItems.add(new OrderItem(request.getProductId(), request.getQuantity(), product.getPrice()));
        }

        return orderItems;
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }
}