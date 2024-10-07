package com.example.tropics_app;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.ParseException;
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
import java.util.Date;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class InventoryFragment extends Fragment {
    private RecyclerView rvInventory;
    private FirebaseFirestore db;
    private InventoryAdapter adapter;
    private List<Map<String, Object>> inventoryList;
    private List<Map<String, Object>> filteredList;
    private FloatingActionButton fabAdd;
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
        Date currentDate = new Date(); // Get current date
        SimpleDateFormat sdf = new SimpleDateFormat("MM/d/yyyy"); // Define the date format
        String formattedDate = sdf.format(currentDate);

        EditText datePicker = view.findViewById(R.id.date_picker);
        datePicker.setText(formattedDate);
        adapter.setSelectedDate(formattedDate);
        adapter.setSelectedDate(datePicker.getText().toString());
        datePicker.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), R.style.datepicker, (view1, year1, monthOfYear, dayOfMonth) -> {
                String selectedDate = (monthOfYear + 1) + "/" + dayOfMonth + "/" + year1;
                datePicker.setText(selectedDate);
                loadUsedItemsForDate(selectedDate);
                adapter.setSelectedDate(selectedDate);
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
       // loadInventoryData();
        loadUsedItemsForDate(getTodayDate());
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

   /* private void loadInventoryData() {
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

    */
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
               int inUseToAdd = 0; // You can change this value as needed
               addQuantityToFirestore(item, quantityToAdd, inUseToAdd); // Updated to include inUseToAdd
               dialog.dismiss();
           }
       });
   }
//
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
    // use stock
    private void updateStocksAndInUse(Map<String, Object> item, int quantityToUpdate) {
        String documentId = (String) item.get("id");
        String todayDate = getTodayDate();
        String yesterdayDate = getYesterdayDate();

        // Disable the ability to update for any date that is not today
        if (!todayDate.equals(getTodayDate())) {
            Toast.makeText(getContext(), "You can only update stocks for today", Toast.LENGTH_SHORT).show();
            return;
        }

        final int[] yesterdayStocks = {0};
        final int[] yesterdayInUse = {0};
        final int[] todayStocks = {0};
        final int[] todayInUse = {0};

        // Retrieve yesterday's stocks and in_use only once
        db.collection("inventory").document(documentId)
                .collection("dailyRecords").document(yesterdayDate)
                .get().addOnCompleteListener(yesterdayTask -> {
                    if (yesterdayTask.isSuccessful() && yesterdayTask.getResult() != null) {
                        DocumentSnapshot yesterdaySnapshot = yesterdayTask.getResult();
                        yesterdayStocks[0] = yesterdaySnapshot.getLong("stocks") != null ? yesterdaySnapshot.getLong("stocks").intValue() : 0;
                        yesterdayInUse[0] = yesterdaySnapshot.getLong("in_use") != null ? yesterdaySnapshot.getLong("in_use").intValue() : 0;

                        // Now retrieve today's stocks and in_use
                        db.collection("inventory").document(documentId)
                                .collection("dailyRecords").document(todayDate)
                                .get().addOnCompleteListener(todayTask -> {
                                    if (todayTask.isSuccessful() && todayTask.getResult() != null && todayTask.getResult().exists()) {
                                        DocumentSnapshot todaySnapshot = todayTask.getResult();
                                        todayStocks[0] = todaySnapshot.getLong("stocks") != null ? todaySnapshot.getLong("stocks").intValue() : 0;
                                        todayInUse[0] = todaySnapshot.getLong("in_use") != null ? todaySnapshot.getLong("in_use").intValue() : 0;

                                        // Ensure there is enough stock to use
                                        if (quantityToUpdate > todayStocks[0]) {
                                            Toast.makeText(getContext(), "Not enough stock available", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        // Update today's stock and in_use
                                        int newTodayStocks = todayStocks[0] - quantityToUpdate;
                                        int newInUse = todayInUse[0] + quantityToUpdate;

                                        Map<String, Object> updateFields = new HashMap<>();
                                        updateFields.put("stocks", newTodayStocks);
                                        updateFields.put("in_use", newInUse);

                                        // Update today's stock in Firestore
                                        db.collection("inventory").document(documentId).collection("dailyRecords")
                                                .document(todayDate).update(updateFields)
                                                .addOnCompleteListener(updateTask -> {
                                                    if (updateTask.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Stock updated successfully", Toast.LENGTH_SHORT).show();
                                                        loadUsedItemsForDate(todayDate);
                                                    } else {
                                                        Toast.makeText(getContext(), "Failed to update stock: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        // If no today record, create a new one using yesterday's data
                                        int newStocks = Math.max(0, yesterdayStocks[0] - quantityToUpdate);
                                        int newInUse = yesterdayInUse[0] + quantityToUpdate;

                                        Map<String, Object> newRecord = new HashMap<>();
                                        newRecord.put("date", todayDate);
                                        newRecord.put("stocks", newStocks);
                                        newRecord.put("in_use", newInUse);

                                        // Create new document for today
                                        db.collection("inventory").document(documentId).collection("dailyRecords")
                                                .document(todayDate).set(newRecord)
                                                .addOnCompleteListener(createTask -> {
                                                    if (createTask.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Stock created and updated successfully", Toast.LENGTH_SHORT).show();
                                                        loadUsedItemsForDate(todayDate);
                                                    } else {
                                                        Toast.makeText(getContext(), "Failed to create stock record: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Failed to retrieve yesterday's stock records", Toast.LENGTH_SHORT).show();
                    }
                });
    }
// jari nor
    private void addQuantityToFirestore(Map<String, Object> item, int quantityToAdd, int inUseToAdd) {
        String documentId = (String) item.get("id"); // Get the document ID
        String todayDate = getTodayDate(); // Get today's date
        String yesterdayDate = getYesterdayDate(); // Get yesterday's date

        if (documentId != null) {
            // First, fetch yesterday's stock and in_use
            db.collection("inventory").document(documentId)
                    .collection("dailyRecords").document(yesterdayDate)
                    .get().addOnCompleteListener(yesterdayTask -> {
                        if (yesterdayTask.isSuccessful() && yesterdayTask.getResult() != null) {
                            DocumentSnapshot yesterdaySnapshot = yesterdayTask.getResult();
                            int yesterdayStocks = yesterdaySnapshot.getLong("stocks") != null ? yesterdaySnapshot.getLong("stocks").intValue() : 0;
                            int yesterdayInUse = yesterdaySnapshot.getLong("in_use") != null ? yesterdaySnapshot.getLong("in_use").intValue() : 0;

                            // Now fetch today's stock and in_use (if exists)
                            db.collection("inventory").document(documentId)
                                    .collection("dailyRecords").document(todayDate)
                                    .get().addOnCompleteListener(todayTask -> {
                                        if (todayTask.isSuccessful() && todayTask.getResult() != null && todayTask.getResult().exists()) {
                                            DocumentSnapshot todaySnapshot = todayTask.getResult();
                                            int todayStocks = todaySnapshot.getLong("stocks") != null ? todaySnapshot.getLong("stocks").intValue() : 0;
                                            int todayInUse = todaySnapshot.getLong("in_use") != null ? todaySnapshot.getLong("in_use").intValue() : 0;

                                            // jari an pag check kung an date kuman is today amo na maka modify
                                            if (todayDate.equals(getTodayDate())) {
                                                // Update today's stock by adding the new quantity to the current today's stock
                                                int updatedTodayStocks = todayStocks + quantityToAdd;

                                                // Update today's in_use by adding the new in_use to the current today's in_use
                                                int updatedTodayInUse = todayInUse + inUseToAdd;

                                                // Prepare the update
                                                Map<String, Object> updateFields = new HashMap<>();
                                                updateFields.put("stocks", updatedTodayStocks); // Add to today's stock
                                                updateFields.put("in_use", updatedTodayInUse); // Add to today's in_use

                                                // Update Firestore document for today
                                                db.collection("inventory").document(documentId).collection("dailyRecords")
                                                        .document(todayDate).update(updateFields)
                                                        .addOnCompleteListener(updateTask -> {
                                                            if (updateTask.isSuccessful()) {
                                                                Toast.makeText(getContext(), "Stock and usage updated successfully", Toast.LENGTH_SHORT).show();
                                                                loadUsedItemsForDate(todayDate); // Load data for today's date
                                                            } else {
                                                                Toast.makeText(getContext(), "Failed to update stock and usage: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            } else {
                                                Toast.makeText(getContext(), "Cannot edit data for previous dates", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            // If no today record, create a new one with yesterday's stock and add today's values
                                            int updatedTodayStocks = yesterdayStocks + quantityToAdd; // Use yesterday's stock as the base
                                            int updatedTodayInUse = yesterdayInUse + inUseToAdd; // Only add today's in_use on the first use today

                                            // Create a new record for today
                                            Map<String, Object> newRecord = new HashMap<>();
                                            newRecord.put("date", todayDate);
                                            newRecord.put("stocks", updatedTodayStocks); // Set today's stock to yesterday's stock + added quantity
                                            newRecord.put("in_use", updatedTodayInUse); // Set today's in_use to yesterday's in_use + today's usage

                                            // Set the new document in Firestore
                                            db.collection("inventory").document(documentId).collection("dailyRecords")
                                                    .document(todayDate).set(newRecord)
                                                    .addOnCompleteListener(createTask -> {
                                                        if (createTask.isSuccessful()) {
                                                            Toast.makeText(getContext(), "New stock and usage record created successfully", Toast.LENGTH_SHORT).show();
                                                            loadUsedItemsForDate(todayDate); // Load data for today's date
                                                        } else {
                                                            Toast.makeText(getContext(), "Failed to create stock and usage record: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    });
                        } else {
                            Toast.makeText(getContext(), "Failed to retrieve yesterday's stock and usage records", Toast.LENGTH_SHORT).show();
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
                loadUsedItemsForDate(getTodayDate());
            } else {
                // Upload image and then add product
                uploadImageToFirebase(name, quantity);
                dialog.dismiss();
            }
        });
    }
    private void logAdditionInDailyRecords(String documentId, int quantityAdded) {
        // Create a date string in the format you want (e.g., "yyyy-MM-dd")
        String date = getTodayDate(); // Implement this method to get today's date

        // Prepare the daily record data
        Map<String, Object> dailyRecord = new HashMap<>();
        dailyRecord.put("date", new Timestamp(new Date())); // Record the current timestamp
        dailyRecord.put("stockAdded", quantityAdded); // Store the quantity added

        // Add the daily record to the inventory document's dailyRecords subcollection
        db.collection("inventory").document(documentId)
                .collection("dailyRecords")
                .document(date) // Use date as document ID to ensure uniqueness
                .set(dailyRecord)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Addition recorded successfully", Toast.LENGTH_SHORT).show();
                        loadUsedItemsForDate(getTodayDate());
                    } else {
                        Toast.makeText(getContext(), "Failed to record addition: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
        // Create a map to store product data
        Map<String, Object> product = new HashMap<>();
        product.put("name", name);
        product.put("imageUrl", imageUrl); // Store the image URL

        // Add product to Firestore in the main inventory collection
        db.collection("inventory")
                .add(product)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            DocumentReference productRef = task.getResult();
                            loadUsedItemsForDate(getTodayDate());
                            // Track the initial stocks in the dailyRecords collection
                            Map<String, Object> dailyRecord = new HashMap<>();
                            dailyRecord.put("date", getTodayDate());
                            dailyRecord.put("stocks", Long.parseLong(stocks));
                            dailyRecord.put("in_use", 0);

                            // Create a document for today in the dailyRecords subcollection
                            productRef.collection("dailyRecords").document(getTodayDate()).set(dailyRecord)
                                    .addOnCompleteListener(recordTask -> {
                                        if (recordTask.isSuccessful()) {
                                            Toast.makeText(getContext(), "Product added successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e("Firestore Error", "Failed to add daily record: " + recordTask.getException().getMessage());
                                        }
                                    });
                        } else {
                            Log.e("Firestore Error", "Failed to add product: " + task.getException().getMessage());
                            Toast.makeText(getContext(), "Failed to add product", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Show a dialog to edit the product
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
                            //loadInventoryData(); // Refresh the inventory list if needed
                            loadUsedItemsForDate(getTodayDate());
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
                            //loadInventoryData();
                            loadUsedItemsForDate(getTodayDate());
                        } else {
                            Toast.makeText(getContext(), "Failed to delete: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean isInventoryDataLoaded = false; // Flag to control inventory loading

    private String getTodayDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy"); // Adjust the format as needed
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    private void loadUsedItemsForDate(String selectedDate) {
        // Check if a date was selected; if not, use yesterday's date
        if (selectedDate == null || selectedDate.isEmpty()) {
            selectedDate = getYesterdayDate(); // Get yesterday's date if no date is selected
            Log.d("Date Selection", "No date selected, using yesterday's date: " + selectedDate);
        } else {
            Log.d("Date Selection", "Selected date: " + selectedDate);
        }

        // Ensure the date is in the Firestore format "MMMM dd, yyyy"
        String formattedDate = formatDateForFirestore(selectedDate);
        Log.d("Date Formatting", "Formatted date for Firestore: " + formattedDate);

        filteredList.clear();
        Log.d("Inventory Update", "Cleared filtered list.");

        if (!isInventoryDataLoaded) {
            Log.d("Firestore Query", "Fetching inventory data from Firestore...");
            db.collection("inventory")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("Firestore Query", "Successfully fetched inventory data.");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("Firestore Inventory", "Document ID: " + document.getId() + ", Data: " + document.getData());

                                // Start with the selected date
                                fetchDailyRecordForDate(document, formattedDate);
                            }
                        } else {
                            Log.e("Firestore Error", "Error fetching inventory: " + task.getException());
                        }
                    });
        } else {
            Log.d("Inventory Update", "Inventory data already loaded, filtering by date.");
            filterInventoryByDate(formattedDate);
        }
    }

    // Method to format date into Firestore's "MMMM dd, yyyy" format
    private String formatDateForFirestore(String inputDate) {
        // Firestore date format: "MMMM dd, yyyy"
        DateTimeFormatter firestoreFormat = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault());

        // Other possible input formats
        DateTimeFormatter inputFormat1 = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault()); // e.g., 10/07/2024
        DateTimeFormatter inputFormat2 = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.getDefault());   // e.g., 10/7/2024

        try {
            // Try to parse the input date in the MM/dd/yyyy format
            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(inputDate, inputFormat1);
            } catch (DateTimeParseException e) {
                // If the first format fails, try the M/d/yyyy format
                parsedDate = LocalDate.parse(inputDate, inputFormat2);
            }

            // Return the date formatted in Firestore format
            return parsedDate.format(firestoreFormat);
        } catch (DateTimeParseException e) {
            Log.e("Date Parsing Error", "Error parsing input date: " + e.getMessage());
            return inputDate; // Return the original date if parsing fails
        }
    }

    // Method to fetch daily record for a specific date
    private void fetchDailyRecordForDate(QueryDocumentSnapshot document, String date) {
        DocumentReference dailyRecordRef = document.getReference().collection("dailyRecords").document(date); // Use date to match Firestore

        Log.d("Firestore Query", "Fetching daily record for date: " + date + " in inventory ID: " + document.getId());

        // Fetch the daily record for the specified date
        dailyRecordRef.get().addOnCompleteListener(innerTask -> {
            if (innerTask.isSuccessful() && innerTask.getResult() != null) {
                DocumentSnapshot dailyDoc = innerTask.getResult();
                if (dailyDoc.exists()) {
                    long used = dailyDoc.getLong("in_use") != null ? dailyDoc.getLong("in_use") : 0;
                    long stocks = dailyDoc.getLong("stocks") != null ? dailyDoc.getLong("stocks") : 0;

                    Log.d("Firestore Daily Record", "Daily record found for " + date + ": in_use = " + used + ", stocks = " + stocks);

                    // Prepare combined data for display
                    Map<String, Object> combinedData = new HashMap<>(document.getData());
                    combinedData.put("id", document.getId());
                    combinedData.put("quantity", stocks); // Stocks remaining
                    combinedData.put("used", used); // in_use
                    combinedData.put("date", date);

                    // Add to the filtered list
                    filteredList.add(combinedData);
                    Log.d("Inventory Update", "Filtered list updated with data: " + combinedData);

                    // Check stocks and update the UI
                    adapter.updateList(filteredList);
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e("Firestore Error", "No daily record found for " + date + " in inventory ID: " + document.getId());
                    // If no record found, check the previous date
                    String previousDate = getPreviousDate(date);
                    Log.d("Date Check", "No record found. Checking previous date: " + previousDate);
                    fetchDailyRecordForDate(document, previousDate);
                }
            } else {
                Log.e("Firestore Error", "Error fetching daily record: " + innerTask.getException());
            }
        });
    }


    // Method to get the previous date based on the provided date
    private String getPreviousDate(String currentDate) {
        DateTimeFormatter formatterFirestore = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()); // Firestore format

        try {
            // Parse the current date using Firestore's expected format
            LocalDate date = LocalDate.parse(currentDate, formatterFirestore);

            // Get the previous date
            LocalDate previousDate = date.minusDays(1);

            // Return the previous date in Firestore format
            return previousDate.format(formatterFirestore);
        } catch (DateTimeParseException e) {
            Log.e("Date Parsing Error", "Error parsing date in Firestore format: " + e.getMessage());
            return currentDate; // Stop recursion and return the current date if parsing fails
        }
    }

    private String getYesterdayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()); // Use the same format as in Firestore
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Subtract one day
        return sdf.format(calendar.getTime()); // Return formatted date
    }

    private void filterInventoryByDate(String selectedDate) {
        // Here you can filter the inventory based on the selected date
        List<Map<String, Object>> filteredByDate = new ArrayList<>();

        for (Map<String, Object> item : filteredList) {
            if (item.get("date").equals(selectedDate)) {
                filteredByDate.add(item);
            }
        }

        // Sort the filtered items, if needed
        Collections.sort(filteredByDate, (a, b) -> 0); // Currently does not sort; implement as needed

        // Update the adapter with the filtered items
        adapter.updateList(filteredByDate);
        adapter.notifyDataSetChanged();
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
