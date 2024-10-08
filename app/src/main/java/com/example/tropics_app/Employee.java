package com.example.tropics_app;
public class Employee {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private Double salary;
    private String image;
    private Double coms; // Change from Double to String
    private String imageUrl;
    private String therapist;

    // Empty constructor required for Firebase
    public Employee() {}

    // Constructor with parameters
    public Employee(String id, String name, String address, String phone, String email, Double salary, String image, Double coms, String therapist) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.salary = salary;
        this.image = image;
        this.coms = coms; // Now stored as String
        this.therapist = therapist;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }

    public Double getComs() { return coms; } // Change getter to return String
    public void setComs(Double coms) { this.coms = coms; } // Change setter to accept String

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; } // Set the imageUrl

    public String getTherapist() {
        return therapist;
    }
}
