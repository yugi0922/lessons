package org.example.practice1_2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice1_2.PaymentProcessor;
import org.example.practice1_2.PaymentProcessorFactory;
import org.example.practice1_2.PaymentProcessorFactory.PaymentMethodType;
import org.example.practice1_2.method.*;

public class FactoryTest {

    @Test
    public void testCreditCardPayment() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(PaymentMethodType.CREDIT_CARD, "1234567890");
        PaymentProcessor processor = factory.getInstance();
        String result = processor.processPayment(10000);
        assertEquals("10000 円をクレジットカードで支払いました", result);
    }

    @Test
    public void testBankTransferPayment() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(PaymentMethodType.BANK_TRANSFER, "1234567890");
        PaymentProcessor processor = factory.getInstance();
        String result = processor.processPayment(15000);
        assertEquals("15000 円を銀行振込で支払いました", result);
    }
}
