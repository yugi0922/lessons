package org.example.answers.practice1_2.method;

public class BTCStrategy implements PaymentMethodStrategy {
    // private String name;
    private String accountNumber;

    public BTCStrategy(String accountNumber){
        this.accountNumber = accountNumber;
    }

    @Override
    public String pay(int amount) {
        int result = (int) amount/1000;
        return result + "BTC支払いました";
    }
}
