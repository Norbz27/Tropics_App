package com.example.tropics_app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
    private List<String> employeeIdList;  // Holds employee IDs for appointments

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
        });

        return view;
    }

    private String getCurrentDate() {
        // Format today's date as needed (e.g., "yyyy-MM-dd")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

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

    private void showSampleThreeIcons(View parentView) {
        ImageView imageView = parentView.findViewById(R.id.imageview1);
        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE); // Make the ImageView visible
            Drawable threeIconsDrawable = getResources().getDrawable(R.drawable.sample_three_icons, null);
            imageView.setImageDrawable(threeIconsDrawable); // Set the drawable to the ImageView
        }
    }

    private void hideSampleThreeIcons(View parentView) {
        ImageView imageView = parentView.findViewById(R.id.imageview1);
        if (imageView != null) {
            imageView.setVisibility(View.GONE); // Hide the ImageView
        }
    }

    @Override
    public void onItemLongClick(Appointment appointment) {
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
        TextView fullnameTextView = dialogView.findViewById(R.id.tvName);
        TextView emailTextView = dialogView.findViewById(R.id.tvEmail);
        TextView dateTextView = dialogView.findViewById(R.id.tvdate);
        TextView timeTextView = dialogView.findViewById(R.id.tvTime);
        TextView phoneNumberTextView = dialogView.findViewById(R.id.tvPhoneNumber);
        TextView empNameTextView = dialogView.findViewById(R.id.empname);
        TextView serviceTextView = dialogView.findViewById(R.id.services);
        TextView totalPriceTextView = dialogView.findViewById(R.id.tvTotalPrice);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        // Set appointment details
        fullnameTextView.setText(appointment.getFullName());
        emailTextView.setText("Email: " + appointment.getEmail());
        dateTextView.setText("Date: " + appointment.getDate());
        timeTextView.setText("Time: " + appointment.getTime());
        phoneNumberTextView.setText("Phone: " + appointment.getPhone());

        // Close the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Fetch employee name using Firestore if available
        String employeeId = appointment.getEmployeeId(); // Assuming you have this method
        if (employeeId == null || employeeId.isEmpty()) {
            empNameTextView.setText("Employee ID not available");
            return; // Exit early if the employee ID is invalid
        }

        db.collection("Employees").document(employeeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String employeeName = documentSnapshot.getString("name"); // Adjust field name if necessary
                        empNameTextView.setText(employeeName);
                    } else {
                        empNameTextView.setText("Employee not found");
                    }
                })
                .addOnFailureListener(e -> {
                    empNameTextView.setText("Error fetching employee");
                    Log.e("ViewAppointment", "Error fetching employee: ", e);
                });

        // Format service details and calculate total price
        StringBuilder serviceDetails = new StringBuilder();
        double totalPrice = 0.0;

        for (Map<String, Object> service : appointment.getServices()) {
            // Safe casting to String and double
            String parentServiceName = (String) service.get("parentServiceName");
            String serviceName = (String) service.get("serviceName");
            Double servicePrice = service.get("servicePrice") != null ? (double) service.get("servicePrice") : 0.0;

            serviceDetails.append(parentServiceName).append(": ")
                    .append(serviceName).append(" - ")
                    .append(servicePrice).append(" PHP\n");

            totalPrice += servicePrice; // Accumulate total price
        }

        // Set the services and total price in the dialog
        serviceTextView.setText(serviceDetails.toString());
        totalPriceTextView.setText("Total Price: " + totalPrice + " PHP");
    }

    private void showAppointmentOptionsDialog(Appointment appointment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Options")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditAppointmentDialog(appointment);
                            break;
                        case 1:
                            deleteAppointment(appointment);
                            break;
                    }
                })
                .create()
                .show();
    }
    private void deleteAppointment(Appointment appointment) {
        db.collection("appointments").document(appointment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Appointment deleted", Toast.LENGTH_SHORT).show();
                    loadAppointmentData(selectedDate); // Refresh data
                })
                .addOnFailureListener(e -> {
                    Log.e("DeleteAppointment", "Error deleting appointment: ", e);
                    Toast.makeText(getActivity(), "Failed to delete appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditAppointmentDialog(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogbox_editappointment, null);

        EditText fullNameEditText = dialogView.findViewById(R.id.etFullName);
        EditText dateEditText = dialogView.findViewById(R.id.etDate);
        EditText timeEditText = dialogView.findViewById(R.id.etTime);
        Spinner spinnerEmployees = dialogView.findViewById(R.id.employee); // Spinner for employee selection
        Button applyChangesButton = dialogView.findViewById(R.id.empsub); // Apply changes button

        fullNameEditText.setText(appointment.getFullName());
        dateEditText.setText(appointment.getDate());
        timeEditText.setText(appointment.getTime());

        // Load employees into spinner
        employeeIdList = new ArrayList<>(); // Initialize employeeIdList
        loadEmployees(spinnerEmployees, employeeIdList);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        // Set click listener for the Apply Changes button
        applyChangesButton.setOnClickListener(v -> {
            String newFullName = fullNameEditText.getText().toString();
            String newDate = dateEditText.getText().toString();
            String newTime = timeEditText.getText().toString();
            String selectedEmployeeId = employeeIdList.get(spinnerEmployees.getSelectedItemPosition());

            // Update appointment details
            updateAppointment(appointment.getId(), newFullName, newDate, newTime, selectedEmployeeId);

            // Dismiss dialog after applying changes
            dialog.dismiss();
        });
    }

    private void loadEmployees(Spinner spinnerEmployees, List<String> employeeIdList) {
        List<String> employeeList = new ArrayList<>();

        db.collection("Employees")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        String employeeName = documentSnapshot.getString("name");
                        String employeeId = documentSnapshot.getId(); // Get the document ID
                        employeeList.add(employeeName);
                        employeeIdList.add(employeeId); // Add employee ID to the list
                    }

                    // Set up the spinner with employee names
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, employeeList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerEmployees.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e("LoadEmployees", "Error loading employees: ", e);
                    Toast.makeText(getActivity(), "Failed to load employees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAppointment(String appointmentId, String fullName, String date, String time, String employeeId) {
        Map<String, Object> updatedAppointment = new HashMap<>();
        updatedAppointment.put("fullName", fullName);
        updatedAppointment.put("date", date);
        updatedAppointment.put("time", time);
        updatedAppointment.put("employeeId", employeeId); // Update with the selected employee ID

        db.collection("appointments").document(appointmentId)
                .update(updatedAppointment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Appointment updated", Toast.LENGTH_SHORT).show();
                    loadAppointmentData(selectedDate); // Refresh appointment data after update
                })
                .addOnFailureListener(e -> {
                    Log.e("UpdateAppointment", "Error updating appointment: ", e);
                    Toast.makeText(getActivity(), "Failed to update appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
