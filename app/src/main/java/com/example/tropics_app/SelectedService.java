package com.example.tropics_app;

import java.util.ArrayList;
import java.util.List;

public class SelectedService {
    private String subServiceName;
    private String parentServiceName; // Field for the parent service name
    private String serviceName; // Field for the service name
    private double price; // Field for the price of the service or sub-service
    private final List<SelectedService> subServices;

    // Updated constructor with price parameter
    public SelectedService(String subServiceName, String parentServiceName, String serviceName, double price) {
        this.subServiceName = subServiceName;
        this.parentServiceName = parentServiceName;
        this.serviceName = serviceName; // Set the service name
        this.price = price; // Set the price
        this.subServices = new ArrayList<>();
    }

    // Method to clear the current service data
    public void clear() {
        subServiceName = null;
        parentServiceName = null;
        serviceName = null;
        price = 0; // Clear the price
        subServices.clear(); // Clear the sub-services list
    }

    public void addSubService(SelectedService subService) {
        subServices.add(subService);
    }

    public String getName() {
        return subServiceName; // Return the sub-service name
    }

    public String getParentServiceName() {
        return parentServiceName;
    }

    public String getServiceName() {
        return serviceName; // Return the service name
    }

    public double getPrice() {
        return price; // Return the price of the service or sub-service
    }

    public void setPrice(double price) {
        this.price = price; // Set or update the price
    }

    public List<SelectedService> getSubServices() {
        return subServices;
    }

    // Calculate total price including sub-services
    public double getTotalPrice() {
        double totalPrice = this.price; // Start with the price of the current service
        for (SelectedService subService : subServices) {
            totalPrice += subService.getTotalPrice(); // Add prices of sub-services recursively
        }
        return totalPrice;
    }
}
