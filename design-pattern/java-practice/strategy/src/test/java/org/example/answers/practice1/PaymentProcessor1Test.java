package org.example.answers.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.answers.practice1.PaymentProcessor;
import org.example.answers.practice1.method.*;

public class PaymentProcessor1Test {

    @Test
    public void testCreditCardPayment() {
        PaymentMethodStrategy strategy = new CreditCardStrategy("1234567890123456");
        PaymentProcessor processor = new PaymentProcessor(strategy);
        String result = processor.processPayment(10000);
        assertEquals("10000 円をクレジットカードで支払いました", result);
    }

    @Test
    public void testBankTransferPayment() {
        PaymentMethodStrategy strategy = new BankTransferStrategy("1234567890123456");
        PaymentProcessor processor = new PaymentProcessor(strategy);
        String result = processor.processPayment(15000);
        assertEquals("15000 円を銀行振込で支払いました", result);
    }
}
