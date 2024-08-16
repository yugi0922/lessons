package org.example.answers.practice1.method;

// 支払い戦略のインターフェース
public interface PaymentMethodStrategy {
    private int calculateFee(int amount) {
        return 0;
    }
    String pay(int amount);
}
