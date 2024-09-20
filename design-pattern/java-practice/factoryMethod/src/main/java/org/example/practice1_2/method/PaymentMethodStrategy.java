package org.example.practice1_2.method;

// 支払い戦略のインターフェース
public interface PaymentMethodStrategy {
    String pay(int amount);
}
