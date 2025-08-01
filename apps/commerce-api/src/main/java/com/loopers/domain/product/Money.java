package com.loopers.domain.product;

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
            throw new IllegalArgumentException("Money cannot be negative.");
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
            throw new IllegalArgumentException("Cannot subtract more money than available.");
        }
        return new Money(this.value - other.value);
    }

    public Money multiply(int multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative.");
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