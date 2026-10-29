package com.myproject.transport;

/** Transport model contract exposed to consumers of the transport module. */
public interface VehicleModel {
    String getBrand();

    double getFuelLevel();

    String getType();
}