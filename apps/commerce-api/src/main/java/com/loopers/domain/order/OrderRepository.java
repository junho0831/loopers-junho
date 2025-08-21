package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findByUserId(String userId);
    Order save(Order order);
    Optional<Order> findById(Long id);
}