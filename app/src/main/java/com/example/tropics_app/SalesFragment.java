package com.example.tropics_app;

import android.graphics.Color;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesFragment extends Fragment {
    private TextView tvAverage, tvHigh, tvLow;
    private LineChart lineChart;
    private FirebaseFirestore db;
    private List<Appointment> appointmentsList; // List to store appointments
    private Spinner monthSpinner, yearSpinner;
    private Calendar calendar;
    private int targetMonth, targetYear;
    private boolean isDaily = true;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        appointmentsList = new ArrayList<>(); // Initialize here to avoid NullPointerException
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_sales, container, false);

        calendar = Calendar.getInstance();
        targetMonth = calendar.get(Calendar.MONTH); // Current month (0-indexed)
        targetYear = calendar.get(Calendar.YEAR);

        tvAverage = rootView.findViewById(R.id.tvAverage);
        tvHigh = rootView.findViewById(R.id.tvHigh);
        tvLow = rootView.findViewById(R.id.tvLow);

        // Find LineChart and Buttons from layout
        lineChart = rootView.findViewById(R.id.lineChart);
        Button btnDaily = rootView.findViewById(R.id.btnDaily);
        Button btnMonthly = rootView.findViewById(R.id.btnMonthly);
        // Inside your Fragment or Activity
        monthSpinner = rootView.findViewById(R.id.month_spinner);
        yearSpinner = rootView.findViewById(R.id.year_spinner);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.months_array, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH); // Note: Months are 0-based (January = 0)
        int currentYear = calendar.get(Calendar.YEAR);

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


        // Fetch data from Firestore
        fetchAppointmentData();

        btnDaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDailyData(appointmentsList, targetMonth, targetYear);
                monthSpinner.setVisibility(View.VISIBLE);
                btnDaily.setBackgroundResource(R.drawable.button_daily_checked);
                btnMonthly.setBackgroundResource(R.drawable.button_monthly);
            }
        });

        btnMonthly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMonthlyData(appointmentsList, targetYear);
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
        return rootView;
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

    // Method to fetch appointments from Firestore
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
                        } else {
                            Log.e("SalesFragment", "Error fetching appointments: ", task.getException());
                        }
                    }
                });
    }

    private void setDailyData(List<Appointment> appointmentsList, int targetMonth, int targetYear) {
        ArrayList<Entry> dailyEntries = new ArrayList<>();
        Map<Integer, Float> dailySales = new HashMap<>();

        float totalSales = 0f;
        int daysWithSales = 0;

        // Variables to track highest and lowest sales
        float highestSales = Float.MIN_VALUE;
        float lowestSales = Float.MAX_VALUE;

        // Group appointments by day of the target month and sum the sales
        Calendar calendar = Calendar.getInstance(); // Initialize calendar
        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getCreatedDateTimeAsDate(); // Use the updated method
            if (date != null) {
                calendar.setTime(date);
                int month = calendar.get(Calendar.MONTH) + 1; // Months are zero-based in Calendar
                int year = calendar.get(Calendar.YEAR);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                // Only consider sales from the current month and year
                if (month == targetMonth && year == targetYear) {
                    float sales = dailySales.getOrDefault(dayOfMonth, 0f);
                    float newSales = sales + (float) appointment.getTotalPrice(); // Convert double to float
                    dailySales.put(dayOfMonth, newSales); // Update sales for the day
                    totalSales += (float) appointment.getTotalPrice();
                    daysWithSales++;

                    // Track highest and lowest sales
                    if (newSales > highestSales) highestSales = newSales;
                    if (newSales < lowestSales) lowestSales = newSales;
                }
            }
        }

        // Generate entries for all days of the current month
        for (int day = 1; day <= calendar.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {
            float sales = dailySales.getOrDefault(day, 0f); // Default to 0 if no sales
            dailyEntries.add(new Entry(day, sales));
        }

        float averageSales = (daysWithSales > 0) ? totalSales / daysWithSales : 0f;
        tvAverage.setText("₱" + String.format("%.2f", averageSales));
        tvHigh.setText("Highest Sales: ₱" + String.format("%.2f", highestSales));
        tvLow.setText("Lowest Sales: ₱" + String.format("%.2f", lowestSales));
        Log.d("SalesFragment", "Average Daily Sales: " + averageSales);

        LineDataSet dataSet = new LineDataSet(dailyEntries, null); // Set label to null
        dataSet.setColor(Color.parseColor("#FFA500")); // Set line color to orange
        dataSet.setCircleColor(Color.parseColor("#FFA500")); // Set circle color to orange
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Set the fill drawable
        Drawable gradientDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(gradientDrawable);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        CustomMarkerView markerView = new CustomMarkerView(requireContext(), R.layout.custom_marker_view); // Replace with your layout resource
        lineChart.setMarker(markerView);

        // Customize chart appearance
        lineChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray)); // Set chart background color

        // Hide the legend and description
        lineChart.getLegend().setEnabled(false);
        lineChart.getDescription().setEnabled(false);

        // Set X-axis labels to display each day of the month
        // Set X-axis labels to display in mm-dd format
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int day = (int) value;
                // Format the date as mm-dd
                String formattedDate = String.format("%02d-%02d", targetMonth, day);
                return formattedDate; // Return the formatted date string
            }
        });


        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // Ensure all month labels are displayed
        xAxis.setDrawLabels(true); // Enable drawing labels
        xAxis.setTextColor(Color.WHITE); // Set X-axis text color to white
        xAxis.setAxisMinimum(1f); // Set the minimum value of X-axis to 1 (January)
        xAxis.setAxisMaximum(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // Set the maximum value of X-axis to 12 (December)

        // Customize Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set Y-axis text color to white
        lineChart.getAxisRight().setEnabled(false);

        lineChart.invalidate(); // Refresh the chart
    }



    private void setMonthlyData(List<Appointment> appointmentsList, int selectedYear) {
        ArrayList<Entry> monthlyEntries = new ArrayList<>();
        Map<Integer, Float> monthlySales = new HashMap<>();

        float totalSales = 0f;
        int monthsWithSales = 0;

        // Variables to track highest and lowest sales
        float highestSales = Float.MIN_VALUE;
        float lowestSales = Float.MAX_VALUE;

        // Initialize sales for all 12 months
        for (int month = 1; month <= 12; month++) {
            monthlySales.put(month, 0f);
        }

        // Group appointments by month and sum the sales only for the selected year
        Calendar calendar = Calendar.getInstance();
        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getCreatedDateTimeAsDate(); // Use the updated method
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR); // Get the year of the appointment
                if (year == selectedYear) { // Only include appointments from the selected year
                    int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is zero-based
                    float sales = monthlySales.get(month);
                    float newSales = sales + (float) appointment.getTotalPrice(); // Convert double to float
                    monthlySales.put(month, newSales); // Update sales for the month
                    totalSales += (float) appointment.getTotalPrice();
                    monthsWithSales++;

                    // Track highest and lowest sales
                    if (newSales > highestSales) highestSales = newSales;
                    if (newSales < lowestSales) lowestSales = newSales;
                }
            }
        }

        // Convert monthly sales map to chart entries
        for (int month = 1; month <= 12; month++) {
            monthlyEntries.add(new Entry(month, monthlySales.get(month)));
        }

        float averageSales = (monthsWithSales > 0) ? totalSales / monthsWithSales : 0f;
        tvAverage.setText("₱" + String.format("%.2f", averageSales));
        tvHigh.setText("Highest Sales: ₱" + String.format("%.2f", highestSales));
        tvLow.setText("Lowest Sales: ₱" + String.format("%.2f", lowestSales));
        Log.d("SalesFragment", "Average Monthly Sales: " + averageSales); // Log the average sales


        LineDataSet dataSet = new LineDataSet(monthlyEntries, null); // Set label to null
        dataSet.setColor(Color.parseColor("#FFA500")); // Set line color to orange
        dataSet.setCircleColor(Color.parseColor("#FFA500")); // Set circle color to orange
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Set the fill drawable
        Drawable gradientDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(gradientDrawable);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        CustomMarkerView markerView = new CustomMarkerView(requireContext(), R.layout.custom_marker_view); // Replace with your layout resource
        lineChart.setMarker(markerView);

        // Customize chart appearance
        lineChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray)); // Set chart background color

        // Hide the legend
        lineChart.getLegend().setEnabled(false);

        // Hide the description
        lineChart.getDescription().setEnabled(false);

        // Set X-axis labels to display all months
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int month = (int) value;
                if (month < 1 || month > 12) {
                    return ""; // Return an empty string for invalid values
                }
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                return monthNames[month - 1]; // Adjust for zero-based index
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Ensure that each month is shown
        xAxis.setLabelCount(12); // Ensure all month labels are displayed
        xAxis.setDrawLabels(true); // Enable drawing labels
        xAxis.setTextColor(Color.WHITE); // Set X-axis text color to white
        xAxis.setAxisMinimum(1f); // Set the minimum value of X-axis to 1 (January)
        xAxis.setAxisMaximum(12f); // Set the maximum value of X-axis to 12 (December)

        // Customize Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set Y-axis text color to white
        lineChart.getAxisRight().setEnabled(false);

        lineChart.invalidate(); // Refresh the chart
    }


}
