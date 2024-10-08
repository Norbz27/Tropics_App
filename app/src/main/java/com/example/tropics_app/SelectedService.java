package com.example.tropics_app;

import java.util.ArrayList;
import java.util.List;

public class SelectedService {
    private String subServiceName;
    private String parentServiceName; // Field for the parent service name
    private String serviceName; // Field for the service name
    private double price; // Field for the price of the service or sub-service
    private List<SelectedService> subServices;

    public SelectedService(){}

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
    public void setSubServiceName(String subServiceName) {
        this.subServiceName = subServiceName;
    }
    public void setName(String subServiceName) {
        this.subServiceName = subServiceName;
    }

    public String getParentServiceName() {
        return parentServiceName;
    }
    public void setParentServiceName(String parentServiceName) {
        this.parentServiceName = parentServiceName;
    }

    public String getServiceName() {
        return serviceName; // Return the service name
    }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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
    public void setSubServices(List<SelectedService> subServices) {
        this.subServices = subServices;
    }
}
