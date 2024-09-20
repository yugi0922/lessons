package org.example.practice1_1.bike;

import org.example.practice1_1.Vehicle;

public class Bike implements Vehicle {
    @Override
    public String start() {
        return "Bike is starting";
    }

    @Override
    public String stop() {
        return "Bike is stopping";
    }
}
