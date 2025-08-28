package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLike;
import com.loopers.domain.like.ProductLikeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaProductLikeRepository extends JpaRepository<ProductLike, Long>, ProductLikeRepository {

    @Query("SELECT pl FROM ProductLike pl WHERE pl.userId = :userId AND pl.productId = :productId")
    Optional<ProductLike> findByUserIdAndProductId(@Param("userId") String userId, @Param("productId") Long productId);

    @Query("SELECT pl FROM ProductLike pl WHERE pl.userId = :userId")
    List<ProductLike> findByUserId(@Param("userId") String userId);

    @Query("SELECT pl FROM ProductLike pl WHERE pl.productId = :productId")
    List<ProductLike> findByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(pl) FROM ProductLike pl WHERE pl.productId = :productId")
    long countByProductId(@Param("productId") Long productId);

    boolean existsByUserIdAndProductId(String userId, Long productId);
}
