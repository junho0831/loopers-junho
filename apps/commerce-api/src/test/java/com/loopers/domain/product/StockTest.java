package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StockTest {

    @Test
    @DisplayName("재고를 정상적으로 생성할 수 있다")
    void createStock() {
        // given & when
        Stock stock = new Stock(10);

        // then
        assertThat(stock.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("음수 재고로 생성하면 예외가 발생한다")
    void createStockWithNegativeQuantity() {
        // when & then
        assertThatThrownBy(() -> new Stock(-1))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("재고를 정상적으로 차감할 수 있다")
    void decreaseStock() {
        // given
        Stock stock = new Stock(10);

        // when
        Stock decreasedStock = stock.decrease(3);

        // then
        assertThat(decreasedStock.getQuantity()).isEqualTo(7);
        assertThat(stock.getQuantity()).isEqualTo(10); // 원본은 변경되지 않음
    }

    @Test
    @DisplayName("재고가 부족하면 차감시 예외가 발생한다")
    void decreaseStockWithInsufficientQuantity() {
        // given
        Stock stock = new Stock(5);

        // when & then
        assertThatThrownBy(() -> stock.decrease(10))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("음수 수량으로 차감하면 예외가 발생한다")
    void decreaseStockWithNegativeAmount() {
        // given
        Stock stock = new Stock(10);

        // when & then
        assertThatThrownBy(() -> stock.decrease(-1))
                .isInstanceOf(CoreException.class);
    }

    @Test
    @DisplayName("0으로 차감하면 새로운 Stock이 반환된다")
    void decreaseStockWithZeroAmount() {
        // given
        Stock stock = new Stock(10);

        // when
        Stock decreasedStock = stock.decrease(0);

        // then
        assertThat(decreasedStock.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("동일한 수량의 Stock 객체는 같다고 판단된다")
    void equalsStock() {
        // given
        Stock stock1 = new Stock(10);
        Stock stock2 = new Stock(10);
        Stock stock3 = new Stock(5);

        // when & then
        assertThat(stock1).isEqualTo(stock2);
        assertThat(stock1).isNotEqualTo(stock3);
        assertThat(stock1.hashCode()).isEqualTo(stock2.hashCode());
    }
}