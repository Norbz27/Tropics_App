package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private ProgressBar progressBar;
    private FrameLayout progressContainer;

    private Map<String, List<Appointment>> appointmentCache = new HashMap<>();
    private Set<String> cachedEventDates = new HashSet<>();
    private boolean isCacheLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.rcview);
        progressContainer = view.findViewById(R.id.progressContainer);
        progressBar = view.findViewById(R.id.progressBar);
        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(appointmentAdapter);
        calendarView = view.findViewById(R.id.calendarView);

        selectedDate = getCurrentDate();
        loadAllAppointments();

        appointmentAdapter.setOnItemLongClickListener(this);
        showToday();

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

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                reloadFragment();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        return view;
    }
    private void reloadFragment() {
        // Reload the current fragment
        getParentFragmentManager().beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
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
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isCacheLoaded = false; // Reset cache flag
        appointmentCache.clear(); // Clear cache to prevent memory leaks
    }


    private void loadAllAppointments() {
        requireActivity().runOnUiThread(() -> progressContainer.setVisibility(View.VISIBLE)); // Show progress

        db.collection("appointments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    new Thread(() -> {
                        if (!isAdded()) return;

                        appointmentCache.clear();
                        cachedEventDates.clear();
                        List<EventDay> allEvents = new ArrayList<>();

                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            if (!isAdded()) return;

                            Appointment appointment = document.toObject(Appointment.class);
                            if (appointment != null) {
                                appointment.setId(document.getId());
                                appointmentCache.putIfAbsent(appointment.getDate(), new ArrayList<>());
                                appointmentCache.get(appointment.getDate()).add(appointment);

                                if (!cachedEventDates.contains(appointment.getDate())) {
                                    Calendar appointmentCalendar = Calendar.getInstance();
                                    try {
                                        Date appointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(appointment.getDate());
                                        appointmentCalendar.setTime(appointmentDate);
                                        Drawable threeIconsDrawable = getResources().getDrawable(R.drawable.sample_three_icons, null);
                                        allEvents.add(new EventDay(appointmentCalendar, threeIconsDrawable));
                                        cachedEventDates.add(appointment.getDate());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                        isCacheLoaded = true;

                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            calendarView.setEvents(allEvents);
                            progressContainer.setVisibility(View.GONE); // Hide progress
                            loadAppointmentData(selectedDate);
                        });
                    }).start();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e("LoadAppointments", "Error loading appointments: ", e);
                    Toast.makeText(getActivity(), "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressContainer.setVisibility(View.GONE);
                });
    }
    private void loadAppointmentData(String date) {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            if (!isAdded()) return; // Stop if fragment is closed

            List<Appointment> newAppointments = appointmentCache.getOrDefault(date, new ArrayList<>());

            newAppointments.sort((a1, a2) -> {
                try {
                    SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm");
                    Date time1 = sdf24.parse(a1.getTime());
                    Date time2 = sdf24.parse(a2.getTime());
                    return time1.compareTo(time2);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            });

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return; // Stop if fragment is closed
                appointmentList.clear();
                appointmentList.addAll(newAppointments);
                appointmentAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    @Override
    public void onItemLongClick(Appointment appointment) {
        showAppointmentOptionsDialog(appointment);
    }

    @Override
    public void onItemClick(Appointment appointment) {
        showViewAppointmentDialog(appointment);
    }

    private void populateAppointmentDetails(Appointment appointment, View dialogView) {
        TextView fullnameTextView = dialogView.findViewById(R.id.tvName);
        TextView emailTextView = dialogView.findViewById(R.id.tvEmail);
        TextView dateTextView = dialogView.findViewById(R.id.tvdate);
        TextView timeTextView = dialogView.findViewById(R.id.tvTime);
        TextView phoneNumberTextView = dialogView.findViewById(R.id.tvPhoneNumber);
        TextView serviceTextView = dialogView.findViewById(R.id.services);
        TextView totalPriceTextView = dialogView.findViewById(R.id.tvTotalPrice);

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

        // Format service details with indention and calculate total price
        StringBuilder serviceDetails = new StringBuilder();
        double totalPrice = 0.0;

        for (Map<String, Object> service : appointment.getServices()) {
            // Safe casting to String and double
            String parentServiceName = (String) service.get("parentServiceName");
            String serviceName = (String) service.get("serviceName");
            String assignEmployee = (String) service.get("assignedEmployee");
            Double servicePrice = service.get("servicePrice") != null ? (double) service.get("servicePrice") : 0.0;

            // Append parent service name
            if(!parentName.equals(parentServiceName)){
                serviceDetails.append(parentServiceName).append(": \n");
                parentName = parentServiceName;
            }

            // Append service name
            if(!servicePrice.equals(0.0)){
                serviceDetails.append("\t").append(serviceName).append(" - ₱")
                        .append(servicePrice).append("\n").append("\tAssigned Employee: "+assignEmployee + "\n");
            }else {
                serviceDetails.append("\t").append(serviceName).append("\n").append("\tAssigned Employee: "+assignEmployee + "\n");
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

    private void showViewAppointmentDialog(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.view_appointment1, null);
        parentName = "";

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
                    AlertDialog.Builder assignDialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
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

                                    // Update Firestore document
                                    appointmentRef.update("services", services)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("Firestore", "Employee assigned successfully.");

                                                // Refresh the data in the dialog
                                                populateAppointmentDetails(appointment, dialogView); // Refresh the dialog content
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

        populateAppointmentDetails(appointment, dialogView);
        // Close the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnClose.setOnClickListener(v -> dialog.dismiss());
    }

    private void showAppointmentOptionsDialog(Appointment appointment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        builder.setTitle("Options")
                .setItems(new CharSequence[]{"Edit Client Info", "Edit Selected Service", "Edit Time", "Delete"}, (dialog, optionIndex) -> {
                    if (optionIndex == 0) {
                        // Handle "Edit" action
                        showEditClientDialog(appointment); // Method to edit client information
                    } else if (optionIndex == 1) {
                        // Handle "Edit" action
                        viewServices(appointment); // Method to view/edit services
                    } else if(optionIndex == 2){
                        showEditTimeDialog(appointment);
                    }else if (optionIndex == 3) {
                        // Handle "Delete" action
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Delete Appointment")
                                .setMessage("Are you sure you want to delete this appointment?")
                                .setPositiveButton("Yes", (confirmDialog, confirmIndex) -> {
                                    deleteAppointment(appointment);
                                    Toast.makeText(getActivity(), "Appointment deleted successfully", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("No", (confirmDialog, confirmIndex) -> {
                                    confirmDialog.dismiss();
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                })
                .create()
                .show();
    }

    private void showEditClientDialog(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_client_info, null);

        // Initialize your input fields
        EditText edFirstname = dialogView.findViewById(R.id.edFirstname);
        EditText edLastname = dialogView.findViewById(R.id.edLastname);
        EditText edAddress = dialogView.findViewById(R.id.edAddress);
        EditText edPhone = dialogView.findViewById(R.id.edPhone);
        EditText edEmail = dialogView.findViewById(R.id.edEmail);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        // Set existing appointment data into input fields
        edFirstname.setText(appointment.getFirstName());
        edLastname.setText(appointment.getLastName());
        edAddress.setText(appointment.getAddress());
        edPhone.setText(appointment.getPhone());
        edEmail.setText(appointment.getEmail());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Submit button
        btnSubmit.setOnClickListener(v -> {
            String firstName = edFirstname.getText().toString().trim();
            String lastName = edLastname.getText().toString().trim();
            String fullName = firstName + " " + lastName;
            String address = edAddress.getText().toString().trim();
            String phone = edPhone.getText().toString().trim();
            String email = edEmail.getText().toString().trim();

            // Validate input if necessary

            // Create a map to hold the updated appointment data
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("fullName", fullName);
            updatedData.put("address", address);
            updatedData.put("phone", phone);
            updatedData.put("email", email);

            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false); // Disable dismissing the dialog on back button press
            progressDialog.show();
            // Query Firestore to find all appointments with the same full name
            db.collection("appointments")
                    .whereEqualTo("fullName", appointment.getFirstName() + " " + appointment.getLastName()) // Original full name
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                // Update each document found in the query
                                db.collection("appointments")
                                        .document(documentSnapshot.getId())
                                        .update(updatedData)
                                        .addOnSuccessListener(aVoid -> {
                                            progressDialog.dismiss();
                                            reloadFragment2();
                                            Date currentDate = new Date(); // Get current date
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                                            String formattedDate = sdf.format(currentDate);
                                            loadAppointmentData(formattedDate);
                                            Toast.makeText(getActivity(), "Appointment updated successfully!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            reloadFragment2();
                                            Date currentDate = new Date(); // Get current date
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                                            String formattedDate = sdf.format(currentDate);
                                            loadAppointmentData(formattedDate);
                                            Toast.makeText(getActivity(), "Error updating appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                            progressDialog.dismiss();
                            reloadFragment2();
                            Date currentDate = new Date(); // Get current date
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                            String formattedDate = sdf.format(currentDate);
                            loadAppointmentData(formattedDate);
                            dialog.dismiss();
                        } else {
                            progressDialog.dismiss();
                            reloadFragment2();
                            Date currentDate = new Date(); // Get current date
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                            String formattedDate = sdf.format(currentDate);
                            loadAppointmentData(formattedDate);
                            Toast.makeText(getActivity(), "No appointments found with the same name.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        reloadFragment2();
                        Date currentDate = new Date(); // Get current date
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                        String formattedDate = sdf.format(currentDate);
                        loadAppointmentData(formattedDate);
                        Toast.makeText(getActivity(), "Error querying appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void showEditTimeDialog(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_time, null);

        // Initialize your input fields
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        String selectedTime = appointment.getTime();
        if (selectedTime != null) {
            String[] timeParts = selectedTime.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());


        // Submit button
        btnSubmit.setOnClickListener(v -> {
            // Get the selected hour and minute from the TimePicker
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            // Format the time as a string (e.g., "HH:mm")
            String newTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);

            // Update the appointment object locally (optional, if needed)
            //appointment.setTime(newTime);
            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false); // Disable dismissing the dialog on back button press
            progressDialog.show();
            // Update the specific document in the "appointments" collection
            db.collection("appointments")
                    .document(appointment.getId()) // Use the document ID from the appointment object
                    .update("time", newTime) // Update the "time" field
                    .addOnSuccessListener(aVoid -> {
                        // Notify the user of success (e.g., Toast message)

                        loadAppointmentData(selectedDate);
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(), "Time updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss(); // Close the dialog
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error (e.g., Toast message)
                        Toast.makeText(getActivity(), "Failed to update time: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

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

    private void viewServices(Appointment appointment) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.view_services, null);
        parentName = "";
        TextView fullnameTextView = dialogView.findViewById(R.id.tvName);
        TextView emailTextView = dialogView.findViewById(R.id.tvEmail);
        TextView dateTextView = dialogView.findViewById(R.id.tvdate);
        TextView timeTextView = dialogView.findViewById(R.id.tvTime);
        TextView phoneNumberTextView = dialogView.findViewById(R.id.tvPhoneNumber);
        TextView totalPriceTextView = dialogView.findViewById(R.id.tvTotalPrice);

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
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);
        Button btnCalc = dialogView.findViewById(R.id.btnCalc);

        LinearLayout servicesContainer = dialogView.findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews(); // Ensure the container is cleared before adding views
        List<String> selectedParentServices = new ArrayList<>();
        List<String> selectedSubServices = new ArrayList<>();
        List<String> selectedSubSubServices = new ArrayList<>();
        for (Map<String, Object> service : appointment.getServices()) {
            String parentServiceName = (String) service.get("parentServiceName");
            String serviceName = (String) service.get("serviceName");

            List<Map<String, Object>> subsubServices = (List<Map<String, Object>>) service.get("subServices");
            selectedParentServices.add(parentServiceName);
            selectedSubServices.add(serviceName);
            if (subsubServices != null) {
                for (Map<String, Object> subService : subsubServices) {
                    String subServiceName = (String) subService.get("serviceName");
                    Log.e("CalendarFragment", "Sub-sub-service: " + subServiceName);

                    selectedSubSubServices.add(subServiceName); // Add all selected sub-sub-services

                }
            }
        }
        loadServices(servicesContainer, selectedSubServices, selectedSubSubServices, selectedParentServices);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();


        btnCalc.setOnClickListener(v -> {
            double totalPrice = 0.0;
            for (int i = 0; i < servicesContainer.getChildCount(); i++) {
                View serviceView = servicesContainer.getChildAt(i);
                LinearLayout subServiceContainer = serviceView.findViewById(R.id.subServiceContainer);

                for (int j = 0; j < subServiceContainer.getChildCount(); j++) {
                    View subServiceView = subServiceContainer.getChildAt(j);
                    CheckBox cbSubService = subServiceView.findViewById(R.id.cbSubService);

                    if (cbSubService.isChecked()) {
                        String priceText = ((TextView) subServiceView.findViewById(R.id.tvSubServicePrice)).getText().toString();
                        double price = parsePrice(priceText);
                        totalPrice += price;

                        // Check for sub-sub-services
                        LinearLayout subSubServiceContainer = subServiceView.findViewById(R.id.subSubServiceContainer);
                        for (int k = 0; k < subSubServiceContainer.getChildCount(); k++) {
                            View subSubServiceView = subSubServiceContainer.getChildAt(k);
                            CheckBox cbSubSubService = subSubServiceView.findViewById(R.id.cbSubSubService);
                            if (cbSubSubService.isChecked()) {
                                String subSubPriceText = ((TextView) subSubServiceView.findViewById(R.id.tvSubSubServicePrice)).getText().toString();
                                double subSubPrice = parsePrice(subSubPriceText);
                                totalPrice += subSubPrice;
                            }
                        }
                    }
                }
            }

            totalPriceTextView.setText(String.format(Locale.getDefault(), "₱%.2f", totalPrice));

        });

        btnSubmit.setOnClickListener(v -> {
            if(totalPriceTextView.getText().toString().equals("₱0.00")){
                Toast.makeText(getActivity(), "Calculate total price first", Toast.LENGTH_SHORT).show();
                return;
            }
            // Prepare to collect selected services
            List<SelectedService> selectedServices = new ArrayList<>();

            for (int i = 0; i < servicesContainer.getChildCount(); i++) {
                View serviceView = servicesContainer.getChildAt(i);
                LinearLayout subServiceContainer = serviceView.findViewById(R.id.subServiceContainer);

                // Get the parent service name
                String parentServiceName = ((TextView) serviceView.findViewById(R.id.tvServiceName2)).getText().toString();

                for (int j = 0; j < subServiceContainer.getChildCount(); j++) {
                    View subServiceView = subServiceContainer.getChildAt(j);
                    CheckBox cbSubService = subServiceView.findViewById(R.id.cbSubService);

                    if (cbSubService.isChecked()) {
                        String serviceName = ((CheckBox) subServiceView.findViewById(R.id.cbSubService)).getText().toString();
                        String priceText = ((TextView) subServiceView.findViewById(R.id.tvSubServicePrice)).getText().toString();
                        double price = parsePrice(priceText);

                        SelectedService selectedService = new SelectedService();
                        selectedService.setParentServiceName(parentServiceName);
                        selectedService.setSubServiceName(serviceName);
                        selectedService.setPrice(price);

                        // Check for sub-sub-services
                        LinearLayout subSubServiceContainer = subServiceView.findViewById(R.id.subSubServiceContainer);
                        List<SelectedService> subSubServices = new ArrayList<>();

                        for (int k = 0; k < subSubServiceContainer.getChildCount(); k++) {
                            View subSubServiceView = subSubServiceContainer.getChildAt(k);
                            CheckBox cbSubSubService = subSubServiceView.findViewById(R.id.cbSubSubService);

                            if (cbSubSubService.isChecked()) {
                                String subSubServiceName = ((CheckBox) subSubServiceView.findViewById(R.id.cbSubSubService)).getText().toString();
                                String subSubPriceText = ((TextView) subSubServiceView.findViewById(R.id.tvSubSubServicePrice)).getText().toString();
                                double subSubPrice = parsePrice(subSubPriceText);

                                SelectedService subSubService = new SelectedService(subSubServiceName, parentServiceName, serviceName, subSubPrice);
                                subSubServices.add(subSubService);
                            }
                        }

                        selectedService.setSubServices(subSubServices); // Set any selected sub-services
                        selectedServices.add(selectedService); // Add the service to the list
                    }
                }
            }

            // Calculate total price
            double totalPrice = 0.0;
            for (SelectedService service : selectedServices) {
                totalPrice += service.getTotalPrice(); // This will include the price of sub-services
            }

            // Prepare the appointment map to submit to Firestore
            Map<String, Object> newAppointment = new HashMap<>();
            newAppointment.put("totalPrice", totalPrice); // Add total price to Firestore

            // Prepare the services list
            if (!selectedServices.isEmpty()) {
                List<Map<String, Object>> serviceHierarchy = new ArrayList<>();
                for (SelectedService service : selectedServices) {
                    Map<String, Object> serviceMap = new HashMap<>();
                    addServiceToFirestore(serviceMap, service, 0); // Build the hierarchy
                    serviceHierarchy.add(serviceMap);
                }
                newAppointment.put("services", serviceHierarchy); // Store the hierarchical services list
            }

            // Get the document ID for the existing appointment (you should have this ID already)
            String appointmentId = appointment.getId(); // Replace with your actual appointment ID
            ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false); // Disable dismissing the dialog on back button press
            progressDialog.show();
            // First, delete the existing services array from the Firestore document
            db.collection("appointments").document(appointmentId)
                    .update("services", FieldValue.delete())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Now, add the new appointment data to Firestore
                            db.collection("appointments").document(appointmentId) // Use the same appointment ID to update
                                    .set(newAppointment, SetOptions.merge()) // Use set with merge to add new fields
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(getContext(), "Appointment submitted successfully!", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        progressDialog.dismiss();
                                        reloadFragment2();
                                        Date currentDate = new Date(); // Get current date
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                                        String formattedDate = sdf.format(currentDate);
                                        eventDays.clear();
                                        loadAppointmentData(formattedDate);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Error submitting appointment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();
                                        reloadFragment2();
                                        Date currentDate = new Date(); // Get current date
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                                        String formattedDate = sdf.format(currentDate);
                                        loadAppointmentData(formattedDate);
                                    });
                        } else {
                            progressDialog.dismiss();
                            Date currentDate = new Date(); // Get current date
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Define the date format
                            String formattedDate = sdf.format(currentDate);
                            loadAppointmentData(formattedDate);
                            reloadFragment2();
                            Toast.makeText(getContext(), "Error removing existing services: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
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

    private void loadServices(LinearLayout servicesContainer, List<String> selectedSubServices, List<String> selectedSubSubServices, List<String> selectedParentServices) {
        db.collection("service").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Don't clear existing services; just update them
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String serviceName = document.getString("service_name");
                    String serviceId = document.getId();

                    View serviceView = createServiceView(serviceName, serviceId, selectedSubServices, selectedSubSubServices, selectedParentServices);
                    servicesContainer.addView(serviceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load services", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private View createServiceView(String serviceName, String serviceId, List<String> selectedSubServices, List<String> selectedSubSubServices, List<String> selectedParentServices) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View serviceView = inflater.inflate(R.layout.accordion_service_item, null);

        TextView cbSubService = serviceView.findViewById(R.id.tvServiceName2); // parent checkbox
        LinearLayout subServiceContainer = serviceView.findViewById(R.id.subServiceContainer);
        ImageView ivExpandIcon = serviceView.findViewById(R.id.ivExpandIcon);

        cbSubService.setText(serviceName);
        subServiceContainer.setVisibility(View.GONE); // Set to GONE initially

        // Check if the current service is selected and expand it
        if (selectedParentServices.contains(serviceName)) {
            subServiceContainer.setVisibility(View.VISIBLE); // Make sub-service container visible
            ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Set expand icon
            loadSubServices(subServiceContainer, serviceId, selectedSubServices, selectedSubSubServices); // Load sub-services for the expanded service
        }

        // Toggle sub-services visibility
        ivExpandIcon.setOnClickListener(v -> {
            if (subServiceContainer.getVisibility() == View.GONE) {
                subServiceContainer.setVisibility(View.VISIBLE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Expand icon
                loadSubServices(subServiceContainer, serviceId, selectedSubServices, selectedSubSubServices); // Load sub-services
            } else {
                subServiceContainer.setVisibility(View.GONE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Collapse icon
            }
        });

        return serviceView;
    }

    private void loadSubServices(LinearLayout subServiceContainer, String serviceId, List<String> selectedSubServices, List<String> selectedSubSubServices) {
        db.collection("sub_services").whereEqualTo("main_service_id", serviceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                subServiceContainer.removeAllViews(); // Clear existing sub-services

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String subServiceName = document.getString("sub_service_name");
                    String subServiceId = document.getId();
                    Number priceNumber = document.getDouble("price"); // Retrieve price as Number
                    String price = priceNumber != null ? String.format("₱%.2f", priceNumber.doubleValue()) : "N/A"; // Format as string
                    if (price.equals("₱0.00")) {
                        price = "";
                    }

                    // Pass the selected sub-sub-services list to the view creation method
                    View subServiceView = createSubServiceView(subServiceName, price, subServiceId, selectedSubServices, selectedSubSubServices);
                    subServiceContainer.addView(subServiceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load sub-services", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private View createSubServiceView(String subServiceName, String price, String subServiceId, List<String> selectedSubServices, List<String> selectedSubSubServices) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subServiceView = inflater.inflate(R.layout.accordion_sub_service_item, null);

        TextView tvSubServicePrice = subServiceView.findViewById(R.id.tvSubServicePrice); // Price TextView
        CheckBox cbSubService = subServiceView.findViewById(R.id.cbSubService); // Sub service checkbox
        LinearLayout subSubServiceContainer = subServiceView.findViewById(R.id.subSubServiceContainer);
        ImageView ivExpandIcon = subServiceView.findViewById(R.id.ivExpandSubIcon);

        cbSubService.setText(subServiceName);
        tvSubServicePrice.setText(price); // Set price

        subSubServiceContainer.setVisibility(View.GONE);
        if (selectedSubServices.contains(subServiceName)) {
            cbSubService.setChecked(true);
            subSubServiceContainer.setVisibility(View.VISIBLE); // Make sub-service container visible
            ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Set expand icon
            loadSubSubServices(subSubServiceContainer, subServiceId, cbSubService, selectedSubSubServices);
        }

        // Load the sub-sub-services and pass the selected list


        ivExpandIcon.setOnClickListener(v -> {
            if (subSubServiceContainer.getVisibility() == View.GONE) {
                subSubServiceContainer.setVisibility(View.VISIBLE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Expand icon
                loadSubSubServices(subSubServiceContainer, subServiceId, cbSubService, selectedSubSubServices); // Pass the list of selected sub-sub-services
            } else {
                subSubServiceContainer.setVisibility(View.GONE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Collapse icon
            }
        });

        return subServiceView;
    }


    private void loadSubSubServices(LinearLayout subSubServiceContainer, String subServiceId, CheckBox parentCheckBox, List<String> selectedSubSubServices) {
        db.collection("sub_sub_services").whereEqualTo("sub_service_id", subServiceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                subSubServiceContainer.removeAllViews(); // Clear existing sub-sub-services

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String subSubServiceName = document.getString("sub_service_name");
                    Number priceNumber = document.getDouble("price"); // Retrieve price as Number
                    String price = priceNumber != null ? String.format("₱%.2f", priceNumber.doubleValue()) : "N/A"; // Format as string

                    // Create sub-sub-service view with a checkbox and price
                    View subSubServiceView = createSubSubServiceView(subSubServiceName, price, parentCheckBox, subSubServiceContainer, selectedSubSubServices);
                    subSubServiceContainer.addView(subSubServiceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load sub-sub-services", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private View createSubSubServiceView(String subSubServiceName, String price, CheckBox parentCheckBox, LinearLayout subSubServiceContainer, List<String> selectedSubSubServices) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subSubServiceView = inflater.inflate(R.layout.accordion_sub_sub_service_item, null);

        TextView tvSubSubServicePrice = subSubServiceView.findViewById(R.id.tvSubSubServicePrice); // Price TextView
        CheckBox cbSubSubService = subSubServiceView.findViewById(R.id.cbSubSubService); // Sub-sub-service checkbox

        cbSubSubService.setText(subSubServiceName);
        tvSubSubServicePrice.setText(price); // Set price

        if (selectedSubSubServices.contains(subSubServiceName)) {
            cbSubSubService.setChecked(true);
        }
        // Listen for checkbox state changes
        cbSubSubService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Automatically check the parent sub-service if any sub-sub-service is checked
                parentCheckBox.setChecked(true);
            } else {
                // If none of the sub-sub-services are checked, uncheck the parent checkbox
                boolean anyChecked = false;
                for (int i = 0; i < subSubServiceContainer.getChildCount(); i++) {
                    View childView = subSubServiceContainer.getChildAt(i);
                    CheckBox cbChild = childView.findViewById(R.id.cbSubSubService);
                    if (cbChild.isChecked()) {
                        anyChecked = true;
                        break;
                    }
                }
                if (!anyChecked) {
                    parentCheckBox.setChecked(false);
                }
            }
        });

        return subSubServiceView;
    }
    private double parsePrice(String priceText) {
        try {
            return Double.parseDouble(priceText.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0.0; // Default to 0.0 if parsing fails
        }
    }
    private void reloadFragment2() {
        Fragment currentFragment = getActivity().getSupportFragmentManager().findFragmentById(R.id.framelayout);
        if (currentFragment != null) {
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.detach(currentFragment).attach(currentFragment).commit();
        }
    }

}
