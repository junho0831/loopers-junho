package com.loopers.interfaces.event.payment;

import com.loopers.application.payment.PaymentApplicationService;
import com.loopers.domain.payment.PaymentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentEventListener {

    private final PaymentApplicationService paymentApplicationService;

    public PaymentEventListener(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentEvent.PaymentCreated event) throws InterruptedException {
        long thresholdMillis = 1000L + (long) (Math.random() * 4000L); // 1000..5000
        Thread.sleep(thresholdMillis);
        paymentApplicationService.handle(event.getTransactionKey());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentEvent.PaymentHandled event) {
        paymentApplicationService.notifyTransactionResult(event.getTransactionKey());
    }
}

