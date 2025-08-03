package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPointRepository extends JpaRepository<Point, String>, PointRepository {
    @Override
    @Query("SELECT p FROM Point p WHERE p.userId = :userId")
    Optional<Point> findByUserId(@Param("userId") String userId);
}