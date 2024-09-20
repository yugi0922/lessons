package org.example.practice1_2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice1_2.PaymentProcessor;
import org.example.practice1_2.PaymentProcessorFactory;
import org.example.practice1_2.PaymentProcessorFactory.PaymentMethodType;
import org.example.practice1_2.method.*;

public class FactoryTest_addBTC {

    // このメソッドは、支払い方法の具体に依存していない点に注目。バリエーションが追加されても変更する必要がない。
    private String pay(PaymentMethodType type, String accountNumber, int amount) {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(type, accountNumber);
        PaymentProcessor processor = factory.getInstance();
        return processor.processPayment(amount);
    }

    @Test
    public void testCreditCardPayment() {
        String result = pay(PaymentMethodType.CREDIT_CARD, "1234567890", 10000);
        assertEquals("10000 円をクレジットカードで支払いました", result);
    }

    @Test
    public void testBankTransferPayment() {
        String result = pay(PaymentMethodType.BANK_TRANSFER, "1234567890", 15000);
        assertEquals("15000 円を銀行振込で支払いました", result);
    }

    @Test
    public void testBTCPayment() {
        // BTCの相場は1BTC=1000円とする
        // typeは変更してください
        String result = pay(PaymentMethodType.BTC, "1234567890", 15000);
        assertEquals("15BTC支払いました", result);
    }
}
