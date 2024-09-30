package com.example.tropics_app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment implements AppointmentAdapter.OnItemLongClickListener, AppointmentAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private String selectedDate;
    private CalendarView calendarView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView and AppointmentAdapter
        recyclerView = view.findViewById(R.id.rcview);
        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList, this); // Pass the listener

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(appointmentAdapter);

        // Load today's appointments
        selectedDate = getCurrentDate(); // Get today's date
        loadAppointmentData(selectedDate); // Load appointments for today

        // Set the long click listener on your adapter
        appointmentAdapter.setOnItemLongClickListener(this);

        // Set up the calendar view
        calendarView = view.findViewById(R.id.calendarView);
        calendarView.setOnDayClickListener(event -> {
            // Get selected date and update the RecyclerView
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(event.getCalendar().getTime());
            loadAppointmentData(selectedDate); // Load appointments for the selected date

            // Show sample_three_icons when a date is clicked
            showSampleThreeIcons(view);
        });

        return view;
    }

    private String getCurrentDate() {
        // Format today's date as needed (e.g., "yyyy-MM-dd")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    // Inside your loadAppointmentData method
    private void loadAppointmentData(String date) {
        db.collection("appointments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    appointmentList.clear();
                    List<EventDay> events = new ArrayList<>(); // List to hold event days for calendar view
                    boolean hasAppointments = false;  // Track if any appointments are available

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Appointment appointment = document.toObject(Appointment.class);
                        if (appointment != null) { // Check if appointment is not null
                            appointment.setId(document.getId()); // Set ID from Firestore

                            // Check if the appointment's date matches the selected date
                            if (appointment.getDate().equals(date)) {
                                appointmentList.add(appointment);
                                hasAppointments = true; // Set flag to true if appointments are found
                            }

                            // Highlight dates with appointments
                            Calendar appointmentCalendar = Calendar.getInstance();
                            try {
                                Date appointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(appointment.getDate());
                                appointmentCalendar.setTime(appointmentDate);

                                // Use sample_three_icons drawable to highlight the date
                                Drawable threeIconsDrawable = getResources().getDrawable(R.drawable.sample_three_icons, null);
                                events.add(new EventDay(appointmentCalendar, threeIconsDrawable)); // Add event to the list
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Set the events to highlight dates with appointments
                    calendarView.setEvents(events);
                    appointmentAdapter.notifyDataSetChanged();

                    // If there are appointments, show the ImageView
                    if (hasAppointments) {
                        showSampleThreeIcons(getView());
                    } else {
                        hideSampleThreeIcons(getView());  // Hide ImageView if no appointments
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("LoadAppointments", "Error loading appointments: ", e);
                    Toast.makeText(getActivity(), "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Show the icons
    private void showSampleThreeIcons(View parentView) {
        ImageView imageView = parentView.findViewById(R.id.imageview1);
        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE); // Make the ImageView visible
            Drawable threeIconsDrawable = getResources().getDrawable(R.drawable.sample_three_icons, null);
            imageView.setImageDrawable(threeIconsDrawable); // Set the drawable to the ImageView
        }
    }

    // Hide the icons when no appointments are found
    private void hideSampleThreeIcons(View parentView) {
        ImageView imageView = parentView.findViewById(R.id.imageview1);
        if (imageView != null) {
            imageView.setVisibility(View.GONE); // Hide the ImageView
        }
    }



    @Override
    public void onItemLongClick(Appointment appointment) {
        // Show options dialog for edit and delete
        showAppointmentOptionsDialog(appointment);
    }

    @Override
    public void onItemClick(Appointment appointment) {
        showViewAppointmentDialog(appointment);
    }

    private void showViewAppointmentDialog(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.view_appointment1, null);

        // Find TextViews
        TextView fullnameTextView = dialogView.findViewById(R.id.tvFullName);
        TextView dateTextView = dialogView.findViewById(R.id.tvdate);
        TextView timeTextView = dialogView.findViewById(R.id.tvTime);
        TextView phoneNumberTextView = dialogView.findViewById(R.id.tvPhoneNumber);  // Updated to Phone Number
        TextView empNameTextView = dialogView.findViewById(R.id.empname);
        TextView serviceTextView = dialogView.findViewById(R.id.textView5);
        TextView totalPriceTextView = dialogView.findViewById(R.id.total);  // Assuming you have a TextView for the total price

        // Set appointment details
        fullnameTextView.setText(appointment.getFullName());
        dateTextView.setText(appointment.getDate());
        timeTextView.setText(appointment.getTime());
        phoneNumberTextView.setText(appointment.getPhone());  // Set phone number from Appointment object
        empNameTextView.setText("Basta pangayan sa employee");

        // Format service details and calculate total price
        StringBuilder serviceDetails = new StringBuilder();
        double totalPrice = 0.0;

        for (Map<String, Object> service : appointment.getServices()) {
            // Extract main service details
            String parentServiceName = (String) service.get("parentServiceName");
            String serviceName = (String) service.get("serviceName");
            double servicePrice = (double) service.get("servicePrice");

            serviceDetails.append(parentServiceName).append(": ")
                    .append(serviceName).append(" - ")
                    .append(servicePrice).append(" PHP\n");

            totalPrice += servicePrice;

            // Check for sub-services
            List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
            if (subServices != null) {
                for (Map<String, Object> subService : subServices) {
                    String subServiceName = (String) subService.get("serviceName");
                    double subServicePrice = (double) subService.get("servicePrice");

                    serviceDetails.append("\tSubService: ").append(subServiceName)
                            .append(" - ").append(subServicePrice)
                            .append(" PHP\n");

                    totalPrice += subServicePrice;
                }
            }
        }

        // Set the services text and total price
        serviceTextView.setText(serviceDetails.toString());
        totalPriceTextView.setText("Total: " + totalPrice + " PHP");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }


    // Helper method to format service details
    private String getServiceDetails(List<Map<String, Object>> services) {
        StringBuilder serviceDetails = new StringBuilder();
        for (Map<String, Object> service : services) {
            String parentServiceName = (String) service.get("parentServiceName");
            String serviceName = (String) service.get("serviceName");
            double servicePrice = (double) service.get("servicePrice");
            serviceDetails.append("Service: ").append(parentServiceName).append(" - ").append(serviceName)
                    .append(" (Price: ").append(servicePrice).append(")\n");

            List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
            if (subServices != null) {
                for (Map<String, Object> subService : subServices) {
                    String subServiceName = (String) subService.get("serviceName");
                    double subServicePrice = (double) subService.get("servicePrice");
                    serviceDetails.append("   Sub-Service: ").append(subServiceName)
                            .append(" (Price: ").append(subServicePrice).append(")\n");
                }
            }
        }
        return serviceDetails.toString();
    }

    private void showAppointmentOptionsDialog(Appointment appointment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Options")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Handle edit action
                            showEditAppointmentDialog(appointment);
                            break;
                        case 1:
                            // Handle delete action
                            deleteAppointment(appointment);
                            break;
                    }
                })
                .create()
                .show();
    }

    private void deleteAppointment(Appointment appointment) {
        // Display a toast message
        Toast.makeText(getActivity(), "test", Toast.LENGTH_SHORT).show();
    }


    private void showEditAppointmentDialog(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogbox_editappointment, null);

        EditText etFullName = dialogView.findViewById(R.id.etFullName);
        EditText etDate = dialogView.findViewById(R.id.etDate);
        EditText etTime = dialogView.findViewById(R.id.etTime);
        Spinner spinnerEmployees = dialogView.findViewById(R.id.employee); // Updated to Spinner

        // Set existing appointment details
        etFullName.setText(appointment.getFullName());
        etDate.setText(appointment.getDate());
        etTime.setText(appointment.getTime());

        // Fetch employees from Firestore
        fetchEmployees(spinnerEmployees);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        dialogBuilder.setPositiveButton("Update", (dialog1, which) -> {
            // Validate and update appointment details
            String updatedFullName = etFullName.getText().toString().trim();
            String updatedDate = etDate.getText().toString().trim();
            String updatedTime = etTime.getText().toString().trim();
            String selectedEmployee = spinnerEmployees.getSelectedItem().toString(); // Get selected employee

            if (!updatedFullName.isEmpty() && !updatedDate.isEmpty() && !updatedTime.isEmpty()) {
                // Update appointment in Firestore
                updateAppointment(appointment.getId(), updatedFullName, updatedDate, updatedTime, selectedEmployee);
                dialog.dismiss();
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialogBuilder.setNegativeButton("Cancel", (dialog12, which) -> dialog12.dismiss());
    }

    private void fetchEmployees(Spinner spinnerEmployees) {
        List<String> employeeList = new ArrayList<>();
        db.collection("Employees")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String employeeName = document.getString("name"); // Assuming the employee's name field is "name"
                        employeeList.add(employeeName);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, employeeList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerEmployees.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e("FetchEmployees", "Error fetching employees: ", e);
                    Toast.makeText(getActivity(), "Failed to load employees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAppointment(String id, String fullName, String date, String time, String employee) {
        db.collection("appointments").document(id)
                .update("fullName", fullName, "date", date, "time", time, "employee", employee) // Add employee field here
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Appointment updated", Toast.LENGTH_SHORT).show();
                    loadAppointmentData(selectedDate); // Refresh the list after updating
                })
                .addOnFailureListener(e -> {
                    Log.e("UpdateAppointment", "Error updating document: ", e);
                    Toast.makeText(getActivity(), "Failed to update appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
