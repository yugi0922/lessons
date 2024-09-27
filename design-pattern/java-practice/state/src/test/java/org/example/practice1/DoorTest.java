package org.example.answers.practice1;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DoorTest {
    private Door door;

    @BeforeEach
    void setUp() {
        door = new Door();
    }

    @Test
    void testInitialState() {
        assertEquals("閉", door.getStatus(), "初期状態は閉じているべきです");
    }

    @Test
    void testOpenClosedDoor() {
        door.open();
        assertEquals("開", door.getStatus(), "閉じたドアを開けると開いた状態になるべきです");
    }

    @Test
    void testCloseOpenDoor() {
        door.open();
        door.close();
        assertEquals("閉", door.getStatus(), "開いたドアを閉めると閉じた状態になるべきです");
    }

    @Test
    void testLockClosedDoor() {
        door.lock();
        assertEquals("施錠", door.getStatus(), "閉じたドアをロックすると施錠状態になるべきです");
    }

    @Test
    void testUnlockLockedDoor() {
        door.lock();
        door.unlock();
        assertEquals("閉", door.getStatus(), "施錠されたドアをアンロックすると閉じた状態になるべきです");
    }

    @Test
    void testOpenLockedDoor() {
        door.lock();
        door.open();
        assertEquals("施錠", door.getStatus(), "施錠されたドアは開けようとしても施錠状態のままです");
    }

    @Test
    void testLockOpenDoor() {
        door.open();
        door.lock();
        assertEquals("開", door.getStatus(), "開いているドアはロックできず、開いた状態のままです");
    }

    @Test
    void testComplexSequence() {
        door.open();
        door.close();
        door.lock();
        door.open();
        door.unlock();
        door.open();
        assertEquals("開", door.getStatus(), "一連の操作の後、最終的に開いた状態になるべきです");
    }
}
