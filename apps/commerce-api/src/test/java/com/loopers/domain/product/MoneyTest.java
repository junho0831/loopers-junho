package com.loopers.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("양의 금액으로 Money를 생성할 수 있다")
    void createMoneyWithPositiveAmount() {
        // given & when
        Money money = new Money(1000);

        // then
        assertThat(money.getValue()).isEqualTo(1000);
    }

    @Test
    @DisplayName("ZERO 상수를 사용할 수 있다")
    void createZeroMoney() {
        // given & when
        Money zeroMoney = Money.ZERO;

        // then
        assertThat(zeroMoney.getValue()).isEqualTo(0);
        assertThat(zeroMoney.isZero()).isTrue();
    }

    @Test
    @DisplayName("음수 금액으로 생성하면 예외가 발생한다")
    void createMoneyWithNegativeAmount() {
        // when & then
        assertThatThrownBy(() -> new Money(-1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Money cannot be negative");
    }

    @Test
    @DisplayName("수량을 곱해서 총 금액을 계산할 수 있다")
    void multiplyByQuantity() {
        // given
        Money unitPrice = new Money(1000);

        // when
        Money totalPrice = unitPrice.multiply(3);

        // then
        assertThat(totalPrice.getValue()).isEqualTo(3000);
    }

    @Test
    @DisplayName("음수 수량으로 곱하면 예외가 발생한다")
    void multiplyByNegativeQuantity() {
        // given
        Money unitPrice = new Money(1000);

        // when & then
        assertThatThrownBy(() -> unitPrice.multiply(-3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Multiplier cannot be negative");
    }

    @Test
    @DisplayName("두 Money를 더할 수 있다")
    void addMoney() {
        // given
        Money money1 = new Money(1000);
        Money money2 = new Money(2000);

        // when
        Money result = money1.add(money2);

        // then
        assertThat(result.getValue()).isEqualTo(3000);
    }

    @Test
    @DisplayName("두 Money를 뺄 수 있다")
    void subtractMoney() {
        // given
        Money money1 = new Money(3000);
        Money money2 = new Money(1000);

        // when
        Money result = money1.subtract(money2);

        // then
        assertThat(result.getValue()).isEqualTo(2000);
    }

    @Test
    @DisplayName("더 큰 금액을 빼면 예외가 발생한다")
    void subtractLargerMoney() {
        // given
        Money money1 = new Money(1000);
        Money money2 = new Money(2000);

        // when & then
        assertThatThrownBy(() -> money1.subtract(money2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot subtract more money than available");
    }

    @Test
    @DisplayName("비교 메서드들이 정상 동작한다")
    void comparisonMethods() {
        // given
        Money money1 = new Money(1000);
        Money money2 = new Money(2000);
        Money money3 = new Money(1000);

        // when & then
        assertThat(money1.isGreaterThan(money2)).isFalse();
        assertThat(money2.isGreaterThan(money1)).isTrue();
        assertThat(money1.isLessThan(money2)).isTrue();
        assertThat(money2.isLessThan(money1)).isFalse();
        assertThat(money1.isLessThanOrEqualTo(money3)).isTrue();
    }

    @Test
    @DisplayName("동일한 금액의 Money 객체는 같다고 판단된다")
    void equalsMoney() {
        // given
        Money money1 = new Money(1000);
        Money money2 = new Money(1000);
        Money money3 = new Money(2000);

        // when & then
        assertThat(money1).isEqualTo(money2);
        assertThat(money1).isNotEqualTo(money3);
        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }
}