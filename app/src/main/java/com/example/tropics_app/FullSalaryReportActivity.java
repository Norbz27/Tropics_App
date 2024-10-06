package com.example.tropics_app;

import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class FullSalaryReportActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private List<Employee> employeeList;
    private List<Appointment> appointmentsList;
    private TableLayout daily_table, weekend_table;
    private Spinner month_spinner, year_spinner, week_num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_salary_report);
        db = FirebaseFirestore.getInstance();
        daily_table = findViewById(R.id.daily_table);
        weekend_table = findViewById(R.id.weekend_table);

        month_spinner = findViewById(R.id.month_spinner);
        year_spinner = findViewById(R.id.year_spinner);
        week_num = findViewById(R.id.week_num);

        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();

        loadAppointmentData();
        loadEmployeeData();
        spinnerSetup();
        filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());

    }
    private void spinnerSetup() {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        String[] years = new String[10]; // For example, the next 10 years
        String[] weeks = {"1", "2", "3", "4", "5"}; // Up to 5 weeks in a month

        // Get the current date
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY); // Set the first day of the week to Monday

        int currentMonth = calendar.get(Calendar.MONTH); // January = 0, ..., December = 11
        int currentYear = calendar.get(Calendar.YEAR);
        int currentWeek = calendar.get(Calendar.WEEK_OF_MONTH);

        // Populate the years array with the current year and the next 9 years
        for (int i = 0; i < 10; i++) {
            years[i] = String.valueOf(currentYear + i);
        }

        // Set up the spinners with adapters
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        month_spinner.setAdapter(monthAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        year_spinner.setAdapter(yearAdapter);

        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weeks);
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        week_num.setAdapter(weekAdapter);

        // Set default selection to current month, year, and week
        month_spinner.setSelection(currentMonth); // Current month
        year_spinner.setSelection(0); // The current year is at index 0 in the years array
        week_num.setSelection(currentWeek - 1); // Week number in Calendar is 1-based, so subtract 1

        // Add listeners for spinners to filter data
        month_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedMonth = months[position];
                filterDataByMonthYearWeek(selectedMonth, year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        year_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedYear = years[position];
                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), selectedYear, week_num.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        week_num.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedWeek = weeks[position];
                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), selectedWeek);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }
    private int getMonthNumber(String monthName) {
        switch (monthName) {
            case "January":
                return Calendar.JANUARY;
            case "February":
                return Calendar.FEBRUARY;
            case "March":
                return Calendar.MARCH;
            case "April":
                return Calendar.APRIL;
            case "May":
                return Calendar.MAY;
            case "June":
                return Calendar.JUNE;
            case "July":
                return Calendar.JULY;
            case "August":
                return Calendar.AUGUST;
            case "September":
                return Calendar.SEPTEMBER;
            case "October":
                return Calendar.OCTOBER;
            case "November":
                return Calendar.NOVEMBER;
            case "December":
                return Calendar.DECEMBER;
            default:
                throw new IllegalArgumentException("Invalid month name: " + monthName);
        }
    }
    private void filterDataByMonthYearWeek(String selectedMonth, String selectedYear, String selectedWeekNumber) {
        Calendar calendar = Calendar.getInstance();
        Map<String, Double> employeeSalesMap = new HashMap<>(); // To store total sales by employee
        Map<String, double[]> dailySalesMap = new HashMap<>(); // To store daily sales by employee for each day of the week (Monday to Thursday)
        double totalSales = 0.0;

        try {
            // Create a mapping from month names to month numbers
            int month = getMonthNumber(selectedMonth); // Convert month name to month number
            int year = Integer.parseInt(selectedYear);
            int weekNumber = Integer.parseInt(selectedWeekNumber);

            // Set the calendar to the first day of the selected week
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.WEEK_OF_MONTH, weekNumber);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start from Monday
            Log.d("SalesFragment", "Selected Calendar: " + calendar);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

            // Clear previous rows
            daily_table.removeViews(1, daily_table.getChildCount() - 1); // Keep header
            weekend_table.removeViews(1, weekend_table.getChildCount() - 1); // Keep header

            // Create a header row for dates (only for Monday to Thursday)
            TableRow dateRow = new TableRow(this);
            dateRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            // Create layout parameters for TextView with layout_weight
            TableRow.LayoutParams params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // 0dp width, weight of 1
            TextView TextView = new TextView(this);
            TextView.setLayoutParams(params);
            TextView.setText("");
            TextView.setTextColor(Color.WHITE);
            TextView.setPadding(5, 5, 5, 5);
            dateRow.addView(TextView);

            TableRow dateRowWeekend = new TableRow(this);
            dateRowWeekend.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            // Create layout parameters for TextView with layout_weight
            TableRow.LayoutParams params2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // 0dp width, weight of 1
            TextView TextView2 = new TextView(this);
            TextView2.setLayoutParams(params2);
            TextView2.setText("");
            TextView2.setTextColor(Color.WHITE);
            TextView2.setPadding(5, 5, 5, 5);
            dateRowWeekend.addView(TextView2);
            // Add dates to the dateRow (only Monday to Thursday)
            Calendar displayCalendar = (Calendar) calendar.clone(); // Clone to keep the original calendar unchanged
            for (int i = 0; i < 7; i++) { // Loop for Monday to Thursday
                if(i > 3){
                    TextView dateTextView = new TextView(this);
                    dateTextView.setLayoutParams(params);
                    String dateStr = sdf.format(displayCalendar.getTime());
                    dateTextView.setText(dateStr);
                    dateTextView.setTextColor(Color.WHITE);
                    dateTextView.setPadding(5, 5, 5, 5);
                    dateRowWeekend.addView(dateTextView);
                    displayCalendar.add(Calendar.DAY_OF_MONTH, 1);

                    TextView TextView3 = new TextView(this);
                    TextView3.setLayoutParams(params2);
                    TextView3.setText("");
                    TextView3.setTextColor(Color.WHITE);
                    TextView3.setPadding(5, 5, 5, 5);
                    dateRowWeekend.addView(TextView3);
                }else {
                    TextView dateTextView = new TextView(this);
                    dateTextView.setLayoutParams(params);
                    String dateStr = sdf.format(displayCalendar.getTime());
                    dateTextView.setText(dateStr);
                    dateTextView.setTextColor(Color.WHITE);
                    dateTextView.setPadding(5, 5, 5, 5);
                    dateRow.addView(dateTextView);
                    displayCalendar.add(Calendar.DAY_OF_MONTH, 1);

                    TextView TextView3 = new TextView(this);
                    TextView3.setLayoutParams(params2);
                    TextView3.setText("");
                    TextView3.setTextColor(Color.WHITE);
                    TextView3.setPadding(5, 5, 5, 5);
                    dateRow.addView(TextView3);
                }
            }

            TextView TextView3 = new TextView(this);
            TextView3.setLayoutParams(params2);
            TextView3.setText("");
            TextView3.setTextColor(Color.WHITE);
            TextView3.setPadding(5, 5, 5, 5);
            dateRowWeekend.addView(TextView3);

            TextView TextView4 = new TextView(this);
            TextView4.setLayoutParams(params2);
            TextView4.setText("");
            TextView4.setTextColor(Color.WHITE);
            TextView4.setPadding(5, 5, 5, 5);
            dateRowWeekend.addView(TextView4);

            // Add the dateRow to the table
            daily_table.addView(dateRow);
            weekend_table.addView(dateRowWeekend);
            // Reset calendar to the start of the selected week for filtering appointments
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.WEEK_OF_MONTH, weekNumber);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start from Monday

            // Calculate start and end of the week
            Date startOfWeek = calendar.getTime(); // Start of the selected week
            calendar.add(Calendar.DAY_OF_MONTH, 6); // Add 6 days to get to Sunday
            Date endOfWeek = calendar.getTime(); // End of the selected week

            // Initialize daily sales for each employee for each day of the week (only Monday to Thursday)
            for (Appointment appointment : appointmentsList) {
                Date appointmentDate = appointment.getClientDateTimeAsDate();

                if (appointmentDate != null && (appointmentDate.after(startOfWeek) || appointmentDate.equals(startOfWeek)) &&
                        (appointmentDate.before(endOfWeek) || appointmentDate.equals(endOfWeek))) {

                    // Check for and display sub-services
                    List<Map<String, Object>> services = appointment.getServices();
                    // Update sales per employee
                    for (Map<String, Object> service : services) {
                        String employee = service.get("assignedEmployee").toString();
                        double totalPriceForService = (Double) service.get("servicePrice");

                        List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                        if (subServices != null) {
                            for (Map<String, Object> subService : subServices) {
                                Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;
                                totalPriceForService += subServicePrice;
                            }
                        }

                        // Update the employee's total sales
                        employeeSalesMap.put(employee, employeeSalesMap.getOrDefault(employee, 0.0) + totalPriceForService);

                        // Initialize daily sales array if not already done (only for Monday to Thursday)
                        if (!dailySalesMap.containsKey(employee)) {
                            dailySalesMap.put(employee, new double[7]); // Array for 4 days of the week (Monday to Thursday)
                        }

                        // Calculate which day of the week the appointment occurred
                        Calendar appointmentCalendar = Calendar.getInstance();
                        appointmentCalendar.setTime(appointmentDate);
                        int dayOfWeek = appointmentCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY; // Adjust to 0-based index (Monday = 0)

                        if (dayOfWeek >= 0 && dayOfWeek < 7) { // Ensure the day is Monday to Thursday
                            dailySalesMap.get(employee)[dayOfWeek] += totalPriceForService;
                        }
                    }
                }
            }

            double totalCommission = 0.0;
            for (Map.Entry<String, Double> entry : employeeSalesMap.entrySet()) {
                String employeeName = entry.getKey();
                double sales = entry.getValue();

                Employee employee = findEmployeeByName(employeeName);
                if (employee != null) {
                    double commissionRate = employee.getComs();
                    double employeeCommission = (sales * commissionRate) / 100.0;
                    totalCommission += employeeCommission;

                    TableRow rowCommission = new TableRow(this);
                    rowCommission.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    // Create layout parameters for TextView with layout_weight
                    TableRow.LayoutParams paramsCommission = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // 0dp width, weight of 1

                    // Add employee name
                    TextView nameTextView = new TextView(this);
                    nameTextView.setLayoutParams(paramsCommission);
                    nameTextView.setText(employeeName);
                    nameTextView.setTextColor(Color.WHITE);
                    nameTextView.setPadding(5, 5, 5, 5);
                    rowCommission.addView(nameTextView);

                    TableRow rowWeekend = new TableRow(this);
                    rowWeekend.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    // Create layout parameters for TextView with layout_weight
                    TableRow.LayoutParams paramsWeekend = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // 0dp width, weight of 1

                    // Add employee name
                    TextView nameTextView2 = new TextView(this);
                    nameTextView2.setLayoutParams(paramsWeekend);
                    nameTextView2.setText(employeeName);
                    nameTextView2.setTextColor(Color.WHITE);
                    nameTextView2.setPadding(5, 5, 5, 5);
                    rowWeekend.addView(nameTextView2);

                    double totalSalesPerEmp = 0.0;
                    double totalCommissionPerEmp = 0.0;
                    // Add daily sales and commissions for each day of the week (only Monday to Thursday)
                    for (int i = 0; i < 7; i++) { // Change to 4 to match the number of days (Monday to Thursday)
                        double dailyCommission = 0.0;
                        if(i > 3){
                            TextView dailySalesTextView = new TextView(this);
                            dailySalesTextView.setLayoutParams(paramsCommission);
                            dailySalesTextView.setText(String.format("₱%.2f", dailySalesMap.get(employeeName)[i]));
                            dailySalesTextView.setTextColor(Color.WHITE);
                            rowWeekend.addView(dailySalesTextView);

                            // Add daily commission (assuming commission is calculated based on daily sales)
                            TextView dailyCommissionTextView = new TextView(this);
                            dailyCommissionTextView.setLayoutParams(paramsCommission);
                            dailyCommission = (dailySalesMap.get(employeeName)[i] * commissionRate) / 100.0;
                            dailyCommissionTextView.setText(String.format("₱%.2f", dailyCommission));
                            dailyCommissionTextView.setTextColor(Color.WHITE);
                            rowWeekend.addView(dailyCommissionTextView);
                        }else {
                            TextView dailySalesTextView = new TextView(this);
                            dailySalesTextView.setLayoutParams(paramsCommission);
                            dailySalesTextView.setText(String.format("₱%.2f", dailySalesMap.get(employeeName)[i]));
                            dailySalesTextView.setTextColor(Color.WHITE);
                            rowCommission.addView(dailySalesTextView);

                            // Add daily commission (assuming commission is calculated based on daily sales)
                            TextView dailyCommissionTextView = new TextView(this);
                            dailyCommissionTextView.setLayoutParams(paramsCommission);
                            dailyCommission = (dailySalesMap.get(employeeName)[i] * commissionRate) / 100.0;
                            dailyCommissionTextView.setText(String.format("₱%.2f", dailyCommission));
                            dailyCommissionTextView.setTextColor(Color.WHITE);
                            rowCommission.addView(dailyCommissionTextView);
                        }
                        totalSalesPerEmp += dailySalesMap.get(employeeName)[i];
                        totalCommissionPerEmp += dailyCommission;
                    }
                    TextView TotalSalesTextView = new TextView(this);
                    TotalSalesTextView.setLayoutParams(paramsCommission);
                    TotalSalesTextView.setText(String.format("₱%.2f", totalSalesPerEmp));
                    TotalSalesTextView.setTextColor(Color.WHITE);
                    rowWeekend.addView(TotalSalesTextView);

                    TextView TotalComsTextView = new TextView(this);
                    TotalComsTextView.setLayoutParams(paramsCommission);
                    TotalComsTextView.setText(String.format("₱%.2f", totalCommissionPerEmp));
                    TotalComsTextView.setTextColor(Color.WHITE);
                    rowWeekend.addView(TotalComsTextView);

                    daily_table.addView(rowCommission);
                    weekend_table.addView(rowWeekend);
                }
            }

        } catch (Exception e) {
            Log.e("SalesFragment", "Error filtering data: ", e);
        }
    }


    private Employee findEmployeeByName(String employeeName) {
        for (Employee employee : employeeList) {
            if (employee.getName().equalsIgnoreCase(employeeName)) {
                return employee; // Return the matching employee object
            }
        }
        return null; // If no match found, return null
    }


    private void loadEmployeeData() {
        db.collection("Employees").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        employeeList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            employee.setId(document.getId());
                            employeeList.add(employee);
                        }

                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void loadAppointmentData() {
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
                        } else {
                            Log.e("SalesFragment", "Error fetching appointments: ", task.getException());
                        }
                    }
                });
    }
}