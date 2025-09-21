package com.loopers.application.payment;

import com.loopers.domain.payment.*;
import com.loopers.domain.user.UserInfo;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class PaymentApplicationService {

    private static final Random RANDOM = new Random();

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentRelay paymentRelay;
    private final TransactionKeyGenerator transactionKeyGenerator;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                     PaymentEventPublisher paymentEventPublisher,
                                     PaymentRelay paymentRelay,
                                     TransactionKeyGenerator transactionKeyGenerator) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.paymentRelay = paymentRelay;
        this.transactionKeyGenerator = transactionKeyGenerator;
    }

    @Transactional
    public TransactionInfo createTransaction(PaymentCommand.CreateTransaction command) {
        command.validate();

        String transactionKey = transactionKeyGenerator.generate();
        Payment payment = paymentRepository.save(new Payment(
                transactionKey,
                command.getUserId(),
                command.getOrderId(),
                command.getCardType(),
                command.getCardNo(),
                command.getAmount(),
                command.getCallbackUrl()
        ));

        paymentEventPublisher.publish(PaymentEvent.PaymentCreated.from(payment));
        return TransactionInfo.from(payment);
    }

    @Transactional(readOnly = true)
    public TransactionInfo getTransactionDetailInfo(UserInfo userInfo, String transactionKey) {
        Payment payment = paymentRepository.findByTransactionKey(userInfo.getUserId(), transactionKey);
        if (payment == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "(transactionKey: " + transactionKey + ") 결제건이 존재하지 않습니다.");
        }
        return TransactionInfo.from(payment);
    }

    @Transactional(readOnly = true)
    public OrderInfo findTransactionsByOrderId(UserInfo userInfo, String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(userInfo.getUserId(), orderId);
        if (payments == null || payments.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "(orderId: " + orderId + ") 에 해당하는 결제건이 존재하지 않습니다.");
        }
        return new OrderInfo(orderId, payments.stream().map(TransactionInfo::from).collect(Collectors.toList()));
    }

    @Transactional
    public void handle(String transactionKey) {
        Payment payment = paymentRepository.findByTransactionKey(transactionKey);
        if (payment == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "(transactionKey: " + transactionKey + ") 결제건이 존재하지 않습니다.");
        }

        int rate = RANDOM.nextInt(100) + 1; // 1..100
        if (rate <= 20) {
            payment.limitExceeded();
        } else if (rate <= 30) {
            payment.invalidCard();
        } else {
            payment.approve();
        }
        paymentEventPublisher.publish(PaymentEvent.PaymentHandled.from(payment));
    }

    public void notifyTransactionResult(String transactionKey) {
        Payment payment = paymentRepository.findByTransactionKey(transactionKey);
        if (payment == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "(transactionKey: " + transactionKey + ") 결제건이 존재하지 않습니다.");
        }
        paymentRelay.notify(payment.getCallbackUrl(), TransactionInfo.from(payment));
    }
}

