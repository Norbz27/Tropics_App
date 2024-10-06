package com.example.tropics_app;

import java.security.Timestamp;

public class EmployeeSalaryDetails {
    private String employeeId;
    private String daysPresent;
    private String lateDeduction;
    private String deductedSalary;
    private String caDeduction;
    private int month;
    private String year;
    private String week;
    private String timestamp;

    // Default constructor
    public EmployeeSalaryDetails() {
    }

    // Constructor
    public EmployeeSalaryDetails(String employeeId, String daysPresent, String lateDeduction, String deductedSalary, String caDeduction, int month, String year, String week, String timestamp) {
        this.employeeId = employeeId;
        this.daysPresent = daysPresent;
        this.lateDeduction = lateDeduction;
        this.deductedSalary = deductedSalary;
        this.caDeduction = caDeduction;
        this.month = month;
        this.year = year;
        this.week = week;
        this.timestamp = timestamp;
    }

    // Getters
    public String getEmployeeId() {
        return employeeId;
    }

    public String getDaysPresent() {
        return daysPresent;
    }

    public String getLateDeduction() {
        return lateDeduction;
    }

    public String getDeductedSalary() {
        return deductedSalary;
    }

    public String getCaDeduction() {
        return caDeduction;
    }
    public int getMonth() {
        return month;
    }
    public String getYear() {
        return year;
    }
    public String getWeek() {
        return week;
    }

    // Setters
    public void setEmployeeName(String employeeName) {
        this.employeeId = employeeName;
    }

    public void setDaysPresent(String daysPresent) {
        this.daysPresent = daysPresent;
    }

    public void setLateDeduction(String lateDeduction) {
        this.lateDeduction = lateDeduction;
    }

    public void setDeductedSalary(String deductedSalary) {
        this.deductedSalary = deductedSalary;
    }

    public void setCaDeduction(String caDeduction) {
        this.caDeduction = caDeduction;
    }

    public void setMonth(int month) {
        this.month = month;
    }
    public void setYear(String year) {
        this.year = year;
    }
    public void setWeek(String week) {
        this.week = week;
    }
    public String getTimestamp() {
        return timestamp;
    }

}
