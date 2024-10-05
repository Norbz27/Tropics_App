package com.example.tropics_app;
public class Gcash {
    private String id;
    private Double amount;
    private String timestamp;

    // Empty constructor required for Firebase
    public Gcash() {}

    // Constructor with parameters
    public Gcash(String id, Double amount, String timestamp) {
        this.id = id;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }


}
