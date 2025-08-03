package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {
    Page<Product> findByBrandId(Long brandId, Pageable pageable);
    Page<Product> findAllOrderBy(ProductSortType sortType, Pageable pageable);
}