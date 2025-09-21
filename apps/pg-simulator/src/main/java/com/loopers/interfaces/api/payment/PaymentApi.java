package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentApplicationService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.domain.user.UserInfo;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApi {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentApi(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping
    public ApiResponse<PaymentDto.TransactionResponse> request(
            UserInfo userInfo,
            @RequestBody PaymentDto.PaymentRequest request
    ) throws InterruptedException {
        request.validate();

        // 100ms ~ 500ms 지연
        long delay = 100 + (long)(Math.random() * 401);
        Thread.sleep(delay);

        // 40% 확률로 요청 실패
        if (Math.random() <= 0.4) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "현재 서버가 불안정합니다. 잠시 후 다시 시도해주세요.");
        }

        var info = paymentApplicationService.createTransaction(request.toCommand(userInfo.getUserId()));
        return ApiResponse.success(PaymentDto.TransactionResponse.from(info));
    }

    @GetMapping("/{transactionKey}")
    public ApiResponse<PaymentDto.TransactionDetailResponse> getTransaction(
            UserInfo userInfo,
            @PathVariable("transactionKey") String transactionKey
    ) {
        var info = paymentApplicationService.getTransactionDetailInfo(userInfo, transactionKey);
        return ApiResponse.success(PaymentDto.TransactionDetailResponse.from(info));
    }

    @GetMapping
    public ApiResponse<PaymentDto.OrderResponse> getTransactionsByOrder(
            UserInfo userInfo,
            @RequestParam(value = "orderId", required = false) String orderId
    ) {
        var order = paymentApplicationService.findTransactionsByOrderId(userInfo, orderId);
        return ApiResponse.success(PaymentDto.OrderResponse.from(order));
    }
}

