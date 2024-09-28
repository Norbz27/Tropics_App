package com.example.tropics_app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppointmentAdapter appointmentAdapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;

    public CalendarFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize CalendarView and RecyclerView
        CalendarView calendarView = view.findViewById(R.id.calendarView);
        recyclerView = view.findViewById(R.id.rcview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize appointment list and adapter
        appointmentList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(appointmentList);
        recyclerView.setAdapter(appointmentAdapter);

        // Load today's appointments on fragment creation
        loadAppointmentsForToday();

        // Set date click listener on CalendarView
        calendarView.setOnDayClickListener(eventDay -> {
            // Get the selected date and format it
            Calendar selectedCalendar = eventDay.getCalendar();
            String selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(selectedCalendar.getTime());

            // Load appointments for the selected date from Firestore
            loadAppointmentsFromFirestore(selectedDate);
        });

        return view;
    }

    // Method to load today's appointments
    private void loadAppointmentsForToday() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        loadAppointmentsFromFirestore(todayDate);
    }

    // Method to load appointments from Firestore based on the selected date
    private void loadAppointmentsFromFirestore(String selectedDate) {
        db.collection("appointments")
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        appointmentList.clear(); // Clear the list before adding new data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String fullName = document.getString("fullName");
                            String time = document.getString("time");
                            String date = document.getString("date");
                            double totalPrice = document.getDouble("totalPrice"); // Get totalPrice
                            String createdDateTime = document.getString("createdDateTime"); // Get createdDateTime

                            // Add appointment to list
                            Appointment appointment = new Appointment(fullName, date, time);
                            appointment.setTotalPrice(totalPrice);
                            appointment.setCreatedDateTime(createdDateTime); // Set the createdDateTime
                            appointmentList.add(appointment);

                            Log.d("Firestore", "Fetched appointment: " + fullName + " on " + date + " at " + time);
                        }
                        // Notify adapter that data has changed
                        appointmentAdapter.notifyDataSetChanged();
                    } else {
                        Log.w("Firestore", "Error getting documents.", task.getException());
                    }
                });
    }
}
