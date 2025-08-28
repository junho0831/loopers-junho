package com.loopers.domain.order;

public enum OrderStatus {
    PENDING("결제 처리 중입니다"),        // 결제 대기중
    COMPLETED("결제가 완료되었습니다"),      // 주문 완료
    CANCELLED("주문이 취소되었습니다"),      // 주문 취소
    PAYMENT_FAILED("결제에 실패했습니다");  // 결제 실패

    private final String displayMessage;

    OrderStatus(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public boolean isPaymentCompleted() {
        return this == COMPLETED;
    }

    public String getPaymentDisplayMessage() {
        switch (this) {
            case PENDING:
                return "결제 요청이 처리 중입니다. 잠시만 기다려 주세요.";
            case COMPLETED:
                return "결제가 성공적으로 완료되었습니다.";
            case PAYMENT_FAILED:
                return "결제 처리 중 문제가 발생했습니다. 다시 시도해 주세요.";
            case CANCELLED:
                return "주문이 취소되어 결제가 중단되었습니다.";
            default:
                return "결제 상태를 확인할 수 없습니다.";
        }
    }
}