package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    List<Order> findByUserId(@Param("userId") String userId);
}