package org.example.practice2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.example.practice2.Notification;

public class NotificationTest {

    @Test
    public void testEmailNotification() {
        Notification notification = new Notification(); // strategyを指定するように変更する必要があります。
        String result = notification.send("テストメッセージ");
        assertEquals("Eメールを送信: テストメッセージ", result);
    }

    @Test
    public void testSMSNotification() {
        Notification notification = new Notification(); // strategyを指定するように変更する必要があります。
        String result = notification.send("テストメッセージ");
        assertEquals("SMSを送信: テストメッセージ", result);
    }

    @Test
    public void testPushNotification() {
        Notification notification = new Notification(); // strategyを指定するように変更する必要があります。
        String result = notification.send("テストメッセージ");
        assertEquals("プッシュ通知を送信: テストメッセージ", result);
    }
}
