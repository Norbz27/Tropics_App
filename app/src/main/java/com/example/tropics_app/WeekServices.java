package com.example.tropics_app;

import java.util.List;

public class WeekServices {
    private String weekLabel;
    private List<AssignedService> services;
    private double totalCommission; // Add this field

    public WeekServices(String weekLabel, List<AssignedService> services, double totalCommission) {
        this.weekLabel = weekLabel;
        this.services = services;
        this.totalCommission = totalCommission; // Initialize it
    }

    public double getTotalCommission() {
        return totalCommission;
    }

    public String getWeekLabel() {
        return weekLabel;
    }

    public List<AssignedService> getServices() {
        return services;
    }
}

