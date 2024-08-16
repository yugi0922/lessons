package org.example.answers.practice1.method;

public class CreditCardStrategy implements PaymentMethodStrategy {
    private String name;
    private String accountNumber;
    // private String cardNumber;
    // private String cvv;
    // private String dateOfExpiry;

    public CreditCardStrategy(String accountNumber){
        this.accountNumber = accountNumber;
    }

    private int calculateFee(int amount){
        return (int)(amount*0.05);
    }

    @Override
    public String pay(int amount) {
        int result = amount + calculateFee(amount);
        return result + " 円をクレジットカードで支払いました";
    }
}
