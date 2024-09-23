package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceFragment extends Fragment implements ServiceAdapter.OnItemClickListener {
    private RecyclerView rvService;
    private FloatingActionButton fabAdd;
    private SearchView searchView;
    private FirebaseFirestore db;
    private ServiceAdapter adapter;
    private List<Map<String, Object>> serviceList;
    private List<Map<String, Object>> filteredList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_service, container, false);
        searchView = view.findViewById(R.id.searchView);
        searchView.setOnClickListener(v -> searchView.setIconified(false));
        fabAdd = view.findViewById(R.id.fabAdd);
        rvService = view.findViewById(R.id.rvService);

        fabAdd.setOnClickListener(v -> showNewServiceDialog());

        db = FirebaseFirestore.getInstance();
        rvService.setLayoutManager(new LinearLayoutManager(getContext()));
        serviceList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Pass 'this' as the listener
        adapter = new ServiceAdapter(getContext(), filteredList, this);
        rvService.setAdapter(adapter);

        setupSearchView();
        loadServiceData();
        return view;
    }

    @Override
    public void onItemClick(Map<String, Object> item) {
        // Handle the click event here and navigate to the child interface
        Intent intent = new Intent(getContext(), SubServiceActivity.class); // Replace with your child activity
        intent.putExtra("service_id", (String) item.get("id"));
        intent.putExtra("service_name", (String) item.get("service_name"));
        startActivity(intent);
    }
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterInventory(newText);
                return true;
            }
        });
    }
    private void filterInventory(String query) {
        filteredList.clear(); // Clear the filtered list

        if (query.isEmpty() || query.equals("")) {
            filteredList.addAll(serviceList); // Add all items back
        } else {
            for (Map<String, Object> item : serviceList) {
                String name = (String) item.get("name");
                if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }

        adapter.updateList(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void loadServiceData() {
        db.collection("service")
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
                                data.put("id", doc.getId()); // Add document ID to the item
                                serviceList.add(data);
                                Log.d("Firestore Document", "Loaded: " + data);
                            }

                            filteredList.clear();
                            filteredList.addAll(serviceList);
                            adapter.updateList(filteredList);
                        } else {
                            Log.d("Inventory", "No items found in inventory.");
                            filteredList.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
    private void showNewServiceDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_new_service, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etService = dialogView.findViewById(R.id.etService);
        Button btnAddProduct = dialogView.findViewById(R.id.btnSubmit);


        btnAddProduct.setOnClickListener(v -> {
            String name = etService.getText().toString();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            } else {
                newService(name);
                dialog.dismiss();
            }
        });
    }

    private void newService(String name) {
        // Create a map to store product data
        Map<String, Object> product = new HashMap<>();
        product.put("service_name", name);

        // Add product to Firestore
        db.collection("service")
                .add(product)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Product added successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e("Firestore Error", "Failed to add product: " + task.getException().getMessage());
                            Toast.makeText(getContext(), "Failed to add product", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}