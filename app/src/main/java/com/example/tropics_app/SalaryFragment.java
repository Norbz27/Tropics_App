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

import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.content.Intent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        fabAdd = view.findViewById(R.id.fabAdd);
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
        EditText empSal = dialogView.findViewById(R.id.empcomm);
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
            String salary = empSal.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty() && !salary.isEmpty() && selectedImageUri != null) {
                // Create a map to store employee data
                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("name", name);
                employeeData.put("address", address);
                employeeData.put("phone", phone);
                employeeData.put("email", email);
                employeeData.put("salary", salary);
                employeeData.put("coms", 0.0); // Set commission to 0 as a Double

                // Upload image to Firebase Storage
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("employee_images/" + UUID.randomUUID().toString());
                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Save the download URL in Firestore
                            employeeData.put("image", uri.toString());

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
                        }))
                        .addOnFailureListener(e -> {
                            // Handle any errors in image upload
                            Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                            Log.e("Storage Error", "Failed to upload image: " + e.getMessage());
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
    private void showViewEmployeeDialog(Employee employee) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_viewemployee, null); // Your dialog layout for viewing employee

        ImageView imgEmp = dialogView.findViewById(R.id.imgemp);
        EditText empName = dialogView.findViewById(R.id.empname);
        EditText empAddress = dialogView.findViewById(R.id.empadd);
        EditText empPhone = dialogView.findViewById(R.id.empphone);
        EditText empEmail = dialogView.findViewById(R.id.empemail);
        EditText empSal = dialogView.findViewById(R.id.empsal1);
        EditText empComm = dialogView.findViewById(R.id.empcomm);

        // Set employee details
        empName.setText(employee.getName());
        empAddress.setText(employee.getAddress());
        empPhone.setText(employee.getPhone());
        empEmail.setText(employee.getEmail());
        empSal.setText(employee.getSalary());
        empComm.setText(String.valueOf(employee.getComs()));


        empName.setEnabled(false);
        empAddress.setEnabled(false);
        empPhone.setEnabled(false);
        empEmail.setEnabled(false);
        empSal.setEnabled(false);
        empComm.setEnabled(false);
        // Load the employee's image using Glide
        Glide.with(this)
                .load(employee.getImage()) // Make sure to call the correct method to get the image URL
                .placeholder(R.drawable.ic_image_placeholder) // Placeholder image
                .into(imgEmp);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @Override
    public void onEmployeeClick(Employee employee) {
        showViewEmployeeDialog(employee);

    }

}
