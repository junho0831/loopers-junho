package com.loopers.domain.order;

import java.util.List;

public interface OrderRepository {
    List<Order> findByUserId(String userId);
}