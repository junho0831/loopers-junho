package com.loopers.domain.example;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

@Entity
@Table(name = "points")
public class Point extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private int amount;

    public Point(String userId, int amount) {
        validate(userId, amount);
        this.userId = userId;
        this.amount = amount;
    }

    protected Point() {}

    private void validate(String userId, int amount) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.INVALID_USER_ID);
        }
        if (amount < 0) {
            throw new CoreException(ErrorType.INVALID_CHARGE_AMOUNT);
        }
    }

    public void charge(int chargeAmount) {
        if (chargeAmount < 0) {
            throw new CoreException(ErrorType.INVALID_CHARGE_AMOUNT);
        }
        this.amount += chargeAmount;
    }

    public void use(int useAmount) {
        if (useAmount < 0) {
            throw new CoreException(ErrorType.INVALID_USE_AMOUNT);
        }
        if (this.amount < useAmount) {
            throw new CoreException(ErrorType.INSUFFICIENT_POINTS);
        }
        this.amount -= useAmount;
    }


    public String getUserId() {
        return userId;
    }

    public int getAmount() {
        return amount;
    }
}