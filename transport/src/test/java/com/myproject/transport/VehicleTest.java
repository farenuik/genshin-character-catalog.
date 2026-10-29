package com.myproject.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    @Test
    void carShouldReportCorrectInfo() {
        Vehicle car = new Car("BMW", 14.5);

        assertEquals("BMW", car.getBrand());
        assertEquals("Car", car.getType());
        assertEquals(14.5, car.getFuelLevel(), 0.001);
    }

    @Test
    void truckShouldStartAndStop() {
        Truck truck = new Truck("Volvo", 120, 60);

        assertTrue(truck.start());
        assertEquals("Truck is moving with cargo: 120 kg", truck.getStatus());

        truck.stop();
        assertEquals("Truck stopped", truck.getStatus());
    }
}
