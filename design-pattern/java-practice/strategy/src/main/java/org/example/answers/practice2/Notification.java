package org.example.answers.practice2;

import org.example.answers.practice2.strategy.NotificationStrategy;

public class Notification {
    private NotificationStrategy notificationStrategy;

    public Notification(NotificationStrategy notificationStrategy) {
        this.notificationStrategy = notificationStrategy;
    }
    public String send(String message) {
        return notificationStrategy.notify(message);
    }
}
