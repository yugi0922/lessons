package org.example.answers.practice1.state;

public interface DoorState {
    DoorState open();
    DoorState close();
    DoorState lock();
    DoorState unlock();
    String getStatus();
}
