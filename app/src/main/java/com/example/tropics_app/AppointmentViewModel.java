package com.example.tropics_app;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class AppointmentViewModel extends ViewModel {
    private String fullName;
    private String address;
    private String phone;
    private String email;

    // Fields for date and time
    private String selectedDate;
    private String selectedTime;

    // LiveData to hold selected services
    private final MutableLiveData<List<SelectedService>> selectedServices = new MutableLiveData<>(new ArrayList<>());

    // Field to track the total price of all selected services
    private double totalPrice = 0.0;

    // Getters for selected services
    public LiveData<List<SelectedService>> getSelectedServices() {
        return selectedServices;
    }

    // Method to add a selected service and update the total price
    public void addSelectedService(SelectedService service) {
        List<SelectedService> currentList = selectedServices.getValue();
        if (currentList != null) {
            currentList.add(service);
            selectedServices.setValue(currentList);
            updateTotalPrice();
        }
    }

    // Method to clear selected services and reset the total price
    public void clearSelectedServices() {
        selectedServices.setValue(new ArrayList<>());
        totalPrice = 0.0; // Reset total price
    }

    // Getter for total price
    public double getTotalPrice() {
        return totalPrice;
    }

    // Private method to update the total price based on selected services and sub-services
    private void updateTotalPrice() {
        double calculatedPrice = 0.0;
        List<SelectedService> currentList = selectedServices.getValue();
        if (currentList != null) {
            for (SelectedService service : currentList) {
                calculatedPrice += service.getPrice(); // Add main service price
                for (SelectedService subService : service.getSubServices()) {
                    calculatedPrice += subService.getPrice(); // Add sub-service prices
                }
            }
        }
        totalPrice = calculatedPrice;
    }

    // Getters and setters for personal information
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

    // Getters and setters for date and time
    public String getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedTime() {
        return selectedTime;
    }

    public void setSelectedTime(String selectedTime) {
        this.selectedTime = selectedTime;
    }
}
