package org.example.practice1_2;

import org.example.practice1_2.method.PaymentMethodStrategy;
import org.example.practice1_2.method.CreditCardStrategy;
import org.example.practice1_2.method.BankTransferStrategy;

public class PaymentProcessorFactory {
    private PaymentMethodType paymentMethod;
    private String accountNumber;

    public PaymentProcessorFactory(PaymentMethodType paymentMethod, String accountNumber) {
        this.paymentMethod = paymentMethod;
        this.accountNumber = accountNumber;
    }

    public PaymentProcessor getInstance() {
        if(paymentMethod.equals(PaymentMethodType.CREDIT_CARD)) {
            return new PaymentProcessor(new CreditCardStrategy(accountNumber));
        } else if(paymentMethod.equals(PaymentMethodType.BANK_TRANSFER)) {
            return new PaymentProcessor(new BankTransferStrategy(accountNumber));
        }
        throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
    }

    public static enum PaymentMethodType {
        CREDIT_CARD, BANK_TRANSFER
    }
}
