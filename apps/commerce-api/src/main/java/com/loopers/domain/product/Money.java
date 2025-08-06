package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Money {
    public static final Money ZERO = new Money(0);

    private long value;

    protected Money() {
        // JPA를 위한 기본 생성자
    }

    public Money(long value) {
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0원 이상이어야 합니다");
        }
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public Money add(Money other) {
        return new Money(this.value + other.value);
    }

    public Money subtract(Money other) {
        if (this.value < other.value) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 금액이 보유 금액보다 큽니다");
        }
        return new Money(this.value - other.value);
    }

    public Money multiply(int multiplier) {
        if (multiplier < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "곱하기 값은 0 이상이어야 합니다");
        }
        return new Money(this.value * multiplier);
    }

    public boolean isZero() {
        return this.value == 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.value > other.value;
    }

    public boolean isLessThan(Money other) {
        return this.value < other.value;
    }

    public boolean isLessThanOrEqualTo(Money other) {
        return this.value <= other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Money money = (Money) o;
        return value == money.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}