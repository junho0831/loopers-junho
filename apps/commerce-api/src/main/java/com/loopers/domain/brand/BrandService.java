package com.loopers.domain.brand;

import com.loopers.infrastructure.brand.JpaBrandRepository;
import org.springframework.stereotype.Service;

@Service
public class BrandService {
    private final JpaBrandRepository brandRepository;

    public BrandService(JpaBrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public Brand loadBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + brandId));
    }

    public Brand saveBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    public void validateBrand(Long brandId) {
        if (brandId == null) {
            throw new IllegalArgumentException("Brand ID cannot be null");
        }
        if (!brandRepository.findById(brandId).isPresent()) {
            throw new IllegalArgumentException("Brand not found: " + brandId);
        }
    }
    
    public java.util.Optional<Brand> findById(Long brandId) {
        return brandRepository.findById(brandId);
    }
}