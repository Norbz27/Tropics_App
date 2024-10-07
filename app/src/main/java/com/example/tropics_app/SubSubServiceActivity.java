package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SubSubServiceActivity extends AppCompatActivity implements SubServiceAdapter.OnItemClickListener {
    private FloatingActionButton fabAdd;
    private RecyclerView rvService;
    private FirebaseFirestore db;
    private String serviceId;
    private SubServiceAdapter adapter;
    private List<Map<String, Object>> serviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_sub_service);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.darkgray));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.darkgray));
        }
        serviceId = getIntent().getStringExtra("service_id");
        String serviceName = getIntent().getStringExtra("service_name");
        rvService = findViewById(R.id.rvService);
        fabAdd = findViewById(R.id.fabAdd);
        TextView tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceName.setText(serviceName);

        rvService.setLayoutManager(new LinearLayoutManager(this));
        serviceList = new ArrayList<>();

        boolean shouldRemoveDrawable = true;
        adapter = new SubServiceAdapter(this, serviceList, this, shouldRemoveDrawable);
        rvService.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        db = FirebaseFirestore.getInstance();

        fabAdd.setOnClickListener(v -> showNewServiceDialog());

        adapter.setOnItemLongClickListener(new SubServiceAdapter.OnItemLongClickListener() {
            @Override
            public void onEditClick(Map<String, Object> item) {
                // Show the edit dialog
                showEditServiceDialog(item);
            }

            @Override
            public void onDeleteClick(Map<String, Object> item) {
                // Show a confirmation dialog and delete the item
                showDeleteConfirmationDialog(item);
            }
        });
        loadServiceData();
    }

    private void loadServiceData() {
        db.collection("sub_sub_services")
                .whereEqualTo("sub_service_id", serviceId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("Firestore Error", "Error fetching inventory: " + error.getMessage());
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            serviceList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Map<String, Object> data = doc.getData();
                                if (data != null) {
                                    data.put("id", doc.getId()); // Add document ID to the item
                                    serviceList.add(data);
                                    Log.d("Firestore Document", "Loaded: " + data);
                                } else {
                                    Log.e("Firestore Document", "Document data is null for ID: " + doc.getId());
                                }
                            }

                            adapter.updateList(serviceList);
                        } else {
                            Log.d("Inventory", "No items found in inventory.");
                            serviceList.clear(); // Clear list if no items found
                            adapter.updateList(serviceList); // Update adapter with empty list
                        }
                    }
                });
    }


    private void showNewServiceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_sub_service, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etService = dialogView.findViewById(R.id.etService);
        EditText etPrice = dialogView.findViewById(R.id.etPrice); // New field for price
        Button btnAddProduct = dialogView.findViewById(R.id.btnSubmit);

        btnAddProduct.setOnClickListener(v -> {
            String name = etService.getText().toString();
            String priceStr = etPrice.getText().toString();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            } else {
                double price = Double.parseDouble(priceStr); // Parse price to double
                addSubService(name, price);
                dialog.dismiss();
            }
        });
    }

    private void addSubService(String name, double price) {
        Map<String, Object> subService = new HashMap<>();
        subService.put("sub_service_name", name);
        subService.put("price", price); // Add price to the sub-service
        subService.put("sub_service_id", serviceId);

        db.collection("sub_sub_services")
                .add(subService)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Sub-service added successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to add sub-service", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showEditServiceDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_sub_service, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etService = dialogView.findViewById(R.id.etService);
        EditText etPrice = dialogView.findViewById(R.id.etPrice); // New field for price
        Button btnUpdateName = dialogView.findViewById(R.id.btnSubmit);

        etService.setText(item.get("sub_service_name") != null ? (String) item.get("sub_service_name") : "");
        DecimalFormat decimalFormat = new DecimalFormat("#.##"); // Remove trailing zeros
        Object priceObj = item.get("price");

        if (priceObj != null) {
            double price = (Double) priceObj;
            etPrice.setText(decimalFormat.format(price));
        } else {
            etPrice.setText(""); // Set empty if the price is null
        }


        btnUpdateName.setOnClickListener(v -> {
            String newServiceName = etService.getText().toString();
            String newPrice = etPrice.getText().toString();

            if (newServiceName.isEmpty() && newPrice.isEmpty()) {
                Toast.makeText(this, "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                double price = Double.parseDouble(newPrice);
                updateProductNameInFirestore(item, newServiceName, price);
                dialog.dismiss();
            }
        });
    }
    private void updateProductNameInFirestore(Map<String, Object> item, String newServiceName, double newPrice) {
        String documentId = (String) item.get("id"); // Ensure you have the document ID

        if (documentId != null) {
            // Update the name in Firestore
            db.collection("sub_sub_services").document(documentId)
                    .update("sub_service_name", newServiceName, "price", newPrice)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                            loadServiceData(); // Refresh the inventory list if needed
                        } else {
                            Toast.makeText(this, "Failed to update: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }
    private void showDeleteConfirmationDialog(Map<String, Object> item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + item.get("sub_service_name") + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the product from Firestore
                    deleteProductFromFirestore(item);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteProductFromFirestore(Map<String, Object> item) {
        String documentId = (String) item.get("id"); // Get the document ID

        if (documentId != null) {
            db.collection("sub_sub_services").document(documentId)
                    .delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Deleted: " + item.get("sub_service_name"), Toast.LENGTH_SHORT).show();
                            // Optionally refresh the inventory list
                            loadServiceData();
                        } else {
                            Toast.makeText(this, "Failed to delete: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(Map<String, Object> service) {


    }
}
