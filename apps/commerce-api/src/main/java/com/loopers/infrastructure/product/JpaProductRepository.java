package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaProductRepository extends JpaRepository<Product, Long> {

    // 브랜드별 조회 (기본)
    @Query("SELECT p FROM Product p WHERE p.brand.id = :brandId")
    Page<Product> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);
    
    // ID로 단일 상품 조회 (PK 인덱스 활용) - JPA 기본 제공하지만 명시적으로 정의
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByProductId(@Param("id") Long id);

    // N+1 문제 해결: 브랜드 정보와 함께 조회
    @Query("SELECT p FROM Product p JOIN FETCH p.brand WHERE p.id = :id")
    Optional<Product> findByIdWithBrand(@Param("id") Long id);

    // 동시성 제어를 위한 비관적 락 적용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    // 여러 상품을 한번에 락과 함께 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findByIdsWithLock(@Param("ids") List<Long> ids);
}
