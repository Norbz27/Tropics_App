package com.example.tropics_app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Funds {
    private String id;
    private Double amount;
    private String reason;
    private String timestamp;

    // Empty constructor required for Firebase
    public Funds() {}

    // Constructor with parameters
    public Funds(String id, Double amount, String reason, String timestamp) {
        this.id = id;
        this.amount = amount;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Date getParsedDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            return sdf.parse(this.timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
