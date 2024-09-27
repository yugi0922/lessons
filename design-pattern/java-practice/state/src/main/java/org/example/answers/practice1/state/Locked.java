package org.example.answers.practice1.state;

public class Locked implements DoorState {
    @Override
    public DoorState open() {
        return this;
    }

    @Override
    public DoorState close() {
        return this;
    }

    @Override
    public DoorState lock() {
        return this;
    }

    @Override
    public DoorState unlock() {
        return new Closed();
    }

    @Override
    public String getStatus() {
        return "施錠";
    }
}
