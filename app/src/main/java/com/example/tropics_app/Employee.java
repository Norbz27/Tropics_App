package com.example.tropics_app;

public class Employee {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String salary;
    private String image;
    private String imageUrl;

    public Employee() {
        // Empty constructor required for Firebase
    }
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
    public Employee(String id, String name, String address, String phone, String email, String salary) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.salary = salary;
        this.imageUrl = imageUrl;
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

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getImageUrl() {
        return imageUrl; // Add this methodd
    }
}
