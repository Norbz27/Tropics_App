package com.example.tropics_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Updated import
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.content.Intent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageView;



public class SalaryFragment extends Fragment implements EmployeeAdapter.OnEmployeeClickListener {
    private FloatingActionButton fabAdd;
    private RecyclerView rvSalary;
    private FirebaseFirestore db; // Firestore instance
    private EmployeeAdapter adapter; // Declare adapter
    private List<Employee> employeeList; // List of employees

    private static final int IMAGE_PICK_REQUEST = 100; // Request code for image picker
    private ImageView imgEmp; // Declare ImageView for displaying selected image
    private Uri selectedImageUri; // To hold the URI of the selected image

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
        imgEmp = dialogView.findViewById(R.id.imgemp); // Initialize the ImageView

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        imgEmp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_REQUEST); // Open image picker
        });

        btnSubmit.setOnClickListener(v -> {
            String name = empName.getText().toString().trim();
            String address = empAddress.getText().toString().trim();
            String phone = empPhone.getText().toString().trim();
            String email = empEmail.getText().toString().trim();
            String salary = empComm.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty() && !salary.isEmpty() && selectedImageUri != null) {
                // Create a map to store employee data
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("name", name);
                employeeData.put("address", address);
                employeeData.put("phone", phone);
                employeeData.put("email", email);
                employeeData.put("salary", salary);
                employeeData.put("commission", 0); // Set commission to 0

                // Convert image URI to a string (you may need to upload it to a storage service first)
                employeeData.put("image", selectedImageUri.toString());

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

    // Handle the result of the image selection
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData(); // Get the selected image URI
            imgEmp.setImageURI(selectedImageUri); // Display the selected image in the ImageView
        }
    }

    private void loadEmployeeData() {
        db.collection("Employees").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        employeeList.clear(); // Clear the existing list
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
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

    @Override
    public void onEmployeeClick(Employee employee) {
        // Handle employee click events here
       // Toast.makeText(getActivity(), "Clicked: " + employee.getName(), Toast.LENGTH_SHORT).show();

        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.dialogbox_employee, null);

        // Initialize views from the dialog layout
        ImageView imgEmp = dialogView.findViewById(R.id.imgemp); // Employee image
        TextInputEditText empName = dialogView.findViewById(R.id.empname);
        TextInputEditText empAddress = dialogView.findViewById(R.id.empadd);
        TextInputEditText empPhone = dialogView.findViewById(R.id.empphone);
        TextInputEditText empEmail = dialogView.findViewById(R.id.empemail);
        TextInputEditText empSalary = dialogView.findViewById(R.id.empcomm);
        Button empSubmit = dialogView.findViewById(R.id.empsub); // Submit button

        // Load the employee's image using Glide
        Glide.with(getActivity())
                .load(employee.getImageUrl()) // Assuming there's a method to get the image URL
                .placeholder(R.drawable.ic_image_placeholder) // Placeholder image
                .into(imgEmp);

        // Populate the views with employee data
        empName.setText(employee.getName());
        empAddress.setText(employee.getAddress());
        empPhone.setText(employee.getPhone());
        empEmail.setText(employee.getEmail());
      //  empSalary.setText(String.valueOf(employee.getWeeklySalary()));

        // Disable input for viewing purposes
        empName.setEnabled(false);
        empAddress.setEnabled(false);
        empPhone.setEnabled(false);
        empEmail.setEnabled(false);
        empSalary.setEnabled(false);

        // Disable the submit button
        empSubmit.setEnabled(false); // Disable the button
        empSubmit.setVisibility(View.GONE); // Optionally hide the button

        // Create and show the dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Employee Details"); // Set dialog title
        dialogBuilder.setNegativeButton("Close", null); // Close button

        // Show the dialog
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }


}
