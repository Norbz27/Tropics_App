package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentDateTimeFragment extends Fragment {
    private AppointmentViewModel viewModel;
    Date selectedDate;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_date_time, container, false);

        CalendarView calendarView = view.findViewById(R.id.calendarView);
        TimePicker timePicker = view.findViewById(R.id.timePicker);
        Button nextButton = view.findViewById(R.id.btnNext);
        List<EventDay> events = new ArrayList<>();
        // Handle date selection
        calendarView.setOnDayClickListener(eventDay -> {
            events.clear();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            selectedDate = eventDay.getCalendar().getTime();
            String formattedDate = sdf.format(selectedDate);
            viewModel.setSelectedDate(formattedDate);
            Toast.makeText(getActivity(), formattedDate, Toast.LENGTH_SHORT).show();
            if (selectedDate != null) {
                try {
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date startDate = dateFormat.parse(formattedDate);

                    // Create a Calendar instance for the selected date
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(startDate);

                    // Set up EventDay for the selected date
                    EventDay selectedEvent = new EventDay(calendar, R.drawable.custom_selector); // Use your custom drawable for the selected date
                    events.add(selectedEvent);

                    // Add the selected event to the CalendarView
                    calendarView.setEvents(events); // Set all events (current date and selected date)
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        // Handle time selection
        nextButton.setOnClickListener(v -> {
            if (selectedDate != null) {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                viewModel.setSelectedTime(selectedTime);

                // Navigate to the next fragment
                ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
            } else {
                // Show a dialog
                new AlertDialog.Builder(getContext())
                        .setTitle("No Date Selected")
                        .setMessage("Please select a date to continue.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Do nothing (or handle OK click if needed)
                        })
                        .show();
            }
        });

        Button backButton = view.findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        });
        return view;
    }

}