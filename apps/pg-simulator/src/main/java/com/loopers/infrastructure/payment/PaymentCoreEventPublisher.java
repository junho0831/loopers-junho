package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentEvent;
import com.loopers.domain.payment.PaymentEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PaymentCoreEventPublisher implements PaymentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public PaymentCoreEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(PaymentEvent.PaymentCreated event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(PaymentEvent.PaymentHandled event) {
        applicationEventPublisher.publishEvent(event);
    }
}

