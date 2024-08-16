package org.example.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice1.PaymentProcessor;
import org.example.practice1.method.*;

public class PaymentProcessor3AddBTCTest {

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

    @Test
    public void testBTCPayment() {
        // TODO 新しいmethodの追加（コンパイル通るようにbanktransfer設定しておく）
        PaymentMethodStrategy method = new BankTransferStrategy("1234567890123456");
        PaymentProcessor processor = new PaymentProcessor(method);
        String result = processor.processPayment(15000);
        // BTCの相場は1BTC=1000円とする。手数料は1BTCとする(今の相場からは考えにくいけど)
        assertEquals("16BTC支払いました", result);
    }
}
