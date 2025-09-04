package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Money;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "`order`")
public class Order extends BaseEntity {
    private String userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Embedded
    private Money totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;


    protected Order() {
    }

    public Order(String userId, List<OrderItem> orderItems, Money totalAmount) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;

        // OrderItem에 Order 참조 설정
        orderItems.forEach(item -> item.setOrder(this));
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
        recalculateTotalAmount();
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void completePayment() {
        if (this.status == OrderStatus.PENDING) {
            this.status = OrderStatus.COMPLETED;
        }
    }

    public void failPayment() {
        // 결제 실패 시 PAYMENT_FAILED 상태로 변경
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    private void recalculateTotalAmount() {
        this.totalAmount = orderItems.stream()
                .map(OrderItem::calculateTotalPrice)
                .reduce(Money.ZERO, Money::add);
    }

    public boolean isCompleted() {
        return this.status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public String getUserId() {
        return this.userId;
    }

    public Money getTotalAmount() {
        return this.totalAmount;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public List<OrderItem> getOrderItems() {
        return this.orderItems;
    }

    public ZonedDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    /**
     * pg-simulator에서 요구하는 6자리 이상의 주문 ID를 생성합니다.
     * 형식: ORD + 6자리 숫자 (예: ORD000001)
     */
    public String getOrderIdForPayment() {
        return String.format("ORD%06d", this.getId());
    }
}
