package org.example.answers.practice2.strategy;

public class SMSNotificationStrategy implements NotificationStrategy {

    public SMSNotificationStrategy(){
    }

    @Override
    public String notify(String message) {
        return "SMSを送信: " + message;
    }
}
