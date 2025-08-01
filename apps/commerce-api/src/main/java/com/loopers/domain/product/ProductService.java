package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.JpaBrandRepository;
import com.loopers.infrastructure.like.JpaProductLikeRepository;
import com.loopers.infrastructure.product.JpaProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final JpaProductRepository productRepository;
    private final JpaBrandRepository brandRepository;
    private final JpaProductLikeRepository likeRepository;

    public ProductService(JpaProductRepository productRepository, JpaBrandRepository brandRepository,
            JpaProductLikeRepository likeRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.likeRepository = likeRepository;
    }

    public ProductDetail getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        Brand brand = brandRepository.findById(product.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found for product: " + productId));
        return new ProductDetail(product, brand);
    }

    public Page<Product> getProducts(ProductSortType sortType, Long brandId, Pageable pageable) {
        if (brandId != null) {
            return productRepository.findByBrandId(brandId, pageable);
        } else {
            return productRepository.findAllOrderBy(sortType, pageable);
        }
    }

    public Product createProduct(String name, long price, int stock, Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + brandId));

        Product product = new Product(name, new Money(price), new Stock(stock), brand);
        return productRepository.save(product);
    }

    public void decreaseProductStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        product.decreaseStock(quantity);
        productRepository.save(product);
    }
}