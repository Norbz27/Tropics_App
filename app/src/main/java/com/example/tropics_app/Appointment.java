package com.example.tropics_app;

public class Appointment {
    private String fullName;
    private String date;
    private String time;

    public Appointment(String fullName, String date, String time) {
        this.fullName = fullName;
        this.date = date;
        this.time = time;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
