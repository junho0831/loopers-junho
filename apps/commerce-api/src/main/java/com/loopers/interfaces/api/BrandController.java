package com.loopers.interfaces.api;

import com.loopers.application.brand.BrandFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandFacade brandFacade;

    public BrandController(BrandFacade brandFacade) {
        this.brandFacade = brandFacade;
    }

    @GetMapping("/{brandId}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrand(@PathVariable Long brandId) {
        Brand brand = brandFacade.getBrandById(brandId)
                .orElseThrow(() -> new CoreException(ErrorType.BRAND_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success(new BrandResponse(brand)));
    }

    public static class BrandResponse {
        private final Brand brand;

        public BrandResponse(Brand brand) {
            this.brand = brand;
        }

        public Long getBrandId() {
            return brand.getId();
        }

        public String getName() {
            return brand.getName();
        }
    }
}