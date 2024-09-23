package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SubServiceActivity extends AppCompatActivity implements SubServiceAdapter.OnItemClickListener {
    private FloatingActionButton fabAdd;
    private RecyclerView rvService;
    private FirebaseFirestore db;
    private String serviceId; // Store the service ID
    private SubServiceAdapter adapter;
    private List<Map<String, Object>> serviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_service);

        serviceId = getIntent().getStringExtra("service_id");
        String serviceName = getIntent().getStringExtra("service_name");
        rvService = findViewById(R.id.rvService);
        fabAdd = findViewById(R.id.fabAdd);
        TextView tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceName.setText(serviceName);

        rvService.setLayoutManager(new LinearLayoutManager(this));
        serviceList = new ArrayList<>();

        boolean shouldRemoveDrawable = false; // Or false, depending on your logic
        adapter = new SubServiceAdapter(this, serviceList, this, shouldRemoveDrawable);
        rvService.setAdapter(adapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        db = FirebaseFirestore.getInstance();

        fabAdd.setOnClickListener(v -> showNewServiceDialog());

        loadServiceData();
    }

    private void loadServiceData() {
        db.collection("sub_services")
                .whereEqualTo("main_service_id", serviceId)
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
        subService.put("main_service_id", serviceId);

        db.collection("sub_services")
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

    @Override
    public void onItemClick(Map<String, Object> service) {
        // Handle the item click here
        Toast.makeText(this, "Clicked: " + service.get("id"), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SubSubServiceActivity.class); // Replace with your child activity
        intent.putExtra("service_id", (String) service.get("id"));
        intent.putExtra("service_name", (String) service.get("sub_service_name"));
        startActivity(intent);
    }
}
