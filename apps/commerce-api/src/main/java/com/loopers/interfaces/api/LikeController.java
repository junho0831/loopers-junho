package com.loopers.interfaces.api;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.like.ProductLike;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/like")
public class LikeController {

    private final LikeFacade likeFacade;

    public LikeController(LikeFacade likeFacade) {
        this.likeFacade = likeFacade;
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> addLike(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long productId,
            HttpServletRequest request) {

        likeFacade.addLike(userId, productId, 
            request.getSession().getId(),
            request.getHeader("User-Agent"),
            getClientIpAddress(request));
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @RequestHeader("X-USER-ID") String userId,
            @PathVariable Long productId,
            HttpServletRequest request) {

        likeFacade.removeLike(userId, productId,
            request.getSession().getId(),
            request.getHeader("User-Agent"),
            getClientIpAddress(request));
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<LikedProductResponse>>> getLikedProducts(
            @RequestHeader("X-USER-ID") String userId) {

        List<LikedProductResponse> response = likeFacade.getLikedProductsWithDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    public static class LikedProductResponse {
        private final Long productId;
        private final String name;

        public LikedProductResponse(Long productId, String name) {
            this.productId = productId;
            this.name = name;
        }

        public Long getProductId() {
            return productId;
        }

        public String getName() {
            return name;
        }
    }
}