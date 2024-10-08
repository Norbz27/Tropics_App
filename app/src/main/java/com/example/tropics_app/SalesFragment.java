package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
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
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesFragment extends Fragment {
    private TextView tvAverage, tvHigh, tvLow,tvDayOfWeek;
    private LineChart lineChart;
    private FirebaseFirestore db;
    private List<Appointment> appointmentsList; // List to store appointments
    private Spinner monthSpinner, yearSpinner;
    private Calendar calendar;
    private int targetMonth, targetYear;
    private boolean isDaily = true;
    private TableLayout tableLayout, tableLayout2, tableLayout3, tblGcash, tblExpenses;
    private List<Employee> employeeList;
    private List<Expenses> expensesList;
    private List<Gcash> gcashList;
    private FloatingActionButton fabExpenses, fabGcash;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();
        expensesList = new ArrayList<>();
        gcashList = new ArrayList<>();
        loadEmployeeData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_sales, container, false);
        fabExpenses = rootView.findViewById(R.id.fabExpenses);
        fabGcash = rootView.findViewById(R.id.fabGcash);

        EditText DatePicker = rootView.findViewById(R.id.date_picker);
        tableLayout = rootView.findViewById(R.id.tblayout);
        tableLayout2 = rootView.findViewById(R.id.tblayout2);
        tableLayout3 = rootView.findViewById(R.id.tblayout3);
        tblGcash = rootView.findViewById(R.id.tblGcash);
        tblExpenses = rootView.findViewById(R.id.tblExpenses);
        tvDayOfWeek = rootView.findViewById(R.id.day_of_Week);
        DatePicker.setOnClickListener(v -> showDatePickerDialog(DatePicker));
        Date dateNow = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        DatePicker.setText(dateFormat.format(dateNow));

        fabExpenses.setOnClickListener(v -> showExpensesDialog(DatePicker.getText()));
        fabGcash.setOnClickListener(v -> showGcashDialog(DatePicker.getText()));

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

    private void showExpensesDialog(Editable selectedDate) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_new_expenses, null);

        EditText edAmount = dialogView.findViewById(R.id.etAmount);
        EditText edReason = dialogView.findViewById(R.id.etReason);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        // Create an instance of Firebase Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnSubmit.setOnClickListener(v -> {
            // Get input values
            String amount = edAmount.getText().toString().trim();
            String reason = edReason.getText().toString().trim();

            // Validate input
            if (amount.isEmpty() || reason.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter both amount and reason", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation warning
            new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                    .setTitle("Confirm Submission")
                    .setMessage("Once submitted, this expense cannot be erased. Do you want to continue?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        // Submit expense
                        Date dateNow = new Date();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                        Map<String, Object> expense = new HashMap<>();
                        expense.put("amount", Double.parseDouble(amount));  // Store amount as a number
                        expense.put("reason", reason);
                        expense.put("timestamp", dateFormat.format(dateNow)); // Add a timestamp

                        // Submit the expense to Firestore
                        db.collection("expenses")
                                .add(expense)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getActivity(), "Expense added successfully", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();  // Close the original dialog
                                    loadExpensesData();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getActivity(), "Failed to add expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("No", null)  // Dismiss the confirmation dialog if 'No' is clicked
                    .show();
        });
    }

    private void showGcashDialog(Editable selectedDate) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_gcash, null);

        EditText edAmount = dialogView.findViewById(R.id.etAmount);
        EditText edClientName = dialogView.findViewById(R.id.etClient);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        // Create an instance of Firebase Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnSubmit.setOnClickListener(v -> {
            // Get input values
            String amount = edAmount.getText().toString().trim();
            String clientName = edClientName.getText().toString().trim();

            // Validate input
            if (amount.isEmpty() && clientName.isEmpty()) {
                Toast.makeText(getActivity(), "Please enter the GCash amount", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show confirmation warning
            new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme)
                    .setTitle("Confirm Submission")
                    .setMessage("Once submitted, this GCash payment cannot be erased. Do you want to continue?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        // Submit GCash payment
                        Date dateNow = new Date();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                        Map<String, Object> gcashPayment = new HashMap<>();
                        gcashPayment.put("amount", Double.parseDouble(amount));  // Store amount as a number
                        gcashPayment.put("paymentMethod", "GCash");
                        gcashPayment.put("timestamp", dateFormat.format(dateNow));  // Add a timestamp

                        // Submit the GCash payment to Firestore
                        db.collection("gcash_payments")
                                .add(gcashPayment)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getActivity(), "GCash payment added successfully", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();  // Close the original dialog
                                    loadGcashData();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getActivity(), "Failed to add GCash payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("No", null)  // Dismiss the confirmation dialog if 'No' is clicked
                    .show();
        });
    }

    @SuppressLint("RtlHardcoded")
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

                // Format the day of the week to a word
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE"); // "EEEE" for full name
                String dayOfWeekWord = dayFormat.format(date);

                tvDayOfWeek.setText(dayOfWeekWord);

                // Clear previous rows
                tableLayout.removeViews(1, tableLayout.getChildCount() - 1); // Keep header
                tableLayout2.removeViews(1, tableLayout2.getChildCount() - 1); // Keep header
                tableLayout3.removeViews(1, tableLayout3.getChildCount() - 1);
                tblGcash.removeViews(1, tblGcash.getChildCount() - 1);
                tblExpenses.removeViews(1, tblExpenses.getChildCount() - 1);

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
                                // Skip the service if "assignedEmployee" is null
                                String assignedEmployee = (String) service.get("assignedEmployee");
                                if (assignedEmployee == null) {
                                    continue;
                                }
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

                                tableLayout.addView(row);
                            }

                            List<Map<String, Object>> services2 = appointment.getServices();
                            for (Map<String, Object> service : services2) {
                                String assignedEmployee = (String) service.get("assignedEmployee");
                                if (assignedEmployee == null) {
                                    continue;
                                }
                                String employee = service.get("assignedEmployee").toString();
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
                double totalCommission = 0.0;
                // Assuming employeeSalesMap contains employee names and their corresponding sales
                for (Map.Entry<String, Double> entry : employeeSalesMap.entrySet()) {
                    String employeeName = entry.getKey();
                    double sales = entry.getValue();

                    // Find the employee by name
                    Employee employee = findEmployeeByName(employeeName);
                    if (employee != null) {
                        // Retrieve the commission rate from the employee's data
                        double commissionRate = employee.getComs();
                        double employeeCommission = (sales * commissionRate) / 100.0;
                        totalCommission += employeeCommission;
                        // Store the calculated commission in the employeeCommissionMap (if needed)
                        employeeCommissionMap.put(employeeName, employeeCommission);

                        // Create a new table row for the employee's commission
                        TableRow rowCommission = new TableRow(getContext());
                        rowCommission.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                        // Add the employee's name to the row
                        TextView nameTextView = new TextView(getContext());
                        nameTextView.setText(employeeName);
                        nameTextView.setTextColor(Color.WHITE);
                        nameTextView.setPadding(5, 5, 5, 5);
                        rowCommission.addView(nameTextView);

                        // Add the total sales to the row
                        TextView salesTextView = new TextView(getContext());
                        salesTextView.setText(String.format("₱%.2f", sales));
                        salesTextView.setTextColor(Color.WHITE);
                        salesTextView.setPadding(5, 5, 5, 5);
                        rowCommission.addView(salesTextView);

                        // Add the calculated commission to the row
                        TextView commissionTextView = new TextView(getContext());
                        commissionTextView.setText(String.format("₱%.2f", employeeCommission));
                        commissionTextView.setTextColor(Color.WHITE);
                        commissionTextView.setPadding(5, 5, 5, 5);
                        rowCommission.addView(commissionTextView);

                        // Add the row to the commission table layout (tableLayout2)
                        tableLayout2.addView(rowCommission);
                    }
                }


                // Display total sales
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

                TableRow rowTotalCom = new TableRow(getContext());
                rowTotalCom.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                TextView tv1Com = new TextView(getContext());
                tv1Com.setText("");
                tv1Com.setTextColor(Color.LTGRAY);
                tv1Com.setPadding(10, 5, 5, 5);
                rowTotalCom.addView(tv1Com);

                TextView tv3Com = new TextView(getContext());
                tv3Com.setText("Total Commission:");
                tv3Com.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                tv3Com.setGravity(Gravity.RIGHT);
                tv3Com.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
                tv3Com.setPadding(10, 5, 5, 5);
                rowTotalCom.addView(tv3Com);

                TextView tvTotalCom = new TextView(getContext());
                tvTotalCom.setText(String.format("₱%.2f", totalCommission));
                tvTotalCom.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                tvTotalCom.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
                tvTotalCom.setPadding(10, 5, 5, 5);
                rowTotalCom.addView(tvTotalCom);

                tableLayout2.addView(rowTotalCom);
                tableLayout.addView(rowTotal);

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
        TextView tvTEGD = getActivity().findViewById(R.id.tvTEGD);
        TextView tvGcash = getActivity().findViewById(R.id.tvGcash);
        TextView tvExpenses = getActivity().findViewById(R.id.tvExpenses);

        if(expensesList.size() == 0 && gcashList.size() == 0){
            tableLayout3.setVisibility(View.GONE);
            tvTEGD.setVisibility(View.GONE);

            tblExpenses.setVisibility(View.GONE);
            tblGcash.setVisibility(View.GONE);
            tvGcash.setVisibility(View.GONE);
            tvExpenses.setVisibility(View.GONE);
            return;
        }else {
            tableLayout3.setVisibility(View.VISIBLE);
            tvTEGD.setVisibility(View.VISIBLE);

            tblExpenses.setVisibility(View.VISIBLE);
            tblGcash.setVisibility(View.VISIBLE);
            tvGcash.setVisibility(View.VISIBLE);
            tvExpenses.setVisibility(View.VISIBLE);
        }
        for (Expenses expense : expensesList) {
            TableRow expenseRow = new TableRow(getContext());
            expenseRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            TextView reasonTextView = new TextView(getContext());
            reasonTextView.setText(expense.getReason());
            reasonTextView.setTextColor(Color.LTGRAY);
            reasonTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
            reasonTextView.setPadding(10, 5, 5, 5);
            expenseRow.addView(reasonTextView);

            TextView expenseAmountTextView = new TextView(getContext());
            expenseAmountTextView.setText(String.format("₱%.2f", expense.getAmount()));
            expenseAmountTextView.setTextColor(Color.LTGRAY);
            expenseAmountTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
            expenseAmountTextView.setPadding(10, 5, 5, 5);
            expenseRow.addView(expenseAmountTextView);

            tblExpenses.addView(expenseRow); // Display in tableLayout3 for expenses

            // Accumulate total expenses
            totalExpenses += expense.getAmount();
        }
        TableRow expenseRow = new TableRow(getContext());
        expenseRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView tv1 = createTextViewBold("Total: ");
        tv1.setGravity(Gravity.RIGHT);
        TextView tvTotalExpenses = createTextViewBold(String.format("₱%.2f", totalExpenses));
        expenseRow.addView(tv1);
        expenseRow.addView(tvTotalExpenses);
        tblExpenses.addView(expenseRow);

        // Use the updated method
        for (Gcash gcash : gcashList) {
            TableRow gcashRow = new TableRow(getContext());
            gcashRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            TextView reasonTextView = new TextView(getContext());
            reasonTextView.setText("Gcash");
            reasonTextView.setTextColor(Color.LTGRAY);
            reasonTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
            reasonTextView.setPadding(10, 5, 5, 5);
            gcashRow.addView(reasonTextView);

            TextView expenseAmountTextView = new TextView(getContext());
            expenseAmountTextView.setText(String.format("₱%.2f", gcash.getAmount()));
            expenseAmountTextView.setTextColor(Color.LTGRAY);
            expenseAmountTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
            expenseAmountTextView.setPadding(10, 5, 5, 5);
            gcashRow.addView(expenseAmountTextView);

            tblGcash.addView(gcashRow); // Display in tableLayout3 for expenses
            // Accumulate total expenses
            totalGcash += gcash.getAmount();
        }
        TableRow gcashRow = new TableRow(getContext());
        gcashRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView tv2 = createTextViewBold("Total: ");
        tv2.setGravity(Gravity.RIGHT);
        TextView tvTotalGcash = createTextViewBold(String.format("₱%.2f", totalGcash));
        gcashRow.addView(tv2);
        gcashRow.addView(tvTotalGcash);
        tblGcash.addView(gcashRow);

        TableRow calc = new TableRow(getContext());
        calc.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        TextView tv5 = createTextView("Total Expenses: ");
        TextView tv3 = createTextView(String.format("₱%.2f", totalExpenses));
        calc.addView(tv5);
        calc.addView(tv3);
        tableLayout3.addView(calc);

        TableRow calc2 = new TableRow(getContext());
        calc2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        TextView tv6 = createTextView("Total Gcash: ");
        TextView tv4 = createTextView(String.format("₱%.2f", totalGcash));

        calc2.addView(tv6);
        calc2.addView(tv4);
        tableLayout3.addView(calc2);

        TableRow UndeRow = new TableRow(getContext());
        UndeRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));  // Full width, height 10px

        View line2 = new View(getContext());
        line2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));  // Full width, height 10px
        line2.setBackgroundColor(Color.LTGRAY);

        View line = new View(getContext());
        line.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));  // Full width, height 10px
        line.setBackgroundColor(Color.LTGRAY);

        UndeRow.addView(line);
        UndeRow.addView(line2);

        tableLayout3.addView(UndeRow);

        double total = totalExpenses + totalGcash;
        double balance = totalSales - total;
        TableRow TotalRow = new TableRow(getContext());
        TotalRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView TextView = new TextView(getContext());
        TextView.setText("");
        TextView.setTextColor(Color.LTGRAY);
        TextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        TextView.setPadding(10, 5, 5, 5);
        TotalRow.addView(TextView);

        TextView totalTextView = new TextView(getContext());
        totalTextView.setText(String.format("₱%.2f", total));
        totalTextView.setTextColor(Color.LTGRAY);
        totalTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        totalTextView.setPadding(10, 5, 5, 5);
        TotalRow.addView(totalTextView);
        tableLayout3.addView(TotalRow);

        TableRow TotalSalesRow = new TableRow(getContext());
        TotalSalesRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView TextView1 = new TextView(getContext());
        TextView1.setText("Total Sales:");
        TextView1.setTextColor(Color.LTGRAY);
        TextView1.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        TextView1.setPadding(10, 5, 5, 5);
        TotalSalesRow.addView(TextView1);

        TextView totalSalesTextView = new TextView(getContext());
        totalSalesTextView.setText(String.format("-₱%.2f", totalSales));
        totalSalesTextView.setTextColor(Color.LTGRAY);
        totalSalesTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        totalSalesTextView.setPadding(10, 5, 5, 5);
        TotalSalesRow.addView(totalSalesTextView);

        tableLayout3.addView(TotalSalesRow);

        TableRow UndeRow2 = new TableRow(getContext());
        UndeRow2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));  // Full width, height 10px

        View line3 = new View(getContext());
        line3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));  // Full width, height 10px
        line3.setBackgroundColor(Color.LTGRAY);

        View line4 = new View(getContext());
        line4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 2));  // Full width, height 10px
        line4.setBackgroundColor(Color.LTGRAY);
        UndeRow2.addView(line4);
        UndeRow2.addView(line3);

        tableLayout3.addView(UndeRow2);

        TableRow balSalesRow = new TableRow(getContext());
        balSalesRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView TextView2 = new TextView(getContext());
        TextView2.setText("");
        TextView2.setTextColor(Color.LTGRAY);
        TextView2.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        TextView2.setPadding(10, 5, 5, 5);
        balSalesRow.addView(TextView2);

        TextView balTextView = new TextView(getContext());
        balTextView.setText(String.format("₱%.2f", balance));
        balTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
        balTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
        balTextView.setPadding(10, 5, 5, 5);
        balSalesRow.addView(balTextView);
        tableLayout3.addView(balSalesRow);
    }
    private TextView createTextViewBold(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setPadding(10, 5, 5, 5);
        textView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
        textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
        return textView;
    }
    private TextView createTextView(String text) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setPadding(10, 5, 5, 5);
        textView.setTextColor(Color.LTGRAY);
        textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        return textView;
    }
    private Employee findEmployeeByName(String employeeName) {
        for (Employee employee : employeeList) {
            if (employee.getName().equalsIgnoreCase(employeeName)) {
                return employee; // Return the matching employee object
            }
        }
        return null; // If no match found, return null
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
                        fetchAppointmentData();
                        loadExpensesData();
                        loadGcashData();
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
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
                        fetchAppointmentData();
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
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
                        fetchAppointmentData();
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
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
                            Date dateNow = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

                            filterDataByDate(dateFormat.format(dateNow));
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

        // Get the current date
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);

        // Group appointments by day of the target month and sum the sales
        Calendar calendar = Calendar.getInstance(); // Initialize calendar
        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getClientDateTimeAsDate(); // Use the updated method
            if (date != null) {
                calendar.setTime(date);
                int month = calendar.get(Calendar.MONTH); // Months are zero-based in Calendar
                int year = calendar.get(Calendar.YEAR);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                // Only consider sales from the past and today of the current month and year
                if (month == targetMonth && year == targetYear &&
                        (year < currentYear || (year == currentYear && month < currentMonth) ||
                                (year == currentYear && month == currentMonth && dayOfMonth <= currentDay))) {

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

        // Set X-axis labels to display in mm-dd format
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int day = (int) value;
                // Format the date as mm-dd
                String formattedDate = String.format("%02d-%02d", targetMonth + 1, day); // Add 1 to month for display
                return formattedDate; // Return the formatted date string
            }
        });

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // Ensure all month labels are displayed
        xAxis.setDrawLabels(true); // Enable drawing labels
        xAxis.setTextColor(Color.WHITE); // Set X-axis text color to white
        xAxis.setAxisMinimum(1f); // Set the minimum value of X-axis to 1 (first day of the month)
        xAxis.setAxisMaximum(calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // Set the maximum value of X-axis

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

        // Get the current date
        Calendar today = Calendar.getInstance();
        int currentMonth = today.get(Calendar.MONTH) + 1; // Calendar.MONTH is zero-based
        int currentYear = today.get(Calendar.YEAR);

        // Initialize sales for all 12 months
        for (int month = 1; month <= 12; month++) {
            monthlySales.put(month, 0f); // Initialize all months to 0 sales
        }

        // Group appointments by month and sum the sales only for the selected year
        Calendar calendar = Calendar.getInstance();
        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getClientDateTimeAsDate(); // Use the updated method
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR); // Get the year of the appointment
                int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is zero-based

                // Only include appointments from the selected year and up to the current month
                if (year == selectedYear) {
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
            // Add sales values to entries, using 0 for future months
            float salesValue = (month <= currentMonth || selectedYear < currentYear) ? monthlySales.get(month) : 0f;
            monthlyEntries.add(new Entry(month, salesValue));
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
