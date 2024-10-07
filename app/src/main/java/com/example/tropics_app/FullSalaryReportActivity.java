package com.example.tropics_app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class FullSalaryReportActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FloatingActionButton fabCompFiSal;
    private List<Employee> employeeList;
    private List<Appointment> appointmentsList;
    private List<EmployeeSalaryDetails> salaryDetailsList ;
    private List<Expenses> expensesList;
    private List<Gcash> gcashList;
    private TableLayout daily_table, weekend_table, finalSalaryTable, breakdown_table;
    private Spinner month_spinner, year_spinner, week_num;
    private double totalSalesFtS = 0.0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_salary_report);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.darkgray));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.darkgray));
        }
        db = FirebaseFirestore.getInstance();
        daily_table = findViewById(R.id.daily_table);
        weekend_table = findViewById(R.id.weekend_table);
        finalSalaryTable = findViewById(R.id.final_salary_table);
        breakdown_table = findViewById(R.id.breakdown_table);
        fabCompFiSal = findViewById(R.id.fabCompFiSal);

        month_spinner = findViewById(R.id.month_spinner);
        year_spinner = findViewById(R.id.year_spinner);
        week_num = findViewById(R.id.week_num);

        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();
        salaryDetailsList = new ArrayList<>();
        expensesList = new ArrayList<>();
        gcashList = new ArrayList<>();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        loadExpensesData();
        loadGcashData();
        loadAppointmentData();
        loadEmployeeData();
        loadSalaryData();
        spinnerSetup();
        filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());

        fabCompFiSal.setOnClickListener(v -> showSalaryComputationDialog(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString()));
    }

    // Helper method to create TextViews for each column
    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(10, 10, 10, 10);
        textView.setTextColor(getResources().getColor(android.R.color.white));
        textView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope));
        return textView;
    }

    private void showSalaryComputationDialog(String month, String year, String week) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_salary_deduction_salary_days);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TableLayout employeeTable = dialog.findViewById(R.id.employeeTable);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmit);

        // Clear previous rows if any
        employeeTable.removeViews(1, employeeTable.getChildCount() - 1);

        // Fetch employee data from Firestore
        db.collection("Employees")
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

                            // Fetch salary details for this employee from Firestore
                            db.collection("salary_details")
                                    .whereEqualTo("employeeId", employee.getName())
                                    .whereEqualTo("month", getMonthNumber(month))
                                    .whereEqualTo("year", year)
                                    .whereEqualTo("week", week)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        if (!queryDocumentSnapshots.isEmpty()) {
                                            for (QueryDocumentSnapshot salaryDoc : queryDocumentSnapshots) {
                                                Map<String, Object> salaryData = salaryDoc.getData();
                                                etDaysPresent.setText(salaryData.get("daysPresent").toString());
                                                etLateDeduction.setText(salaryData.get("lateDeduction").toString());
                                                etCADeduction.setText(salaryData.get("caDeduction").toString());
                                                // Store the document ID to update later
                                                String docId = salaryDoc.getId();

                                                // Rename variables inside onSubmit listener to avoid scope conflicts
                                                btnSubmit.setOnClickListener(v -> {
                                                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                                                    for (int i = 0; i < employeeTable.getChildCount() - 1; i++) {
                                                        TableRow row = (TableRow) employeeTable.getChildAt(i + 1);

                                                        View secondChild = row.getChildAt(1);
                                                        View thirdChild = row.getChildAt(2);
                                                        View fourthChild = row.getChildAt(3);

                                                        if (secondChild instanceof EditText && thirdChild instanceof EditText && fourthChild instanceof EditText) {

                                                            // Use unique names in this scope
                                                            EditText daysPresentInput = (EditText) secondChild;
                                                            EditText lateDeductionInput = (EditText) thirdChild;
                                                            EditText caDeductionInput = (EditText) fourthChild;

                                                            String daysPresent = daysPresentInput.getText().toString();
                                                            String lateDeduction = lateDeductionInput.getText().toString();
                                                            String caDeduction = caDeductionInput.getText().toString();

                                                            // Create a map to hold the updated data
                                                            Map<String, Object> deductionData = new HashMap<>();
                                                            deductionData.put("daysPresent", daysPresent);
                                                            deductionData.put("lateDeduction", lateDeduction);
                                                            deductionData.put("caDeduction", caDeduction);
                                                            deductionData.put("timestamp", currentDate);

                                                            // Update the existing salary_details document
                                                            db.collection("salary_details")
                                                                    .document(docId) // Use the stored document ID
                                                                    .update(deductionData)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Toast.makeText(this, "Salary deduction data updated!", Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(this, "Error updating data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    });
                                                        } else {
                                                            Log.e("DEBUG", "Row " + i + " does not contain the expected views.");
                                                        }
                                                    }
                                                    dialog.dismiss();
                                                });
                                            }
                                        } else {
                                            // If no salary data exists, set a default onSubmit listener to create a new entry
                                            btnSubmit.setOnClickListener(v -> {
                                                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                                                for (int i = 0; i < employeeTable.getChildCount() - 1; i++) {
                                                    TableRow row = (TableRow) employeeTable.getChildAt(i + 1);

                                                    View secondChild = row.getChildAt(1);
                                                    View thirdChild = row.getChildAt(2);
                                                    View fourthChild = row.getChildAt(3);

                                                    if (secondChild instanceof EditText && thirdChild instanceof EditText && fourthChild instanceof EditText) {

                                                        // Use unique names in this scope
                                                        EditText daysPresentInput = (EditText) secondChild;
                                                        EditText lateDeductionInput = (EditText) thirdChild;
                                                        EditText caDeductionInput = (EditText) fourthChild;

                                                        String daysPresent = daysPresentInput.getText().toString();
                                                        String lateDeduction = lateDeductionInput.getText().toString();
                                                        String caDeduction = caDeductionInput.getText().toString();

                                                        // Create a map to hold the data
                                                        Map<String, Object> deductionData = new HashMap<>();
                                                        deductionData.put("daysPresent", daysPresent);
                                                        deductionData.put("lateDeduction", lateDeduction);
                                                        deductionData.put("caDeduction", caDeduction);
                                                        deductionData.put("employeeId", employeeList.get(i).getName());
                                                        deductionData.put("month", getMonthNumber(month));
                                                        deductionData.put("year", year);
                                                        deductionData.put("week", week);
                                                        deductionData.put("timestamp", currentDate);

                                                        // Add new salary_details document
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
                                                }
                                                dialog.dismiss();
                                            });
                                        }
                                    });

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

        dialog.show();
    }

    private void spinnerSetup() {
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        List<String> years = new ArrayList<>();
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY); // Set the first day of the week to Monday

        int currentMonth = calendar.get(Calendar.MONTH); // January = 0, ..., December = 11
        int currentYear = calendar.get(Calendar.YEAR);

        // Correctly calculate the current week of the month
        int currentWeek = getCurrentWeekOfMonth(calendar);

        // Populate the years array with the current year and the next 9 years

        for (int year = 2024; year <= currentYear; year++) {
            years.add(String.valueOf(year));
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
                int selectedYear = Integer.parseInt(parentView.getItemAtPosition(position).toString());
                int selectedMonth = month_spinner.getSelectedItemPosition();
                updateWeekSpinner(selectedYear, selectedMonth, currentWeek); // Update the week spinner based on the new year
                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), String.valueOf(selectedYear), week_num.getSelectedItem().toString());
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
            List<EmployeeSalaryDetails> EmployeeSalList = getSalaryForDate(month, selectedYear, selectedWeekNumber);
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
            finalSalaryTable.removeViews(1, finalSalaryTable.getChildCount() - 1);
            breakdown_table.removeViews(1, breakdown_table.getChildCount() - 1);

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
                    filterDataByDate(dateStr);
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

            // Clear the table before starting the population
            finalSalaryTable.removeViews(1, finalSalaryTable.getChildCount() - 1);  // Keep the header row


            Map<String, Double> sortedEmployeeSalesMap = new TreeMap<>(employeeSalesMap);

            for (Map.Entry<String, Double> entry : sortedEmployeeSalesMap.entrySet()) {
                String employeeName = entry.getKey();
                double sales = entry.getValue();

                // Find the employee details
                Employee employee = findEmployeeByName(employeeName);
                if (employee != null) {
                    double commissionRate = employee.getComs();
                    double employeeCommission = (sales * commissionRate) / 100.0;
                    totalCommission += employeeCommission;

                    // Create rows for weekdays and weekends
                    TableRow rowCommission = new TableRow(this);
                    rowCommission.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    TableRow rowWeekend = new TableRow(this);
                    rowWeekend.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    // Add employee name for weekdays and weekends
                    TextView nameTextView = createTextView(employeeName);
                    rowCommission.addView(nameTextView);
                    TextView nameTextView2 = createTextView(employeeName);
                    rowWeekend.addView(nameTextView2);

                    double totalSalesPerEmp = 0.0;
                    double totalCommissionPerEmp = 0.0;

                    // Process sales and commissions for 7 days (weekdays and weekends)
                    for (int i = 0; i < 7; i++) {
                        double dailySales = dailySalesMap.get(employeeName)[i];
                        double dailyCommission = (dailySales * commissionRate) / 100.0;

                        TextView dailySalesTextView = createTextView(String.format("₱%.2f", dailySales));
                        TextView dailyCommissionTextView = createTextView(String.format("₱%.2f", dailyCommission));

                        if (i < 4) {  // Monday to Thursday (weekdays)
                            rowCommission.addView(dailySalesTextView);
                            rowCommission.addView(dailyCommissionTextView);
                        } else {  // Friday to Sunday (weekends)
                            rowWeekend.addView(dailySalesTextView);
                            rowWeekend.addView(dailyCommissionTextView);

                        }

                        totalSalesPerEmp += dailySales;
                        totalCommissionPerEmp += dailyCommission;
                    }

                    // Add total sales and commission for weekends
                    TextView totalSalesTextView = createTextView(String.format("₱%.2f", totalSalesPerEmp));
                    totalSalesTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    totalSalesTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
                    TextView totalCommissionTextView = createTextView(String.format("₱%.2f", totalCommissionPerEmp));
                    totalCommissionTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    totalCommissionTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
                    rowWeekend.addView(totalSalesTextView);
                    rowWeekend.addView(totalCommissionTextView);

                    // Add rows to respective tables
                    daily_table.addView(rowCommission);
                    weekend_table.addView(rowWeekend);
                }
            }


            // Sort EmployeeSalList alphabetically by employee name (or ID)
            Collections.sort(EmployeeSalList, new Comparator<EmployeeSalaryDetails>() {
                @Override
                public int compare(EmployeeSalaryDetails emp1, EmployeeSalaryDetails emp2) {
                    return emp1.getEmployeeId().compareToIgnoreCase(emp2.getEmployeeId());
                }
            });
            double overAllTotalSalary = 0.0;
            for (EmployeeSalaryDetails emp : EmployeeSalList) {
                Employee employee = findEmployeeByName(emp.getEmployeeId());
                TableRow tableRow = new TableRow(this);

                // Calculate the basic weekly salary and deductions
                double weekSal = employee.getSalary() * Integer.parseInt(emp.getDaysPresent());
                double deductedWeekSal = weekSal - Integer.parseInt(emp.getLateDeduction());
                double totalCommissionPerEmp = 0.0;
                double commissionRate = employee.getComs();

                // Check if the employee has sales data in the dailySalesMap
                double[] dailySalesArray = dailySalesMap.get(emp.getEmployeeId());
                if (dailySalesArray == null) {
                    dailySalesArray = new double[7];  // Initialize with zero sales for all 7 days if not found
                }

                // Process sales and commissions for 7 days (weekdays and weekends)
                for (int i = 0; i < 7; i++) {
                    double dailySales = dailySalesArray[i];
                    double dailyCommission = (dailySales * commissionRate) / 100.0;
                    totalCommissionPerEmp += dailyCommission;
                }

                // Calculate total salary including commission and deductions
                double totalSalary = deductedWeekSal + totalCommissionPerEmp;
                double totalSalaryDeducted = totalSalary - Double.parseDouble(emp.getCaDeduction());
                overAllTotalSalary += totalSalaryDeducted;
                // Create TextViews for displaying employee details
                TextView name = createTextView(emp.getEmployeeId());
                TextView perDay = createTextView(String.format("₱%.2f", employee.getSalary()));
                TextView daysPresentTextView = createTextView(emp.getDaysPresent());
                TextView weekSalary = createTextView(String.format("₱%.2f", weekSal));
                TextView lateDeductionTextView = createTextView(String.format("₱%.2f", Double.parseDouble(emp.getLateDeduction())));
                TextView deductedSalary = createTextView(String.format("₱%.2f", deductedWeekSal));
                TextView commissionTextView = createTextView(String.format("₱%.2f", totalCommissionPerEmp));
                TextView totalSalaryTextView = createTextView(String.format("₱%.2f", totalSalary));
                TextView caDeductionTextView = createTextView(String.format("₱%.2f", Double.parseDouble(emp.getCaDeduction())));
                TextView overallSalaryTextView = createTextView(String.format("₱%.2f", totalSalaryDeducted));

                // Styling for the overall salary
                overallSalaryTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                overallSalaryTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));

                // Add TextViews to the table row
                tableRow.addView(name);
                tableRow.addView(perDay);
                tableRow.addView(daysPresentTextView);
                tableRow.addView(weekSalary);
                tableRow.addView(lateDeductionTextView);
                tableRow.addView(deductedSalary);
                tableRow.addView(commissionTextView);
                tableRow.addView(totalSalaryTextView);
                tableRow.addView(caDeductionTextView);
                tableRow.addView(overallSalaryTextView);

                // Add the row to the final salary table
                finalSalaryTable.addView(tableRow);
            }
            TableRow tableRow4 = new TableRow(this);
            for(int i = 0; i < 8; i++){
                TextView TextView5 = createTextView("");
                tableRow4.addView(TextView5);
            }

            TextView dateTextView4 = createTextView("Overall Salary");
            dateTextView4.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
            dateTextView4.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            TextView ovSal1 = createTextView(String.format("₱%.2f", overAllTotalSalary));
            ovSal1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
            ovSal1.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            tableRow4.addView(dateTextView4);
            tableRow4.addView(ovSal1);
            finalSalaryTable.addView(tableRow4);

            double remainingBal = totalSalesFtS - overAllTotalSalary;
            TableRow tableRow = new TableRow(this);
            tableRow.setBackgroundResource(R.color.gray);
            // Set the formatted date to TextView
            TextView dateTextView = createTextView("Total");
            TextView bal = createTextView(String.format("₱%.2f", totalSalesFtS));


            tableRow.addView(dateTextView);
            tableRow.addView(bal);

            TableRow tableRow2 = new TableRow(this);
            TextView dateTextView2 = createTextView("Overall Salary");
            TextView ovSal = createTextView(String.format("-₱%.2f", overAllTotalSalary));
            tableRow2.addView(dateTextView2);
            tableRow2.addView(ovSal);

            TableRow tableRow3 = new TableRow(this);
            tableRow3.setBackgroundResource(R.color.gray);
            TextView dateTextView3 = createTextView("");
            TextView Rbal = createTextView(String.format("₱%.2f", remainingBal));
            Rbal.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
            Rbal.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            Rbal.setTextSize(20);
            tableRow3.addView(dateTextView3);
            tableRow3.addView(Rbal);
            breakdown_table.addView(tableRow);
            breakdown_table.addView(tableRow2);
            breakdown_table.addView(tableRow3);
        } catch (Exception e) {
            Log.e("SalesFragment", "Error filtering data: ", e);
        }
    }

    private void filterDataByDate(String selectedDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Map<String, Double> employeeSalesMap = new HashMap<>(); // To store sales by employee
        Map<String, Double> employeeCommissionMap = new HashMap<>(); // To store commission by employee
        double totalSales = 0.0;
        try {
            Date date = dateFormat.parse(selectedDate);
            if (date != null) {
                calendar.setTime(date);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH); // 0-based
                int year = calendar.get(Calendar.YEAR);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

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

                            // Check for and display sub-services
                            List<Map<String, Object>> services = appointment.getServices();
                            for (Map<String, Object> service : services) {
                                double totalPriceForParentService = (Double) service.get("servicePrice"); // Variable to hold total price for this parent service

                                List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                                if (subServices != null) {
                                    for (Map<String, Object> subService : subServices) {
                                        // Get the sub-service name and price
                                        Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;

                                        totalPriceForParentService += subServicePrice;

                                    }
                                }

                                totalSales += totalPriceForParentService;

                            }
                        }
                    }
                }

                displayTotalSalesWithDeductions(selectedDate, totalSales);
            }
        } catch (ParseException e) {
            Log.e("SalesFragment", "Error parsing date: ", e);
        }
    }
    private void displayTotalSalesWithDeductions(String selectedDate, double totalSales) {
        double totalExpenses = 0.0;
        double totalGcash = 0.0;
        List<Expenses> expensesList = getExpensesForDate(selectedDate);
        List<Gcash> gcashList = getGcashForDate(selectedDate);

        for (Expenses expense : expensesList) {
            // Accumulate total expenses
            totalExpenses += expense.getAmount();
        }

        // Use the updated method
        for (Gcash gcash : gcashList) {
            totalGcash += gcash.getAmount();
        }
        double total = totalExpenses + totalGcash;
        double balance = totalSales - total;
        TableRow tableRow = new TableRow(this);

        SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yyyy"); // Adjust this format to match your input date format

        try {
            // Parse the String to Date
            Date date2 = parser.parse(selectedDate);

            // Format the date as MM/dd/yyyy with day of the week
            SimpleDateFormat dateFormat2 = new SimpleDateFormat("MM/dd/yyyy EEEE"); // EEEE gives the full day name
            String formattedDate = dateFormat2.format(date2);

            // Set the formatted date to TextView
            TextView dateTextView = createTextView(formattedDate);
            TextView bal = createTextView(String.format("₱%.2f", balance));
            totalSalesFtS += balance;
            tableRow.addView(dateTextView);
            tableRow.addView(bal);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Add the row to the final salary table
        breakdown_table.addView(tableRow);
    }
    private List<Expenses> getExpensesForDate(String selectedDate) {
        List<Expenses> matchingExpenses = new ArrayList<>();
        for (Expenses expense : expensesList) {
            if (expense.getTimestamp().equals(selectedDate)) {
                matchingExpenses.add(expense); // Add matching expense to the list
            }
        }
        return matchingExpenses; // Return the list of matching expenses
    }
    private List<Gcash> getGcashForDate(String selectedDate) {
        List<Gcash> matchingGcash = new ArrayList<>();
        for (Gcash gcash : gcashList) {
            if (gcash.getTimestamp().equals(selectedDate)) {
                matchingGcash.add(gcash); // Add matching expense to the list
            }
        }
        return matchingGcash; // Return the list of matching expenses
    }
    private void loadExpensesData() {
        db.collection("expenses").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        expensesList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Expenses expenses = document.toObject(Expenses.class);
                            expenses.setId(document.getId());
                            expensesList.add(expenses);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void loadGcashData() {
        db.collection("gcash_payments").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        gcashList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Gcash gcash = document.toObject(Gcash.class);
                            gcash.setId(document.getId());
                            gcashList.add(gcash);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private Employee findEmployeeByName(String employeeName) {
        for (Employee employee : employeeList) {
            if (employee.getName().equalsIgnoreCase(employeeName)) {
                return employee; // Return the matching employee object
            }
        }
        return null; // If no match found, return null
    }
    private List<EmployeeSalaryDetails> getSalaryForDate(int month, String year, String selectedDate) {
        Log.d("SalesFragments", "Month: " + month + ", Year: " + year + ", Week: " + selectedDate);
        List<EmployeeSalaryDetails> matchingDate = new ArrayList<>();
        for (EmployeeSalaryDetails salary : salaryDetailsList) {
            if (salary.getMonth() == month && salary.getYear().equalsIgnoreCase(year) && salary.getWeek().equalsIgnoreCase(selectedDate)) {
                matchingDate.add(salary); // Add matching expense to the list
            }
        }
        Log.d("SalesFragments", "Matching Date: " + matchingDate);
        return matchingDate; // Return the list of matching expenses
    }
    private void loadSalaryData() {
        db.collection("salary_details").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        salaryDetailsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EmployeeSalaryDetails employee = document.toObject(EmployeeSalaryDetails.class);
                            if (employee != null) { // Check if employee is not null
                                salaryDetailsList.add(employee);
                                Log.d("SalesFragments", "Salary Details: " + employee);
                            } else {
                                Log.d("SalesFragments", "Null EmployeeSalaryDetails for document: " + document.getId());
                            }
                        }
                        Log.d("SalesFragments", "Total Salary Details Loaded: " + salaryDetailsList.size());
                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}