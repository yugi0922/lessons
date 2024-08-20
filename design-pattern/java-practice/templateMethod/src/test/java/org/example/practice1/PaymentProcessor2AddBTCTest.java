package org.example.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice1.PaymentProcessor;
import org.example.practice1.CreditCardPaymentProcessor;
import org.example.practice1.BankTransferPaymentProcessor;

public class PaymentProcessor2AddBTCTest {

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

    @Test
    public void testBTCPayment() {
        // TODO: build失敗しないためBankTransferPaymentProcessorを記述している
        PaymentProcessor processor = new BankTransferPaymentProcessor();
        // 固定で1BTCの手数料がかかる
        int result = processor.processPayment(10000);
        assertEquals(11, result);
    }
}
