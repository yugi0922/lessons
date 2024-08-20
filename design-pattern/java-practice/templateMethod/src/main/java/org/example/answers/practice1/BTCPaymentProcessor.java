package org.example.answers.practice1;

import org.example.answers.practice1.PaymentProcessor;

public class BTCPaymentProcessor extends PaymentProcessor {

    public BTCPaymentProcessor() {
        super();
    }

    @Override
    protected int calculateFee(int amount) {
        return 1;
    }

    @Override
    protected int exchangeCurrency(int amount) {
        return (int)(amount/1000);
    }

}
