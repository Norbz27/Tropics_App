package com.example.tropics_app;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import java.text.NumberFormat;
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


public class SalesTrackingFragment extends Fragment {
    private Button btnDaily, btnMonthly;
    private Spinner monthSpinner, yearSpinner;
    private BarChart barChart;
    private boolean isDaily = true;
    private int targetMonth, targetYear;
    private TextView tvAverage, tvHigh, tvLow, tvTotal;
    private List<Appointment> appointmentsList;
    private FirebaseFirestore db;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        appointmentsList = new ArrayList<>();
        fetchAppointmentData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_sales_tracking, container, false);

        tvAverage = view.findViewById(R.id.tvAverage);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvHigh = view.findViewById(R.id.tvHigh);
        tvLow = view.findViewById(R.id.tvLow);

        // Find LineChart and Buttons from layout
        barChart = view.findViewById(R.id.barChart);
        btnDaily = view.findViewById(R.id.btnDaily);
        btnMonthly = view.findViewById(R.id.btnMonthly);
        // Inside your Fragment or Activity
        monthSpinner = view.findViewById(R.id.month_spinner);
        yearSpinner = view.findViewById(R.id.year_spinner);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.months_array, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH); // Note: Months are 0-based (January = 0)
        int currentYear = calendar.get(Calendar.YEAR);
        targetMonth = currentMonth; // Current month (0-indexed)
        targetYear = currentYear;

        List<String> years = new ArrayList<>();
        for (int year = 2024; year <= currentYear; year++) {
            years.add(String.valueOf(year));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        monthSpinner.setSelection(currentMonth); // Set to current month
        yearSpinner.setSelection(years.indexOf(String.valueOf(currentYear))); // Set to current year

        btnDaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedMonth = monthSpinner.getSelectedItem().toString();
                int monthIndex = getMonthIndex(selectedMonth);
                int selectedYear = Integer.parseInt(yearSpinner.getSelectedItem().toString());
                setDailyData(appointmentsList, monthIndex, selectedYear);
                monthSpinner.setVisibility(View.VISIBLE);
                btnDaily.setBackgroundResource(R.drawable.button_daily_checked);
                btnMonthly.setBackgroundResource(R.drawable.button_monthly);
            }
        });

        btnMonthly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedYear = Integer.parseInt(yearSpinner.getSelectedItem().toString());
                setMonthlyData(appointmentsList, selectedYear);
                isDaily = false;
                monthSpinner.setVisibility(View.GONE);
                btnDaily.setBackgroundResource(R.drawable.button_daily);
                btnMonthly.setBackgroundResource(R.drawable.button_monthly_checked);
            }
        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonth = parent.getItemAtPosition(position).toString();
                Log.d("SalesFragment", "Selected Month: " + selectedMonth);
                int monthIndex = getMonthIndex(selectedMonth); // Convert month to int
                int selectedYear = Integer.parseInt(yearSpinner.getSelectedItem().toString());
                setDailyData(appointmentsList, monthIndex, selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedYear = parent.getItemAtPosition(position).toString();
                Log.d("SalesFragment", "Selected Year: " + selectedYear);
                int selectedYear2 = Integer.parseInt(parent.getItemAtPosition(position).toString()); // Convert year to int
                int monthIndex = getMonthIndex(monthSpinner.getSelectedItem().toString());
                if(isDaily){
                    setDailyData(appointmentsList, monthIndex, selectedYear2);
                }else {
                    setMonthlyData(appointmentsList, selectedYear2);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return view;
    }

    private void fetchAppointmentData() {
        db.collection("appointments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            appointmentsList.clear(); // Clear the list before adding new data
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Appointment appointment = document.toObject(Appointment.class);
                                appointmentsList.add(appointment);
                            }
                            setDailyData(appointmentsList, targetMonth, targetYear);
                            Calendar calendarCur = Calendar.getInstance();
                            int hour = calendarCur.get(Calendar.HOUR_OF_DAY); // Get the current hour (24-hour format)

                            if (hour < 1) { // If the current time is before 1 AM
                                calendarCur.add(Calendar.DAY_OF_YEAR, -1); // Move the date to yesterday
                            }

                            /*Date date = calendarCur.getTime(); // Get the updated date
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                            String formattedDate = dateFormat.format(date);

                            filterDataByDate(formattedDate);*/
                        } else {
                            Log.e("SalesFragment", "Error fetching appointments: ", task.getException());
                        }
                    }
                });
    }

    private int getMonthIndex(String monthName) {
        String[] months = getResources().getStringArray(R.array.months_array);
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(monthName)) {
                return i; // Return the month index (0-11)
            }
        }
        return -1; // Return -1 if not found (error case)
    }
    private void setDailyData(List<Appointment> appointmentsList, int targetMonth, int targetYear) {
        ArrayList<BarEntry> dailyEntries = new ArrayList<>();
        Map<Integer, Float> dailySales = new HashMap<>();

        float totalSales = 0f;
        int daysWithSales = 0;

        float highestSales = Float.MIN_VALUE;
        float lowestSales = Float.MAX_VALUE;

        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);

        Calendar calendar = Calendar.getInstance();
        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getClientDateTimeAsDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // Check if the appointment is in the target month and year and before or on today
                if (month == targetMonth && year == targetYear &&
                        (year < currentYear || (year == currentYear && month < currentMonth) ||
                                (year == currentYear && month == currentMonth && day <= currentDay))) {

                    List<Map<String, Object>> services = appointment.getServices();
                    for (Map<String, Object> service : services) {
                        String assignedEmployee = (String) service.get("assignedEmployee");
                        if (assignedEmployee == null || assignedEmployee.equals("None")) {
                            continue;
                        }

                        double totalPriceForParentService = (Double) service.get("servicePrice");

                        List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                        if (subServices != null) {
                            for (Map<String, Object> subService : subServices) {
                                Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;
                                totalPriceForParentService += subServicePrice;
                            }
                        }

                        // Update the daily sales map
                        float sales = dailySales.getOrDefault(day, 0f);
                        float newSales = sales + (float) totalPriceForParentService;
                        dailySales.put(day, newSales);
                        totalSales += (float) totalPriceForParentService;

                        // If this day has sales, count it
                        if (sales == 0f) {
                            daysWithSales++;
                        }

                        // Track highest sales
                        if (newSales > highestSales) highestSales = newSales;
                    }
                }
            }
        }

        // Recalculate the correct lowest sales
        lowestSales = Float.MAX_VALUE;
        for (Map.Entry<Integer, Float> entry : dailySales.entrySet()) {
            float sales = entry.getValue();
            if (sales > 0f && sales < lowestSales) {
                lowestSales = sales;
            }
        }

        // If no valid sales exist, set lowestSales to 0
        if (lowestSales == Float.MAX_VALUE) {
            lowestSales = 0f;
        }

        // Determine the number of days in the target month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Create a list of bar colors based on the gradient
        List<Integer> barColors = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            float sales = dailySales.getOrDefault(day, 0f);
            dailyEntries.add(new BarEntry(day, sales));

            // For gradient, use LinearGradient for the dataset
            Shader shader = new LinearGradient(0f, 0f, 0f, 300f,
                    ContextCompat.getColor(requireContext(), R.color.orange), // Start color
                    ContextCompat.getColor(requireContext(), R.color.yellow), // End color
                    Shader.TileMode.MIRROR); // Optional TileMode for repetition
        }

        // Format the currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        // Calculate and display the daily average sales based on actual days with sales
        float averageSales = (daysWithSales > 0) ? totalSales / daysWithSales : 0f;
        tvAverage.setText(currencyFormat.format(averageSales));
        tvTotal.setText(currencyFormat.format(totalSales));
        tvHigh.setText("Highest Sales: " + currencyFormat.format(highestSales));
        tvLow.setText("Lowest Sales: " + currencyFormat.format(lowestSales));

        // Set up the bar chart data
        BarDataSet dataSet = new BarDataSet(dailyEntries, null);

        // Apply gradient shader
        dataSet.setGradientColor(ContextCompat.getColor(requireContext(), R.color.orange), ContextCompat.getColor(requireContext(), R.color.yellow));

        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.setMarker(new CustomMarkerView(requireContext(), R.layout.custom_marker_view));
        barChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray));
        barChart.setFitBars(true);
        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);

        // Set up the X-Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%02d-%02d", targetMonth + 1, (int)value);
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(daysInMonth);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisMaximum(daysInMonth);
        xAxis.setAxisMinimum(0.5f);

        // Set up the Y-Axis
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f); // Force Y-axis to start at 0

        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
    }


    private void setMonthlyData(List<Appointment> appointmentsList, int selectedYear) {
        ArrayList<BarEntry> monthlyEntries = new ArrayList<>();
        Map<Integer, Float> monthlySales = new HashMap<>();

        float totalSales = 0f;
        int monthsWithSales = 0;

        float highestSales = Float.MIN_VALUE;
        float lowestSales = Float.MAX_VALUE;

        Calendar today = Calendar.getInstance();
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentYear = today.get(Calendar.YEAR);

        // Initialize monthly sales
        for (int month = 1; month <= 12; month++) {
            monthlySales.put(month, 0f);
        }

        Calendar calendar = Calendar.getInstance();
        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getClientDateTimeAsDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;

                if (year == selectedYear) {
                    List<Map<String, Object>> services = appointment.getServices();
                    for (Map<String, Object> service : services) {
                        String assignedEmployee = (String) service.get("assignedEmployee");
                        if (assignedEmployee == null || assignedEmployee.equals("None")) {
                            continue;
                        }

                        double totalPriceForParentService = (Double) service.get("servicePrice");

                        List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                        if (subServices != null) {
                            for (Map<String, Object> subService : subServices) {
                                Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;
                                totalPriceForParentService += subServicePrice;
                            }
                        }

                        float sales = monthlySales.get(month);
                        float newSales = sales + (float) totalPriceForParentService;
                        monthlySales.put(month, newSales);
                        totalSales += (float) totalPriceForParentService;

                        if (newSales > highestSales) highestSales = newSales;
                        if (newSales < lowestSales) lowestSales = newSales;
                    }
                }
            }
        }

        // Recalculate monthsWithSales to count only months with actual sales
        for (Map.Entry<Integer, Float> entry : monthlySales.entrySet()) {
            if (entry.getValue() > 0f) {
                monthsWithSales++;
            }
        }

        // Recalculate the correct lowest sales
        lowestSales = Float.MAX_VALUE;
        for (Map.Entry<Integer, Float> entry : monthlySales.entrySet()) {
            float sales = entry.getValue();
            if (sales > 0f && sales < lowestSales) {
                lowestSales = sales;
            }
        }

        // If no valid sales exist, set lowestSales to 0
        if (lowestSales == Float.MAX_VALUE) {
            lowestSales = 0f;
        }

        // Create the Bar Entries for each month
        for (int month = 1; month <= 12; month++) {
            float salesValue = (month <= currentMonth || selectedYear < currentYear) ? monthlySales.get(month) : 0f;
            monthlyEntries.add(new BarEntry(month, salesValue));
        }

        // Calculate the average sales
        float averageSales = (monthsWithSales > 0) ? totalSales / monthsWithSales : 0f;

        // Update UI with results
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        tvTotal.setText(currencyFormat.format(totalSales));
        tvAverage.setText(currencyFormat.format(averageSales));
        tvHigh.setText("Highest Sales: " + currencyFormat.format(highestSales));
        tvLow.setText("Lowest Sales: " + currencyFormat.format(lowestSales));

        BarDataSet dataSet = new BarDataSet(monthlyEntries, null);

        // Create a gradient using color resources
        Shader shader = new LinearGradient(0f, 0f, 0f, 300f,
                ContextCompat.getColor(requireContext(), R.color.orange),
                ContextCompat.getColor(requireContext(), R.color.yellow),
                Shader.TileMode.MIRROR);
        dataSet.setGradientColor(ContextCompat.getColor(requireContext(), R.color.orange), ContextCompat.getColor(requireContext(), R.color.yellow));

        // Set properties for the dataset
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.setMarker(new CustomMarkerView(requireContext(), R.layout.custom_marker_view));
        barChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray));
        barChart.setFitBars(true);
        barChart.getLegend().setEnabled(false);
        barChart.getDescription().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                if (value >= 1 && value <= 12) return months[(int)value - 1];
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12); // or day count
        xAxis.setDrawLabels(true);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisMinimum(0.5f);
        xAxis.setAxisMaximum(12.5f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f); // <-- Add this line to make sure it starts from 0

        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
    }

}