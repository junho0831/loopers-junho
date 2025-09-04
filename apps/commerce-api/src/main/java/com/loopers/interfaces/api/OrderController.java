package com.loopers.interfaces.api;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItemRequest;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    public OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        Order order = orderFacade.createOrder(userId, request.getItems(), 
            httpRequest.getSession().getId(),
            httpRequest.getHeader("User-Agent"),
            getClientIpAddress(httpRequest),
            request.getCardCompany(),
            request.getCardNumber());
        
        return ResponseEntity.ok(ApiResponse.success(
            "주문이 접수되었습니다. 결제 처리 중입니다...", 
            new OrderResponse(order)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @RequestHeader("X-USER-ID") String userId) {
        
        List<OrderResponse> response = orderFacade.getUserOrders(userId).stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        Order order = orderFacade.getOrderById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success(new OrderResponse(order)));
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

    public static class CreateOrderRequest {
        private List<OrderItemRequest> items;
        private String cardCompany;
        private String cardNumber;

        public List<OrderItemRequest> getItems() {
            return items;
        }

        public void setItems(List<OrderItemRequest> items) {
            this.items = items;
        }

        public String getCardCompany() {
            return cardCompany;
        }

        public void setCardCompany(String cardCompany) {
            this.cardCompany = cardCompany;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }
    }

    public static class OrderResponse {
        private final Order order;

        public OrderResponse(Order order) {
            this.order = order;
        }

        public Long getOrderId() {
            return order != null ? order.getId() : null;
        }

        public String getUserId() {
            return order != null ? order.getUserId() : null;
        }

        public long getTotalAmount() {
            return order != null && order.getTotalAmount() != null ? order.getTotalAmount().getValue() : 0L;
        }

        public OrderStatus getStatus() {
            return order != null ? order.getStatus() : null;
        }

        public ZonedDateTime getCreatedAt() {
            return order != null ? order.getCreatedAt() : null;
        }

        public List<OrderItemResponse> getOrderItems() {
            return order != null && order.getOrderItems() != null ? 
                order.getOrderItems().stream()
                    .map(OrderItemResponse::new)
                    .collect(Collectors.toList()) : 
                List.of();
        }
    }

    public static class OrderItemResponse {
        private final OrderItem orderItem;

        public OrderItemResponse(OrderItem orderItem) {
            this.orderItem = orderItem;
        }

        public Long getProductId() {
            return orderItem != null ? orderItem.getProductId() : null;
        }

        public int getQuantity() {
            return orderItem != null ? orderItem.getQuantity() : 0;
        }

        public long getUnitPrice() {
            return orderItem != null && orderItem.getUnitPrice() != null ? orderItem.getUnitPrice().getValue() : 0L;
        }

        public long getTotalPrice() {
            return orderItem != null ? orderItem.calculateTotalPrice().getValue() : 0L;
        }
    }

}