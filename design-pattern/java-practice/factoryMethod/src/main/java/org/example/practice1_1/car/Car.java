package org.example.practice1_1.car;

import org.example.practice1_1.Vehicle;

public class Car implements Vehicle {
    @Override
    public String start() {
        return "Car is starting";
    }

    @Override
    public String stop() {
        return "Car is stopping";
    }
}
