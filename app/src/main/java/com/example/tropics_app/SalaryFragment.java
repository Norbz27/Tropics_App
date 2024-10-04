package com.example.tropics_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Updated import

import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.content.Intent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;



public class SalaryFragment extends Fragment implements EmployeeAdapter.OnEmployeeClickListener {
    private FloatingActionButton fabAdd;
    private RecyclerView rvSalary;
    private FirebaseFirestore db;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList;
    private SearchView searchView;
    private static final int IMAGE_PICK_REQUEST = 100;
    private ImageView imgEmp;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    public SalaryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        employeeList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_salary, container, false);
        searchView = view.findViewById(R.id.searchView);
        setupSearchView();
        fabAdd = view.findViewById(R.id.fabAdd);
        rvSalary = view.findViewById(R.id.rvSalary);
        rvSalary.setHasFixedSize(true);
        rvSalary.setLayoutManager(new LinearLayoutManager(getActivity()));

        db = FirebaseFirestore.getInstance();
        loadEmployeeData();

        fabAdd.setOnClickListener(v -> showAddEmployeeDialog());

        return view;
    }

    private void setupSearchView() {
        searchView.setOnClickListener(v -> searchView.setIconified(false));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterEmployeeList(newText);
                return true;
            }
        });
    }

    private void filterEmployeeList(String searchText) {
        if (searchText.isEmpty()) {
            loadEmployeeData();
            return;
        }

        String lowercaseSearchText = searchText.toLowerCase();
        List<Employee> filteredList = new ArrayList<>();

        for (Employee employee : employeeList) {
            if (employee.getName().toLowerCase().contains(lowercaseSearchText)) {
                filteredList.add(employee);
            }
        }

        adapter.updateEmployeeList(filteredList);
    }

    private void showAddEmployeeDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogbox_employee, null);

        EditText empName = dialogView.findViewById(R.id.empname);
        EditText empAddress = dialogView.findViewById(R.id.empadd);
        EditText empPhone = dialogView.findViewById(R.id.empphone);
        EditText empEmail = dialogView.findViewById(R.id.empemail);
        EditText empSal = dialogView.findViewById(R.id.empcomm);
        EditText comsEditText = dialogView.findViewById(R.id.comission);
        Button btnSubmit = dialogView.findViewById(R.id.empsub);
        imgEmp = dialogView.findViewById(R.id.imgemp);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        imgEmp.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> {
            String name = empName.getText().toString().trim();
            String address = empAddress.getText().toString().trim();
            String phone = empPhone.getText().toString().trim();
            String email = empEmail.getText().toString().trim();
            String salaryString = empSal.getText().toString().trim();
            String commissionString = comsEditText.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty() && selectedImageUri != null) {
                try {
                    double salary = Double.parseDouble(salaryString);
                    double commission = Double.parseDouble(commissionString);

                    Map<String, Object> employeeData = new HashMap<>();
                    employeeData.put("name", name);
                    employeeData.put("address", address);
                    employeeData.put("phone", phone);
                    employeeData.put("email", email);
                    employeeData.put("salary", salary);
                    employeeData.put("coms", commission);

                    // Show ProgressDialog
                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Uploading data...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    StorageReference storageRef = FirebaseStorage.getInstance()
                            .getReference().child("employee_images/" + UUID.randomUUID().toString());
                    storageRef.putFile(selectedImageUri)
                            .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                employeeData.put("image", uri.toString());
                                db.collection("Employees").add(employeeData)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getActivity(), "Employee added", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                                loadEmployeeData();
                                            } else {
                                                Log.e("Firestore Error", "Failed to add employee: " + task.getException().getMessage());
                                                Toast.makeText(getActivity(), "Failed to add employee", Toast.LENGTH_SHORT).show();
                                            }
                                            // Hide ProgressDialog
                                            progressDialog.dismiss();
                                        });
                            }))
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                                Log.e("Storage Error", "Failed to upload image: " + e.getMessage());
                                // Hide ProgressDialog on failure
                                progressDialog.dismiss();
                            });
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Please enter valid salary and commission", Toast.LENGTH_SHORT).show();
                    Log.e("Input Error", "Invalid salary or commission: " + e.getMessage());
                }
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields and select an image", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgEmp.setImageURI(selectedImageUri);
        }
        if (requestCode == PICK_IMAGE_REQUEST) {
            getActivity();
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                imgEmp.setImageURI(selectedImageUri); // Display the selected image
            }
        }
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
                        if (adapter == null) {
                            adapter = new EmployeeAdapter(this, employeeList);
                            rvSalary.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showViewEmployeeDialog(Employee employee) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_viewemployee, null);

        ImageView imgEmp = dialogView.findViewById(R.id.imgemp);
        TextView empName = dialogView.findViewById(R.id.empname);
        TextView empAddress = dialogView.findViewById(R.id.empadd);
        TextView empPhone = dialogView.findViewById(R.id.empphone);
        TextView empEmail = dialogView.findViewById(R.id.empemail);
        TextView empSal = dialogView.findViewById(R.id.empsal1);
        TextView empComm = dialogView.findViewById(R.id.empcomm);

        empName.setText(employee.getName());
        empAddress.setText("Address: " + employee.getAddress());
        empPhone.setText("Phone: " + employee.getPhone());
        empEmail.setText("Email: " + employee.getEmail());
        empSal.setText("Weekly Salary: ₱" + employee.getSalary());
        empComm.setText("Commission: ₱" + employee.getComs());

        Glide.with(this)
                .load(employee.getImage())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imgEmp);

        Spinner monthSpinner = dialogView.findViewById(R.id.month_spinner);
        RecyclerView assignedServicesRecyclerView = dialogView.findViewById(R.id.assigned_services_recycler_view);
        assignedServicesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(adapter);

        // Set the default selection to the current month
        Calendar calendar = Calendar.getInstance();
        monthSpinner.setSelection(calendar.get(Calendar.MONTH));

        // Handle month selection
        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Fetch assigned services for the selected month
                fetchAssignedServicesByMonth(employee, position, assignedServicesRecyclerView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void fetchAssignedServicesByMonth(Employee employee, int monthNumber, RecyclerView assignedServicesRecyclerView) {
        CollectionReference appointmentsRef = db.collection("appointments");
        Map<Integer, List<AssignedService>> weeklyServicesMap = new HashMap<>();
        Map<Integer, Double> weeklyCommissionMap = new HashMap<>(); // To store total commission per week

        appointmentsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    List<Map<String, Object>> services = (List<Map<String, Object>>) document.get("services");

                    for (Map<String, Object> service : services) {
                        String assignedEmployee = (String) service.get("assignedEmployee");
                        String serviceName = (String) service.get("serviceName");

                        if (assignedEmployee != null && assignedEmployee.equals(employee.getName())) {
                            String appointmentDate = document.getString("date");

                            // Parse the appointment date and check if it matches the selected month
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date date = sdf.parse(appointmentDate);
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(date);

                                // Check if the appointment is in the selected month
                                if (calendar.get(Calendar.MONTH) == monthNumber) {
                                    int weekNumber = calendar.get(Calendar.WEEK_OF_MONTH); // Get the week of the month

                                    // Create AssignedService object
                                    AssignedService assignedService = new AssignedService(serviceName, document.getString("fullName"), appointmentDate, weekNumber);

                                    // Group by week
                                    weeklyServicesMap.putIfAbsent(weekNumber, new ArrayList<>());
                                    weeklyServicesMap.get(weekNumber).add(assignedService);

                                    // Update commission for this week (assuming a fixed commission per service)
                                    double commissionPerService = employee.getComs(); // Set your commission rate here
                                    weeklyCommissionMap.put(weekNumber, weeklyCommissionMap.getOrDefault(weekNumber, 0.0) + commissionPerService);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                // Prepare the data for RecyclerView
                List<WeekServices> weekServicesList = new ArrayList<>();
                for (Map.Entry<Integer, List<AssignedService>> entry : weeklyServicesMap.entrySet()) {
                    int week = entry.getKey();
                    List<AssignedService> services = entry.getValue();

                    // Calculate total commission for this week
                    double totalCommission = weeklyCommissionMap.getOrDefault(week, 0.0);

                    // Only add the week if there are services for that week
                    if (!services.isEmpty()) {
                        weekServicesList.add(new WeekServices("Week " + week, services, totalCommission)); // Add week, services, and commission
                    }
                }

                // Check if there are no services for the selected month
                if (weekServicesList.isEmpty()) {
                    weekServicesList.add(new WeekServices("No services available for this month", Collections.emptyList(), 0.0)); // Display a message if no services
                }

                // Set the adapter
                AssignedServicesAdapter adapter = new AssignedServicesAdapter(getContext(), weekServicesList);
                assignedServicesRecyclerView.setAdapter(adapter);
            } else {
                Log.e("Firestore", "Error getting documents: ", task.getException());
            }
        });
    }


    @Override
    public void onEmployeeClick(Employee employee) {
        showViewEmployeeDialog(employee);
    }

    @Override
    public void onEmployeeLongClick(View view, Employee employee) {
        showEmployeeOptionsDialog(view, employee);
    }


    private void showEmployeeOptionsDialog(View view, Employee employee) {
        // Create a PopupMenu
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);

        // Set the gravity to the right (you can also use Gravity.END)
        popupMenu.setGravity(Gravity.END);

        // Inflate the menu layout (assuming you have a menu resource)
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.employee_item_menu, popupMenu.getMenu());

        Menu menu = popupMenu.getMenu();

        // Set a listener for menu item clicks
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.action_edit) {
                // Call the edit action
                showEditEmployeeDialog(employee);
                return true;
            } else if (id == R.id.action_delete) {
                // Create a confirmation dialog
                new AlertDialog.Builder(getActivity())
                        .setTitle("Delete Employee")
                        .setMessage("Are you sure you want to delete this employee?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Call the delete action if user confirms
                            deleteEmployee(employee);
                            Toast.makeText(getActivity(), "Employee deleted successfully", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Dismiss the dialog if user cancels
                            dialog.dismiss();
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();

                return true;
            }
            else {
                return false; // Unhandled item
            }
        });

        // Show the popup menu
        popupMenu.show();
    }

    private void showEditEmployeeDialog(Employee employee) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogbox_employee, null);

        EditText empName = dialogView.findViewById(R.id.empname);
        EditText empAddress = dialogView.findViewById(R.id.empadd);
        EditText empPhone = dialogView.findViewById(R.id.empphone);
        EditText empEmail = dialogView.findViewById(R.id.empemail);
        EditText empSal = dialogView.findViewById(R.id.empcomm);
        EditText comsEditText = dialogView.findViewById(R.id.comission);
        Button btnSubmit = dialogView.findViewById(R.id.empsub);
        imgEmp = dialogView.findViewById(R.id.imgemp);
        Log.d("Employee", "Showing edit dialog for employee: " + employee.getImage());
        // Pre-fill the fields with the current employee data
        empName.setText(employee.getName());
        empAddress.setText(employee.getAddress());
        empPhone.setText(employee.getPhone());
        empEmail.setText(employee.getEmail());
        empSal.setText(String.valueOf(employee.getSalary()));
        comsEditText.setText(String.valueOf(employee.getComs()));
        Glide.with(getContext())
                .load(employee.getImage())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imgEmp);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        imgEmp.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> {
            String name = empName.getText().toString().trim();
            String address = empAddress.getText().toString().trim();
            String phone = empPhone.getText().toString().trim();
            String email = empEmail.getText().toString().trim();
            String salaryString = empSal.getText().toString().trim();
            String commissionString = comsEditText.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty()) {
                try {
                    double salary = Double.parseDouble(salaryString);
                    double commission = Double.parseDouble(commissionString);

                    Map<String, Object> updatedEmployeeData = new HashMap<>();
                    updatedEmployeeData.put("name", name);
                    updatedEmployeeData.put("address", address);
                    updatedEmployeeData.put("phone", phone);
                    updatedEmployeeData.put("email", email);
                    updatedEmployeeData.put("salary", salary);
                    updatedEmployeeData.put("coms", commission);

                    ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Updating data...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    if (selectedImageUri != null) {
                        // Upload image to Firebase Storage
                        StorageReference storageRef = FirebaseStorage.getInstance()
                                .getReference().child("employee_images/" + UUID.randomUUID().toString());
                        storageRef.putFile(selectedImageUri)
                                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    updatedEmployeeData.put("image", uri.toString());
                                    updateEmployee(employee.getId(), updatedEmployeeData);
                                    // Dismiss the dialog once done
                                    progressDialog.dismiss();
                                    Toast.makeText(getActivity(), "Employee updated successfully", Toast.LENGTH_SHORT).show();
                                }))
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss(); // Dismiss in case of failure
                                    Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                                    Log.e("Storage Error", "Failed to upload image: " + e.getMessage());
                                });
                    } else {
                        // Update employee without an image
                        updateEmployee(employee.getId(), updatedEmployeeData);
                        // Dismiss the dialog once done
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(), "Employee updated successfully", Toast.LENGTH_SHORT).show();
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "Please enter valid salary and commission", Toast.LENGTH_SHORT).show();
                    Log.e("Input Error", "Invalid salary or commission: " + e.getMessage());
                }
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateEmployee(String employeeId, Map<String, Object> updatedEmployeeData) {
        db.collection("Employees").document(employeeId).update(updatedEmployeeData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Employee updated", Toast.LENGTH_SHORT).show();
                        loadEmployeeData();
                    } else {
                        Log.e("Firestore Error", "Failed to update employee: " + task.getException().getMessage());
                        Toast.makeText(getActivity(), "Failed to update employee", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteEmployee(Employee employee) {
        db.collection("Employees").document(employee.getId()).delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Employee deleted", Toast.LENGTH_SHORT).show();
                        loadEmployeeData();
                    } else {
                        Log.e("Firestore Error", "Failed to delete employee: " + task.getException().getMessage());
                        Toast.makeText(getActivity(), "Failed to delete employee", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
