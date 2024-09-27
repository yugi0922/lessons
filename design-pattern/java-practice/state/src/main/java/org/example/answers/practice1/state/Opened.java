package org.example.answers.practice1.state;

public class Opened implements DoorState {
    @Override
    public DoorState open() {
        return this;
    }

    @Override
    public DoorState close() {
        return new Closed();
    }

    @Override
    public DoorState lock() {
        return this;
    }

    @Override
    public DoorState unlock() {
        return this;
    }

    @Override
    public String getStatus() {
        return "開";
    }
}
