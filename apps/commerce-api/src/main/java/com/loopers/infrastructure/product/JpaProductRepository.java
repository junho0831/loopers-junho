package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long>, ProductRepository {

    @Override
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId")
    Page<Product> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    default Page<Product> findAllOrderBy(ProductSortType sortType, Pageable pageable) {
        return switch (sortType) {
            case LATEST -> findAllByOrderByCreatedAtDesc(pageable);
            case PRICE_ASC -> findAllByOrderByPriceValueAsc(pageable);
            case LIKES_DESC -> findAllByOrderByLikesCountDesc(pageable);
        };
    }

    @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Product p ORDER BY p.price.value ASC")
    Page<Product> findAllByOrderByPriceValueAsc(Pageable pageable);

    @Query("SELECT p FROM Product p ORDER BY p.likesCount DESC")
    Page<Product> findAllByOrderByLikesCountDesc(Pageable pageable);
}
