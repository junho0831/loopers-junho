package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

@Embeddable
public class Point {

    private int amount;

    public Point(int amount) {
        validate(amount);
        this.amount = amount;
    }

    protected Point() {}

    private void validate(int amount) {
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

    public int getAmount() {
        return amount;
    }
}