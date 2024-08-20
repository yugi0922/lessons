package org.example.answers.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.answers.practice1.PaymentProcessor;
import org.example.answers.practice1.CreditCardPaymentProcessor;
import org.example.answers.practice1.BankTransferPaymentProcessor;
import org.example.answers.practice1.BTCPaymentProcessor;

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
        PaymentProcessor processor = new BTCPaymentProcessor();
        // 固定で1BTCの手数料がかかる
        int result = processor.processPayment(10000);
        assertEquals(11, result);
    }
}
