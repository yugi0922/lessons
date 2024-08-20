package org.example.practice1;

import org.example.practice1.PaymentProcessor;

public class CreditCardPaymentProcessor extends PaymentProcessor {

    public CreditCardPaymentProcessor() {
        super();
    }

    @Override
    protected int calculateFee(int amount) {
        return (int)(amount * 5 / 100);
    }

}
