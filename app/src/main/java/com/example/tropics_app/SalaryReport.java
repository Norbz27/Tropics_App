package com.example.tropics_app;

public class SalaryReport {
    String name;
    double[] dailySales; // array to store daily sales
    double[] dailyCommission; // array to store daily commission

    public SalaryReport(String name, double[] dailySales, double[] dailyCommission) {
        this.name = name;
        this.dailySales = dailySales;
        this.dailyCommission = dailyCommission;
    }
}
