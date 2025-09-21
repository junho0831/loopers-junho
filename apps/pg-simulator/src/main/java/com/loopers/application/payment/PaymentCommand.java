package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class PaymentCommand {
    public static class CreateTransaction {
        private final String userId;
        private final String orderId;
        private final CardType cardType;
        private final String cardNo;
        private final long amount;
        private final String callbackUrl;

        public CreateTransaction(String userId, String orderId, CardType cardType, String cardNo, long amount, String callbackUrl) {
            this.userId = userId;
            this.orderId = orderId;
            this.cardType = cardType;
            this.cardNo = cardNo;
            this.amount = amount;
            this.callbackUrl = callbackUrl;
        }

        public void validate() {
            if (amount <= 0L) {
                throw new CoreException(ErrorType.BAD_REQUEST, "요청 금액은 0 보다 큰 정수여야 합니다.");
            }
        }

        public String getUserId() { return userId; }
        public String getOrderId() { return orderId; }
        public CardType getCardType() { return cardType; }
        public String getCardNo() { return cardNo; }
        public long getAmount() { return amount; }
        public String getCallbackUrl() { return callbackUrl; }
    }
}

