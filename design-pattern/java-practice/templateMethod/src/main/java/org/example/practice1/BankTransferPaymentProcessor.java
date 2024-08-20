package org.example.practice1;

import org.example.practice1.BankTransferPaymentProcessor;

public class BankTransferPaymentProcessor extends PaymentProcessor {

    public BankTransferPaymentProcessor() {
        super();
    }

    // @Override
    protected int calculateFee(int amount) {
        return 300;
    }

}
