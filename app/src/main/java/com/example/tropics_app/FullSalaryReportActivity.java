package com.example.tropics_app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private FloatingActionButton fabCompFiSal;
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

        fabCompFiSal = findViewById(R.id.fabCompFiSal);

        month_spinner = findViewById(R.id.month_spinner);
        year_spinner = findViewById(R.id.year_spinner);
        week_num = findViewById(R.id.week_num);

        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();

        loadAppointmentData();
        loadEmployeeData();
        spinnerSetup();
        filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());

        fabCompFiSal.setOnClickListener(v -> showSalaryComputationDialog());
    }
    private void showSalaryComputationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_salary_deduction_salary_days);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TableLayout employeeTable = dialog.findViewById(R.id.employeeTable);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmit);

        // Clear previous rows if any
        employeeTable.removeViews(1, employeeTable.getChildCount() - 1);

        // Fetch employee data from Firestore
        db.collection("Employees") // Replace with your actual collection name
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class); // Assuming you have an Employee class
                            TableRow tableRow = new TableRow(this);

                            TextView employeeName = new TextView(this);
                            employeeName.setText(employee.getName());
                            employeeName.setTextSize(16);
                            employeeName.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
                            employeeName.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
                            employeeName.setTextColor(getResources().getColor(android.R.color.white));

                            EditText etDaysPresent = new EditText(this);
                            etDaysPresent.setHint("0");
                            etDaysPresent.setBackgroundResource(R.drawable.custom_input);
                            etDaysPresent.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
                            etDaysPresent.setInputType(InputType.TYPE_CLASS_NUMBER);
                            etDaysPresent.setPadding(12, 5, 12, 5);
                            etDaysPresent.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
                            etDaysPresent.setId(View.generateViewId());

                            EditText etLateDeduction = new EditText(this);
                            etLateDeduction.setHint("0");
                            etLateDeduction.setBackgroundResource(R.drawable.custom_input);
                            etLateDeduction.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
                            etLateDeduction.setInputType(InputType.TYPE_CLASS_NUMBER);
                            etLateDeduction.setPadding(12, 5, 12, 5);
                            etLateDeduction.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
                            etLateDeduction.setId(View.generateViewId());

                            EditText etCADeduction = new EditText(this);
                            etCADeduction.setHint("0");
                            etCADeduction.setBackgroundResource(R.drawable.custom_input);
                            etCADeduction.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_medium));
                            etCADeduction.setInputType(InputType.TYPE_CLASS_NUMBER);
                            etCADeduction.setPadding(12, 5, 12, 5);
                            etCADeduction.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
                            etCADeduction.setId(View.generateViewId());

                            // Add views to the table row
                            tableRow.addView(employeeName);
                            tableRow.addView(etDaysPresent);
                            tableRow.addView(etLateDeduction);
                            tableRow.addView(etCADeduction);

                            // Add the table row to the TableLayout
                            employeeTable.addView(tableRow);
                        }
                    } else {
                        Log.e("DEBUG", "Error getting documents: ", task.getException());
                    }
                });

        btnSubmit.setOnClickListener(v -> {
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            for (int i = 0; i < employeeTable.getChildCount(); i++) {
                TableRow row = (TableRow) employeeTable.getChildAt(i);

                if (row.getChildCount() >= 4) {
                    View firstChild = row.getChildAt(0);
                    View secondChild = row.getChildAt(1);
                    View thirdChild = row.getChildAt(2);
                    View fourthChild = row.getChildAt(3);

                    if (firstChild instanceof TextView && secondChild instanceof EditText &&
                            thirdChild instanceof EditText && fourthChild instanceof EditText) {

                        EditText etDaysPresent = (EditText) secondChild;
                        EditText etLateDeduction = (EditText) thirdChild;
                        EditText etCADeduction = (EditText) fourthChild;

                        String daysPresent = etDaysPresent.getText().toString();
                        String lateDeduction = etLateDeduction.getText().toString();
                        String caDeduction = etCADeduction.getText().toString();

                        // Create a map to hold the data
                        Map<String, Object> deductionData = new HashMap<>();
                        deductionData.put("daysPresent", daysPresent);
                        deductionData.put("lateDeduction", lateDeduction);
                        deductionData.put("caDeduction", caDeduction);
                        deductionData.put("employeeId", employeeList.get(i).getId()); // Ensure this reflects the current employee
                        deductionData.put("date", currentDate);

                        db.collection("salary_details")
                                .add(deductionData)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(this, "Salary deduction data submitted!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error submitting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e("DEBUG", "Row " + i + " does not contain the expected views.");
                    }
                } else {
                    Log.e("DEBUG", "Row " + i + " has insufficient children: " + row.getChildCount());
                }
            }

            dialog.dismiss();
        });

        dialog.show();
    }



    private void spinnerSetup() {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        String[] years = new String[10]; // For example, the next 10 years

        // Get the current date
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY); // Set the first day of the week to Monday

        int currentMonth = calendar.get(Calendar.MONTH); // January = 0, ..., December = 11
        int currentYear = calendar.get(Calendar.YEAR);

        // Correctly calculate the current week of the month
        int currentWeek = getCurrentWeekOfMonth(calendar);

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

        // Set default selection to current month and year
        month_spinner.setSelection(currentMonth); // Current month
        year_spinner.setSelection(0); // The current year is at index 0 in the years array

        // Set default week number
        updateWeekSpinner(currentYear, currentMonth, currentWeek);

        // Add listeners for month and year spinners to update the week spinner dynamically
        month_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                int selectedMonth = position;
                int selectedYear = Integer.parseInt(year_spinner.getSelectedItem().toString());
                updateWeekSpinner(selectedYear, selectedMonth, currentWeek); // Update the week spinner based on the new month and year
                filterDataByMonthYearWeek(months[selectedMonth], year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        year_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                int selectedYear = Integer.parseInt(years[position]);
                int selectedMonth = month_spinner.getSelectedItemPosition();
                updateWeekSpinner(selectedYear, selectedMonth, currentWeek); // Update the week spinner based on the new year
                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), years[position], week_num.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        week_num.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedWeek = week_num.getSelectedItem().toString();
                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), selectedWeek);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    // Update the week spinner based on the selected month and year
    private void updateWeekSpinner(int year, int month, int currentWeek) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.setFirstDayOfWeek(Calendar.MONDAY); // Ensure Monday is the first day of the week

        // Find the number of weeks in the selected month
        int maxWeeks = calendar.getActualMaximum(Calendar.WEEK_OF_MONTH);

        // Create a new array for the weeks based on the max number of weeks
        String[] weeks = new String[maxWeeks];
        for (int i = 0; i < maxWeeks; i++) {
            weeks[i] = String.valueOf(i + 1); // Week numbers start at 1
        }

        // Update the week spinner with the new week data
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, weeks);
        weekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        week_num.setAdapter(weekAdapter);

        // Set the default selection to the current week (if it's valid for the selected month and year)
        if (currentWeek <= maxWeeks) {
            week_num.setSelection(currentWeek - 1); // Week is 1-based, so subtract 1
        } else {
            week_num.setSelection(0); // Default to first week if the current week doesn't exist
        }
    }

    // Correctly calculate the current week of the month based on the first day of the week
    private int getCurrentWeekOfMonth(Calendar calendar) {
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        return calendar.get(Calendar.WEEK_OF_MONTH);
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
            TextView.setText("");
            TextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
            TextView.setTextColor(Color.WHITE);
            TextView.setPadding(10, 10, 10, 10);
            dateRow.addView(TextView);

            TableRow dateRowWeekend = new TableRow(this);
            dateRowWeekend.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            // Create layout parameters for TextView with layout_weight
            TableRow.LayoutParams params2 = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // 0dp width, weight of 1
            TextView TextView2 = new TextView(this);
            TextView2.setText("");
            TextView2.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
            TextView2.setTextColor(Color.WHITE);
            TextView2.setPadding(10, 10, 10, 10);
            dateRowWeekend.addView(TextView2);
            // Add dates to the dateRow (only Monday to Thursday)
            Calendar displayCalendar = (Calendar) calendar.clone(); // Clone to keep the original calendar unchanged
            for (int i = 0; i < 7; i++) { // Loop for Monday to Thursday
                if(i > 3){
                    TextView dateTextView = new TextView(this);
                    String dateStr = sdf.format(displayCalendar.getTime());
                    dateTextView.setText(dateStr);
                    dateTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                    dateTextView.setTextColor(Color.WHITE);
                    dateTextView.setPadding(10, 10, 10, 10);
                    dateRowWeekend.addView(dateTextView);
                    displayCalendar.add(Calendar.DAY_OF_MONTH, 1);

                    TextView TextView3 = new TextView(this);
                    TextView3.setText("");
                    TextView3.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                    TextView3.setTextColor(Color.WHITE);
                    TextView3.setPadding(10, 10, 10, 10);
                    dateRowWeekend.addView(TextView3);
                }else {
                    TextView dateTextView = new TextView(this);
                    String dateStr = sdf.format(displayCalendar.getTime());
                    dateTextView.setText(dateStr);
                    dateTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                    dateTextView.setTextColor(Color.WHITE);
                    dateTextView.setPadding(10, 10, 10, 10);
                    dateRow.addView(dateTextView);
                    displayCalendar.add(Calendar.DAY_OF_MONTH, 1);

                    TextView TextView3 = new TextView(this);
                    TextView3.setText("");
                    TextView3.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                    TextView3.setTextColor(Color.WHITE);
                    TextView3.setPadding(10, 10, 10, 10);
                    dateRow.addView(TextView3);
                }
            }

            TextView TextView3 = new TextView(this);
            TextView3.setText("");
            TextView3.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
            TextView3.setTextColor(Color.WHITE);
            TextView3.setPadding(10, 10, 10, 10);
            dateRowWeekend.addView(TextView3);

            TextView TextView4 = new TextView(this);
            TextView4.setText("");
            TextView4.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
            TextView4.setTextColor(Color.WHITE);
            TextView4.setPadding(10, 10, 10, 10);
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
                    

                    // Add employee name
                    TextView nameTextView = new TextView(this);
                    nameTextView.setText(employeeName);
                    nameTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                    nameTextView.setTextColor(Color.WHITE);
                    nameTextView.setPadding(10, 10, 10, 10);
                    rowCommission.addView(nameTextView);

                    TableRow rowWeekend = new TableRow(this);
                    rowWeekend.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    // Add employee name
                    TextView nameTextView2 = new TextView(this);
                    nameTextView2.setText(employeeName);
                    nameTextView2.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                    nameTextView2.setTextColor(Color.WHITE);
                    nameTextView2.setPadding(10, 10, 10, 10);
                    rowWeekend.addView(nameTextView2);

                    double totalSalesPerEmp = 0.0;
                    double totalCommissionPerEmp = 0.0;
                    // Add daily sales and commissions for each day of the week (only Monday to Thursday)
                    for (int i = 0; i < 7; i++) { // Change to 4 to match the number of days (Monday to Thursday)
                        double dailyCommission = 0.0;
                        if(i > 3){
                            TextView dailySalesTextView = new TextView(this);
                            dailySalesTextView.setText(String.format("₱%.2f", dailySalesMap.get(employeeName)[i]));
                            dailySalesTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                            dailySalesTextView.setTextColor(Color.WHITE);
                            rowWeekend.addView(dailySalesTextView);

                            // Add daily commission (assuming commission is calculated based on daily sales)
                            TextView dailyCommissionTextView = new TextView(this);
                            dailyCommission = (dailySalesMap.get(employeeName)[i] * commissionRate) / 100.0;
                            dailyCommissionTextView.setText(String.format("₱%.2f", dailyCommission));
                            dailyCommissionTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                            dailyCommissionTextView.setTextColor(Color.WHITE);
                            rowWeekend.addView(dailyCommissionTextView);
                        }else {
                            TextView dailySalesTextView = new TextView(this);
                            dailySalesTextView.setText(String.format("₱%.2f", dailySalesMap.get(employeeName)[i]));
                            dailySalesTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                            dailySalesTextView.setTextColor(Color.WHITE);
                            rowCommission.addView(dailySalesTextView);

                            // Add daily commission (assuming commission is calculated based on daily sales)
                            TextView dailyCommissionTextView = new TextView(this);
                            dailyCommission = (dailySalesMap.get(employeeName)[i] * commissionRate) / 100.0;
                            dailyCommissionTextView.setText(String.format("₱%.2f", dailyCommission));
                            dailyCommissionTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
                            dailyCommissionTextView.setTextColor(Color.WHITE);
                            rowCommission.addView(dailyCommissionTextView);
                        }
                        totalSalesPerEmp += dailySalesMap.get(employeeName)[i];
                        totalCommissionPerEmp += dailyCommission;
                    }
                    TextView TotalSalesTextView = new TextView(this);
                    TotalSalesTextView.setText(String.format("₱%.2f", totalSalesPerEmp));
                    TotalSalesTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    TotalSalesTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
                    TotalSalesTextView.setPadding(10, 10, 10, 10);
                    rowWeekend.addView(TotalSalesTextView);

                    TextView TotalComsTextView = new TextView(this);
                    TotalComsTextView.setText(String.format("₱%.2f", totalCommissionPerEmp));
                    TotalComsTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
                    TotalComsTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    TotalComsTextView.setPadding(10, 10, 10, 10);
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