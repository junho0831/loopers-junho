package com.loopers.application.payment;

import java.util.List;

public class OrderInfo {
    private final String orderId;
    private final List<TransactionInfo> transactions;

    public OrderInfo(String orderId, List<TransactionInfo> transactions) {
        this.orderId = orderId;
        this.transactions = transactions;
    }

    public String getOrderId() { return orderId; }
    public List<TransactionInfo> getTransactions() { return transactions; }
}

