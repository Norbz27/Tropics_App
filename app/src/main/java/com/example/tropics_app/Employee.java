package com.example.tropics_app;

public class Employee {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String salary;
    private String image;
    private Double coms; // Commission as a String
    private String imageUrl;

    // Empty constructor required for Firebase
    public Employee() {}

    // Constructor with parameters
    public Employee(String id, String name, String address, String phone, String email, String salary, String image, Double coms) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.salary = salary;
        this.image = image; // Use 'image' instead of 'imageUrl'
        this.coms = coms; // Store commission as a String
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

    public double getComs() { return coms; } // Commission as String

    public void setComs(double coms) { this.coms = coms; } // Set commission as String

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; } // Set the imageUrl
}
