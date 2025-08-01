package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(name = "product_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "userId", "productId" })
})
public class ProductLike extends BaseEntity {
    private String userId;
    private Long productId;

    protected ProductLike() {
    }

    public ProductLike(String userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }

    public String getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }
}