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
import androidx.appcompat.widget.SearchView;



public class SalaryFragment extends Fragment implements EmployeeAdapter.OnEmployeeClickListener {
    private FloatingActionButton fabAdd;
    private RecyclerView rvSalary;
    private FirebaseFirestore db; // Firestore instance
    private EmployeeAdapter adapter; // Declare adapter
    private List<Employee> employeeList;
    private SearchView searchView;// List of employees

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

        // Initialize SearchView
        searchView = view.findViewById(R.id.searchView);
        setupSearchView(); // Call setupSearchView method

        // Initialize other views
        fabAdd = view.findViewById(R.id.fabAdd);
        rvSalary = view.findViewById(R.id.rvSalary);
        rvSalary.setHasFixedSize(true);
        rvSalary.setLayoutManager(new LinearLayoutManager(getActivity()));

        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        loadEmployeeData(); // Load employee data from Firestore

        fabAdd.setOnClickListener(v -> showAddEmployeeDialog());

        return view;
    }

    private void setupSearchView() {
        searchView.setOnClickListener(v -> searchView.setIconified(false));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // No action needed on submit
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterEmployeeList(newText); // Call the filtering method
                return true; // Indicate that the event is handled
            }
        });
    }

    private void filterEmployeeList(String searchText) {
        if (searchText.isEmpty()) {
            loadEmployeeData(); // Reload all employees if search text is empty
            return;
        }

        String lowercaseSearchText = searchText.toLowerCase();
        List<Employee> filteredList = new ArrayList<>();

        // Filter the employeeList based on the name
        for (Employee employee : employeeList) {
            if (employee.getName().toLowerCase().contains(lowercaseSearchText)) {
                filteredList.add(employee); // Add matched employees to the filtered list
            }
        }

        // Update the adapter with the filtered list
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
                            employee.setId(document.getId()); // Set the Firestore document ID
                            employeeList.add(employee); // Add the employee to the list
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

        // Set dynamic hints
        empName.setHint("Name: " + employee.getName());
        empAddress.setHint("Address: " + employee.getAddress());
        empPhone.setHint("Phone: " + employee.getPhone());
        empEmail.setHint("Email: " + employee.getEmail());
        empSal.setHint("Salary: " + employee.getSalary());
        empComm.setHint("Commission: " + employee.getComs());

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

    public void searchEmployees(String searchText) {
        if (searchText.isEmpty()) {
            loadEmployeeData(); // Reload all employees if search text is empty
            return;
        }

        String lowercaseSearchText = searchText.toLowerCase();

        // Firestore query to search employees
        db.collection("Employees")
                .whereGreaterThanOrEqualTo("name", lowercaseSearchText)
                .whereLessThanOrEqualTo("name", lowercaseSearchText + '\uf8ff')
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        employeeList.clear(); // Clear current list
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Employee employee = document.toObject(Employee.class);
                            employeeList.add(employee); // Add matched employees to list
                        }

                        // Notify adapter of data changes
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(getActivity(), "No employees found", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateEmployee(String employeeId, String name, String address, String phone, String email, String salary, double coms) {
        // Create a map to hold the updated data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", name);
        updatedData.put("address", address);
        updatedData.put("phone", phone);
        updatedData.put("email", email);
        updatedData.put("salary", salary);
        updatedData.put("coms", coms);

        // Update the employee document in Firestore
        db.collection("Employees").document(employeeId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Employee updated successfully", Toast.LENGTH_SHORT).show();
                    loadEmployeeData(); // Refresh the employee list after updating
                })
                .addOnFailureListener(e -> {
                    Log.e("UpdateEmployee", "Error updating document: ", e);
                    Toast.makeText(getActivity(), "Failed to update employee: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override

    public void onEmployeeClick(Employee employee) {
        // Handle click event
        showViewEmployeeDialog(employee);
    }

    @Override
    public void onEmployeeLongClick(Employee employee) {
        // Handle long click event
        showEmployeeOptionsDialog(employee); // Call a method to show options for edit/delete
    }

    private void showEmployeeOptionsDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Options")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Handle edit action
                            showEditEmployeeDialog(employee); // Uncommented to show the edit dialog
                            break;
                        case 1:
                            // Handle delete action
                            deleteEmployee(employee);
                            break;
                    }
                })
                .create()
                .show();
    }

    private void deleteEmployee(Employee employee) {
        Log.d("DeleteEmployee", "Attempting to delete Employee: " + employee.getName() + " with ID: " + employee.getId()); // Log name and ID
        if (employee.getId() != null && !employee.getId().isEmpty()) {
            db.collection("Employees").document(employee.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getActivity(), "Employee deleted", Toast.LENGTH_SHORT).show();
                        loadEmployeeData(); // Refresh the list after deletion
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DeleteEmployee", "Error deleting document: ", e);
                        Toast.makeText(getActivity(), "Failed to delete employee: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getActivity(), "Invalid employee ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditEmployeeDialog(Employee employee) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogbox_editemployee, null);

        EditText empName = dialogView.findViewById(R.id.empname);
        EditText empAddress = dialogView.findViewById(R.id.empadd);
        EditText empPhone = dialogView.findViewById(R.id.empphone);
        EditText empEmail = dialogView.findViewById(R.id.empemail);
        EditText empSal = dialogView.findViewById(R.id.empsal1);
        EditText empComm = dialogView.findViewById(R.id.empcoms);
        Button btnSubmit = dialogView.findViewById(R.id.empsub);
        ImageView imgEmp = dialogView.findViewById(R.id.imgemp);

        // Set existing employee details
        empName.setText(employee.getName());
        empAddress.setText(employee.getAddress());
        empPhone.setText(employee.getPhone());
        empEmail.setText(employee.getEmail());
        empSal.setText(employee.getSalary());
        empComm.setText(String.valueOf(employee.getComs()));

        // Load employee image
        Glide.with(this)
                .load(employee.getImage())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imgEmp);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        btnSubmit.setOnClickListener(v -> {
            // Validate fields
            String updatedName = empName.getText().toString().trim();
            String updatedAddress = empAddress.getText().toString().trim();
            String updatedPhone = empPhone.getText().toString().trim();
            String updatedEmail = empEmail.getText().toString().trim();
            String updatedSalary = empSal.getText().toString().trim();
            double updatedComs = Double.parseDouble(empComm.getText().toString().trim());

            if (!updatedName.isEmpty() && !updatedAddress.isEmpty() && !updatedPhone.isEmpty() &&
                    !updatedEmail.isEmpty() && !updatedSalary.isEmpty()) {
                // Update employee in Firestore
                updateEmployee(employee.getId(), updatedName, updatedAddress, updatedPhone, updatedEmail, updatedSalary, updatedComs);
              //  Toast.makeText(getActivity(), "Employee updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }


}

