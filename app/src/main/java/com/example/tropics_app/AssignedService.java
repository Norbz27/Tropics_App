package com.example.tropics_app;

public class AssignedService {
    private String serviceName;
    private String clientName;
    private String appointmentDate;
    private int weekNumber; // Add week number
    private double commission;

    public AssignedService(String serviceName, String clientName, String appointmentDate, int weekNumber) {
        this.serviceName = serviceName;
        this.clientName = clientName;
        this.appointmentDate = appointmentDate;
        this.weekNumber = weekNumber; // Initialize week number
    }

    // Getters
    public String getServiceName() { return serviceName; }
    public String getClientName() { return clientName; }
    public String getAppointmentDate() { return appointmentDate; }
    public int getWeekNumber() { return weekNumber; } // Getter for week number
}

