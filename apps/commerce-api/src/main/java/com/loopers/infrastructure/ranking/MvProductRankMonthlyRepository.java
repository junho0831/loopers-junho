package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MvProductRankMonthlyRepository extends JpaRepository<MvProductRankMonthly, Long> {
    Page<MvProductRankMonthly> findByYearMonthOrderByRankAsc(String yearMonth, Pageable pageable);
    long countByYearMonth(String yearMonth);
    Optional<MvProductRankMonthly> findByYearMonthAndProductId(String yearMonth, Long productId);
}

