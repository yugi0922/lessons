package org.example.practice1_2.method;

public class BankTransferStrategy implements PaymentMethodStrategy {
    // private String bankName;
    // private String accountNumber;;
    private String accountNumber;


    public BankTransferStrategy(String accountNumber){
        this.accountNumber = accountNumber;
    }

    @Override
    public String pay(int amount) {
        return amount + " 円を銀行振込で支払いました";
    }
}
