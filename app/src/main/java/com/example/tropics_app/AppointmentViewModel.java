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

    // Add fields for date and time
    private String selectedDate;
    private String selectedTime;

    private final MutableLiveData<List<SelectedService>> selectedServices = new MutableLiveData<>(new ArrayList<>());

    // Getters for selected services
    public LiveData<List<SelectedService>> getSelectedServices() {
        return selectedServices;
    }

    // Method to add a selected service
    public void addSelectedService(SelectedService service) {
        List<SelectedService> currentList = selectedServices.getValue();
        if (currentList != null) {
            currentList.add(service);
            selectedServices.setValue(currentList);
        }
    }

    // Method to clear selected services
    public void clearSelectedServices() {
        selectedServices.setValue(new ArrayList<>());
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

    // Getter and Setter for date and time
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

