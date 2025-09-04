package com.loopers.domain.order;

import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.point.Point;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void validateProductsStock(List<Product> products, List<OrderItemRequest> itemRequests) {
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            
            if (!product.hasEnoughStock(request.getQuantity())) {
                throw new CoreException(ErrorType.INSUFFICIENT_STOCK);
            }
        }
    }

    public Money calculateTotalAmount(List<Product> products, List<OrderItemRequest> itemRequests) {
        Money totalAmount = Money.ZERO;
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            totalAmount = totalAmount.add(product.calculateTotalPrice(request.getQuantity()));
        }
        return totalAmount;
    }

    public List<OrderItem> createOrderItems(List<Product> products, List<OrderItemRequest> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            orderItems.add(new OrderItem(request.getProductId(), request.getQuantity(), product.getPrice()));
        }
        return orderItems;
    }

    public void validateUserPoints(Point userPoints, Money totalAmount) {
        if (!userPoints.hasEnoughPoints(totalAmount)) {
            throw new IllegalArgumentException("Insufficient points to place order.");
        }
    }

    public void processStockAndPointsDeduction(List<Product> products, List<OrderItemRequest> itemRequests, Point userPoints, Money totalAmount) {
        // 재고 차감
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            OrderItemRequest request = itemRequests.get(i);
            product.decreaseStock(request.getQuantity());
        }

        // 포인트 차감
        userPoints.use(totalAmount);
    }
    
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }
    
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
    
    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId);
    }
}