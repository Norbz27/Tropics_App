package com.example.tropics_app;

public class AssignedService {
    private String serviceName;
    private String clientName;
    private String appointmentDate;
    private int weekNumber;
    private double price; // Add this field for total price

    // Constructor
    public AssignedService(String serviceName, String clientName, String appointmentDate, int weekNumber, double price) {
        this.serviceName = serviceName;
        this.clientName = clientName;
        this.appointmentDate = appointmentDate;
        this.weekNumber = weekNumber;
        this.price = price;
    }

    // Getters
    public String getServiceName() {
        return serviceName;
    }

    public String getClientName() {
        return clientName;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public double getPrice() {
        return price;
    }
}
