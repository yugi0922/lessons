package org.example.practice1;

abstract class PaymentProcessor {

    public PaymentProcessor() {
    }

    protected int calculateFee(int amount) {
        return 0;
    }

    protected int processPayment(int amount) {
        int amountWithFee = amount + calculateFee(amount);
        return amountWithFee;
    }
}
