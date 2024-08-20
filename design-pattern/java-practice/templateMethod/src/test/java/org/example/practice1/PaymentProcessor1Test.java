package org.example.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice1.PaymentProcessor;
import org.example.practice1.CreditCardPaymentProcessor;

public class PaymentProcessor1Test {

    @Test
    public void testCreditCardPayment() {
        PaymentProcessor processor = new CreditCardPaymentProcessor();
        // 5%の手数料がかかる
        int result = processor.processPayment(10000);
        assertEquals(10500, result);
    }

    @Test
    public void testBankTransferPayment() {
        PaymentProcessor processor = new BankTransferPaymentProcessor();
        // 固定で300円の手数料がかかる
        int result = processor.processPayment(10000);
        assertEquals(10300, result);
    }
}
