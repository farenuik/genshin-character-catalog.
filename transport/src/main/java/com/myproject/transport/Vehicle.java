package com.myproject.transport;

public abstract class Vehicle implements VehicleModel {
    private final String brand;
    private final double fuelLevel;

    protected Vehicle(String brand, double fuelLevel) {
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Brand must not be blank");
        }
        if (fuelLevel < 0) {
            throw new IllegalArgumentException("Fuel level must be non-negative");
        }
        this.brand = brand;
        this.fuelLevel = fuelLevel;
    }

    public String getBrand() {
        return brand;
    }

    public double getFuelLevel() {
        return fuelLevel;
    }

    public abstract String getType();
}
