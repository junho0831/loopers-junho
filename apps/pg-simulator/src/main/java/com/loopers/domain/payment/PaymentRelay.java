package com.loopers.domain.payment;

import com.loopers.application.payment.TransactionInfo;

public interface PaymentRelay {
    void notify(String callbackUrl, TransactionInfo transactionInfo);
}

