package org.example.answers.practice2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.answers.practice2.Notification;
import org.example.answers.practice2.strategy.*;

public class NotificationTest {

    @Test
    public void testEmailNotification() {
        Notification notification = new Notification(new EmailNotificationStrategy());
        String result = notification.send("テストメッセージ");
        assertEquals("Eメールを送信: テストメッセージ", result);
    }

    @Test
    public void testSMSNotification() {
        Notification notification = new Notification(new SMSNotificationStrategy());
        String result = notification.send("テストメッセージ");
        assertEquals("SMSを送信: テストメッセージ", result);
    }

    @Test
    public void testPushNotification() {
        Notification notification = new Notification(new  PushNotificationStrategy());
        String result = notification.send("テストメッセージ");
        assertEquals("プッシュ通知を送信: テストメッセージ", result);
    }
}
