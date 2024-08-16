package org.example.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice1.PaymentProcessor;
import org.example.practice1.method.*;

public class PaymentProcessor2WithFeeTest {

    @Test
    public void testCreditCardPayment() {
        PaymentMethodStrategy method = new CreditCardStrategy("1234567890123456");
        PaymentProcessor processor = new PaymentProcessor(method);
        String result = processor.processPayment(10000);
        // 5%の手数料がかかる
        assertEquals("10500 円をクレジットカードで支払いました", result);
    }

    @Test
    public void testBankTransferPayment() {
        PaymentMethodStrategy method = new BankTransferStrategy("1234567890123456");
        PaymentProcessor processor = new PaymentProcessor(method);
        String result = processor.processPayment(15000);
        // 固定で300円の手数料がかかる
        assertEquals("15300 円を銀行振込で支払いました", result);
    }
}
