package com.example.tropics_app;

public class Accounts {
    private String uid;
    private String email;

    public Accounts() {
        // Default constructor required for Firebase
    }

    public Accounts(String uid, String email) {
        this.uid = uid;
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }
}
