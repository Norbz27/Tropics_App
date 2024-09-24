package com.example.tropics_app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Updated import
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SalaryFragment extends Fragment implements EmployeeAdapter.OnEmployeeClickListener {
    private FloatingActionButton fabAdd;
    private RecyclerView rvSalary;
    private FirebaseFirestore db; // Firestore instance
    private EmployeeAdapter adapter; // Declare adapter
    private List<Employee> employeeList; // List of employees

    public SalaryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        employeeList = new ArrayList<>(); // Initialize employee list
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_salary, container, false);

        fabAdd = view.findViewById(R.id.fabAdd1);
        rvSalary = view.findViewById(R.id.rvSalary);
        rvSalary.setHasFixedSize(true);
        rvSalary.setLayoutManager(new LinearLayoutManager(getActivity()));

        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        loadEmployeeData(); // Load employee data from Firestore

        fabAdd.setOnClickListener(v -> showAddEmployeeDialog());

        return view;
    }

    private void showAddEmployeeDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogbox_employee, null);

        EditText empName = dialogView.findViewById(R.id.empname);
        EditText empAddress = dialogView.findViewById(R.id.empadd);
        EditText empPhone = dialogView.findViewById(R.id.empphone);
        EditText empEmail = dialogView.findViewById(R.id.empemail);
        EditText empComm = dialogView.findViewById(R.id.empcomm);
        Button btnSubmit = dialogView.findViewById(R.id.empsub);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnSubmit.setOnClickListener(v -> {
            String name = empName.getText().toString().trim();
            String address = empAddress.getText().toString().trim();
            String phone = empPhone.getText().toString().trim();
            String email = empEmail.getText().toString().trim();
            String salary = empComm.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty() && !salary.isEmpty()) {
                // Create a map to store employee data
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("name", name);
                employeeData.put("address", address);
                employeeData.put("phone", phone);
                employeeData.put("email", email);
                employeeData.put("salary", salary);
                employeeData.put("commission", 0); // Set commission to 0

                // Add employee data to Firestore
                db.collection("Employees") // Your Firestore collection name
                        .add(employeeData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getActivity(), "Employee added", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadEmployeeData(); // Refresh the employee list after adding a new employee
                            } else {
                                Log.e("Firestore Error", "Failed to add employee: " + task.getException().getMessage());
                                Toast.makeText(getActivity(), "Failed to add employee", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEmployeeData() {
        db.collection("Employees").get() // Fetch data from Firestore
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        employeeList.clear(); // Clear the existing list
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            employeeList.add(employee);
                        }

                        // Initialize and set the adapter only once
                        if (adapter == null) {
                            adapter = new EmployeeAdapter(this, employeeList); // Use 'this' to pass the listener
                            rvSalary.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged(); // Notify adapter of data change
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onEmployeeClick(Employee employee) {
        // Handle employee click events here
        Toast.makeText(getActivity(), "Clicked: " + employee.getName(), Toast.LENGTH_SHORT).show();
        // You can add further actions here, like opening a detail view for the employee
    }
}
