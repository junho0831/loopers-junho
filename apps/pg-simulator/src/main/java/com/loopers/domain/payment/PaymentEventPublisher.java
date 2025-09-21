package com.loopers.domain.payment;

public interface PaymentEventPublisher {
    void publish(PaymentEvent.PaymentCreated event);
    void publish(PaymentEvent.PaymentHandled event);
}

