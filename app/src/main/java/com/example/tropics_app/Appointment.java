package com.example.tropics_app;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Represents an appointment with all its details.
 */
public class Appointment {
    private String id; // Unique ID for the appointment
    private String fullName; // Full name of the person
    private String date; // Appointment date
    private String time; // Appointment time
    private double totalPrice; // Total price associated with the appointment
    private String address; // Address of the person
    private String email; // Email of the person
    private String phone; // Phone number of the person
    private String serviceId; // Service ID associated with the appointment
    private String employeeId; // Employee ID associated with the appointment
    private List<Map<String, Object>> services; // List of services associated with the appointment
    private String createdDateTime;

    // Required empty constructor for Firestore
    public Appointment() {
        this.services = new ArrayList<>(); // Initialize services to avoid null pointer
    }

    // Constructor to initialize fullName, date, time, and ID fields
    public Appointment(String id, String fullName, String date, String time, String serviceId, List<Map<String, Object>> services, String createdDateTime) {
        this.id = id;
        this.fullName = fullName;
        this.date = date;
        this.time = time;
        this.serviceId = serviceId;
        this.services = services != null ? services : new ArrayList<>(); // Avoid null
        this.createdDateTime = createdDateTime;
    }

    // Constructor to initialize fullName, date, and time fields without ID
    public Appointment(String fullName, String date, String time, String serviceId, List<Map<String, Object>> services, String createdDateTime) {
        this("", fullName, date, time, serviceId, services, createdDateTime); // Call the main constructor
    }

    // Getters and Setters
    public List<Map<String, Object>> getServices() {
        return services;
    }

    public void setServices(List<Map<String, Object>> services) {
        this.services = services != null ? services : new ArrayList<>(); // Avoid null
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmployeeId() { // Add this method
        return employeeId;
    }

    public void setEmployeeId(String employeeId) { // Add this method
        this.employeeId = employeeId;
    }

    // Getter and Setter for createdDateTime
    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    // Method to convert createdDateTime to Date object
    public Date getCreatedDateTimeAsDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust the format as needed
        try {
            return format.parse(createdDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }
    public Date getClientDateTimeAsDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd"); // Adjust the format as needed
        try {
            return format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }
    public String getFirstName() {
        return fullName != null ? fullName.split(" ")[0] : null; // Assumes first name is the first word
    }

    public String getLastName() {
        return fullName != null && fullName.split(" ").length > 1 ? fullName.split(" ")[1] : null; // Assumes last name is the second word
    }
}
