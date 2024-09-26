// SelectedService.java
package com.example.tropics_app;

import java.util.ArrayList;
import java.util.List;

public class SelectedService {
    private String subServiceName;
    private String parentServiceName; // Field for the parent service name
    private String serviceName; // Field for the service name
    private List<SelectedService> subServices;

    public SelectedService(String subServiceName, String parentServiceName, String serviceName) {
        this.subServiceName = subServiceName;
        this.parentServiceName = parentServiceName;
        this.serviceName = serviceName; // Set the service name
        this.subServices = new ArrayList<>();
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
        return serviceName; // New method to get the service name
    }

    public List<SelectedService> getSubServices() {
        return subServices;
    }
}
