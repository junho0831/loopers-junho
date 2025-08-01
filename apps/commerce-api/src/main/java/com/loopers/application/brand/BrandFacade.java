package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BrandFacade {

    private final JpaBrandRepository brandRepository;

    public BrandFacade(JpaBrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public Optional<Brand> getBrandById(Long brandId) {
        return brandRepository.findById(brandId);
    }
}