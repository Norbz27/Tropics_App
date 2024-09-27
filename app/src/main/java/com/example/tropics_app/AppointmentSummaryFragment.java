package com.example.tropics_app;

import android.annotation.SuppressLint;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class AppointmentSummaryFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private TextView tvFullName, tvAddress, tvPhone, tvEmail;
    private View view;
    private String parentname = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_appointment_summary, container, false);

        LinearLayout summaryContainer = view.findViewById(R.id.summaryContainer);
        CalendarView disCalendar = view.findViewById(R.id.disCalendar);
        TimePicker disTime = view.findViewById(R.id.disTime);

        List<EventDay> events = new ArrayList<>();

        // Display current date with custom background
        Calendar calendarnow = Calendar.getInstance();
        EventDay todayEvent = new EventDay(calendarnow, R.drawable.custom_selector2);
        events.add(todayEvent);

        // Get the selected date from ViewModel
        String selectedDate = viewModel.getSelectedDate();
        if (selectedDate != null) {
            try {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
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
        }

        viewModel.getSelectedServices().observe(getViewLifecycleOwner(), services -> {
            summaryContainer.removeAllViews();
            Log.d("AppointmentSummaryFragment", "Selected Services: " + services);

            if (services.isEmpty()) {
                TextView emptyTextView = new TextView(getContext());
                emptyTextView.setText("No services selected.");
                emptyTextView.setTextColor(getResources().getColor(android.R.color.white));
                summaryContainer.addView(emptyTextView);
            } else {
                // Reset parentname for each load
                parentname = "";
                for (SelectedService service : services) {
                    Log.d("Service", "Parent Service: " + service.getParentServiceName());
                    addServiceToSummary(summaryContainer, service, 0); // Start at indentation level 0
                }
            }
        });

        // Initialize TextViews
        tvFullName = view.findViewById(R.id.tvFname);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);

        Button backButton = view.findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        });

        Button nextButton = view.findViewById(R.id.btnSubmitAppointment);
        nextButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        });

        // Call the update method to display the latest data
        updateSummary();

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();

        updateSummary();
    }
    private void updateSummary() {
        tvFullName.setText(viewModel.getFullName());
        tvAddress.setText(viewModel.getAddress());
        tvPhone.setText(viewModel.getPhone());
        tvEmail.setText(viewModel.getEmail());
    }

    private void addServiceToSummary(LinearLayout parent, SelectedService service, int indentLevel) {
        Log.d("addServiceToSummary", "Adding service: " + service.getName() + " at level: " + indentLevel);

        Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.manrope); // Replace your_font

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

        // Add SubService
        TextView subServiceTextView = new TextView(getContext());
        subServiceTextView.setText(service.getName());
        subServiceTextView.setPadding((indentLevel + 1) * 30, 0, 0, 0);
        subServiceTextView.setTextColor(Color.parseColor("#B6B6B6"));
        subServiceTextView.setTypeface(typeface); // Set typeface
        parent.addView(subServiceTextView);

        // Recursively add sub-sub-services
        for (SelectedService subService : service.getSubServices()) {
            TextView subSubServiceTextView = new TextView(getContext());
            subSubServiceTextView.setText(subService.getName());
            subSubServiceTextView.setPadding((indentLevel + 2) * 30, 0, 0, 0);
            subSubServiceTextView.setTextColor(Color.parseColor("#B6B6B6"));
            subSubServiceTextView.setTypeface(typeface); // Set typeface
            parent.addView(subSubServiceTextView);
        }
    }
}
