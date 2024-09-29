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
    private FirebaseFirestore db;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList;
    private SearchView searchView;
    private static final int IMAGE_PICK_REQUEST = 100;
    private ImageView imgEmp;
    private Uri selectedImageUri;

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
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> {
            String name = empName.getText().toString().trim();
            String address = empAddress.getText().toString().trim();
            String phone = empPhone.getText().toString().trim();
            String email = empEmail.getText().toString().trim();
            String salaryString = empSal.getText().toString().trim();
            String commissionString = comsEditText.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty() && selectedImageUri != null) {
                double salary = Double.parseDouble(salaryString);
                double commission = Double.parseDouble(commissionString);

                Map<String, Object> employeeData = new HashMap<>();
                employeeData.put("name", name);
                employeeData.put("address", address);
                employeeData.put("phone", phone);
                employeeData.put("email", email);
                employeeData.put("salary", salary);
                employeeData.put("coms", commission);

                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("employee_images/" + UUID.randomUUID().toString());
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
                                    });
                        }))
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                            Log.e("Storage Error", "Failed to upload image: " + e.getMessage());
                        });
            } else {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
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
        EditText empName = dialogView.findViewById(R.id.empname);
        EditText empAddress = dialogView.findViewById(R.id.empadd);
        EditText empPhone = dialogView.findViewById(R.id.empphone);
        EditText empEmail = dialogView.findViewById(R.id.empemail);
        EditText empSal = dialogView.findViewById(R.id.empsal1);
        EditText empComm = dialogView.findViewById(R.id.empcomm);

        empName.setText(employee.getName());
        empAddress.setText(employee.getAddress());
        empPhone.setText(employee.getPhone());
        empEmail.setText(employee.getEmail());
        empSal.setText(employee.getSalary());
        empComm.setText(String.valueOf(employee.getComs()));

        Glide.with(this)
                .load(employee.getImage())
                .placeholder(R.drawable.ic_image_placeholder)
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

    @Override
    public void onEmployeeLongClick(Employee employee) {
        showEmployeeOptionsDialog(employee);
    }

    private void showEmployeeOptionsDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Options")
                .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditEmployeeDialog(employee);
                            break;
                        case 1:
                            deleteEmployee(employee);
                            break;
                    }
                })
                .show();
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

        empName.setText(employee.getName());
        empAddress.setText(employee.getAddress());
        empPhone.setText(employee.getPhone());
        empEmail.setText(employee.getEmail());
        empSal.setText(String.valueOf(employee.getSalary()));
        comsEditText.setText(String.valueOf(employee.getComs()));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        imgEmp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, IMAGE_PICK_REQUEST);
        });

        btnSubmit.setOnClickListener(v -> {
            String name = empName.getText().toString().trim();
            String address = empAddress.getText().toString().trim();
            String phone = empPhone.getText().toString().trim();
            String email = empEmail.getText().toString().trim();
            String salaryString = empSal.getText().toString().trim();
            String commissionString = comsEditText.getText().toString().trim();

            if (!name.isEmpty() && !address.isEmpty() && !phone.isEmpty() && !email.isEmpty()) {
                double salary = Double.parseDouble(salaryString);
                double commission = Double.parseDouble(commissionString);

                Map<String, Object> updatedEmployeeData = new HashMap<>();
                updatedEmployeeData.put("name", name);
                updatedEmployeeData.put("address", address);
                updatedEmployeeData.put("phone", phone);
                updatedEmployeeData.put("email", email);
                updatedEmployeeData.put("salary", salary);
                updatedEmployeeData.put("coms", commission);

                if (selectedImageUri != null) {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("employee_images/" + UUID.randomUUID().toString());
                    storageRef.putFile(selectedImageUri)
                            .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                updatedEmployeeData.put("image", uri.toString());
                                updateEmployee(employee.getId(), updatedEmployeeData);
                                dialog.dismiss();
                            }))
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                                Log.e("Storage Error", "Failed to upload image: " + e.getMessage());
                            });
                } else {
                    updateEmployee(employee.getId(), updatedEmployeeData);
                    dialog.dismiss();
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
