package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.OrderInfo;
import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.TransactionInfo;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.TransactionStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PaymentDto {

    public static class PaymentRequest {
        private String orderId;
        private CardTypeDto cardType;
        private String cardNo;
        private long amount;
        private String callbackUrl;

        private static final Pattern REGEX_CARD_NO = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$");
        private static final String PREFIX_CALLBACK_URL = "http://localhost:8080";

        public void validate() {
            if (orderId == null || orderId.isBlank() || orderId.length() < 6) {
                throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 6자리 이상 문자열이어야 합니다.");
            }
            if (cardNo == null || !REGEX_CARD_NO.matcher(cardNo).matches()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다.");
            }
            if (amount <= 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "결제금액은 양의 정수여야 합니다.");
            }
            if (callbackUrl == null || !callbackUrl.startsWith(PREFIX_CALLBACK_URL)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "콜백 URL 은 " + PREFIX_CALLBACK_URL + " 로 시작해야 합니다.");
            }
        }

        public PaymentCommand.CreateTransaction toCommand(String userId) {
            return new PaymentCommand.CreateTransaction(userId, orderId, cardType.toCardType(), cardNo, amount, callbackUrl);
        }

        public String getOrderId() { return orderId; }
        public CardTypeDto getCardType() { return cardType; }
        public String getCardNo() { return cardNo; }
        public long getAmount() { return amount; }
        public String getCallbackUrl() { return callbackUrl; }

        public void setOrderId(String orderId) { this.orderId = orderId; }
        public void setCardType(CardTypeDto cardType) { this.cardType = cardType; }
        public void setCardNo(String cardNo) { this.cardNo = cardNo; }
        public void setAmount(long amount) { this.amount = amount; }
        public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    }

    public static class TransactionDetailResponse {
        private String transactionKey;
        private String orderId;
        private CardTypeDto cardType;
        private String cardNo;
        private long amount;
        private TransactionStatusResponse status;
        private String reason;

        public static TransactionDetailResponse from(TransactionInfo info) {
            TransactionDetailResponse r = new TransactionDetailResponse();
            r.transactionKey = info.getTransactionKey();
            r.orderId = info.getOrderId();
            r.cardType = CardTypeDto.from(info.getCardType());
            r.cardNo = info.getCardNo();
            r.amount = info.getAmount();
            r.status = TransactionStatusResponse.from(info.getStatus());
            r.reason = info.getReason();
            return r;
        }

        public String getTransactionKey() { return transactionKey; }
        public String getOrderId() { return orderId; }
        public CardTypeDto getCardType() { return cardType; }
        public String getCardNo() { return cardNo; }
        public long getAmount() { return amount; }
        public TransactionStatusResponse getStatus() { return status; }
        public String getReason() { return reason; }
    }

    public static class TransactionResponse {
        private String transactionKey;
        private TransactionStatusResponse status;
        private String reason;

        public static TransactionResponse from(TransactionInfo info) {
            TransactionResponse r = new TransactionResponse();
            r.transactionKey = info.getTransactionKey();
            r.status = TransactionStatusResponse.from(info.getStatus());
            r.reason = info.getReason();
            return r;
        }

        public String getTransactionKey() { return transactionKey; }
        public TransactionStatusResponse getStatus() { return status; }
        public String getReason() { return reason; }
    }

    public static class OrderResponse {
        private String orderId;
        private List<TransactionResponse> transactions;

        public static OrderResponse from(OrderInfo info) {
            OrderResponse r = new OrderResponse();
            r.orderId = info.getOrderId();
            r.transactions = info.getTransactions().stream()
                    .map(TransactionResponse::from)
                    .collect(Collectors.toList());
            return r;
        }

        public String getOrderId() { return orderId; }
        public List<TransactionResponse> getTransactions() { return transactions; }
    }

    public enum CardTypeDto {
        SAMSUNG,
        KB,
        HYUNDAI;

        public CardType toCardType() {
            return switch (this) {
                case SAMSUNG -> CardType.SAMSUNG;
                case KB -> CardType.KB;
                case HYUNDAI -> CardType.HYUNDAI;
            };
        }

        public static CardTypeDto from(CardType cardType) {
            return switch (cardType) {
                case SAMSUNG -> SAMSUNG;
                case KB -> KB;
                case HYUNDAI -> HYUNDAI;
            };
        }
    }

    public enum TransactionStatusResponse {
        PENDING,
        SUCCESS,
        FAILED;

        public static TransactionStatusResponse from(TransactionStatus status) {
            return switch (status) {
                case PENDING -> PENDING;
                case SUCCESS -> SUCCESS;
                case FAILED -> FAILED;
            };
        }
    }
}

