package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
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

import java.text.ParseException;
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
    private TableLayout tableLayout;
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
        EditText DatePicker = rootView.findViewById(R.id.date_picker);
        tableLayout = rootView.findViewById(R.id.tblayout);
        DatePicker.setOnClickListener(v -> showDatePickerDialog(DatePicker));
        Date dateNow = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        DatePicker.setText(dateFormat.format(dateNow));
        filterDataByDate(dateFormat.format(dateNow));
        DatePicker.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                DatePicker.clearFocus(); // To close the keyboard
                String selectedDate = DatePicker.getText().toString();
                if (!selectedDate.isEmpty()) {
                    filterDataByDate(selectedDate);
                }
            }
        });
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
        return rootView;
    }
    @SuppressLint("RtlHardcoded")
    private void filterDataByDate(String selectedDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        try {
            // Parse the selected date
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                calendar.setTime(date);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH); // Month is 0-based
                int year = calendar.get(Calendar.YEAR);
                double totalSales = 0.0;
                // Clear previous rows
                tableLayout.removeViews(1, tableLayout.getChildCount() - 1); // Keep the header

                // Filter appointments for the selected date
                for (Appointment appointment : appointmentsList) {
                    Date appointmentDate = appointment.getClientDateTimeAsDate();
                    String appointmentTime = appointment.getTime();
                    SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm");
                    Date dateTime = sdf24.parse(appointmentTime);
                    SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a");
                    String formattedTime = sdf12.format(dateTime);

                    if (appointmentDate != null) {
                        Calendar appointmentCalendar = Calendar.getInstance();
                        appointmentCalendar.setTime(appointmentDate);
                        if (appointmentCalendar.get(Calendar.DAY_OF_MONTH) == day &&
                                appointmentCalendar.get(Calendar.MONTH) == month &&
                                appointmentCalendar.get(Calendar.YEAR) == year) {

                            String name = "";
                            String time = "";

                            // Check for and display sub-services
                            List<Map<String, Object>> services = appointment.getServices();
                            for (Map<String, Object> service : services) {
                                TableRow row = new TableRow(getContext());
                                row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                                if(appointment.getFullName() != name && appointment.getTime() != time){
                                    TextView timeTextView = new TextView(getContext());
                                    timeTextView.setText(formattedTime);
                                    timeTextView.setTextColor(Color.WHITE);
                                    timeTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
                                    timeTextView.setPadding(5, 5, 5, 5);
                                    row.addView(timeTextView);

                                    TextView nameTextView = new TextView(getContext());
                                    nameTextView.setText(appointment.getFullName());
                                    nameTextView.setTextColor(Color.WHITE);
                                    nameTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
                                    nameTextView.setPadding(5, 5, 5, 5);
                                    row.addView(nameTextView);

                                    name = appointment.getFullName();
                                    time = appointment.getTime();
                                }else {
                                    TextView timeTextView = new TextView(getContext());
                                    timeTextView.setText("");
                                    timeTextView.setTextColor(Color.WHITE);
                                    timeTextView.setPadding(5, 5, 5, 5);
                                    row.addView(timeTextView);

                                    TextView nameTextView = new TextView(getContext());
                                    nameTextView.setText("");
                                    nameTextView.setTextColor(Color.WHITE);
                                    nameTextView.setPadding(5, 5, 5, 5);
                                    row.addView(nameTextView);
                                }

                                TextView subServiceNameTextView = new TextView(getContext());
                                subServiceNameTextView.setText((String) service.get("serviceName"));
                                subServiceNameTextView.setTextColor(Color.LTGRAY);
                                subServiceNameTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
                                subServiceNameTextView.setPadding(10, 5, 5, 5);
                                row.addView(subServiceNameTextView);

                                // Assuming you already have a parentServiceName from the service map
                                String subServiceName = (String) service.get("serviceName"); // Get the parent service name
                                double totalPriceForParentService = (Double) service.get("servicePrice"); // Variable to hold total price for this parent service

                                List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                                if (subServices != null) {
                                    for (Map<String, Object> subService : subServices) {
                                        // Get the sub-service name and price
                                        Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;

                                            totalPriceForParentService += subServicePrice;

                                    }
                                }

                                TextView subServicePriceTextView = new TextView(getContext());
                                subServicePriceTextView.setText(String.format("₱%.2f", totalPriceForParentService));
                                subServicePriceTextView.setTextColor(Color.LTGRAY);
                                subServicePriceTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
                                subServicePriceTextView.setPadding(10, 5, 5, 5);
                                row.addView(subServicePriceTextView);

                                TextView HandlerTextView = new TextView(getContext());
                                HandlerTextView.setText((String) service.get("assignedEmployee"));
                                HandlerTextView.setTextColor(Color.LTGRAY);
                                HandlerTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
                                HandlerTextView.setPadding(10, 5, 5, 5);
                                row.addView(HandlerTextView);
                                totalSales += totalPriceForParentService;
                                // Add the sub-service row to the TableLayout
                                tableLayout.addView(row);
                            }
                        }
                    }
                }

                TableRow rowTotal = new TableRow(getContext());
                rowTotal.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                TextView tv1 = new TextView(getContext());
                tv1.setText("");
                tv1.setTextColor(Color.LTGRAY);
                tv1.setPadding(10, 5, 5, 5);
                rowTotal.addView(tv1);

                TextView tv2 = new TextView(getContext());
                tv2.setText("");
                tv2.setTextColor(Color.LTGRAY);
                tv2.setPadding(10, 5, 5, 5);
                rowTotal.addView(tv2);

                TextView tv3 = new TextView(getContext());
                tv3.setText("Total Sales:");
                tv3.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                tv3.setGravity(Gravity.RIGHT);
                tv3.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
                tv3.setPadding(10, 5, 5, 5);
                rowTotal.addView(tv3);

                TextView tvTotal = new TextView(getContext());
                tvTotal.setText(String.format("₱%.2f", totalSales));
                tvTotal.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                tvTotal.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
                tvTotal.setPadding(10, 5, 5, 5);
                rowTotal.addView(tvTotal);

                tableLayout.addView(rowTotal);
            }
        } catch (ParseException e) {
            Log.e("SalesFragment", "Error parsing date: ", e);
        }
    }

    private void showDatePickerDialog(final EditText dateField) {
        String dateString = dateField.getText().toString(); // Get the text from EditText
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); // Define the date format
        try {
            Date date = sdf.parse(dateString);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    R.style.datepicker,
                    (view, year1, month1, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", month1 + 1, dayOfMonth, year1);
                        dateField.setText(selectedDate);
                        filterDataByDate(selectedDate);
                    }, year, month, day);

            datePickerDialog.show();
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
