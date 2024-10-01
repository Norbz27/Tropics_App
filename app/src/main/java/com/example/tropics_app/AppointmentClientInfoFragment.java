package com.example.tropics_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppointmentClientInfoFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private EditText edFirstname, edLastname, edAddress, edPhone, edEmail, edSearchFirstname;
    private RecyclerView rvSearchResults;
    private List<Client> clientList = new ArrayList<>();
    private ClientAdapter clientAdapter;
    private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
    }
    @Override
    public void onResume() {
        super.onResume();

        if (viewModel.isAppointmentDone()) {
            clearEditTexts();
            viewModel.setAppointmentDone(false); // Reset the flag
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_client_info, container, false);

        edFirstname = view.findViewById(R.id.edFirstname);
        edLastname = view.findViewById(R.id.edLastname);
        edAddress = view.findViewById(R.id.edAddress);
        edPhone = view.findViewById(R.id.edPhone);
        edEmail = view.findViewById(R.id.edEmail);
        edSearchFirstname = view.findViewById(R.id.edSearchFirstname);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);

        // Set up RecyclerView
        clientAdapter = new ClientAdapter(clientList, this::onClientSelected);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(clientAdapter);

        // Add TextWatcher to search EditText
        edSearchFirstname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchClientByFirstName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Button nextButton = view.findViewById(R.id.btnNext);
        nextButton.setOnClickListener(v -> {
            if (edFirstname.getText().toString().isEmpty()) {
                edFirstname.setError("Enter First name");
                edFirstname.requestFocus();
                return;
            }
            if (edLastname.getText().toString().isEmpty()) {
                edLastname.setError("Enter Last name");
                edLastname.requestFocus();
                return;
            }
            if (edAddress.getText().toString().isEmpty()) {
                edAddress.setError("Enter Address");
                edAddress.requestFocus();
                return;
            }
            if (edPhone.getText().toString().isEmpty()) {
                edPhone.setError("Enter Phone number");
                edPhone.requestFocus();
                return;
            }
            if (edEmail.getText().toString().isEmpty()) {
                edEmail.setError("Enter Email");
                edEmail.requestFocus();
                return;
            }
            saveData(); // Save data before navigating
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        });

        return view;
    }

    private void clearEditTexts() {
        edFirstname.setText("");
        edLastname.setText("");
        edAddress.setText("");
        edPhone.setText("");
        edEmail.setText("");
        edSearchFirstname.setText(""); // Clear the search field as well
        rvSearchResults.setVisibility(View.GONE); // Hide RecyclerView
        clientList.clear(); // Clear the client list
        clientAdapter.notifyDataSetChanged(); // Notify adapter of data change
    }

    private void searchClientByFirstName(String firstName) {
        if (firstName.isEmpty()) {
            rvSearchResults.setVisibility(View.GONE);
            clientList.clear();
            clientAdapter.notifyDataSetChanged();
            return;
        }

        rvSearchResults.setVisibility(View.VISIBLE); // Show RecyclerView when searching
        // Optionally, show a loading indicator here

        db.collection("appointments")
                .whereGreaterThanOrEqualTo("fullName", firstName)
                .whereLessThanOrEqualTo("fullName", firstName + "\uf8ff") // Get clients starting with that name
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        clientList.clear();
                        Set<String> uniqueFullNames = new HashSet<>(); // Set to track unique full names

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Client client = document.toObject(Client.class); // Ensure Client class matches Firestore structure
                            String fullName = client.getFullName(); // Adjust according to your Client class structure

                            // Check if the full name is unique
                            if (uniqueFullNames.add(fullName)) { // add() returns true if the name was added, false if it was already present
                                clientList.add(client); // Only add the client if the name is unique
                            }
                        }

                        clientAdapter.notifyDataSetChanged();
                        rvSearchResults.setVisibility(clientList.isEmpty() ? View.GONE : View.VISIBLE);
                    } else {
                        // Handle error
                        Log.e("Firestore Error", "Error getting documents: ", task.getException());
                    }
                    // Hide loading indicator here
                });
    }

    private void onClientSelected(Client client) {
        edFirstname.setText(client.getFirstName()); // Assume Client class has a method to get first name
        edLastname.setText(client.getLastName()); // Assume Client class has a method to get last name
        edAddress.setText(client.getAddress()); // Assume Client class has a method to get address
        edPhone.setText(client.getPhone()); // Assume Client class has a method to get phone
        edEmail.setText(client.getEmail()); // Assume Client class has a method to get email
        rvSearchResults.setVisibility(View.GONE); // Hide the RecyclerView after selection
    }

    private void saveData() {
        String fullName = edFirstname.getText().toString() + " " + edLastname.getText().toString();
        viewModel.setFullName(fullName);
        viewModel.setAddress(edAddress.getText().toString());
        viewModel.setPhone(edPhone.getText().toString());
        viewModel.setEmail(edEmail.getText().toString());
    }
}
