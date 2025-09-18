package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MvProductRankWeeklyRepository extends JpaRepository<MvProductRankWeekly, Long> {
    Page<MvProductRankWeekly> findByYearWeekOrderByRankAsc(String yearWeek, Pageable pageable);
    long countByYearWeek(String yearWeek);
    Optional<MvProductRankWeekly> findByYearWeekAndProductId(String yearWeek, Long productId);
}

