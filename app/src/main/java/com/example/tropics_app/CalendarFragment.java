package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
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
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
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
    private List<String> employeeIdList;
    private String parentName = "";
    private List<CalendarDay> eventDays = new ArrayList<>();
    private List<CalendarDay> today = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView and AppointmentAdapter
        recyclerView = view.findViewById(R.id.rcview);
        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(appointmentAdapter);
        calendarView = view.findViewById(R.id.calendarView);
        // Load today's appointments
        selectedDate = getCurrentDate();
        loadAppointmentData(selectedDate);

        // Set the long click listener on your adapter
        appointmentAdapter.setOnItemLongClickListener(this);
        showToday();
        // Set up the calendar view
        calendarView.setOnDayClickListener(event -> {
            eventDays.clear();
            Date clickedDateString = event.getCalendar().getTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(clickedDateString);

            CalendarDay calendarDay = new CalendarDay(calendar);
            calendarDay.setBackgroundResource(R.drawable.circle_indicator);
            calendarDay.setLabelColor(R.color.white);
            eventDays.add(calendarDay);
            showToday();
            calendarView.setCalendarDays(eventDays);

            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(event.getCalendar().getTime());
            loadAppointmentData(selectedDate);
        });

        return view;
    }
    private void showToday(){
        Calendar calendarnow = Calendar.getInstance();
        CalendarDay calendarDaynow = new CalendarDay(calendarnow);
        calendarDaynow.setBackgroundResource(R.drawable.circle_indicator_day);
        eventDays.add(calendarDaynow);
        calendarView.setCalendarDays(eventDays);
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


                })
                .addOnFailureListener(e -> {
                    Log.e("LoadAppointments", "Error loading appointments: ", e);
                    Toast.makeText(getActivity(), "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        parentName = "";
        // Find TextViews
        TextView fullnameTextView = dialogView.findViewById(R.id.tvName);
        TextView emailTextView = dialogView.findViewById(R.id.tvEmail);
        TextView dateTextView = dialogView.findViewById(R.id.tvdate);
        TextView timeTextView = dialogView.findViewById(R.id.tvTime);
        TextView phoneNumberTextView = dialogView.findViewById(R.id.tvPhoneNumber);
        TextView serviceTextView = dialogView.findViewById(R.id.services);
        TextView totalPriceTextView = dialogView.findViewById(R.id.tvTotalPrice);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnAssign = dialogView.findViewById(R.id.btnAssign);
        btnAssign.setOnClickListener(v -> {
            // Fetch employee names from Firestore collection
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference employeesRef = db.collection("Employees");

            // Create the dialog view for assigning employees
            LayoutInflater inflaterAssign = getLayoutInflater();
            View assignDialogView = inflaterAssign.inflate(R.layout.dialog_assign_sub_services, null);

            // LinearLayout to hold the dynamically added Spinner views
            LinearLayout subServiceContainer = assignDialogView.findViewById(R.id.subServiceContainer);

            // Fetch employee names and proceed with setting up the dialog
            employeesRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<String> employeeNames = new ArrayList<>();
                    employeeNames.add("None");
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String employeeName = document.getString("name");
                        if (employeeName != null) {
                            employeeNames.add(employeeName);
                        }
                    }

                    // Iterate through the services and sub-services to create Spinners
                    for (Map<String, Object> service : appointment.getServices()) {
                        String subServiceName = (String) service.get("serviceName");
                        String assignedEmployee = (String) service.get("assignedEmployee"); // Fetch the assigned employee if it exists

                        // Create a TextView to display the sub-service name
                        TextView subServiceTextView = new TextView(getActivity());
                        subServiceTextView.setText(subServiceName);
                        subServiceTextView.setPadding(16, 16, 16, 0); // Optional padding

                        // Create a Spinner to assign the employee
                        Spinner employeeSpinner = new Spinner(getActivity());
                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(),
                                android.R.layout.simple_spinner_item, employeeNames);
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        employeeSpinner.setAdapter(spinnerAdapter);

                        // If an employee is already assigned, set that as the selected item
                        if (assignedEmployee != null && employeeNames.contains(assignedEmployee)) {
                            employeeSpinner.setSelection(employeeNames.indexOf(assignedEmployee));
                        }

                        // Add the TextView and Spinner to the container
                        subServiceContainer.addView(subServiceTextView);
                        subServiceContainer.addView(employeeSpinner);
                    }

                    // Build and display the dialog
                    AlertDialog.Builder assignDialogBuilder = new AlertDialog.Builder(getActivity());
                    assignDialogBuilder.setView(assignDialogView)
                            .setTitle("Assign Employees")
                            .setPositiveButton("Assign", (dialogInterface, which) -> {
                                // Handle assigning employees here
                                int childCount = subServiceContainer.getChildCount();
                                for (int i = 0; i < childCount; i += 2) { // Skip the TextViews (i.e., every second view)
                                    TextView subServiceTextView = (TextView) subServiceContainer.getChildAt(i);
                                    Spinner employeeSpinner = (Spinner) subServiceContainer.getChildAt(i + 1);

                                    String subServiceName = subServiceTextView.getText().toString();
                                    String assignedEmployee = employeeSpinner.getSelectedItem().toString();

                                    // Log assignment (for debug)
                                    Log.d("Assignment", "Sub-service: " + subServiceName + ", Employee: " + assignedEmployee);

                                    // Update the Firestore document
                                    DocumentReference appointmentRef = db.collection("appointments").document(appointment.getId());

                                    // Update the specific service in the services array
                                    List<Map<String, Object>> services = appointment.getServices();
                                    for (Map<String, Object> service : services) {
                                        if (subServiceName.equals(service.get("serviceName"))) {
                                            // Add the assigned employee to the service
                                            service.put("assignedEmployee", assignedEmployee);
                                            break;
                                        }
                                    }

                                    // Update the document in Firestore
                                    appointmentRef.update("services", services)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("Firestore", "Employee assigned successfully.");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w("Firestore", "Error updating document", e);
                                            });
                                }
                            })
                            .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss());

                    // Show the assign dialog
                    AlertDialog assignDialog = assignDialogBuilder.create();
                    assignDialog.setOnShowListener(dialogInterface -> {
                        // Load the custom font
                        Typeface manrope = ResourcesCompat.getFont(getActivity(), R.font.manrope);

                        // Set the custom font for the positive button ("Assign")
                        Button positiveButton = assignDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.orange));
                        positiveButton.setTypeface(manrope);

                        // Set the custom font for the negative button ("Cancel")
                        Button negativeButton = assignDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                        negativeButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.orange));
                        negativeButton.setTypeface(manrope);

                        // Iterate through the views in the dialog and set the custom font
                        int childCount = subServiceContainer.getChildCount();
                        for (int i = 0; i < childCount; i++) {
                            View child = subServiceContainer.getChildAt(i);

                            if (child instanceof TextView) {
                                ((TextView) child).setTypeface(manrope); // Set custom font for TextViews
                            }
                        }
                    });
                    assignDialog.show();
                } else {
                    Log.d("Firestore", "Error getting documents: ", task.getException());
                }
            });
        });


        // Set appointment details
        fullnameTextView.setText(appointment.getFullName());
        emailTextView.setText("Email: " + appointment.getEmail());

        // Assume these are the input formats of the strings
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm");

        // Desired output formats
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy");
        SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a");

        try {
            // Parse the date and time strings to Date objects
            String appointmentDateStr = appointment.getDate(); // "2024-01-27"
            String appointmentTimeStr = appointment.getTime(); // "15:30"

            Date appointmentDate = inputDateFormat.parse(appointmentDateStr);
            Date appointmentTime = inputTimeFormat.parse(appointmentTimeStr);

            // Format the Date objects to the desired format
            dateTextView.setText("Date: " + outputDateFormat.format(appointmentDate));
            timeTextView.setText("Time: " + outputTimeFormat.format(appointmentTime));

        } catch (ParseException e) {
            e.printStackTrace(); // Handle the exception if parsing fails
            dateTextView.setText("Date: Invalid");
            timeTextView.setText("Time: Invalid");
        }
        phoneNumberTextView.setText("Phone: " + appointment.getPhone());

        // Close the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Format service details with indention and calculate total price
        StringBuilder serviceDetails = new StringBuilder();
        double totalPrice = 0.0;

        for (Map<String, Object> service : appointment.getServices()) {
            // Safe casting to String and double
            String parentServiceName = (String) service.get("parentServiceName");
            String serviceName = (String) service.get("serviceName"); // Check for sub-services
            Double servicePrice = service.get("servicePrice") != null ? (double) service.get("servicePrice") : 0.0;

            // Append parent service name
            if(!parentName.equals(parentServiceName)){
                serviceDetails.append(parentServiceName).append(": \n");
                parentName = parentServiceName;
            }

            // Append service name
            if(!servicePrice.equals(0.0)){
                serviceDetails.append("\t").append(serviceName).append(" - ₱")
                        .append(servicePrice).append("\n");
            }else {
                serviceDetails.append("\t").append(serviceName).append("\n");
            }


            totalPrice += servicePrice; // Accumulate total price

            // Check for sub-sub-services
            List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
            if (subServices != null) {
                for (Map<String, Object> subService : subServices) {
                    String subServiceName = (String) subService.get("serviceName");
                    Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;

                    if(!subServicePrice.equals(0.0)){
                        serviceDetails.append("\t\t").append(subServiceName).append(" - ₱")
                                .append(subServicePrice).append("\n");
                    }else {
                        serviceDetails.append("\t\t").append(subServiceName).append("\n");
                    }

                    totalPrice += subServicePrice;
                }
            }
        }

        // Set the services and total price in the dialog
        serviceTextView.setText(serviceDetails.toString());
        totalPriceTextView.setText("₱" + totalPrice);
    }



    private void showAppointmentOptionsDialog(Appointment appointment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Options")
                .setItems(new CharSequence[]{"Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
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
}
