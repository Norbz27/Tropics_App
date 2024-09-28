package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.exceptions.OutOfDateRangeException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentSummaryFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private TextView tvFullName, tvAddress, tvPhone, tvEmail, tvTotalPrice;
    private View view;
    private String parentname = "";
    private CalendarView disCalendar; // Declare CalendarView here
    private TimePicker disTime; // Declare TimePicker here
    private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_appointment_summary, container, false);

        LinearLayout summaryContainer = view.findViewById(R.id.summaryContainer);
        disCalendar = view.findViewById(R.id.disCalendar);
        disTime = view.findViewById(R.id.disTime);

        // Initialize TextViews
        tvFullName = view.findViewById(R.id.tvFname);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);

        // Set up back button
        Button backButton = view.findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        });

        // Set up next button
        Button btnSubmitAppointment = view.findViewById(R.id.btnSubmitAppointment);
        btnSubmitAppointment.setOnClickListener(v -> {
            showConfirmationDialog();
        });


        viewModel.getSelectedServices().observe(getViewLifecycleOwner(), services -> {
            summaryContainer.removeAllViews();
            Log.d("AppointmentSummaryFragment", "Selected Services: " + services);

            if (services.isEmpty()) {
                TextView emptyTextView = new TextView(getContext());
                emptyTextView.setText("No services selected.");
                emptyTextView.setTextColor(Color.parseColor("#B6B6B6"));
                summaryContainer.addView(emptyTextView);
            } else {
                // Reset parentname for each load
                parentname = "";
                for (SelectedService service : services) {
                    Log.d("Service", "Parent Service: " + service.getParentServiceName());
                    addServiceToSummary(summaryContainer, service, 0); // Start at indentation level 0
                }
            }

            double totalPrice = viewModel.getTotalPrice();
            tvTotalPrice.setText(String.format(Locale.getDefault(), "Total Price: ₱%.2f", totalPrice));
        });
        // Call the update method to display the latest data
        updateSummary();

        return view;
    }
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Appointment");
        builder.setMessage("Are you sure you want to submit the appointment?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            submitAppointment(); // Call the method to submit the appointment
        });

        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss(); // Dismiss the dialog if user selects "No"
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void submitAppointment() {
        // Get appointment details from ViewModel
        String fullName = viewModel.getFullName();
        String address = viewModel.getAddress();
        String phone = viewModel.getPhone();
        String email = viewModel.getEmail();
        String selectedDate = viewModel.getSelectedDate();
        String selectedTime = viewModel.getSelectedTime();
        String currentDateTime = getCurrentDateTime();

        // Create a Map to store appointment data
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("fullName", fullName);
        appointment.put("address", address);
        appointment.put("phone", phone);
        appointment.put("email", email);
        appointment.put("date", selectedDate);
        appointment.put("time", selectedTime);
        appointment.put("createdDateTime", currentDateTime);

        // Get selected services and total price
        List<SelectedService> services = viewModel.getSelectedServices().getValue();
        double totalPrice = viewModel.getTotalPrice();
        appointment.put("totalPrice", totalPrice); // Add total price to Firestore

        if (services != null && !services.isEmpty()) {
            List<Map<String, Object>> serviceHierarchy = new ArrayList<>();
            for (SelectedService service : services) {
                Map<String, Object> serviceMap = new HashMap<>();
                addServiceToFirestore(serviceMap, service, 0); // Build the hierarchy
                serviceHierarchy.add(serviceMap);
            }
            appointment.put("services", serviceHierarchy); // Store the hierarchical services list
        }

        // Save to Firestore under "appointments" collection
        db.collection("appointments")
                .add(appointment)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Appointment submitted successfully with ID: " + documentReference.getId());
                    showMessageDialog("Success", "Appointment submitted successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error submitting appointment", e);
                    showMessageDialog("Error", "Error submitting appointment. Please try again.");
                });
    }

    // Method to get the current date and time as a formatted string
    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    private void showMessageDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void addServiceToFirestore(Map<String, Object> parentMap, SelectedService service, int indentLevel) {
        Log.d("addServiceToFirestore", "Adding service: " + service.getName() + " at level: " + indentLevel);

        // Add Parent Service
        if (indentLevel == 0) {
            parentMap.put("parentServiceName", service.getParentServiceName());
        }

        // Add SubService Name and Price
        parentMap.put("serviceName", service.getName());
        parentMap.put("servicePrice", service.getPrice());

        // Check if there are sub-services
        if (!service.getSubServices().isEmpty()) {
            List<Map<String, Object>> subServiceList = new ArrayList<>();

            // Recursively add sub-services
            for (SelectedService subService : service.getSubServices()) {
                Map<String, Object> subServiceMap = new HashMap<>();
                addServiceToFirestore(subServiceMap, subService, indentLevel + 1); // Increase indent level
                subServiceList.add(subServiceMap);
            }
            parentMap.put("subServices", subServiceList);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // Update summary and refresh calendar/time every time the fragment resumes
        updateSummary();
        refreshCalendarAndTime();
    }

    private void refreshCalendarAndTime() {
        // Clear previous events
        disCalendar.setEvents(new ArrayList<>());

        // Display current date with custom background
        Calendar calendarnow = Calendar.getInstance();
        EventDay todayEvent = new EventDay(calendarnow, R.drawable.custom_selector2);
        List<EventDay> events = new ArrayList<>();
        events.add(todayEvent);

        // Get the selected date from ViewModel
        String selectedDate = viewModel.getSelectedDate();
        if (selectedDate != null) {
            try {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date startDate = dateFormat.parse(selectedDate);

                // Create a Calendar instance for the selected date
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDate);

                // Set up EventDay for the selected date
                EventDay selectedEvent = new EventDay(calendar, R.drawable.custom_selector); // Use your custom drawable for the selected date
                events.add(selectedEvent);

                // Add the selected event to the CalendarView
                disCalendar.setEvents(events); // Set all events (current date and selected date)

                // Focus the calendar on the selected date
                disCalendar.setDate(calendar.getTime()); // This sets the calendar to display the selected date
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Set the selected time in TimePicker
        String selectedTime = viewModel.getSelectedTime();
        if (selectedTime != null) {
            String[] timeParts = selectedTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            disTime.setHour(hour);
            disTime.setMinute(minute);
            disTime.setEnabled(false);
            disTime.setOnTouchListener((v, event) -> true);
            disTime.setClickable(false);
            disTime.setFocusable(false);
            disTime.setFocusableInTouchMode(false);
            disTime.setOnClickListener(null);
        }
    }

    private void updateSummary() {
        tvFullName.setText(viewModel.getFullName());
        tvAddress.setText(viewModel.getAddress());
        tvPhone.setText(viewModel.getPhone());
        tvEmail.setText(viewModel.getEmail());
    }

    private void addServiceToSummary(LinearLayout parent, SelectedService service, int indentLevel) {
        Log.d("addServiceToSummary", "Adding service: " + service.getName() + " at level: " + indentLevel);

        Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.manrope); // Set the typeface

        // Add Parent Service only if it's not already displayed
        if (!parentname.equals(service.getParentServiceName())) {
            TextView parentTextView = new TextView(getContext());
            parentTextView.setText(service.getParentServiceName());
            parentTextView.setPadding(indentLevel * 30, 0, 0, 0);
            parentTextView.setTextColor(Color.parseColor("#B6B6B6"));
            parentTextView.setTypeface(typeface); // Set typeface
            parent.addView(parentTextView);
            parentname = service.getParentServiceName();
        }

        TextView subServiceTextView = new TextView(getContext());
        if(service.getPrice() != 0.0) {
            subServiceTextView.setText(service.getName() + " - ₱" + service.getPrice());
        }else {
            subServiceTextView.setText(service.getName());
        }
        subServiceTextView.setPadding((indentLevel + 1) * 30, 0, 0, 0);
        subServiceTextView.setTextColor(Color.parseColor("#B6B6B6"));
        subServiceTextView.setTypeface(typeface); // Set typeface
        parent.addView(subServiceTextView);

        // Recursively add sub-sub-services with their prices
        for (SelectedService subService : service.getSubServices()) {
            TextView subSubServiceTextView = new TextView(getContext());
            subSubServiceTextView.setText(subService.getName() + " - ₱" + subService.getPrice());
            subSubServiceTextView.setPadding((indentLevel + 2) * 30, 0, 0, 0);
            subSubServiceTextView.setTextColor(Color.parseColor("#B6B6B6"));
            subSubServiceTextView.setTypeface(typeface); // Set typeface
            parent.addView(subSubServiceTextView);
        }
    }
}

