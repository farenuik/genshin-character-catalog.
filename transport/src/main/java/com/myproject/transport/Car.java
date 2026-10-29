package com.myproject.transport;

public class Car extends Vehicle {
    public Car(String brand, double fuelLevel) {
        super(brand, fuelLevel);
    }

    @Override
    public String getType() {
        return "Car";
    }
}
