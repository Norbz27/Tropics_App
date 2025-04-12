package com.example.tropics_app;

import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class SalesTrackingFragment extends Fragment {
    private Button btnDaily, btnMonthly;
    private Spinner monthSpinner, yearSpinner;
    private BarChart barChart;
    private ProgressBar progressBar;
    private FrameLayout progressContainer1;
    private boolean isDaily = true;
    private int targetMonth, targetYear;
    private TextView tvAverage, tvHigh, tvLow, tvTotal;
    private List<Appointment> appointmentsList;
    private FirebaseFirestore db;
    private List<Employee> employeeList;
    private List<Expenses> expensesList;
    private List<Gcash> gcashList;
    private List<Funds> fundsList;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();
        expensesList = new ArrayList<>();
        gcashList = new ArrayList<>();
        fundsList = new ArrayList<>();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_sales_tracking, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        progressContainer1 = view.findViewById(R.id.progressContainer1);
        fetchAppointmentData();
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


    private void setDailyData(List<Appointment> appointmentsList, int targetMonth, int targetYear) {
        ArrayList<BarEntry> dailyEntries = new ArrayList<>();
        Map<Integer, Float> dailySales = new HashMap<>();
        Map<Integer, Float> dailyGcash = new HashMap<>();
        Map<Integer, Float> dailyFunds = new HashMap<>();
        Map<Integer, Float> dailyExpenses = new HashMap<>();
        Map<Integer, Float> dailyTherapistCommissions = new HashMap<>();

        float totalSales = 0f;
        int daysWithSales = 0;
        float highestSales = Float.MIN_VALUE;
        float lowestSales = Float.MAX_VALUE;

        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH);
        int currentYear = today.get(Calendar.YEAR);

        Calendar calendar = Calendar.getInstance();

        // Initialize daily maps
        for (int day = 1; day <= 31; day++) {
            dailySales.put(day, 0f);
            dailyExpenses.put(day, 0f);
            dailyTherapistCommissions.put(day, 0f);
            dailyGcash.put(day, 0f);
            dailyFunds.put(day, 0f);
        }

        for (Appointment appointment : appointmentsList) {
            Date date = appointment.getClientDateTimeAsDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

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

                        // Update daily sales
                        float updatedSales = dailySales.get(day) + (float) totalPriceForParentService;
                        dailySales.put(day, updatedSales);

                        // Therapist commission
                        Employee employee = findEmployeeByName(assignedEmployee);
                        if (employee != null) {
                            String therapistRole = employee.getTherapist();
                            if ("Therapist".equals(therapistRole) || employee.getSalary() == 0) {
                                double commissionRate = getCommissionRateByDate(employee, new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date));
                                double commission = (totalPriceForParentService * commissionRate) / 100.0;
                                float updatedCommission = dailyTherapistCommissions.get(day) + (float) commission;
                                dailyTherapistCommissions.put(day, updatedCommission);
                            }
                        }
                    }
                }
            }
        }

        // Calculate expenses per day
        for (Expenses expense : expensesList) {
            Date date = expense.getParsedDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                if (month == targetMonth && year == targetYear) {
                    float updatedExpense = dailyExpenses.getOrDefault(day, 0f) + (float) expense.getAmount();
                    dailyExpenses.put(day, updatedExpense);
                }
            }
        }

        for (Gcash gcash : gcashList) {
            Date date = gcash.getParsedDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                if (month == targetMonth && year == targetYear) {
                    float updatedExpense = dailyGcash.getOrDefault(day, 0f) + (float) gcash.getAmount();
                    dailyGcash.put(day, updatedExpense);
                }
            }
        }

        for (Funds funds : fundsList) {
            Date date = funds.getParsedDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                if (month == targetMonth && year == targetYear) {
                    float updatedExpense = dailyFunds.getOrDefault(day, 0f) + (float) funds.getAmount();
                    dailyFunds.put(day, updatedExpense);
                }
            }
        }


        // Get actual number of days in the target month
        calendar.set(Calendar.MONTH, targetMonth);
        calendar.set(Calendar.YEAR, targetYear);
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            float grossSales = dailySales.getOrDefault(day, 0f);
            float expenses = dailyExpenses.getOrDefault(day, 0f);
            float gcash = dailyGcash.getOrDefault(day, 0f);
            float funds = dailyFunds.getOrDefault(day, 0f);
            float commissions = dailyTherapistCommissions.getOrDefault(day, 0f);
            float netDeductions = gcash + expenses + commissions;
            float netSales = (grossSales + funds) - netDeductions;

            dailyEntries.add(new BarEntry(day, netSales));

            if (netSales > 0f) {
                daysWithSales++;
                totalSales += netSales;
                if (netSales > highestSales) highestSales = netSales;
                if (netSales < lowestSales) lowestSales = netSales;
            }
        }

        if (lowestSales == Float.MAX_VALUE) {
            lowestSales = 0f;
        }

        // Format the currency
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        float averageSales = (daysWithSales > 0) ? totalSales / daysWithSales : 0f;
        tvAverage.setText(currencyFormat.format(averageSales));
        tvTotal.setText(currencyFormat.format(totalSales));
        tvHigh.setText("Highest Sales: " + currencyFormat.format(highestSales));
        tvLow.setText("Lowest Sales: " + currencyFormat.format(lowestSales));

        BarDataSet dataSet = new BarDataSet(dailyEntries, null);
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
                return String.format("%02d-%02d", targetMonth + 1, (int) value);
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(daysInMonth);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisMaximum(daysInMonth);
        xAxis.setAxisMinimum(0.5f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
    }


    private void setMonthlyData(List<Appointment> appointmentsList, int selectedYear) {
        ArrayList<BarEntry> monthlyEntries = new ArrayList<>();
        Map<Integer, Float> monthlySales = new HashMap<>();
        Map<Integer, Float> monthlyGcash = new HashMap<>();
        Map<Integer, Float> monthlyFunds = new HashMap<>();
        Map<Integer, Float> monthlyExpenses = new HashMap<>();
        Map<Integer, Float> monthlyTherapistCommissions = new HashMap<>();

        float totalSales = 0f;
        int monthsWithSales = 0;
        float highestSales = Float.MIN_VALUE;
        float lowestSales = Float.MAX_VALUE;

        Calendar today = Calendar.getInstance();
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentYear = today.get(Calendar.YEAR);

        Calendar calendar = Calendar.getInstance();

        // Initialize monthly maps
        for (int month = 1; month <= 12; month++) {
            monthlySales.put(month, 0f);
            monthlyExpenses.put(month, 0f);
            monthlyTherapistCommissions.put(month, 0f);
            monthlyGcash.put(month, 0f);
            monthlyFunds.put(month, 0f);
        }

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

                        // Add to monthly sales
                        float updatedSales = monthlySales.get(month) + (float) totalPriceForParentService;
                        monthlySales.put(month, updatedSales);

                        // Therapist commission deduction
                        Employee employee = findEmployeeByName(assignedEmployee);
                        if (employee != null) {
                            String therapistRole = employee.getTherapist();
                            if ("Therapist".equals(therapistRole) || employee.getSalary() == 0) {
                                double commissionRate = getCommissionRateByDate(employee, new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(date));
                                double commission = (totalPriceForParentService * commissionRate) / 100.0;

                                float updatedCommission = monthlyTherapistCommissions.get(month) + (float) commission;
                                monthlyTherapistCommissions.put(month, updatedCommission);
                            }
                        }
                    }
                }
            }
        }

        // Aggregate expenses by month
        for (Expenses expense : expensesList) {
            Date date = expense.getParsedDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;

                if (year == selectedYear) {
                    float updatedExpenses = monthlyExpenses.getOrDefault(month, 0f) + (float) expense.getAmount();
                    monthlyExpenses.put(month, updatedExpenses);
                }
            }
        }

        for (Gcash gcash : gcashList) {
            Date date = gcash.getParsedDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;

                if (year == selectedYear) {
                    float updatedExpenses = monthlyGcash.getOrDefault(month, 0f) + (float) gcash.getAmount();
                    monthlyGcash.put(month, updatedExpenses);
                }
            }
        }

        for (Funds funds : fundsList) {
            Date date = funds.getParsedDate();
            if (date != null) {
                calendar.setTime(date);
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;

                if (year == selectedYear) {
                    float updatedExpenses = monthlyFunds.getOrDefault(month, 0f) + (float) funds.getAmount();
                    monthlyFunds.put(month, updatedExpenses);
                }
            }
        }

        // Compute monthly net sales and statistics
        for (int month = 1; month <= 12; month++) {
            float grossSales = monthlySales.get(month);
            float expenses = monthlyExpenses.get(month);
            float gcash = monthlyGcash.get(month);
            float funds = monthlyFunds.get(month);
            float commissions = monthlyTherapistCommissions.get(month);
            float netDeductions = gcash + expenses + commissions;
            float netSales = (grossSales + funds) - netDeductions;

            if (month <= currentMonth || selectedYear < currentYear) {
                monthlyEntries.add(new BarEntry(month, netSales));
            } else {
                monthlyEntries.add(new BarEntry(month, 0f));
            }

            if (netSales > 0f) {
                monthsWithSales++;
                totalSales += netSales;
                if (netSales > highestSales) highestSales = netSales;
                if (netSales < lowestSales) lowestSales = netSales;
            }
        }

        if (lowestSales == Float.MAX_VALUE) {
            lowestSales = 0f;
        }

        float averageSales = (monthsWithSales > 0) ? totalSales / monthsWithSales : 0f;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(2);

        tvTotal.setText(currencyFormat.format(totalSales));
        tvAverage.setText(currencyFormat.format(averageSales));
        tvHigh.setText("Highest Sales: " + currencyFormat.format(highestSales));
        tvLow.setText("Lowest Sales: " + currencyFormat.format(lowestSales));

        BarDataSet dataSet = new BarDataSet(monthlyEntries, null);
        dataSet.setGradientColor(ContextCompat.getColor(requireContext(), R.color.orange),
                ContextCompat.getColor(requireContext(), R.color.yellow));
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
                if (value >= 1 && value <= 12) return months[(int) value - 1];
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12);
        xAxis.setDrawLabels(true);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAxisMinimum(0.5f);
        xAxis.setAxisMaximum(12.5f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);
        barChart.invalidate();
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
    private double getCommissionRateByDate(Employee employee, String appointmentDate) {
        List<Map<String, Object>> commissionHistory = employee.getCommissionsHistory();
        double commissionRate = employee.getComs(); // Default to current commission rate

        if (commissionHistory == null || commissionHistory.isEmpty()) {
            return commissionRate; // No history, return default
        }

        // Parse the appointment date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); // Adjust date format to match your Firebase data
            Date appointment = sdf.parse(appointmentDate);

            // Iterate through commission history to find the appropriate rate based on date
            for (Map<String, Object> history : commissionHistory) {
                String changeDateStr = (String) history.get("dateChanged");
                double rateAtChange = (Double) history.get("commission");

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

    private Employee findEmployeeByName(String employeeName) {
        for (Employee employee : employeeList) {
            if (employee.getName().equalsIgnoreCase(employeeName)) {
                return employee; // Return the matching employee object
            }
        }
        return null;
    }
    private void fetchAppointmentData() {
        requireActivity().runOnUiThread(() -> progressContainer1.setVisibility(View.VISIBLE));

        // Create a list of tasks to execute in parallel
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        // Fetch appointment data concurrently
        tasks.add(db.collection("appointments").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        appointmentsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Appointment appointment = document.toObject(Appointment.class);
                            appointmentsList.add(appointment);
                        }
                        setDailyData(appointmentsList, targetMonth, targetYear);
                    } else {
                        Log.e("SalesFragment", "Error fetching appointments: ", task.getException());
                    }
                }));

        // Fetch employee data concurrently
        tasks.add(db.collection("Employees").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        employeeList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            employee.setId(document.getId());
                            employeeList.add(employee);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load employee data", Toast.LENGTH_SHORT).show();
                    }
                }));

        // Fetch expenses data concurrently
        tasks.add(db.collection("expenses").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        expensesList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Expenses expenses = document.toObject(Expenses.class);
                            expenses.setId(document.getId());
                            expensesList.add(expenses);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load expenses data", Toast.LENGTH_SHORT).show();
                    }
                }));

        // Fetch funds data concurrently
        tasks.add(db.collection("add_funds").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        fundsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Funds funds = document.toObject(Funds.class);
                            funds.setId(document.getId());
                            fundsList.add(funds);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load funds data", Toast.LENGTH_SHORT).show();
                    }
                }));

        // Fetch gcash data concurrently
        tasks.add(db.collection("gcash_payments").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        gcashList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Gcash gcash = document.toObject(Gcash.class);
                            gcash.setId(document.getId());
                            gcashList.add(gcash);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load gcash data", Toast.LENGTH_SHORT).show();
                    }
                }));

        // Use Task.whenAllComplete to wait for all tasks to complete
        Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
            if (isAdded()) {
                // Once all tasks are done, hide the progress bar
                progressContainer1.setVisibility(View.GONE);
            }
        });
    }

}