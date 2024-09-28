package com.example.tropics_app;

import com.google.firebase.firestore.PropertyName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Appointment {
    private String id; // Assuming you have an ID field
    private String createdDateTime; // Store as String initially, change to Date if possible
    private double totalPrice;

    // Required empty constructor for Firestore
    public Appointment() {
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

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
