package com.myproject.transport;

public class Truck extends Vehicle {
    private final int cargoWeight;
    private final int maxLoad;
    private String status = "Truck stopped";

    public Truck(String brand, int cargoWeight, int maxLoad) {
        super(brand, maxLoad > 0 ? maxLoad : 0);
        this.cargoWeight = cargoWeight;
        this.maxLoad = maxLoad;
    }

    public boolean start() {
        status = "Truck is moving with cargo: " + cargoWeight + " kg";
        return true;
    }

    public void stop() {
        status = "Truck stopped";
    }

    public String getStatus() {
        return status;
    }

    public int getCargoWeight() {
        return cargoWeight;
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    @Override
    public String getType() {
        return "Truck";
    }
}
