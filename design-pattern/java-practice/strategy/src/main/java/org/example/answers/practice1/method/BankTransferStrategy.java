package org.example.answers.practice1.method;

public class BankTransferStrategy implements PaymentMethodStrategy {
    // private String bankName;
    // private String accountNumber;;
    private String accountNumber;


    public BankTransferStrategy(String accountNumber){
        this.accountNumber = accountNumber;
    }

    private int calculateFee(int amount) {
        return 300;
    }


    @Override
    public String pay(int amount) {
        int result = amount + calculateFee(amount);
        return result + " 円を銀行振込で支払いました";
    }
}
