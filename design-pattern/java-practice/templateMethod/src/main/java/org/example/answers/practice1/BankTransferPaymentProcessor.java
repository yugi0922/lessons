package org.example.answers.practice1;

import org.example.answers.practice1.BankTransferPaymentProcessor;

public class BankTransferPaymentProcessor extends PaymentProcessor {

    public BankTransferPaymentProcessor() {
        super();
    }

    // @Override
    protected int calculateFee(int amount) {
        return 300;
    }

}
