package com.example.tropics_app;

import com.google.firebase.firestore.PropertyName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Appointment {
    private String id; // Assuming you have an ID field
    private String fullName; // Added fullName field
    private String date; // Added date field
    private String time; // Added time field
    private String createdDateTime; // Store as String initially, change to Date if possible
    private double totalPrice;

    // Required empty constructor for Firestore
    public Appointment() {
    }

    // Constructor to initialize fullName, date, and time fields
    public Appointment(String fullName, String date, String time) {
        this.fullName = fullName;
        this.date = date;
        this.time = time;
    }

    // Getter and Setter for fullName
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // Getter and Setter for date
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    // Getter and Setter for time
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    // Getter for createdDateTime
    public String getCreatedDateTime() {
        return createdDateTime;
    }

    // Set createdDateTime directly
    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    // Convert string date to Date object
    public Date getCreatedDateTimeAsDate() {
        return parseDate(createdDateTime);
    }

    private Date parseDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null; // Return null if parsing fails
        }
    }

    // Getter and Setter for totalPrice
    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
