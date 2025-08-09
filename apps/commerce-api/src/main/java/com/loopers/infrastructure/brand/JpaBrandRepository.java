package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaBrandRepository extends JpaRepository<Brand, Long> {
}