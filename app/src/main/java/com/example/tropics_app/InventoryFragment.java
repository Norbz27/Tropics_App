package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryFragment extends Fragment {
    private RecyclerView rvInventory;
    private FirebaseFirestore db;
    private InventoryAdapter adapter;
    private List<Map<String, Object>> inventoryList;
    private List<Map<String, Object>> filteredList;
    private FloatingActionButton fabAdd;
    private Calendar calendar;
    private SearchView searchView;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView imgProduct;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);

        searchView = view.findViewById(R.id.searchView);
        searchView.setOnClickListener(v -> searchView.setIconified(false));

        fabAdd = view.findViewById(R.id.fabAdd);
        rvInventory = view.findViewById(R.id.rvInventory);

        fabAdd.setOnClickListener(v -> showAddProductDialog());

        db = FirebaseFirestore.getInstance();
        rvInventory.setLayoutManager(new LinearLayoutManager(getContext()));
        inventoryList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new InventoryAdapter(getContext(), filteredList); // Use filteredList
        rvInventory.setAdapter(adapter);
        EditText datePicker = view.findViewById(R.id.date_picker);
        datePicker.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view1, year1, monthOfYear, dayOfMonth) -> {
                String selectedDate = (monthOfYear + 1) + "/" + dayOfMonth + "/" + year1;
                datePicker.setText(selectedDate);
                loadUsedItemsForDate(selectedDate);

            }, year, month, day);
            datePickerDialog.show();
        });

        adapter.setOnItemLongClickListener(new InventoryAdapter.OnItemLongClickListener() {
            @Override
            public void onAddClick(Map<String, Object> item) {
                showAddQuantityDialog(item);
            }

            @Override
            public void onEditClick(Map<String, Object> item) {
                // Show the edit dialog
                showEditProductDialog(item);
            }

            @Override
            public void onUseClick(Map<String, Object> item) {
                showUpdateQuantityDialog(item);
            }

            @Override
            public void onDeleteClick(Map<String, Object> item) {
                // Show a confirmation dialog and delete the item
                showDeleteConfirmationDialog(item);
            }
        });

        setupSearchView();
        loadInventoryData();
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
                filterInventory(newText);
                return true;
            }
        });
    }
    private void filterInventory(String query) {
        filteredList.clear(); // Clear the filtered list

        if (query.isEmpty() || query.equals("")) {
            filteredList.addAll(inventoryList); // Add all items back
        } else {
            for (Map<String, Object> item : inventoryList) {
                String name = (String) item.get("name");
                if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }

        adapter.updateList(filteredList);
        adapter.notifyDataSetChanged();
    }

    private void loadInventoryData() {
        db.collection("inventory")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("Firestore Error", "Error fetching inventory: " + error.getMessage());
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            inventoryList.clear();
                            for (QueryDocumentSnapshot doc : value) {
                                Map<String, Object> data = doc.getData();
                                data.put("id", doc.getId()); // Add document ID to the item
                                inventoryList.add(data);
                                Log.d("Firestore Document", "Loaded: " + data);
                            }

                            filteredList.clear();
                            filteredList.addAll(inventoryList);
                            adapter.updateList(filteredList);
                        } else {
                            Log.d("Inventory", "No items found in inventory.");
                            filteredList.clear();
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }
    private void showAddQuantityDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quantity_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        EditText etProductQuantity = dialogView.findViewById(R.id.etProductQuantity);
        Button btnAddQuantity = dialogView.findViewById(R.id.btnAddProduct);

        btnAddQuantity.setOnClickListener(v -> {
            String quantityStr = etProductQuantity.getText().toString();

            if (quantityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                int quantityToAdd = Integer.parseInt(quantityStr);
                addQuantityToFirestore(item, quantityToAdd);
                dialog.dismiss();
            }
        });
    }
    private void showUpdateQuantityDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quantity_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etProductQuantity = dialogView.findViewById(R.id.etProductQuantity);
        Button btnUpdateQuantity = dialogView.findViewById(R.id.btnAddProduct);

        btnUpdateQuantity.setOnClickListener(v -> {
            String quantityStr = etProductQuantity.getText().toString();

            if (quantityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                int quantityToUpdate = Integer.parseInt(quantityStr);
                // Choose action (subtract from stocks and add to in_use)
                updateStocksAndInUse(item, quantityToUpdate);
                dialog.dismiss();
            }
        });
    }
    private void updateStocksAndInUse(Map<String, Object> item, int quantityToUpdate) {
        String documentId = (String) item.get("id");
        int currentStocks = Integer.parseInt((String) item.get("stocks"));
        int currentInUse = Integer.parseInt((String) item.get("in_use"));

        if (documentId != null) {
            if (quantityToUpdate > currentStocks) {
                Toast.makeText(getContext(), "Not enough stock available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate the new stocks and in_use values
            int newStocks = currentStocks - quantityToUpdate;
            int newInUse = currentInUse + quantityToUpdate;

            // Update Firestore
            db.collection("inventory").document(documentId)
                    .update("stocks", String.valueOf(newStocks), "in_use", String.valueOf(newInUse))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Stock updated successfully", Toast.LENGTH_SHORT).show();
                            // Optionally refresh the inventory list
                            loadInventoryData();
                        } else {
                            Toast.makeText(getContext(), "Failed to update stock: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void addQuantityToFirestore(Map<String, Object> item, int quantityToAdd) {
        String documentId = (String) item.get("id"); // Get the document ID
        int currentStocks = Integer.parseInt((String) item.get("stocks")); // Get the current stock

        if (documentId != null) {
            // Calculate the new stock quantity
            int newStocks = currentStocks + quantityToAdd;

            // Update the stock quantity in Firestore
            db.collection("inventory").document(documentId)
                    .update("stocks", String.valueOf(newStocks))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Quantity added successfully", Toast.LENGTH_SHORT).show();
                            // Optionally refresh the inventory list
                            loadInventoryData();
                        } else {
                            Toast.makeText(getContext(), "Failed to add quantity: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddProductDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        imgProduct = dialogView.findViewById(R.id.imgProduct); // Initialize imgProduct here
        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etProductQuantity = dialogView.findViewById(R.id.etProductQuantity);
        Button btnAddProduct = dialogView.findViewById(R.id.btnAddProduct);

        imgProduct.setOnClickListener(v -> {
            // Open gallery to select an image
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        btnAddProduct.setOnClickListener(v -> {
            String name = etProductName.getText().toString();
            String quantity = etProductQuantity.getText().toString();

            if (name.isEmpty() || quantity.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Upload image and then add product
                uploadImageToFirebase(name, quantity);
                dialog.dismiss();
            }
        });
    }
    private void loadUsedItemsForDate(String selectedDate) {
        // Parse the selected date to create a date range
        Calendar calendar = Calendar.getInstance();
        String[] dateParts = selectedDate.split("/");

        if (dateParts.length != 3) {
            Log.e("Date Error", "Selected date format is incorrect: " + selectedDate);
            return; // Exit if the date format is incorrect
        }

        int month = Integer.parseInt(dateParts[0]) - 1; // Month is zero-based
        int day = Integer.parseInt(dateParts[1]);
        int year = Integer.parseInt(dateParts[2]);

        // Set the start and end of the date range
        calendar.set(year, month, day, 0, 0, 0);
        Date startDate = calendar.getTime();

        calendar.set(year, month, day, 23, 59, 59);
        Date endDate = calendar.getTime();

        // Convert the start and end dates to Firestore Timestamps
        Timestamp startTimestamp = new Timestamp(startDate);
        Timestamp endTimestamp = new Timestamp(endDate);

        Log.d("Date Range", "Start: " + startTimestamp + ", End: " + endTimestamp);

        // Clear the filtered list before fetching new data
        filteredList.clear();

        // Query Firestore for the records in the date range
        db.collection("inventory")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Check the dailyRecords subcollection for the specified date
                            document.getReference().collection("dailyRecords")
                                    .whereGreaterThanOrEqualTo("date", startTimestamp)
                                    .whereLessThanOrEqualTo("date", endTimestamp)
                                    .get()
                                    .addOnCompleteListener(innerTask -> {
                                        if (innerTask.isSuccessful()) {
                                            for (QueryDocumentSnapshot dailyDoc : innerTask.getResult()) {
                                                Map<String, Object> data = new HashMap<>(document.getData());
                                                data.put("id", document.getId()); // Add document ID to the item
                                                data.put("quantity", dailyDoc.getLong("quantity")); // Add quantity from daily record
                                                filteredList.add(data);
                                                Log.d("Added Item", "ID: " + document.getId() + ", Quantity: " + dailyDoc.getLong("quantity"));
                                            }
                                            // Update the adapter with the new list and notify changes
                                            adapter.updateList(filteredList);
                                            adapter.notifyDataSetChanged(); // Notify the adapter to refresh the data
                                        } else {
                                            Log.e("Firestore Error", "Error fetching daily records: " + innerTask.getException());
                                        }
                                    });
                        }
                    } else {
                        Log.e("Firestore Error", "Error fetching inventory: " + task.getException());
                    }
                });
    }
    private void uploadImageToFirebase(String name, String stocks) {
        if (imageUri != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference("images/" + UUID.randomUUID().toString());
            UploadTask uploadTask = ref.putFile(imageUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                if (task.isSuccessful()) {
                    return ref.getDownloadUrl();
                } else {
                    throw task.getException();
                }
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    addProductToInventory(name, stocks, downloadUri.toString());
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Uploaded", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            if (isAdded()) {
                Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addProductToInventory(String name, String stocks, String imageUrl) {
        // Create a map to store product data
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("stocks", stocks);
        product.put("in_use", "0");
        product.put("imageUrl", imageUrl);

        // Add product to Firestore
        db.collection("inventory")
                .add(product)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String documentId = task.getResult().getId();
                        Toast.makeText(getContext(), "Product added successfully", Toast.LENGTH_SHORT).show();
                        addDailyRecord(documentId, Integer.parseInt(stocks), "Added");
                    } else {
                        Log.e("Firestore Error", "Failed to add product: " + task.getException().getMessage());
                        Toast.makeText(getContext(), "Failed to add product", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void addDailyRecord(String documentId, int quantity, String action) {
        Map<String, Object> record = new HashMap<>();
        record.put("date", new Timestamp(new Date())); // Store the current date as Timestamp
        record.put("quantity", quantity);
        record.put("action", action); // "Added" for adding quantity

        db.collection("inventory").document(documentId)
                .collection("dailyRecords")
                .add(record)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firestore", "Daily record added");
                    } else {
                        Log.e("Firestore Error", "Failed to add daily record: " + task.getException().getMessage());
                    }
                });
    }


    private void showEditProductDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_name_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        Button btnUpdateName = dialogView.findViewById(R.id.btnAddProduct);

        etProductName.setText(item.get("name") != null ? (String) item.get("name") : "");

        btnUpdateName.setOnClickListener(v -> {
            String newProductName = etProductName.getText().toString();

            if (newProductName.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                updateProductNameInFirestore(item, newProductName);
                dialog.dismiss();
            }
        });
    }
    private void updateProductNameInFirestore(Map<String, Object> item, String newProductName) {
        String documentId = (String) item.get("id"); // Ensure you have the document ID

        if (documentId != null) {
            // Update the name in Firestore
            db.collection("inventory").document(documentId)
                    .update("name", newProductName)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Product name updated successfully", Toast.LENGTH_SHORT).show();
                            loadInventoryData(); // Refresh the inventory list if needed
                        } else {
                            Toast.makeText(getContext(), "Failed to update name: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }


    // Show a confirmation dialog to delete the product
    private void showDeleteConfirmationDialog(Map<String, Object> item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + item.get("name") + "?")
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
            db.collection("inventory").document(documentId)
                    .delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Deleted: " + item.get("name"), Toast.LENGTH_SHORT).show();
                            // Optionally refresh the inventory list
                            loadInventoryData();
                        } else {
                            Toast.makeText(getContext(), "Failed to delete: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST) {
            getActivity();
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                imageUri = data.getData();
                imgProduct.setImageURI(imageUri); // Display the selected image
            }
        }
    }
}
