package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("정상적인 정보로 상품을 생성할 수 있다")
    void createProduct() {
        // given
        String name = "테스트 상품";
        Money price = new Money(10000);
        Stock stock = new Stock(100);
        Brand brand = new Brand("테스트 브랜드");

        // when
        Product product = new Product(name, price, stock, brand);

        // then
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getStock()).isEqualTo(stock);
        assertThat(product.getBrand()).isEqualTo(brand);
        assertThat(product.getLikesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품명이 null이면 예외가 발생한다")
    void createProductWithNullName() {
        // given
        Money price = new Money(10000);
        Stock stock = new Stock(100);
        Brand brand = new Brand("테스트 브랜드");

        // when & then
        assertThatThrownBy(() -> new Product(null, price, stock, brand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name cannot be null or empty");
    }

    @Test
    @DisplayName("상품명이 빈 문자열이면 예외가 발생한다")
    void createProductWithBlankName() {
        // given
        Money price = new Money(10000);
        Stock stock = new Stock(100);
        Brand brand = new Brand("테스트 브랜드");

        // when & then
        assertThatThrownBy(() -> new Product("", price, stock, brand))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name cannot be null or empty");
    }

    @Test
    @DisplayName("브랜드가 null이면 예외가 발생한다")
    void createProductWithNullBrand() {
        // given
        String name = "테스트 상품";
        Money price = new Money(10000);
        Stock stock = new Stock(100);

        // when & then
        assertThatThrownBy(() -> new Product(name, price, stock, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product brand cannot be null");
    }

    @Test
    @DisplayName("재고를 차감할 수 있다")
    void decreaseStock() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(10), brand);

        // when
        product.decreaseStock(3);

        // then
        assertThat(product.getStock().getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("재고가 부족하면 차감시 예외가 발생한다")
    void decreaseStockWithInsufficientStock() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(5), brand);

        // when & then
        assertThatThrownBy(() -> product.decreaseStock(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("재고 가용성을 확인할 수 있다")
    void hasEnoughStock() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(10), brand);

        // when & then
        assertThat(product.hasEnoughStock(5)).isTrue();
        assertThat(product.hasEnoughStock(10)).isTrue();
        assertThat(product.hasEnoughStock(15)).isFalse();
    }

    @Test
    @DisplayName("좋아요 수를 증가시킬 수 있다")
    void incrementLikeCount() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(10), brand);

        // when
        product.incrementLikesCount();

        // then
        assertThat(product.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 수를 감소시킬 수 있다")
    void decrementLikeCount() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(10), brand);
        product.incrementLikesCount();
        product.incrementLikesCount();

        // when
        product.decrementLikesCount();

        // then
        assertThat(product.getLikesCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 수가 0일 때 감소시켜도 음수가 되지 않는다")
    void decrementLikeCountWhenZero() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(10), brand);

        // when
        product.decrementLikesCount();

        // then
        assertThat(product.getLikesCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("수량에 따른 총 가격을 계산할 수 있다")
    void calculateTotalPrice() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(1000), new Stock(10), brand);

        // when
        Money totalPrice = product.calculateTotalPrice(3);

        // then
        assertThat(totalPrice.getValue()).isEqualTo(3000);
    }

    @Test
    @DisplayName("브랜드 ID를 가져올 수 있다")
    void getBrandId() {
        // given
        Brand brand = new Brand("테스트 브랜드");
        Product product = new Product("테스트 상품", new Money(10000), new Stock(10), brand);

        // when & then
        assertThat(product.getBrandId()).isEqualTo(brand.getId());
    }
}
