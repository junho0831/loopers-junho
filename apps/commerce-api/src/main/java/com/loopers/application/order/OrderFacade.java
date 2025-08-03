package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderFacade {
    private final OrderService orderService;

    public OrderFacade(OrderService orderService) {
        this.orderService = orderService;
    }

    public Order createOrder(String userId, List<OrderItemRequest> itemRequests) {
        return orderService.createOrder(userId, itemRequests);
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    public List<Order> getUserOrders(String userId) {
        return orderService.getUserOrders(userId);
    }
}