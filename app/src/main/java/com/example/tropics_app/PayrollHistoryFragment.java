package com.example.tropics_app;

import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PayrollHistoryFragment extends Fragment {
    private List<Appointment> appointmentsList;
    private FirebaseFirestore db;
    private List<Employee> employeeList;
    private List<Expenses> expensesList;
    private List<Gcash> gcashList;
    private List<Funds> fundsList;
    private List<EmployeeSalaryDetails> salaryDetailsList ;
    private FrameLayout progressContainer1;
    private Spinner month_spinner;
    private Spinner year_spinner;
    private Spinner week_num;
    private NumberFormat numberFormat;
    private Button btnSearch;
    private TableLayout tblHandler, tblWeekly, tblSalary;
    private TextView tvEmpName, tvRole, tvFSC;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        appointmentsList = new ArrayList<>();
        employeeList = new ArrayList<>();
        expensesList = new ArrayList<>();
        gcashList = new ArrayList<>();
        fundsList = new ArrayList<>();
        salaryDetailsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payroll_history, container, false);

        progressContainer1 = view.findViewById(R.id.progressContainer1);
        month_spinner = view.findViewById(R.id.month_spinner);
        year_spinner = view.findViewById(R.id.year_spinner);
        week_num = view.findViewById(R.id.week_num);
        tblHandler = view.findViewById(R.id.tblHandler);
        tblWeekly = view.findViewById(R.id.tblWeekly);
        tblSalary = view.findViewById(R.id.tblSalary);
        tvEmpName = view.findViewById(R.id.tvEmpName);
        tvRole = view.findViewById(R.id.tvRole);
        tvFSC = view.findViewById(R.id.tvFSC);

        numberFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

        AutoCompleteTextView autoCompleteTVEmployee = view.findViewById(R.id.autoCompleteTextView);
        List<String> employeeNames = new ArrayList<>();
        for (Employee emp : employeeList) {
            employeeNames.add(emp.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                employeeNames
        );

        autoCompleteTVEmployee.setAdapter(adapter);
        autoCompleteTVEmployee.setThreshold(1);
        fetchAppointmentData();
        spinnerSetup();

        btnSearch = view.findViewById(R.id.btnSearch); // You need to add this in XML

        btnSearch.setOnClickListener(v -> {
            int selectedMonth = month_spinner.getSelectedItemPosition(); // 0-based
            int selectedYear = Integer.parseInt(year_spinner.getSelectedItem().toString());
            int selectedWeek = Integer.parseInt(week_num.getSelectedItem().toString());
            String empName = autoCompleteTVEmployee.getText().toString();


            filterDataByMonthYearWeek(selectedMonth, selectedYear, selectedWeek, empName);

        });



        return view;
    }

    private void filterDataByMonthYearWeek(int month, int year, int weekNumber, String empName) {
        progressContainer1.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
        new Thread(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.WEEK_OF_MONTH, weekNumber);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            Date startOfWeek = calendar.getTime();
            calendar.add(Calendar.DAY_OF_MONTH, 6);
            Date endOfWeek = calendar.getTime();

            Collections.sort(appointmentsList, (a1, a2) -> {
                try {
                    Calendar cal1 = Calendar.getInstance();
                    cal1.setTime(a1.getClientDateTimeAsDate());
                    Date time1 = new SimpleDateFormat("HH:mm").parse(a1.getTime());
                    Calendar timeCal1 = Calendar.getInstance();
                    timeCal1.setTime(time1);
                    cal1.set(Calendar.HOUR_OF_DAY, timeCal1.get(Calendar.HOUR_OF_DAY));
                    cal1.set(Calendar.MINUTE, timeCal1.get(Calendar.MINUTE));

                    Calendar cal2 = Calendar.getInstance();
                    cal2.setTime(a2.getClientDateTimeAsDate());
                    Date time2 = new SimpleDateFormat("HH:mm").parse(a2.getTime());
                    Calendar timeCal2 = Calendar.getInstance();
                    timeCal2.setTime(time2);
                    cal2.set(Calendar.HOUR_OF_DAY, timeCal2.get(Calendar.HOUR_OF_DAY));
                    cal2.set(Calendar.MINUTE, timeCal2.get(Calendar.MINUTE));

                    return cal1.getTime().compareTo(cal2.getTime());

                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            });

            Employee employeeObj = findEmployeeByName(empName);
            List<TableRow> generatedRows = new ArrayList<>();
            double totalComms = 0.0;
            String lastClientName = "";
            String lastTime = "";

            for (Appointment appointment : appointmentsList) {
                Date appointmentDate = appointment.getClientDateTimeAsDate();
                if (appointmentDate == null) continue;

                if (!appointmentDate.before(startOfWeek) && !appointmentDate.after(endOfWeek)) {
                    String formattedTime = "";
                    String formattedDate = "";

                    try {
                        formattedTime = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                .format(new SimpleDateFormat("HH:mm").parse(appointment.getTime()));
                        formattedDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                .format(appointmentDate);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    for (Map<String, Object> service : appointment.getServices()) {
                        String assignedEmployee = (String) service.get("assignedEmployee");
                        if (assignedEmployee == null || assignedEmployee.equals("None")) continue;
                        if (!assignedEmployee.equals(empName)) continue;

                        TableRow row = new TableRow(getContext());
                        row.setLayoutParams(new TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                        ));

                        if (!appointment.getFullName().equals(lastClientName)
                                || !appointment.getTime().equals(lastTime)) {

                            row.addView(makeTextView(formattedDate, Color.WHITE));
                            row.addView(makeTextView(formattedTime, Color.WHITE));
                            row.addView(makeTextView(appointment.getFullName(), Color.WHITE));

                            lastClientName = appointment.getFullName();
                            lastTime = appointment.getTime();
                        } else {
                            row.addView(emptyTextView());
                            row.addView(emptyTextView());
                            row.addView(emptyTextView());
                        }

                        row.addView(makeTextView((String) service.get("serviceName"), Color.LTGRAY));

                        double totalPrice = service.get("servicePrice") != null ? (Double) service.get("servicePrice") : 0.0;

                        List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                        if (subServices != null) {
                            for (Map<String, Object> sub : subServices) {
                                totalPrice += sub.get("servicePrice") != null ? (double) sub.get("servicePrice") : 0.0;
                            }
                        }

                        row.addView(makeTextView(numberFormat.format(totalPrice), Color.LTGRAY));

                        double commissionRate = getCommissionRateByDate(employeeObj, formattedDate);
                        double commissionAmount = totalPrice * (commissionRate / 100.0);
                        totalComms += commissionAmount;
                        row.addView(makeTextViewBold(numberFormat.format(commissionAmount), ResourcesCompat.getColor(getResources(), R.color.orange, null)));

                        generatedRows.add(row);
                    }
                }
            }

            double finalTotalComms = totalComms;
            requireActivity().runOnUiThread(() -> {
                tblHandler.removeViews(1, tblHandler.getChildCount() - 1);
                tblWeekly.removeViews(1, tblWeekly.getChildCount() - 1);
                tblSalary.removeViews(1, tblSalary.getChildCount() - 1);

                // Create header with actual dates
                Calendar headerCal = Calendar.getInstance();
                headerCal.set(Calendar.YEAR, year);
                headerCal.set(Calendar.MONTH, month);
                headerCal.set(Calendar.WEEK_OF_MONTH, weekNumber);
                headerCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                headerCal.set(Calendar.HOUR_OF_DAY, 0);
                headerCal.set(Calendar.MINUTE, 0);
                headerCal.set(Calendar.SECOND, 0);
                headerCal.set(Calendar.MILLISECOND, 0);

                String empNameSt = employeeObj.getName();
                String empEmailSt = employeeObj.getEmail();
                tvEmpName.setText(empNameSt);
                tvRole.setText(empEmailSt);

                // === First Row: Dates ===
                TableRow dateRow = new TableRow(getContext());
                dateRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                Calendar displayCalendar = (Calendar) headerCal.clone();
                List<String> weekdayDates = new ArrayList<>();

                for (int i = 0; i < 7; i++) {
                    String dateStr = sdf.format(displayCalendar.getTime());
                    weekdayDates.add(dateStr);

                    TextView dateTextView = new TextView(getContext());
                    dateTextView.setText(dateStr);
                    dateTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
                    dateTextView.setTextColor(Color.WHITE);
                    dateTextView.setPadding(10, 10, 10, 10);
                    dateRow.addView(dateTextView);

                    displayCalendar.add(Calendar.DAY_OF_MONTH, 1);
                }
                tblWeekly.addView(dateRow);

                // === Second Row: Commission Totals ===
                TableRow commissionRow = new TableRow(getContext());
                commissionRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                // Initialize commission totals
                Map<String, Double> commissionPerDate = new HashMap<>();
                for (String date : weekdayDates) {
                    commissionPerDate.put(date, 0.0);
                }

                // Loop again to fill commission totals by formatted date
                for (Appointment appointment : appointmentsList) {
                    Date appointmentDate = appointment.getClientDateTimeAsDate();
                    if (appointmentDate == null) continue;

                    if (!appointmentDate.before(startOfWeek) && !appointmentDate.after(endOfWeek)) {
                        String formattedDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(appointmentDate);

                        for (Map<String, Object> service : appointment.getServices()) {
                            String assignedEmployee = (String) service.get("assignedEmployee");
                            if (assignedEmployee == null || assignedEmployee.equals("None")) continue;
                            if (!assignedEmployee.equals(empName)) continue;

                            double totalPrice = service.get("servicePrice") != null ? (Double) service.get("servicePrice") : 0.0;

                            List<Map<String, Object>> subServices = (List<Map<String, Object>>) service.get("subServices");
                            if (subServices != null) {
                                for (Map<String, Object> sub : subServices) {
                                    totalPrice += sub.get("servicePrice") != null ? (double) sub.get("servicePrice") : 0.0;
                                }
                            }

                            double commissionRate = getCommissionRateByDate(employeeObj, formattedDate);
                            double commissionAmount = totalPrice * (commissionRate / 100.0);

                            // Add to existing daily total
                            commissionPerDate.put(formattedDate, commissionPerDate.getOrDefault(formattedDate, 0.0) + commissionAmount);
                        }
                    }
                }

                // Add commission values to row
                for (String date : weekdayDates) {
                    TextView commissionTextView = makeTextViewBold(numberFormat.format(commissionPerDate.get(date)), ResourcesCompat.getColor(getResources(), R.color.orange, null));
                    commissionTextView.setPadding(10, 10, 10, 10);
                    commissionRow.addView(commissionTextView);
                }
                tblWeekly.addView(commissionRow);

                // Add regular service rows
                for (TableRow row : generatedRows) {
                    tblHandler.addView(row);
                }
                TableRow empTableRow = new TableRow(getContext());
                for(int empt = 0; empt <4; empt++){
                    TextView emptyTextView = emptyTextView();
                    empTableRow.addView(emptyTextView);
                }
                TextView TotalLavel = makeTextViewBold("Total Commission",ResourcesCompat.getColor(getResources(), R.color.orange, null));
                TextView TotalComission = makeTextViewBold(numberFormat.format(finalTotalComms),ResourcesCompat.getColor(getResources(), R.color.orange, null));
                empTableRow.addView(TotalLavel);
                empTableRow.addView(TotalComission);
                tblHandler.addView(empTableRow);

                double overAllTotalSalary = 0.0;
                String yearStr = String.valueOf(year);
                String weekStr = String.valueOf(weekNumber);

                List<EmployeeSalaryDetails> EmployeeSalList = getSalaryForDate(month, yearStr, weekStr);
                for (EmployeeSalaryDetails emp : EmployeeSalList) {
                    Employee employee = findEmployeeByName(empName);
                    // Check if the employee and required fields exist before proceeding
                    if (employee != null && emp.getEmployeeId() != null && !"None".equals(emp.getEmployeeId())
                    && emp.getEmployeeId().equals(empName)) {
                        if ("Regular".equals(employee.getTherapist()) || employee.getSalary() != 0) {
                            TableRow tableRow = new TableRow(getContext());

                            // Calculate the basic weekly salary and deductions
                            String daysPresent = emp.getDaysPresent() != "" ? emp.getDaysPresent() : "0";
                            double weekSal = getSalaryByDate(employee, emp.getDaysPresent(), sdf);
                            double lateDeduction = emp.getLateDeduction().isEmpty() ? 0.0 : Double.parseDouble(emp.getLateDeduction());
                            double deductedWeekSal = weekSal - lateDeduction;
                            double totalCommissionPerEmp = finalTotalComms;

                            // Calculate total salary including commission and deductions
                            double totalSalary = deductedWeekSal + totalCommissionPerEmp;
                            double caDeduction = emp.getCaDeduction().isEmpty() ? 0.0 : Double.parseDouble(emp.getCaDeduction());
                            double totalSalaryDeducted = totalSalary - caDeduction;

                            overAllTotalSalary += totalSalaryDeducted;

                            TextView perDay = makeTextView(numberFormat.format(employee.getSalary()), Color.WHITE);
                            TextView daysPresentTextView = makeTextView(daysPresent, Color.WHITE);
                            TextView weekSalary = makeTextView(numberFormat.format(weekSal), Color.WHITE);
                            TextView lateDeductionTextView = makeTextView(numberFormat.format(lateDeduction), Color.WHITE);
                            TextView deductedSalary = makeTextView(numberFormat.format(deductedWeekSal), Color.WHITE);
                            TextView commissionTextView = makeTextView(numberFormat.format(totalCommissionPerEmp), Color.WHITE);
                            TextView totalSalaryTextView = makeTextView(numberFormat.format(totalSalary), Color.WHITE);
                            TextView caDeductionTextView = makeTextView(numberFormat.format(caDeduction), Color.WHITE);
                            TextView overallSalaryTextView = makeTextView(numberFormat.format(totalSalaryDeducted), Color.WHITE);

                            // Styling for the overall salary
                            overallSalaryTextView.setTextColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
                            overallSalaryTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));

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
                            tblSalary.addView(tableRow);
                        }
                    }
                }

                progressContainer1.setVisibility(View.GONE);
                btnSearch.setEnabled(true);
            });

        }).start();
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
            if (salaryHistory != null) { // Add null check here
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
            } else {
                // Handle the case where salaryHistory is null
                System.out.println("salaryHistory is null");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }


        // Calculate total salary for the given number of days present
        return salary * daysPresent;
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
    private TextView makeTextView(String text, int color) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope));
        tv.setPadding(10, 5, 5, 5);
        return tv;
    }
    private TextView makeTextViewBold(String text, int color) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTypeface(ResourcesCompat.getFont(getContext(), R.font.manrope_bold));
        tv.setPadding(10, 5, 5, 5);
        return tv;
    }

    private TextView emptyTextView() {
        return makeTextView("", Color.TRANSPARENT);
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
                        List<String> employeeNames = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            employee.setId(document.getId());
                            employeeList.add(employee);
                            employeeNames.add(employee.getName()); // Assuming getName() exists
                        }

                        // Setup adapter after loading names
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                employeeNames
                        );

                        AutoCompleteTextView autoCompleteTextView = getView().findViewById(R.id.autoCompleteTextView);
                        autoCompleteTextView.setAdapter(adapter);
                        autoCompleteTextView.setThreshold(1);

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

        tasks.add(db.collection("salary_details").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
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
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
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
        Collections.sort(years, (y1, y2) -> Integer.compare(Integer.parseInt(y2), Integer.parseInt(y1)));
        // Set up the spinners with adapters
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        month_spinner.setAdapter(monthAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, years);
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
                //filterDataByMonthYearWeek(months[selectedMonth], year_spinner.getSelectedItem().toString(), week_num.getSelectedItem().toString());
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
                //filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), String.valueOf(selectedYear), week_num.getSelectedItem().toString());
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
                //filterDataByMonthYearWeek(month_spinner.getSelectedItem().toString(), year_spinner.getSelectedItem().toString(), selectedWeek);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }
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
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, weeks);
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

    }
    private int getCurrentWeekOfMonth(Calendar calendar) {
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        return calendar.get(Calendar.WEEK_OF_MONTH);
    }
    private Employee findEmployeeByName(String employeeName) {
        for (Employee employee : employeeList) {
            if (employee.getName().equalsIgnoreCase(employeeName)) {
                return employee; // Return the matching employee object
            }
        }
        return null;
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
}