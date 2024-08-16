package org.example.answers.practice2.strategy;

public class PushNotificationStrategy implements NotificationStrategy {

    public PushNotificationStrategy(){
    }

    @Override
    public String notify(String message) {
        return "プッシュ通知を送信: " + message;
    }
}
