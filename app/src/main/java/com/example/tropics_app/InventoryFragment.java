package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryFragment extends Fragment {
    private RecyclerView rvInventory;
    private FirebaseFirestore db;
    private InventoryAdapter adapter;
    private List<Map<String, Object>> inventoryList;
    private List<Map<String, Object>> filteredList;
    private FloatingActionButton fabAdd;
    private SearchView searchView;
    private String selectedDate;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView imgProduct;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
                selectedDate = String.format("%02d/%02d/%04d", monthOfYear + 1, dayOfMonth, year1); // Store the selected date
                datePicker.setText(selectedDate);
                loadUsedItemsForDate(selectedDate); // Load used items for the selected date
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
                showEditProductDialog(item, selectedDate);
            }
            @Override
            public void onUseClick(Map<String, Object> item) {
                showUpdateQuantityDialog(item);
            }
            @Override
            public void onDeleteClick(Map<String, Object> item) {
                showDeleteConfirmationDialog(item, selectedDate);
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

                                // Check stocks and set the color accordingly
                                String stocksStr = (String) data.get("stocks"); // Assuming stocks are stored as a string
                                int stocks = Integer.parseInt(stocksStr);
                                if (stocks < 3) {
                                    data.put("stockColor", Color.RED); // Set stock color to red if below 3
                                } else {
                                    data.put("stockColor", Color.BLACK); // Default color for stocks
                                }

                                inventoryList.add(data);
                                Log.d("Firestore Document", "Loaded: " + data);

                                // Fetch usage records for each inventory item
                                loadUsageRecords(doc.getId(), data);
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

    private void loadUsageRecords(String productId, Map<String, Object> data) {
        db.collection("inventory").document(productId).collection("usedproducts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalInUse = 0; // Variable to hold total in_use
                        List<Map<String, Object>> usageRecords = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Map<String, Object> usageData = doc.getData();
                            usageRecords.add(usageData);
                            Log.d("Usage Record", "Loaded: " + usageData);

                            // Sum the in_use values
                            String inUse = usageData.get("in_use") != null ? usageData.get("in_use").toString() : "0";
                            try {
                                totalInUse += Integer.parseInt(inUse);
                            } catch (NumberFormatException e) {
                                Log.e("Number Format Error", "Error parsing in_use: " + inUse, e);
                            }
                        }
                        // Log total in_use for debugging
                        Log.d("Total In Use", "Total in_use for product " + productId + ": " + totalInUse);

                        // Add usage records and total in_use to the product data
                        data.put("usageRecords", usageRecords);
                        data.put("in_use", String.valueOf(totalInUse)); // Update total in_use in the product data
                        // After updating data, also add to filteredList if it's not already added
                        if (!filteredList.contains(data)) {
                            filteredList.add(data);
                        }

                    } else {
                        Log.e("Firestore Error", "Error fetching usage records: " + task.getException().getMessage());
                    }
                    // Ensure UI update happens after all tasks are completed
                    adapter.updateList(filteredList);
                    adapter.notifyDataSetChanged();
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
            String quantityStr = etProductQuantity.getText().toString().trim(); // Trim whitespace

            if (quantityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    int quantityToAdd = Integer.parseInt(quantityStr); // Attempt to parse
                    addQuantityToFirestore(item, quantityToAdd);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid quantity entered", Toast.LENGTH_SHORT).show(); // Handle parsing error
                }
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
            String quantityStr = etProductQuantity.getText().toString().trim(); // Trim whitespace
            Log.d("Quantity Input", "Entered quantity: " + quantityStr); // Log input for debugging

            if (quantityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    int quantityToUpdate = Integer.parseInt(quantityStr); // Attempt to parse

                    // Check if quantityToUpdate is a valid positive number before proceeding
                    if (quantityToUpdate < 0) {
                        Toast.makeText(getContext(), "Quantity cannot be negative", Toast.LENGTH_SHORT).show();
                    } else {
                        // Proceed to update stocks and in_use
                        updateStocksAndInUse(item, quantityToUpdate);
                        dialog.dismiss();
                    }
                } catch (NumberFormatException e) {
                    // Log the exception for more context
                    Log.e("Quantity Error", "NumberFormatException: ", e);
                    Toast.makeText(getContext(), "Invalid quantity entered: " + quantityStr, Toast.LENGTH_SHORT).show(); // Show invalid input
                }
            }
        });
    }
    private void updateStocksAndInUse(Map<String, Object> item, int quantityToUpdate) {
        String documentId = (String) item.get("id");
        if (documentId != null) {
            // Reference the usedproducts sub-collection
            db.collection("inventory").document(documentId)
                    .collection("usedproducts")
                    .whereEqualTo("date", getFormattedCurrentDate()) // Check if a record for today exists
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            // If a record for today exists, update it
                            DocumentSnapshot usedProductDoc = task.getResult().getDocuments().get(0);
                            String inUseStr = usedProductDoc.getString("in_use");

                            int currentStocks = Integer.parseInt((String) item.get("stocks"));
                            int currentInUse = Integer.parseInt(inUseStr); // Current in_use for today

                            if (quantityToUpdate > currentStocks) {
                                Toast.makeText(getContext(), "Not enough stock available", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            int newStocks = currentStocks - quantityToUpdate;
                            int newInUse = currentInUse + quantityToUpdate;

                            // Update stocks in the inventory
                            db.collection("inventory").document(documentId)
                                    .update("stocks", String.valueOf(newStocks))
                                    .addOnCompleteListener(stockUpdateTask -> {
                                        if (stockUpdateTask.isSuccessful()) {
                                            // Update today's in_use count
                                            usedProductDoc.getReference()
                                                    .update("in_use", String.valueOf(newInUse))
                                                    .addOnCompleteListener(inUseUpdateTask -> {
                                                        if (inUseUpdateTask.isSuccessful()) {
                                                            Toast.makeText(getContext(), "Stock updated successfully", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(getContext(), "Failed to update in_use: " + inUseUpdateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(getContext(), "Failed to update stocks: " + stockUpdateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // If no record for today, create a new entry
                            createNewInUseRecord(documentId, quantityToUpdate, item);
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to create a new in_use record
    private void createNewInUseRecord(String documentId, int quantityToUpdate, Map<String, Object> item) {
        int currentStocks = Integer.parseInt((String) item.get("stocks"));

        if (quantityToUpdate > currentStocks) {
            Toast.makeText(getContext(), "Not enough stock available", Toast.LENGTH_SHORT).show();
            return;
        }

        int newStocks = currentStocks - quantityToUpdate;

        // Update stocks in the inventory
        db.collection("inventory").document(documentId)
                .update("stocks", String.valueOf(newStocks))
                .addOnCompleteListener(stockUpdateTask -> {
                    if (stockUpdateTask.isSuccessful()) {
                        // Create a new in_use record for today
                        Map<String, Object> newInUseRecord = new HashMap<>();
                        newInUseRecord.put("product", item.get("name"));
                        newInUseRecord.put("in_use", String.valueOf(quantityToUpdate));
                        newInUseRecord.put("date", getFormattedCurrentDate());

                        db.collection("inventory").document(documentId)
                                .collection("usedproducts")
                                .add(newInUseRecord)
                                .addOnCompleteListener(inUseUpdateTask -> {
                                    if (inUseUpdateTask.isSuccessful()) {
                                        Toast.makeText(getContext(), "New usage record created for today", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), "Failed to create new usage record: " + inUseUpdateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Failed to update stocks: " + stockUpdateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper method to get the current date formatted (MM/dd/yyyy)
    private String getFormattedCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        return sdf.format(new Date());
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
    private void uploadImageToFirebase(String name, String stocks) {
        if (imageUri != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference("images/" + UUID.randomUUID().toString());
            UploadTask uploadTask = ref.putFile(imageUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (task.isSuccessful()) {
                        return ref.getDownloadUrl();
                    } else {
                        throw task.getException(); // Throw the exception to be handled in the onComplete
                    }
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        addProductToInventory(name, stocks, downloadUri.toString());
                        Toast.makeText(getContext(), "Uploaded", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
        }
    }
    private void addProductToInventory(String name, String stocks, String imageUrl) {
        // Create a map to store product data for inventory
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("stocks", stocks); // Store the stock count
        product.put("imageUrl", imageUrl); // Store the image URL

        // Add product to Firestore inventory
        db.collection("inventory")
                .add(product)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            String productId = task.getResult().getId();

                            // Just update the stocks for the newly added product
                            db.collection("inventory").document(productId)
                                    .update("stocks", stocks)
                                    .addOnCompleteListener(stockUpdateTask -> {
                                        if (stockUpdateTask.isSuccessful()) {
                                            Toast.makeText(getContext(), "Product added and stocks updated successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e("Firestore Error", "Failed to update stocks: " + stockUpdateTask.getException().getMessage());
                                            Toast.makeText(getContext(), "Failed to update stocks", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Log.e("Firestore Error", "Failed to add product: " + task.getException().getMessage());
                            Toast.makeText(getContext(), "Failed to add product", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showEditProductDialog(Map<String, Object> item, String selectedDate) {
        String yesterdayDate = getYesterdayDate(); // Get yesterday's date

        if (selectedDate.equals(yesterdayDate)) {
            Toast.makeText(getContext(), "Cannot edit products for yesterday's date", Toast.LENGTH_SHORT).show();
            return; // Exit the method if the date is yesterday
        }

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
    private void showDeleteConfirmationDialog(Map<String, Object> item, String selectedDate) {
        String yesterdayDate = getYesterdayDate(); // Get yesterday's date

        if (selectedDate.equals(yesterdayDate)) {
            Toast.makeText(getContext(), "Cannot delete products for yesterday's date", Toast.LENGTH_SHORT).show();
            return; // Exit the method if the date is yesterday
        }

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
    private void loadUsedItemsForDate(String selectedDate) {
        String effectiveDate; // Create a new variable to hold the effective date

        // Determine the effective date
        if (selectedDate == null || selectedDate.trim().isEmpty()) {
            effectiveDate = getCurrentDate(); // Default to today's date
            Log.e("Load Items", "Selected date is invalid. Defaulting to today's date: " + effectiveDate);
        } else {
            effectiveDate = selectedDate; // Use the provided selected date
            Log.d("Selected Date", "Selected Date: " + effectiveDate);
        }

        filteredList.clear(); // Clear previous results

        // Fetch all documents from the inventory collection
        db.collection("inventory")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firestore Data", "Total Inventory Documents: " + task.getResult().size());
                        int totalDocuments = task.getResult().size();
                        AtomicInteger completedTasks = new AtomicInteger(0);

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> itemData = new HashMap<>(document.getData());
                            itemData.put("id", document.getId());
                            itemData.put("product", document.getString("product"));
                            itemData.put("stocks", document.getString("stocks"));

                            // Fetch used products from the sub-collection
                            document.getReference().collection("usedproducts")
                                    .get()
                                    .addOnCompleteListener(innerTask -> {
                                        if (innerTask.isSuccessful()) {
                                            int totalInUse = 0; // To sum used items

                                            for (QueryDocumentSnapshot usedProductDoc : innerTask.getResult()) {
                                                String usedDate = usedProductDoc.getString("date");
                                                String inUse = usedProductDoc.getString("in_use");

                                                // Only sum for valid dates
                                                if (usedDate != null && inUse != null) {
                                                    try {
                                                        // Only sum if usedDate is less than or equal to effectiveDate
                                                        if (usedDate.compareTo(effectiveDate) <= 0) {
                                                            totalInUse += Integer.parseInt(inUse); // Parse safely
                                                            Log.d("Summed In Use", "Used on " + usedDate + ": " + inUse);
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        Log.e("Number Format Error", "Error parsing in_use: " + inUse, e);
                                                    }
                                                }
                                            }

                                            Log.d("Total In Use", "Total in_use for " + itemData.get("product") + ": " + totalInUse);
                                            // Update itemData with total in_use
                                            itemData.put("in_use", String.valueOf(totalInUse));
                                            filteredList.add(itemData); // Add to filteredList

                                        } else {
                                            Log.e("Firestore Error", "Error fetching used products: " + innerTask.getException());
                                        }

                                        // Check if all tasks have completed
                                        if (completedTasks.incrementAndGet() == totalDocuments) {
                                            adapter.updateList(filteredList);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    } else {
                        Log.e("Firestore Error", "Error fetching inventory: " + task.getException());
                    }
                });
    }



    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }


    private String getYesterdayDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        return String.format("%02d/%02d/%04d", month, day, year);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST) {
            getActivity();
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                imageUri = data.getData();
                imgProduct.setImageURI(imageUri);
            }
        }
    }
}
