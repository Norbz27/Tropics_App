package com.example.tropics_app;

import java.util.List;
import java.util.Map;

public class Employee {
    private String id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private Double salary;
    private String image;
    private Double coms; // Commission field
    private String imageUrl;
    private String therapist;
    private List<Map<String, Object>> commissionsHistory; // Field for tracking commission history
    private List<Map<String, Object>> salaryHistory; // Field for tracking salary history
    private String dateLastChange; // Field for tracking last change date
    private String salaryLastChange; // Field for tracking last salary change date

    // Empty constructor required for Firebase
    public Employee() {}

    // Constructor with parameters
    public Employee(String id, String name, String address, String phone, String email, Double salary, String image, Double coms, String therapist, List<Map<String, Object>> commissionsHistory, String dateLastChange, List<Map<String, Object>> salaryHistory, String salaryLastChange) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.salary = salary;
        this.image = image;
        this.coms = coms;
        this.therapist = therapist;
        this.commissionsHistory = commissionsHistory;
        this.salaryHistory = salaryHistory;
        this.dateLastChange = dateLastChange;
        this.salaryLastChange = salaryLastChange;
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

    public Double getComs() { return coms; }
    public void setComs(Double coms) { this.coms = coms; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTherapist() { return therapist; }
    public void setTherapist(String therapist) { this.therapist = therapist; }

    public List<Map<String, Object>> getCommissionsHistory() { return commissionsHistory; }
    public void setCommissionsHistory(List<Map<String, Object>> commissionsHistory) { this.commissionsHistory = commissionsHistory; }

    public String getDateLastChange() { return dateLastChange; }
    public void setDateLastChange(String dateLastChange) { this.dateLastChange = dateLastChange; }

    public List<Map<String, Object>> getSalaryHistory() { return salaryHistory; }
    public void setSalaryHistory(List<Map<String, Object>> salaryHistory) { this.salaryHistory = salaryHistory; }

    public String getDateSalaryLastChange() { return salaryLastChange; }
    public void setDateSalaryLastChange(String salaryLastChange) { this.salaryLastChange = salaryLastChange; }
}
