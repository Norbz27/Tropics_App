package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomerFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView rvCustomer;
    private CustomerAdapter adapter;
    private List<Map<String, Object>> customerList;
    private List<Map<String, Object>> filteredList;
    private SearchView searchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_customer, container, false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        // Use view to find the SearchView in the layout
        searchView = view.findViewById(R.id.searchView);
        rvCustomer = view.findViewById(R.id.rvCustomer);

        rvCustomer.setLayoutManager(new LinearLayoutManager(getContext()));
        customerList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new CustomerAdapter(getContext(), filteredList, customerData -> {
            // Open the dialog with client information
            getAppointmentDataForClient((String) customerData.get("fullName"), customerData);
        });
        rvCustomer.setAdapter(adapter);

        searchView.setOnClickListener(v -> searchView.setIconified(false));

        setupSearchView();
        loadCustomerData();
        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCustomer(newText);
                return true;
            }
        });
    }

    private void filterCustomer(String query) {
        filteredList.clear(); // Clear the filtered list

        if (query.isEmpty() || query.equals("")) {
            filteredList.addAll(customerList); // Add all items back
        } else {
            for (Map<String, Object> item : customerList) {
                String name = (String) item.get("fullName");
                if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }

        adapter.updateList(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void showClientInfoDialog(Map<String, Object> clientData, List<Map<String, Object>> appointmentData) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_client_info, null);

        // Create the AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setView(dialogView);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Show the dialog
        dialog.show();

        // Bind client data
        TextView tvClientName = dialogView.findViewById(R.id.tvClientName);
        TextView tvClientEmail = dialogView.findViewById(R.id.tvClientEmail);
        TextView tvClientPhone = dialogView.findViewById(R.id.tvClientPhone);
        TextView tvClientAddress = dialogView.findViewById(R.id.tvClientAddress);

        tvClientName.setText((String) clientData.get("fullName"));
        tvClientEmail.setText("Email: " + clientData.get("email"));
        tvClientPhone.setText("Phone: " + clientData.get("phone"));
        tvClientAddress.setText("Address: " + clientData.get("address"));

        // Set up RecyclerView for appointments
        RecyclerView rvAppointments = dialogView.findViewById(R.id.rvAppointments);
        CustomerAppointmentAdapter appointmentAdapter = new CustomerAppointmentAdapter(appointmentData);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAppointments.setAdapter(appointmentAdapter);

        // Bind the close button
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());
    }


    private void getAppointmentDataForClient(String clientFullName, Map<String, Object> clientData) {
        List<Map<String, Object>> appointmentData = new ArrayList<>();
        // Query the Firestore appointments collection
        db.collection("appointments")
                .whereEqualTo("fullName", clientFullName) // Filter by full name
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Map<String, Object> data = doc.getData();
                            data.put("id", doc.getId()); // Optionally add document ID
                            appointmentData.add(data);
                        }
                        // Show the client info dialog with the fetched appointments
                        showClientInfoDialog(clientData, appointmentData);
                    } else {
                        Log.e("Firestore Error", "Error fetching appointments: " + task.getException());
                    }
                });
    }

    private void loadCustomerData() {
        db.collection("appointments")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("Firestore Error", "Error fetching appointments: " + error.getMessage());
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            customerList.clear();
                            Set<String> uniqueNames = new HashSet<>(); // To track unique names

                            for (QueryDocumentSnapshot doc : value) {
                                Map<String, Object> data = doc.getData();
                                data.put("id", doc.getId()); // Add document ID to the item

                                String name = (String) data.get("fullName");
                                // Check if name is already in the set
                                if (uniqueNames.add(name)) { // If name is unique, add to list
                                    customerList.add(data);
                                }
                            }

                            filteredList.clear();
                            filteredList.addAll(customerList);
                            adapter.updateList(filteredList);
                        } else {
                            filteredList.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
}
