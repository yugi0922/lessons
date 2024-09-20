package org.example.practice1_1.bike;

import org.example.practice1_1.Vehicle;
import org.example.practice1_1.VehicleFactory;

public class BikeFactory implements VehicleFactory {
    @Override
    public Vehicle create() {
        // 単純すぎますが、実際のアプリケーションではもっと複雑なロジックが必要となる想定ができます
        return new Bike();
    }
}
