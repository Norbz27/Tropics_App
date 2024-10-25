package com.example.tropics_app;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private List<Funds> fundsList;
    private TableLayout daily_table, finalSalaryTable, breakdown_table;
    private Spinner month_spinner, year_spinner, week_num;
    private double totalSalesFtS = 0.0;
    private NumberFormat numberFormat;
    private SwipeRefreshLayout swipeRefreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_salary_report);
        db = FirebaseFirestore.getInstance();
        numberFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        
        daily_table = findViewById(R.id.daily_table);
        //weekend_table = findViewById(R.id.weekend_table);
        finalSalaryTable = findViewById(R.id.final_salary_table);
        breakdown_table = findViewById(R.id.breakdown_table);
        fabCompFiSal = findViewById(R.id.fabCompFiSal);

        month_spinner = findViewById(R.id.month_spinner);
        year_spinner = findViewById(R.id.year_spinner);
        week_num = findViewById(R.id.week_num);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();
        salaryDetailsList = new ArrayList<>();
        expensesList = new ArrayList<>();
        gcashList = new ArrayList<>();
        fundsList = new ArrayList<>();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        loadExpensesData();
        loadGcashData();
        loadFundsData();
        loadAppointmentData();
        loadEmployeeData();
        loadSalaryData();
        spinnerSetup();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadExpensesData();
                loadGcashData();
                loadFundsData();
                loadAppointmentData();
                loadEmployeeData();
                loadSalaryData();
                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
            }
        });
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
        Map<String, String> employeeDocMap = new HashMap<>();
        // Fetch employee data from Firestore
        db.collection("Employees")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            if ("Regular".equals(employee.getTherapist()) || employee.getSalary() != 0) {
                                // Assuming you have an Employee class
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
                                        .whereEqualTo("employeeId", employee.getName()) // Check the employee name is correct
                                        .whereEqualTo("month", getMonthNumber(month))
                                        .whereEqualTo("year", year)
                                        .whereEqualTo("week", week)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            if (!queryDocumentSnapshots.isEmpty()) {
                                                // Store a Map to associate employee names with their document IDs
                                                for (QueryDocumentSnapshot salaryDoc : queryDocumentSnapshots) {
                                                    Map<String, Object> salaryData = salaryDoc.getData();
                                                    String employeeName3 = salaryData.get("employeeId").toString();
                                                    String docId = salaryDoc.getId();

                                                    // Map employee names to their document IDs
                                                    employeeDocMap.put(employeeName3, docId);
                                                    Log.d("DEBUG", "Employee Name: " + employeeName3 + ", Document ID: " + docId);

                                                    // Set existing data in EditTexts
                                                    etDaysPresent.setText(salaryData.get("daysPresent").toString());
                                                    etLateDeduction.setText(salaryData.get("lateDeduction").toString());
                                                    etCADeduction.setText(salaryData.get("caDeduction").toString());
                                                }

                                                // Submit button click listener for updating salary details
                                                btnSubmit.setOnClickListener(v -> {
                                                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                                                    // Loop through the employee table rows and update the data
                                                    for (int i = 0; i < employeeTable.getChildCount() - 1; i++) {
                                                        TableRow row = (TableRow) employeeTable.getChildAt(i + 1);
                                                        Log.d("DEBUG", "Employee Doc Map when submitted: " + employeeDocMap);
                                                        if (row.getChildCount() >= 4) { // Ensure there are enough child views
                                                            TextView employeeNameView = (TextView) row.getChildAt(0); // Employee Name
                                                            EditText daysPresentInput = (EditText) row.getChildAt(1); // Days Present
                                                            EditText lateDeductionInput = (EditText) row.getChildAt(2); // Late Deduction
                                                            EditText caDeductionInput = (EditText) row.getChildAt(3); // CA Deduction

                                                            String employeeName2 = employeeNameView.getText().toString().trim();
                                                            String daysPresent = daysPresentInput.getText().toString().trim();
                                                            String lateDeduction = lateDeductionInput.getText().toString().trim();
                                                            String caDeduction = caDeductionInput.getText().toString().trim();

                                                            String docId = employeeDocMap.get(employeeName2);
                                                            if (docId != null) {
                                                                // Prepare the data for updating
                                                                Map<String, Object> deductionData = new HashMap<>();
                                                                deductionData.put("daysPresent", daysPresent);
                                                                deductionData.put("lateDeduction", lateDeduction);
                                                                deductionData.put("caDeduction", caDeduction);
                                                                deductionData.put("timestamp", currentDate);

                                                                Log.d("DEBUG", "Updating data for employee: " + employeeName2);
                                                                Log.d("DEBUG", "Document ID being updated: " + docId);

                                                                // Update the document in Firestore
                                                                db.collection("salary_details")
                                                                        .document(docId)
                                                                        .update(deductionData)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            Log.d("DEBUG", "Successfully updated Firestore document: " + docId);
                                                                            Toast.makeText(this, "Salary deduction data updated!", Toast.LENGTH_SHORT).show();
                                                                            loadSalaryData();
                                                                            filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Log.d("DEBUG", "Error updating Firestore document: " + e.getMessage());
                                                                            Toast.makeText(this, "Error updating data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                            loadSalaryData();
                                                                            filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                                        });
                                                            } else {
                                                                Log.e("DEBUG", "No document found for employee: " + employeeName2);
                                                                loadSalaryData();
                                                                filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                            }
                                                        } else {
                                                            Log.e("DEBUG", "Row " + i + " does not contain the expected views.");
                                                            loadSalaryData();
                                                            filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                        }
                                                    }

                                                    // Dismiss the dialog after updating
                                                    dialog.dismiss();
                                                    loadSalaryData();
                                                    filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                });

                                            } else {
                                                // If no salary data exists, set a default onSubmit listener to create a new entry
                                                btnSubmit.setOnClickListener(v -> {
                                                    ProgressDialog progressDialog = new ProgressDialog(this);
                                                    progressDialog.setMessage("Uploading data...");
                                                    progressDialog.setCancelable(false);
                                                    progressDialog.show();
                                                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                                                    for (int i = 0; i < employeeTable.getChildCount() - 1; i++) {
                                                        TableRow row = (TableRow) employeeTable.getChildAt(i + 1);

                                                        View firstChild = row.getChildAt(0);
                                                        View secondChild = row.getChildAt(1);
                                                        View thirdChild = row.getChildAt(2);
                                                        View fourthChild = row.getChildAt(3);

                                                        if (secondChild instanceof EditText && thirdChild instanceof EditText && fourthChild instanceof EditText) {

                                                            // Use unique names in this scope
                                                            TextView employeeNameTV = (TextView) firstChild;
                                                            EditText daysPresentInput = (EditText) secondChild;
                                                            EditText lateDeductionInput = (EditText) thirdChild;
                                                            EditText caDeductionInput = (EditText) fourthChild;

                                                            String daysPresent = daysPresentInput.getText().toString();
                                                            String lateDeduction = lateDeductionInput.getText().toString();
                                                            String caDeduction = caDeductionInput.getText().toString();
                                                            String employeeName2 = employeeNameTV.getText().toString();

                                                            // Create a map to hold the data
                                                            Map<String, Object> deductionData = new HashMap<>();
                                                            deductionData.put("daysPresent", daysPresent);
                                                            deductionData.put("lateDeduction", lateDeduction);
                                                            deductionData.put("caDeduction", caDeduction);
                                                            deductionData.put("employeeId", employeeName2);
                                                            deductionData.put("month", getMonthNumber(month));
                                                            deductionData.put("year", year);
                                                            deductionData.put("week", week);
                                                            deductionData.put("timestamp", currentDate);

                                                            // Add new salary_details document
                                                            db.collection("salary_details")
                                                                    .add(deductionData)
                                                                    .addOnSuccessListener(documentReference -> {
                                                                        Toast.makeText(this, "Salary deduction data submitted!", Toast.LENGTH_SHORT).show();
                                                                        loadSalaryData();
                                                                        filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                                        progressDialog.dismiss();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(this, "Error submitting data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        loadSalaryData();
                                                                        filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                                        progressDialog.dismiss();
                                                                    });
                                                        } else {
                                                            Log.e("DEBUG", "Row " + i + " does not contain the expected views.");
                                                            loadSalaryData();
                                                            filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                            progressDialog.dismiss();
                                                        }
                                                    }
                                                    dialog.dismiss();
                                                    loadSalaryData();
                                                    filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
                                                    progressDialog.dismiss();
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
        Log.d("SalesFragment", "Selected Month: " + month_spinner.getSelectedItem().toString());
        Log.d("SalesFragment", "Selected Year: " + year_spinner.getSelectedItem().toString());
        Log.d("SalesFragment", "Selected Week: " + week_num.getSelectedItem().toString());
        filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
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
    private double getSalaryByDate(Employee employee, String daysPresentStr, SimpleDateFormat sdf) {
        double salary = employee.getSalary() != null ? employee.getSalary() : 0.0; // Default to current salary
        List<Map<String, Object>> salaryHistory = employee.getSalaryHistory();

        // Parse the current date for salary calculation
        Calendar currentCalendar = Calendar.getInstance();
        int daysPresent = Integer.parseInt(daysPresentStr!= "" ? daysPresentStr : String.valueOf(0));

        // Calculate the total salary for the days present
        double totalSalary = salary * daysPresent;

        // Iterate through salary history to find the appropriate salary based on the date
        try {
            for (Map<String, Object> history : salaryHistory) {
                String changeDateStr = (String) history.get("dateChanged");
                double salaryAtChange = ((Number) history.get("salary")).doubleValue(); // Handle both Long and Double

                // Parse the change date
                Date changeDate = sdf.parse(changeDateStr);
                Date currentDate = currentCalendar.getTime();

                // If the change date is before or equal to the current date, use this salary
                if (changeDate != null && !changeDate.after(currentDate)) {
                    salary = salaryAtChange; // Update salary to the most recent one before or equal to current date
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Calculate total salary for the given number of days present
        return salary * daysPresent;
    }

    private double getCommissionRateByDate(Employee employee, String appointmentDate) {
        // Get commission history and ensure it is not null
        List<Map<String, Object>> commissionHistory = employee.getCommissionsHistory();
        if (commissionHistory == null) {
            return employee.getComs() != null ? employee.getComs() : 0.0; // Return the current commission rate if history is null
        }

        double commissionRate = employee.getComs() != null ? employee.getComs() : 0.0; // Default to current commission rate

        // Parse the appointment date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); // Adjust date format to match your Firebase data
            Date appointment = sdf.parse(appointmentDate);

            // Iterate through commission history to find the appropriate rate based on date
            for (Map<String, Object> history : commissionHistory) {
                String changeDateStr = (String) history.get("dateChanged");
                Object rateAtChangeObj = history.get("commission");

                // Ensure the rate is properly retrieved as Double
                double rateAtChange = 0.0;
                if (rateAtChangeObj instanceof Long) {
                    rateAtChange = ((Long) rateAtChangeObj).doubleValue();
                } else if (rateAtChangeObj instanceof Double) {
                    rateAtChange = (Double) rateAtChangeObj;
                }

                // Parse the change date
                Date changeDate = sdf.parse(changeDateStr);

                // If the change date is before the appointment date, use this rate
                if (changeDate != null && changeDate.before(appointment)) {
                    commissionRate = rateAtChange;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return commissionRate;
    }


    private void filterDataByMonthYearWeek(String selectedMonth, String selectedYear, String selectedWeekNumber) {

        Calendar calendar = Calendar.getInstance();
        totalSalesFtS = 0.0;
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
            //weekend_table.removeViews(1, weekend_table.getChildCount() - 1); // Keep header
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

            // Add dates to the dateRow (only Monday to Thursday)
            Calendar displayCalendar = (Calendar) calendar.clone(); // Clone to keep the original calendar unchanged
            for (int i = 0; i < 7; i++) { // Loop for Monday to Thursday
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
                if(i > 3){
                    filterDataByDate(dateStr);
                }
            }

            // Add the dateRow to the table
            daily_table.addView(dateRow);
           //weekend_table.addView(dateRowWeekend);
            // Reset calendar to the start of the selected week for filtering appointments
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.WEEK_OF_MONTH, weekNumber);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start from Monday// Reset time to midnight (start of the day)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Date startOfWeek = calendar.getTime();
            Log.d("SalesFragment", "Start of Week (reset to midnight): " + startOfWeek);

            calendar.add(Calendar.DAY_OF_MONTH, 6); // Add 6 days to get to Sunday
            Date endOfWeek = calendar.getTime();
            Log.d("SalesFragment", "End of Week (reset to midnight): " + endOfWeek);

            // Initialize daily sales for each employee for each day of the week (only Monday to Thursday)
            for (Appointment appointment : appointmentsList) {
                Date appointmentDate = appointment.getClientDateTimeAsDate();
                if (appointmentDate != null && (appointmentDate.after(startOfWeek) || appointmentDate.equals(startOfWeek)) && (appointmentDate.before(endOfWeek) || appointmentDate.equals(endOfWeek))) {
                    // Check for and display sub-services
                    List<Map<String, Object>> services = appointment.getServices();
                    for (Map<String, Object> service : services) {
                        Object employeeObj = service.get("assignedEmployee");

                        if (employeeObj != null && !"None".equals(employeeObj.toString())) {
                            String employee = employeeObj.toString();
                            double totalPriceForService = (Double) service.get("servicePrice");

                            // Handle sub-services
                            List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                            if (subServices != null) {
                                for (Map<String, Object> subService : subServices) {
                                    Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;
                                    totalPriceForService += subServicePrice;
                                }
                            }

                            // Update the employee's total sales
                            employeeSalesMap.put(employee, employeeSalesMap.getOrDefault(employee, 0.0) + totalPriceForService);

                            // Ensure dailySalesMap is initialized for 7 days (Monday to Sunday)
                            if (!dailySalesMap.containsKey(employee)) {
                                dailySalesMap.put(employee, new double[7]); // Array for 7 days (Monday to Sunday)
                            }

                            // Calculate which day of the week the appointment occurred
                            Calendar appointmentCalendar = Calendar.getInstance();
                            appointmentCalendar.setTime(appointmentDate);
                            int dayOfWeek = appointmentCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY; // Adjust to 0-based index (Monday = 0)

                            // Correct this logic to handle Sunday as well
                            if (dayOfWeek < 0) {
                                dayOfWeek = 6; // If it's Sunday, set index to 6
                            }

                            if (dayOfWeek >= 0 && dayOfWeek < 7) { // Ensure the day is within the range Monday-Sunday
                                dailySalesMap.get(employee)[dayOfWeek] += totalPriceForService;
                            }
                        }
                    }
                }
            }


            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.WEEK_OF_MONTH, weekNumber);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start from Monday// Reset time to midnight (start of the day)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Map<String, Double> sortedEmployeeSalesMap = new TreeMap<>(employeeSalesMap);
            Calendar displayCalendar2 = (Calendar) calendar.clone();
            for (Map.Entry<String, Double> entry : sortedEmployeeSalesMap.entrySet()) {
                String employeeName = entry.getKey();
                double sales = entry.getValue();
                // Find the employee details
                Employee employee = findEmployeeByName(employeeName);
                if (employee != null) {

                    // Create rows for weekdays and weekends
                    TableRow rowCommission = new TableRow(this);
                    rowCommission.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    TableRow rowWeekend = new TableRow(this);
                    rowWeekend.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    // Add employee name for weekdays and weekends
                    TextView nameTextView = createTextView(employeeName);
                    rowCommission.addView(nameTextView);

                    double totalSalesPerEmp = 0.0;
                    double totalCommissionPerEmp = 0.0;

                    // Process sales and commissions for 7 days (weekdays and weekends)
                    for (int i = 0; i < 7; i++) { // Process 7 days of the week
                        Calendar currentDayCalendar = (Calendar) displayCalendar2.clone();

                        String dateStr = sdf.format(currentDayCalendar.getTime());
                        Log.d("SelectedDate", dateStr);

                        // Ensure that you are displaying the sales data for all 7 days, including Sunday
                        double[] salesData = dailySalesMap.get(employeeName);
                        if (salesData != null && salesData.length > i) {
                            double dailySales = salesData[i];
                            double commissionRate = getCommissionRateByDate(employee, dateStr);
                            double dailyCommission = (dailySales * commissionRate) / 100.0;
                            if (i == 6) { // Index 6 is Sunday
                                Log.d("SundaySales", "Sunday sales for " + employeeName + ": " + dailySales);
                            }
                            // Display sales and commission for each day
                            TextView dailySalesTextView = createTextView(numberFormat.format(dailySales));
                            TextView dailyCommissionTextView = createTextView(numberFormat.format(dailyCommission));

                            rowCommission.addView(dailySalesTextView);
                            rowCommission.addView(dailyCommissionTextView);

                            totalSalesPerEmp += dailySales;
                            totalCommissionPerEmp += dailyCommission;
                        } else {
                            Log.d("SalesData", "No sales data available for day " + i + " for employee " + employeeName);
                        }

                        // Move to the next day
                        displayCalendar2.add(Calendar.DAY_OF_MONTH, 1);
                    }


                    // Add total sales and commission for weekends
                    TextView totalSalesTextView = createTextView(numberFormat.format(totalSalesPerEmp));
                    totalSalesTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    totalSalesTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
                    TextView totalCommissionTextView = createTextView(numberFormat.format(totalCommissionPerEmp));
                    totalCommissionTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    totalCommissionTextView.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
                    rowCommission.addView(totalSalesTextView);
                    rowCommission.addView(totalCommissionTextView);

                    // Add rows to respective tables
                    daily_table.addView(rowCommission);
                    //weekend_table.addView(rowWeekend);
                }
            }


            // Sort EmployeeSalList alphabetically by employee name (or ID)
            Collections.sort(EmployeeSalList, new Comparator<EmployeeSalaryDetails>() {
                @Override
                public int compare(EmployeeSalaryDetails emp1, EmployeeSalaryDetails emp2) {
                    return emp1.getEmployeeId().compareToIgnoreCase(emp2.getEmployeeId());
                }
            });

            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.WEEK_OF_MONTH, weekNumber);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start from Monday// Reset time to midnight (start of the day)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Calendar displayCalendar3 = (Calendar) calendar.clone();
            double overAllTotalSalary = 0.0;
            for (EmployeeSalaryDetails emp : EmployeeSalList) {
                Employee employee = findEmployeeByName(emp.getEmployeeId());
                Log.d("FullSalaryReports", emp.getEmployeeId());

                // Check if the employee and required fields exist before proceeding
                if (employee != null && emp.getEmployeeId() != null && !"None".equals(emp.getEmployeeId())) {

                    // Check if the employee role is "Regular"
                    if ("Regular".equals(employee.getTherapist()) || employee.getSalary() != 0) {
                        TableRow tableRow = new TableRow(this);
                        Log.d("FullSalaryReports", emp.getLateDeduction());

                        // Calculate the basic weekly salary and deductions
                        String daysPresent = emp.getDaysPresent()!= "" ? emp.getDaysPresent() : "0";
                        double weekSal = getSalaryByDate(employee, emp.getDaysPresent(), sdf);
                        double lateDeduction = emp.getLateDeduction().isEmpty() ? 0.0 : Double.parseDouble(emp.getLateDeduction());
                        double deductedWeekSal = weekSal - lateDeduction;
                        double totalCommissionPerEmp = 0.0;

                        // Check if the employee has sales data in the dailySalesMap
                        double[] dailySalesArray = dailySalesMap.get(emp.getEmployeeId());
                        if (dailySalesArray == null) {
                            dailySalesArray = new double[7];  // Initialize with zero sales for all 7 days if not found
                        }

                        // Process sales and commissions for 7 days (weekdays and weekends)
                        for (int i = 0; i < 7; i++) {
                            String dateStr = sdf.format(displayCalendar2.getTime());
                            double commissionRate = getCommissionRateByDate(employee, dateStr);
                            double dailySales = dailySalesArray[i];
                            double dailyCommission = (dailySales * commissionRate) / 100.0;
                            totalCommissionPerEmp += dailyCommission;
                            displayCalendar2.add(Calendar.DAY_OF_MONTH, 1);
                        }

                        // Calculate total salary including commission and deductions
                        double totalSalary = deductedWeekSal + totalCommissionPerEmp;
                        double caDeduction = emp.getCaDeduction().isEmpty() ? 0.0 : Double.parseDouble(emp.getCaDeduction());
                        double totalSalaryDeducted = totalSalary - caDeduction;

                        overAllTotalSalary += totalSalaryDeducted;

                        // Create TextViews for displaying employee details
                        TextView name = createTextView(emp.getEmployeeId());
                        TextView perDay = createTextView(numberFormat.format(employee.getSalary()));
                        TextView daysPresentTextView = createTextView(daysPresent);
                        TextView weekSalary = createTextView(numberFormat.format(weekSal));
                        TextView lateDeductionTextView = createTextView(numberFormat.format(lateDeduction));
                        TextView deductedSalary = createTextView(numberFormat.format(deductedWeekSal));
                        TextView commissionTextView = createTextView(numberFormat.format(totalCommissionPerEmp));
                        TextView totalSalaryTextView = createTextView(numberFormat.format(totalSalary));
                        TextView caDeductionTextView = createTextView(numberFormat.format(caDeduction));
                        TextView overallSalaryTextView = createTextView(numberFormat.format(totalSalaryDeducted));

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
                }
            }

            TableRow tableRow4 = new TableRow(this);
            for(int i = 0; i < 8; i++){
                TextView TextView5 = createTextView("");
                tableRow4.addView(TextView5);
            }
            double remainingBal = 0.0;

            TextView dateTextView4 = createTextView("Overall Salary");
            dateTextView4.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
            dateTextView4.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            TextView ovSal1 = createTextView(numberFormat.format(overAllTotalSalary));
            ovSal1.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
            ovSal1.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            tableRow4.addView(dateTextView4);
            tableRow4.addView(ovSal1);
            finalSalaryTable.addView(tableRow4);

            remainingBal = totalSalesFtS - overAllTotalSalary;
            TableRow tableRow = new TableRow(this);
            tableRow.setBackgroundResource(R.color.gray);
            // Set the formatted date to TextView
            TextView dateTextView = createTextView("Total");
            TextView bal = createTextView(numberFormat.format(totalSalesFtS));


            tableRow.addView(dateTextView);
            tableRow.addView(bal);

            TableRow tableRow2 = new TableRow(this);
            TextView dateTextView2 = createTextView("Overall Salary");
            TextView ovSal = createTextView("-"+numberFormat.format(overAllTotalSalary));
            tableRow2.addView(dateTextView2);
            tableRow2.addView(ovSal);

            TableRow tableRow3 = new TableRow(this);
            tableRow3.setBackgroundResource(R.color.gray);
            TextView dateTextView3 = createTextView("");
            TextView Rbal = createTextView(numberFormat.format(remainingBal));
            Rbal.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
            Rbal.setTypeface(ResourcesCompat.getFont(this, R.font.manrope_bold));
            Rbal.setTextSize(20);
            tableRow3.addView(dateTextView3);
            tableRow3.addView(Rbal);
            breakdown_table.addView(tableRow);
            breakdown_table.addView(tableRow2);
            breakdown_table.addView(tableRow3);
            swipeRefreshLayout.setRefreshing(false);
        } catch (Exception e) {
            Log.e("SalesFragment", "Error filtering data: ", e);
            swipeRefreshLayout.setRefreshing(false);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void filterDataByDate(String selectedDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Map<String, Double> employeeSalesMap = new HashMap<>(); // To store sales by employee
        Map<String, Double> employeeCommissionMap = new HashMap<>();

        double totalSales = 0.0;
        double totalTherCommission = 0.0;
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
                            String name = "";
                            String time = "";

                            // Check for and display sub-services
                            List<Map<String, Object>> services = appointment.getServices();
                            for (Map<String, Object> service : services) {
                                String assignedEmployee = (String) service.get("assignedEmployee");
                                if (assignedEmployee == null || assignedEmployee.equals("None")) {
                                    continue;
                                }

                                if (!appointment.getFullName().equals(name) && !appointment.getTime().equals(time)) {
                                    name = appointment.getFullName();
                                    time = appointment.getTime();
                                }

                                double totalPriceForParentService = (Double) service.get("servicePrice");

                                List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                                if (subServices != null) {
                                    for (Map<String, Object> subService : subServices) {
                                        Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;
                                        totalPriceForParentService += subServicePrice;
                                    }
                                }

                                totalSales += totalPriceForParentService;
                            }

                            // Process employee sales
                            for (Map<String, Object> service : appointment.getServices()) {
                                String employee = (String) service.get("assignedEmployee");
                                if (employee == null || employee.equals("None")) {
                                    continue;
                                }

                                double totalPriceForService = (Double) service.get("servicePrice");
                                List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                                if (subServices != null) {
                                    for (Map<String, Object> subService : subServices) {
                                        Double subServicePrice = subService.get("servicePrice") != null ? (double) subService.get("servicePrice") : 0.0;
                                        totalPriceForService += subServicePrice;
                                    }
                                }

                                // Update employee sales
                                if (employeeSalesMap.containsKey(employee)) {
                                    employeeSalesMap.put(employee, employeeSalesMap.get(employee) + totalPriceForService);
                                } else {
                                    employeeSalesMap.put(employee, totalPriceForService);
                                }
                            }
                        }
                    }
                }

                // Assuming employeeSalesMap contains employee names and their corresponding sales
                for (Map.Entry<String, Double> entry : employeeSalesMap.entrySet()) {
                    String employeeName = entry.getKey();
                    double sales = entry.getValue();

                    // Find the employee by name
                    Employee employee = findEmployeeByName(employeeName);
                    if (employee != null) {
                        // Check if the employee is a therapist
                        String therapistRole = employee.getTherapist();
                        if ("Therapist".equals(therapistRole) || employee.getSalary() == 0) {
                            // Retrieve the appropriate commission rate based on the current date
                            double commissionRate = getCommissionRateByDate(employee, date.toString()); // Pass the current date
                            double employeeCommission = (sales * commissionRate) / 100.0;
                            totalTherCommission += employeeCommission;

                            // Store the calculated commission in the employeeCommissionMap (if needed)
                            employeeCommissionMap.put(employeeName, employeeCommission);
                        }
                    }
                }

                displayTotalSalesWithDeductions(selectedDate, totalSales, totalTherCommission);
            }
        } catch (ParseException e) {
            Log.e("SalesFragment", "Error parsing date: ", e);
        }
    }
    private void displayTotalSalesWithDeductions(String selectedDate, double totalSales, double totalTherCommission) {
        Log.d("SalesFragment", "TotalTherCom: " + totalTherCommission);
        double totalExpenses = 0.0;
        double totalGcash = 0.0;
        double totalFunds = 0.0;
        List<Expenses> expensesList = getExpensesForDate(selectedDate);
        List<Gcash> gcashList = getGcashForDate(selectedDate);
        List<Funds> fundsList = getFundsForDate(selectedDate);

        for (Expenses expense : expensesList) {
            // Accumulate total expenses
            totalExpenses += expense.getAmount();
        }

        // Use the updated method
        for (Gcash gcash : gcashList) {
            totalGcash += gcash.getAmount();
        }

        for (Funds funds : fundsList) {
            // Accumulate total expenses
            totalFunds += funds.getAmount();
        }

        double total = totalExpenses + totalGcash + totalTherCommission;
        double balance = totalSales - total ;
        double overall = balance + totalFunds;

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
            TextView bal = createTextView(numberFormat.format(overall));
            totalSalesFtS += overall;
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
    private List<Funds> getFundsForDate(String selectedDate) {
        List<Funds> matchingFunds = new ArrayList<>();
        for (Funds funds : fundsList) {
            if (funds.getTimestamp().equals(selectedDate)) {
                matchingFunds.add(funds);
            }
        }
        return matchingFunds;
    }

    private void loadFundsData() {
        db.collection("add_funds").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fundsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Funds funds = document.toObject(Funds.class);
                            funds.setId(document.getId());
                            fundsList.add(funds);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
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