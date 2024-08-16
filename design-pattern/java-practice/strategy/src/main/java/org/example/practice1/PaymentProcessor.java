package org.example.practice1;

import org.example.practice1.method.PaymentMethodStrategy;

public class PaymentProcessor {
    private PaymentMethodStrategy method;

    public PaymentProcessor(PaymentMethodStrategy method) {
        this.method = method;
    }

    public String processPayment(int amount) {
        return method.pay(amount);
    }
}
