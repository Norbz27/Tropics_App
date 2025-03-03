package com.example.tropics_app;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountsFragment extends Fragment {
    private RecyclerView rvAccounts;
    private AccountAdapter adapter;
    private List<Accounts> userList = new ArrayList<>();
    private static final String FETCH_USERS_URL = "https://us-central1-tropico-16e1e.cloudfunctions.net/listUsers";
    private static final String DELETE_USERS_URL = "https://us-central1-tropico-16e1e.cloudfunctions.net/deleteUserByEmail";
    private FloatingActionButton fabAdd;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);

        rvAccounts = view.findViewById(R.id.rvAccounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new AccountAdapter(getContext(), userList);
        rvAccounts.setAdapter(adapter);
        fabAdd = view.findViewById(R.id.fabAdd);

        fabAdd.setOnClickListener(v -> showAddAccount());

        fetchUsers();

        adapter.setOnItemLongClickListener(new AccountAdapter.OnItemLongClickListener() {

            @Override
            public void onResetPassClick(Accounts user) {
                    resetAccountPass(user);
            }

            @Override
            public void onDeleteClick(Accounts user) {
                    deleteAccount(user);
            }

            @Override
            public void onPermissionClick(Accounts user) {
                    showAccountPermission(user);
            }
        });

        return view;
    }

    private void fetchUsers() {
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, FETCH_USERS_URL, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        userList.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject userJson = response.getJSONObject(i);
                                String uid = userJson.getString("uid");
                                String email = userJson.getString("email");
                                userList.add(new Accounts(uid, email));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity(), "Failed to load users", Toast.LENGTH_SHORT).show();
                        Log.e("Firebase", "Error: " + error.getMessage());
                    }
                });

        queue.add(request);
    }

    private void showAddAccount() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_account, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        EditText etPassword = dialogView.findViewById(R.id.etPassword);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        btnSubmit.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Email and Password are required", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(getContext(), "User Created: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                fetchUsers();
                            }
                        } else {
                            Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void showAccountPermission(Accounts user) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_permission, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        CheckBox chkb_edit_sales = dialogView.findViewById(R.id.chkb_edit_sales);
        CheckBox chkb_delete_sales = dialogView.findViewById(R.id.chkb_delete_sales);
        CheckBox chkb_edit_salary = dialogView.findViewById(R.id.chkb_edit_salary);
        Button btnSubmit = dialogView.findViewById(R.id.btnSave);

        btnSubmit.setOnClickListener(view -> {
            boolean editSales = chkb_edit_sales.isChecked();
            boolean deleteSales = chkb_delete_sales.isChecked();
            boolean editSalary = chkb_edit_salary.isChecked();

            String uid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(uid);

            // Check if the document exists
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                Map<String, Object> permissions = new HashMap<>();
                permissions.put("editSales", editSales);
                permissions.put("deleteSales", deleteSales);
                permissions.put("editSalary", editSalary);

                if (documentSnapshot.exists()) {
                    // ✅ Document exists -> Update the permissions field
                    userRef.update("permissions", permissions)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "Permissions updated successfully!");
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating permissions", e));
                } else {
                    // ❌ Document doesn't exist -> Create a new document
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("permissions", permissions);

                    userRef.set(userData)  // Set ensures it creates the document
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "New user created with permissions!");
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> Log.e("Firestore", "Error adding new user", e));
                }
            }).addOnFailureListener(e -> Log.e("Firestore", "Error checking document", e));
        });

    }
    private void resetAccountPass(Accounts user) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            Toast.makeText(getContext(), "User email is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(user.getEmail())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password reset email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteAccount(Accounts user) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", user.getEmail()); // Send user email to delete
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, DELETE_USERS_URL, jsonBody,
                response -> {
                    Toast.makeText(getContext(), "User deleted successfully!", Toast.LENGTH_SHORT).show();
                    userList.remove(user); // Remove from RecyclerView
                    adapter.notifyDataSetChanged(); // Refresh RecyclerView
                },
                error -> Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

}
