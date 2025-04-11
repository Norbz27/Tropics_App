package com.example.tropics_app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Gcash {
    private String id;
    private Double amount;
    private String clientname;
    private String timestamp;

    // Empty constructor required for Firebase
    public Gcash() {}

    // Constructor with parameters
    public Gcash(String id, Double amount, String clientname, String timestamp) {
        this.id = id;
        this.amount = amount;
        this.timestamp = timestamp;
        this.clientname = clientname;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getClientname() { return clientname; }
    public void setClientname(String ClientName) { this.clientname = ClientName; }

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
