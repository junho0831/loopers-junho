package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BrandFacade {

    private final BrandService brandService;

    public BrandFacade(BrandService brandService) {
        this.brandService = brandService;
    }

    public Optional<Brand> getBrandById(Long brandId) {
        return brandService.findById(brandId);
    }
}