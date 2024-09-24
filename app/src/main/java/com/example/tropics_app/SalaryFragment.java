package com.example.tropics_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ImageView;
import android.provider.MediaStore;


public class SalaryFragment extends Fragment {
    private FloatingActionButton fabAdd;
    private RecyclerView rvSalary;
    private FirebaseFirestore db; // Firestore instance
    private EmployeeAdapter adapter; // Declare adapter
    private List<Employee> employeeList;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri; // Store the selected image URI
    private ImageView imgEmp; // Declare ImageView for employee image

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
        imgEmp = dialogView.findViewById(R.id.imgemp); // Initialize imgEmp
        Button btnSubmit = dialogView.findViewById(R.id.empsub);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        imgEmp.setOnClickListener(v -> {
            // Open the image picker
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

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

                // If you have an imageUri, you can upload it to Firebase and get the download URL
                if (imageUri != null) {
                    uploadImageToFirebase(imageUri, employeeData, dialog); // Use your existing upload method
                } else {
                    // If no image, add employee without it
                    employeeData.put("image", ""); // or set a default image URL
                    addEmployeeToFirestore(employeeData, dialog);
                }
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageToFirebase(Uri imageUri, Map<String, Object> employeeData, AlertDialog dialog) {
        // Get a reference to Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // Create a unique filename for the image
        String fileName = "employee_images/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        // Upload the image
        imageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                    // Add the image URL to the employee data
                    employeeData.put("image", downloadUrl.toString());

                    // Add employee data to Firestore
                    addEmployeeToFirestore(employeeData, dialog);
                })).addOnFailureListener(e -> {
            Toast.makeText(getActivity(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void addEmployeeToFirestore(Map<String, Object> employeeData, AlertDialog dialog) {
        db.collection("Employees")
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgEmp.setImageURI(imageUri); // Set the selected image to ImageView
        }
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
                            adapter = new EmployeeAdapter(getActivity(), employeeList);
                            rvSalary.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged(); // Notify adapter of data change
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
