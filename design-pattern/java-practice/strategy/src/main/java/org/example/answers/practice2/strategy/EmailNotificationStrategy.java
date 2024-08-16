package org.example.answers.practice2.strategy;

public class EmailNotificationStrategy implements NotificationStrategy {

    public EmailNotificationStrategy(){
    }

    @Override
    public String notify(String message) {
        return "Eメールを送信: " + message;
    }
}
