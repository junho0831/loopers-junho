package com.loopers.domain.payment;

public class PaymentEvent {
    public static class PaymentCreated {
        private final String transactionKey;

        public PaymentCreated(String transactionKey) {
            this.transactionKey = transactionKey;
        }

        public static PaymentCreated from(Payment payment) {
            return new PaymentCreated(payment.getTransactionKey());
        }

        public String getTransactionKey() { return transactionKey; }
    }

    public static class PaymentHandled {
        private final String transactionKey;
        private final TransactionStatus status;
        private final String reason;
        private final String callbackUrl;

        public PaymentHandled(String transactionKey, TransactionStatus status, String reason, String callbackUrl) {
            this.transactionKey = transactionKey;
            this.status = status;
            this.reason = reason;
            this.callbackUrl = callbackUrl;
        }

        public static PaymentHandled from(Payment payment) {
            return new PaymentHandled(payment.getTransactionKey(), payment.getStatus(), payment.getReason(), payment.getCallbackUrl());
        }

        public String getTransactionKey() { return transactionKey; }
        public TransactionStatus getStatus() { return status; }
        public String getReason() { return reason; }
        public String getCallbackUrl() { return callbackUrl; }
    }
}

