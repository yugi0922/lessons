package org.example.practice1_1;

import org.example.practice1_1.car.CarFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.example.practice1_1.bike.BikeFactory;
import org.example.practice1_1.VehicleFactory;
import org.example.practice1_1.Vehicle;
import org.example.practice1_1.car.CarFactory;


public class VehicleFactoryTest {

    @Test
    public void testCreateCar() {
        VehicleFactory factory = new CarFactory();
        Vehicle vehicle = factory.create();
        assertEquals("Car is starting", vehicle.start());
        assertEquals("Car is stopping", vehicle.stop());
    }

    @Test
    public void testCreateBicycle() {
        VehicleFactory factory = new BikeFactory();
        Vehicle vehicle = factory.create();
        assertEquals("Bike is starting", vehicle.start());
        assertEquals("Bike is stopping", vehicle.stop());
    }
}
