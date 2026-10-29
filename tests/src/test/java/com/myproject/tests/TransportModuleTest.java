package com.myproject.tests;

import com.myproject.transport.Car;
import com.myproject.transport.Truck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportModuleTest {
    @Test
    void carExposesItsModelAndRejectsInvalidBrand() {
        Car car = new Car("Volvo", 42.5);

        assertEquals("Volvo", car.getBrand());
        assertEquals("Car", car.getType());
        assertEquals(42.5, car.getFuelLevel(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> new Car(" ", 10));
    }

    @Test
    void truckChangesStatusWhenStartedAndStopped() {
        Truck truck = new Truck("MAN", 120, 500);

        assertEquals("Truck stopped", truck.getStatus());
        assertTrue(truck.start());
        assertEquals("Truck is moving with cargo: 120 kg", truck.getStatus());
        truck.stop();
        assertEquals("Truck stopped", truck.getStatus());
    }
}