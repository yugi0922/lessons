package org.example.answers.practice1;

abstract class PaymentProcessor {

    public PaymentProcessor() {
    }

    protected int calculateFee(int amount) {
        return 0;
    }

    protected int exchangeCurrency(int amount) {
        return amount;
    }

    public int processPayment(int amount) {
        int exchanged = exchangeCurrency(amount);
        int amountWithFee = exchanged + calculateFee(exchanged);
        return amountWithFee;
    }
}
