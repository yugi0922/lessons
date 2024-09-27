package org.example.answers.practice1.state;

public class Closed implements DoorState {
    @Override
    public DoorState open() {
        return new Opened();
    }

    @Override
    public DoorState close() {
        return this;
    }

    @Override
    public DoorState lock() {
        return new Locked();
    }

    @Override
    public DoorState unlock() {
        return this;
    }

    @Override
    public String getStatus() {
        return "閉";
    }
}
