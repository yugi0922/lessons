package org.example.answers.practice1.method;

public class BTCStrategy implements PaymentMethodStrategy {
    private String name;
    private String accountNumber;

    public BTCStrategy(String accountNumber){
        this.accountNumber = accountNumber;
    }

    private int calculateFee(int amount){
        return 1;
    }

    @Override
    public String pay(int amount) {
        int btcAmount = (int) amount/1000;
        int result = btcAmount + calculateFee(amount);
        return result + "BTC支払いました";
    }
}
