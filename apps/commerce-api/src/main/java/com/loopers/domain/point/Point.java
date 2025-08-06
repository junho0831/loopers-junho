package com.loopers.domain.point;

import com.loopers.domain.product.Money;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "point", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "userId" })
})
public class Point {
    @Id
    private String userId;
    private BigDecimal pointBalance;

    protected Point() {
    }

    public Point(String userId, BigDecimal pointBalance) {
        validateUserId(userId);
        validatePointBalance(pointBalance);

        this.userId = userId;
        this.pointBalance = pointBalance;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }

    private void validatePointBalance(BigDecimal pointBalance) {
        if (pointBalance == null) {
            throw new IllegalArgumentException("Point balance cannot be null");
        }
        if (pointBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Point balance cannot be negative");
        }
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getPointBalance() {
        return pointBalance;
    }

    public void addPoints(BigDecimal points) {
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Points to add must be positive");
        }
        this.pointBalance = this.pointBalance.add(points);
    }

    public void deductPoints(BigDecimal points) {
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Points to deduct must be positive");
        }
        if (this.pointBalance.compareTo(points) < 0) {
            throw new IllegalArgumentException("Insufficient points");
        }
        this.pointBalance = this.pointBalance.subtract(points);
    }

    public boolean hasEnoughPoints(Money money) {
        BigDecimal moneyValue = BigDecimal.valueOf(money.getValue());
        return this.pointBalance.compareTo(moneyValue) >= 0;
    }

    public void use(Money money) {
        BigDecimal moneyValue = BigDecimal.valueOf(money.getValue());
        deductPoints(moneyValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Point point = (Point) o;
        return Objects.equals(userId, point.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
