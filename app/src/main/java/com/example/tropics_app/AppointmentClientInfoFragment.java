package com.example.tropics_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppointmentClientInfoFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private EditText edFirstname, edLastname, edAddress, edPhone, edEmail, edSearchFirstname;
    private RecyclerView rvSearchResults;
    private List<Client> clientList = new ArrayList<>();
    private ClientAdapter clientAdapter;
    private FirebaseFirestore db;
    private final List<Client> allClients = new ArrayList<>();
    private FrameLayout progressContainer1;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Task<QuerySnapshot> preloadTask;  // track the task

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
        progressContainer1 = view.findViewById(R.id.progressContainer1);

        // Set up RecyclerView
        clientAdapter = new ClientAdapter(clientList, this::onClientSelected);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(clientAdapter);

        preloadAllClients();

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
    private void preloadAllClients() {
        mainHandler.post(() -> progressContainer1.setVisibility(View.VISIBLE));

        preloadTask = db.collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;

                    if (task.isSuccessful()) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            Set<String> uniqueFullNames = new HashSet<>();
                            List<Client> tempClients = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Client client = document.toObject(Client.class);
                                String fullName = client.getFullName();

                                if (uniqueFullNames.add(fullName)) {
                                    tempClients.add(client);
                                }
                            }

                            // Now update the UI and shared data back on the main thread
                            mainHandler.post(() -> {
                                allClients.clear();
                                allClients.addAll(tempClients);
                                progressContainer1.setVisibility(View.GONE);
                            });
                        });
                    } else {
                        Log.e("Firestore Error", "Error preloading clients: ", task.getException());
                        mainHandler.post(() -> progressContainer1.setVisibility(View.GONE));
                    }
                });
    }

    private void searchClientByFirstName(String firstName) {
        if (firstName.isEmpty()) {
            rvSearchResults.setVisibility(View.GONE);
            clientList.clear();
            clientAdapter.notifyDataSetChanged();
            return;
        }

        rvSearchResults.setVisibility(View.VISIBLE);

        List<Client> filteredClients = new ArrayList<>();
        for (Client client : allClients) {
            if (client.getFullName().toLowerCase().startsWith(firstName.toLowerCase())) {
                filteredClients.add(client);
            }
        }

        clientList.clear();
        clientList.addAll(filteredClients);
        clientAdapter.notifyDataSetChanged();
        rvSearchResults.setVisibility(clientList.isEmpty() ? View.GONE : View.VISIBLE);
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
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        preloadTask = null; // prevent UI update after destroy
    }

}
