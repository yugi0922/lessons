package org.example.practice1_2.method;

public class CreditCardStrategy implements PaymentMethodStrategy {
    private String name;
    private String accountNumber;
    // private String cardNumber;
    // private String cvv;
    // private String dateOfExpiry;

    public CreditCardStrategy(String accountNumber){
        this.accountNumber = accountNumber;
    }

    @Override
    public String pay(int amount) {
        return amount + " 円をクレジットカードで支払いました";
    }
}
