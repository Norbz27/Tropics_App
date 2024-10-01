package com.example.tropics_app;
public class Client {
    private String fullName; // Add this field
    private String address;
    private String phone;
    private String email;

    // Empty constructor needed for Firestore
    public Client() {}

    // Getters and setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Method to split fullName into first and last name
    public String getFirstName() {
        return fullName != null ? fullName.split(" ")[0] : null; // Assumes first name is the first word
    }

    public String getLastName() {
        return fullName != null && fullName.split(" ").length > 1 ? fullName.split(" ")[1] : null; // Assumes last name is the second word
    }
}
