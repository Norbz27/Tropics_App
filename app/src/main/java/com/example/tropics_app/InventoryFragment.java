package com.example.tropics_app;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
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
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ImageView imgProduct;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, container, false);
        //swipeRefreshLayout = view.findViewById(R.id.refreshlayout);
        searchView = view.findViewById(R.id.searchView);
        searchView.setOnClickListener(v -> searchView.setIconified(false));

        fabAdd = view.findViewById(R.id.fabAdd);
        rvInventory = view.findViewById(R.id.rvInventory);

        fabAdd.setOnClickListener(v -> showAddProductDialog());

        db = FirebaseFirestore.getInstance();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        rvInventory.setLayoutManager(new LinearLayoutManager(getContext()));
        inventoryList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new InventoryAdapter(getContext(), filteredList); // Use filteredList
        rvInventory.setAdapter(adapter);

        /*swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Call method to refresh data
                loadUsedItemsForDate(getTodayDate());
            }
        });*/
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/d/yyyy");
        String formattedDate = sdf.format(currentDate);

        EditText datePicker = view.findViewById(R.id.date_picker);
        datePicker.setText(formattedDate);
        adapter.setSelectedDate(formattedDate);

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
            public void onEditClick(Map<String, Object> item) {showEditProductDialog(item);}

            @Override
            public void onRemoveInUsedClick(Map<String, Object> item) {
                showRemoveQuantityDialog(item);
            }

            @Override
            public void onUseClick(Map<String, Object> item) {
                showUpdateQuantityDialog(item);
            }

            @Override
            public void onDeleteClick(Map<String, Object> item) {showDeleteConfirmationDialog(item);}

            @Override
            public void onRemoveStockClick(Map<String, Object> item) {showRemoveStocks(item);}
        });

        // Pass the view parameter to the setupSearchView method
        setupSearchView(view);

        loadUsedItemsForDate(getTodayDate());
        return view;
    }


    private void setupSearchView(View view) {
        SearchView searchView = view.findViewById(R.id.searchView);

        // Open the SearchView when clicked
        searchView.setOnClickListener(v -> searchView.setIconified(false));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // You can implement additional logic here if needed
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterListByName(newText); // Call the filter method whenever the query changes
                return true;
            }
        });
    }

    private void filterListByName(String query) {
        // If the query is empty, reload the items for the currently selected date
        if (query.isEmpty()) {
            // Reload the used items for the selected date
            loadUsedItemsForDate(adapter.getSelectedDate()); // Assuming the adapter has a method to get the selected date
            return; // Exit the method early
        }

        // Otherwise, filter the list based on the query
        List<Map<String, Object>> filteredResults = new ArrayList<>();
        for (Map<String, Object> item : filteredList) {
            String name = (String) item.get("name"); // Assuming there's a "name" field
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                filteredResults.add(item);
            }
        }

        filteredList.clear();

        filterInventoryByDate(filteredResults);
    }

    private boolean isInventoryDataLoaded = false;

    // Method to get today's date in Firestore format
    private String getTodayDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    // Method to get yesterday's date in Firestore format
    private String getYesterdayDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1); // Subtract one day
        return dateFormat.format(calendar.getTime());
    }

    // Method to format a given date string into Firestore format
    private String formatDateForFirestore(String inputDate) {
        SimpleDateFormat firestoreFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat inputFormat1 = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); // e.g., 10/07/2024
        SimpleDateFormat inputFormat2 = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());   // e.g., 10/7/2024

        try {
            // Try parsing with the first format
            return firestoreFormat.format(inputFormat1.parse(inputDate));
        } catch (Exception e1) {
            try {
                // If the first format fails, try the second format
                return firestoreFormat.format(inputFormat2.parse(inputDate));
            } catch (Exception e2) {
                // Return the original input if parsing fails
                Log.e("Date Parsing Error", "Error parsing input date: " + e2.getMessage());
                return inputDate;
            }
        }
    }

    // Method to get the previous date in Firestore format based on a given date string
    private String getPreviousDate(String currentDate) {
        SimpleDateFormat firestoreFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(firestoreFormat.parse(currentDate));
            calendar.add(Calendar.DAY_OF_MONTH, -1); // Subtract one day
            return firestoreFormat.format(calendar.getTime());
        } catch (Exception e) {
            Log.e("Date Parsing Error", "Error parsing current date: " + e.getMessage());
            return currentDate; // Return the original date if parsing fails
        }
    }


    private void loadUsedItemsForDate(String selectedDate) {
        // Hide loading indicator when task completes

        if (selectedDate == null || selectedDate.isEmpty()) {
            selectedDate = getYesterdayDate();
            Log.d("Date Selection", "No date selected, using yesterday's date: " + selectedDate);
        } else {
            Log.d("Date Selection", "Selected date: " + selectedDate);
        }

        String formattedDate = formatDateForFirestore(selectedDate);
        Log.d("Date Formatting", "Formatted date for Firestore: " + formattedDate);

        filteredList.clear(); // Clear the filtered list before loading new data
        Log.d("Inventory Update", "Cleared filtered list.");

        db.collection("inventory")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Firestore Query", "Successfully fetched inventory data.");

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("Firestore Inventory", "Document ID: " + document.getId() + ", Data: " + document.getData());

                            // Start searching with today's date
                            fetchDailyRecordForDate(document, formattedDate);
                            showLoading(false);
                        }
                    } else {
                        Log.e("Firestore Error", "Error fetching inventory: " + task.getException());
                    }
                });
    }

    private void fetchDailyRecordForDate(QueryDocumentSnapshot document, String date) {
        DocumentReference dailyRecordRef = document.getReference().collection("dailyRecords").document(date);
        //Log.d("Firestore Query", "Fetching daily record for date: " + date + " in inventory ID: " + document.getId());

        dailyRecordRef.get().addOnCompleteListener(innerTask -> {
            if (innerTask.isSuccessful() && innerTask.getResult() != null) {
                DocumentSnapshot dailyDoc = innerTask.getResult();
                if (dailyDoc.exists()) {
                    long used = dailyDoc.getLong("in_use") != null ? dailyDoc.getLong("in_use") : 0;
                    long stocks = dailyDoc.getLong("stocks") != null ? dailyDoc.getLong("stocks") : 0;

                    if (used > 0 || date.equals(getTodayDate())) { // Check non-zero in_use or today's date
                        //Log.d("Firestore Daily Record", "Valid daily record for " + date + ": in_use = " + used + ", stocks = " + stocks);

                        // Prepare data for display
                        Map<String, Object> combinedData = new HashMap<>(document.getData());
                        combinedData.put("id", document.getId());
                        combinedData.put("quantity", stocks);
                        combinedData.put("used", used);
                        combinedData.put("date", date);

                        // Update filtered list and UI
                        filteredList.add(combinedData);
                        //Log.d("Inventory Update", "Filtered list updated with data: " + combinedData);
                        filterInventoryByDate(filteredList);
                    } else {
                        // If in_use is zero and not today's date, check the previous date
                        String previousDate = getPreviousDate(date);
                        //Log.d("Date Check", "in_use is zero or no data found. Checking previous date: " + previousDate);
                        fetchDailyRecordForDate(document, previousDate);
                    }
                } else {
                    //Log.e("Firestore Error", "No daily record found for " + date + " in inventory ID: " + document.getId());
                    String previousDate = getPreviousDate(date);
                    //Log.d("Date Check", "No record found. Checking previous date: " + previousDate);
                    fetchDailyRecordForDate(document, previousDate);
                }
            } else {
                //Log.e("Firestore Error", "Error fetching daily record: " + innerTask.getException());
            }
        });

    }

    private void filterInventoryByDate(List<Map<String, Object>> filteredByDate) {
        // Sort the filtered items with letters first and numbers at the end
        Collections.sort(filteredByDate, (a, b) -> {
            String nameA = (String) a.get("name");
            String nameB = (String) b.get("name");

            if (nameA == null) nameA = "";
            if (nameB == null) nameB = "";

            // Check if names start with a letter or a number
            boolean isLetterA = Character.isLetter(nameA.isEmpty() ? ' ' : nameA.charAt(0));
            boolean isLetterB = Character.isLetter(nameB.isEmpty() ? ' ' : nameB.charAt(0));

            if (isLetterA && !isLetterB) {
                return -1; // Letters before numbers
            } else if (!isLetterA && isLetterB) {
                return 1; // Numbers after letters
            } else {
                // Case-insensitive comparison for the same category
                return nameA.compareToIgnoreCase(nameB);
            }
        });

        // Update the adapter with the sorted and filtered items
        adapter.updateList(filteredByDate);
        adapter.notifyDataSetChanged();
    }


    private void showRemoveQuantityDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quantity_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText etProductQuantity = dialogView.findViewById(R.id.etProductQuantity);
        Button btnRemoveQuantity = dialogView.findViewById(R.id.btnAddProduct);

        btnRemoveQuantity.setOnClickListener(v -> {
            String quantityStr = etProductQuantity.getText().toString();

            if (quantityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                int quantityToRemove = Integer.parseInt(quantityStr);
                showLoading(true);
                removeInUsed(item, quantityToRemove);
                dialog.dismiss();
            }
        });
    }
    private void removeInUsed(Map<String, Object> item, int quantityToRemove) {
        String documentId = (String) item.get("id");
        String todayDate = getTodayDate();
        Log.d("Remove Stocks", "Document ID: " + documentId + ", Today's Date: " + todayDate + ", Quantity to remove: " + quantityToRemove);

        if (documentId != null) {
            // Retrieve all daily records
            db.collection("inventory").document(documentId)
                    .collection("dailyRecords")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();

                            // Sort documents by date (descending) in code
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                            documents.sort((doc1, doc2) -> {
                                try {
                                    return dateFormat.parse(doc2.getString("date"))
                                            .compareTo(dateFormat.parse(doc1.getString("date")));
                                } catch (Exception e) {
                                    Log.e("Date Sorting Error", "Error parsing dates: " + e.getMessage());
                                    return 0;
                                }
                            });

                            // Process the latest document (first document after sorting)
                            if (!documents.isEmpty()) {
                                DocumentSnapshot latestDoc = documents.get(0);

                                // Get the latest stock and in_use values
                                int latestStocks = latestDoc.getLong("stocks") != null ? latestDoc.getLong("stocks").intValue() : 0;
                                int latestInUse = latestDoc.getLong("in_use") != null ? latestDoc.getLong("in_use").intValue() : 0;

                                // Retrieve today's record
                                db.collection("inventory").document(documentId)
                                        .collection("dailyRecords").document(todayDate)
                                        .get().addOnCompleteListener(todayTask -> {
                                            if (todayTask.isSuccessful() && todayTask.getResult() != null) {
                                                DocumentSnapshot todaySnapshot = todayTask.getResult();

                                                // If there's a record for today
                                                if (todaySnapshot.exists()) {
                                                    int todayUsed = todaySnapshot.getLong("in_use") != null ? todaySnapshot.getLong("in_use").intValue() : 0;

                                                    // Ensure there is enough stock to remove
                                                    if (quantityToRemove > todayUsed) {
                                                        Toast.makeText(getContext(), "Not enough used available to remove", Toast.LENGTH_SHORT).show();
                                                        showLoading(false);
                                                        return;
                                                    }

                                                    // Update today's stock
                                                    int newTodayUsed = todayUsed - quantityToRemove;


                                                    // Prepare the update fields
                                                    Map<String, Object> updateFields = new HashMap<>();
                                                    updateFields.put("in_use", newTodayUsed);

                                                    // Update today's stock in Firestore
                                                    db.collection("inventory").document(documentId).collection("dailyRecords")
                                                            .document(todayDate).update(updateFields)
                                                            .addOnCompleteListener(updateTask -> {
                                                                if (updateTask.isSuccessful()) {
                                                                    Toast.makeText(getContext(), "Used updated successfully for today", Toast.LENGTH_SHORT).show();

                                                                    // Update the item in the filtered list
                                                                    for (Map<String, Object> inventoryItem : filteredList) {
                                                                        if (inventoryItem.get("id").equals(documentId)) {
                                                                            inventoryItem.put("used", newTodayUsed);  // Update the "used" value
                                                                            break;
                                                                        }
                                                                    }

                                                                    // Notify the adapter about the item change
                                                                    adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                    showLoading(false);
                                                                } else {
                                                                    Toast.makeText(getContext(), "Failed to update used: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    showLoading(false);
                                                                }
                                                            });
                                                } else {
                                                    // If no record for today, create a new one based on the latest available values


                                                    // Ensure stocks do not go negative when creating today's record
                                                    if (quantityToRemove > latestInUse) {
                                                        Toast.makeText(getContext(), "Not enough used available to proceed", Toast.LENGTH_SHORT).show();
                                                        showLoading(false);
                                                        return;
                                                    }
                                                    int newUsed = latestInUse - quantityToRemove; // Ensure stocks don't go negative
                                                    // Create a new record for today, including the latest in_use value
                                                    Map<String, Object> newRecord = new HashMap<>();
                                                    newRecord.put("date", todayDate);
                                                    newRecord.put("stocks", latestStocks);
                                                    newRecord.put("in_use", newUsed); // Add the latest in_use value

                                                    // Set the new document in Firestore
                                                    db.collection("inventory").document(documentId).collection("dailyRecords")
                                                            .document(todayDate).set(newRecord)
                                                            .addOnCompleteListener(createTask -> {
                                                                if (createTask.isSuccessful()) {
                                                                    Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                    // Update the item in the filtered list
                                                                    for (Map<String, Object> inventoryItem : filteredList) {
                                                                        if (inventoryItem.get("id").equals(documentId)) {
                                                                            inventoryItem.put("used", newUsed);  // Update the "used" value
                                                                            break;
                                                                        }
                                                                    }

                                                                    // Notify the adapter about the item change
                                                                    adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                    showLoading(false);
                                                                } else {
                                                                    Toast.makeText(getContext(), "Failed to removed used record: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    showLoading(false);
                                                                }
                                                            });
                                                }
                                            } else {
                                                Toast.makeText(getContext(), "Failed to retrieve today's record: " + todayTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                showLoading(false);
                                            }
                                        });
                            } else {
                                Toast.makeText(getContext(), "No used records found", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to retrieve records: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRemoveStocks(Map<String, Object> item) {
        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quantity_product, null);

        // Create the AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setView(dialogView);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Find the EditText and Button in the dialog layout
        EditText etProductQuantity = dialogView.findViewById(R.id.etProductQuantity);
        Button btnUpdateQuantity = dialogView.findViewById(R.id.btnAddProduct);

        // Set the button click listener
        btnUpdateQuantity.setOnClickListener(v -> {
            String quantityStr = etProductQuantity.getText().toString();

            if (quantityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out the field", Toast.LENGTH_SHORT).show();
            } else {
                int quantityToRemove = Integer.parseInt(quantityStr);
                // Call the method to remove stocks
                showLoading(true);
                onRemoveStocks(item, quantityToRemove);
                dialog.dismiss();
            }
        });
    }

    private void onRemoveStocks(Map<String, Object> item, int quantityToRemove) {
        String documentId = (String) item.get("id");
        String todayDate = getTodayDate();
        Log.d("Remove Stocks", "Document ID: " + documentId + ", Today's Date: " + todayDate + ", Quantity to remove: " + quantityToRemove);

        if (documentId != null) {
            // Retrieve all daily records
            db.collection("inventory").document(documentId)
                    .collection("dailyRecords")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();

                            // Sort documents by date (descending) in code
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                            documents.sort((doc1, doc2) -> {
                                try {
                                    return dateFormat.parse(doc2.getString("date"))
                                            .compareTo(dateFormat.parse(doc1.getString("date")));
                                } catch (Exception e) {
                                    Log.e("Date Sorting Error", "Error parsing dates: " + e.getMessage());
                                    return 0; // If parsing fails, consider them equal
                                }
                            });

// Process the latest document (first document after sorting)
                            if (!documents.isEmpty()) {
                                DocumentSnapshot latestDoc = documents.get(0);

                                // Get the latest stock and in_use values
                                int latestStocks = latestDoc.getLong("stocks") != null ? latestDoc.getLong("stocks").intValue() : 0;
                                int latestInUse = latestDoc.getLong("in_use") != null ? latestDoc.getLong("in_use").intValue() : 0;
                                Log.d("Remove Stocks", "Latest stocks: " + latestDoc);
                                // Retrieve today's record
                                db.collection("inventory").document(documentId)
                                        .collection("dailyRecords").document(todayDate)
                                        .get().addOnCompleteListener(todayTask -> {
                                            if (todayTask.isSuccessful() && todayTask.getResult() != null) {
                                                DocumentSnapshot todaySnapshot = todayTask.getResult();

                                                // If there's a record for today
                                                if (todaySnapshot.exists()) {
                                                    int todayStocks = todaySnapshot.getLong("stocks") != null ? todaySnapshot.getLong("stocks").intValue() : 0;

                                                    // Ensure there is enough stock to remove
                                                    if (quantityToRemove > todayStocks) {
                                                        Toast.makeText(getContext(), "Not enough stock available to remove", Toast.LENGTH_SHORT).show();
                                                        showLoading(false);
                                                        return;
                                                    }

                                                    // Update today's stock
                                                    int newTodayStocks = todayStocks - quantityToRemove;

                                                    // Prepare the update fields
                                                    Map<String, Object> updateFields = new HashMap<>();
                                                    updateFields.put("stocks", newTodayStocks);

                                                    // Update today's stock in Firestore
                                                    db.collection("inventory").document(documentId).collection("dailyRecords")
                                                            .document(todayDate).update(updateFields)
                                                            .addOnCompleteListener(updateTask -> {
                                                                if (updateTask.isSuccessful()) {
                                                                    Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                    // Update the item in the filtered list
                                                                    for (Map<String, Object> inventoryItem : filteredList) {
                                                                        if (inventoryItem.get("id").equals(documentId)) {
                                                                            inventoryItem.put("quantity", newTodayStocks);  // Update the "used" value
                                                                            break;
                                                                        }
                                                                    }

                                                                    // Notify the adapter about the item change
                                                                    adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                    showLoading(false);
                                                                } else {
                                                                    Toast.makeText(getContext(), "Failed to update stock: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    showLoading(false);
                                                                }
                                                            });
                                                } else {
                                                    // If no record for today, create a new one based on the latest available values
                                                    int newStocks = latestStocks - quantityToRemove; // Ensure stocks don't go negative

                                                    // Ensure stocks do not go negative when creating today's record
                                                    if (quantityToRemove > latestStocks) {
                                                        Toast.makeText(getContext(), "Not enough stock available to proceed", Toast.LENGTH_SHORT).show();
                                                        showLoading(false);
                                                        return;
                                                    }

                                                    // Create a new record for today, including the latest in_use value
                                                    Map<String, Object> newRecord = new HashMap<>();
                                                    newRecord.put("date", todayDate);
                                                    newRecord.put("stocks", newStocks);
                                                    newRecord.put("in_use", latestInUse); // Add the latest in_use value

                                                    // Set the new document in Firestore
                                                    db.collection("inventory").document(documentId).collection("dailyRecords")
                                                            .document(todayDate).set(newRecord)
                                                            .addOnCompleteListener(createTask -> {
                                                                if (createTask.isSuccessful()) {
                                                                    Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                    // Update the item in the filtered list
                                                                    for (Map<String, Object> inventoryItem : filteredList) {
                                                                        if (inventoryItem.get("id").equals(documentId)) {
                                                                            inventoryItem.put("quantity", newStocks);  // Update the "used" value
                                                                            break;
                                                                        }
                                                                    }

                                                                    // Notify the adapter about the item change
                                                                    adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                    showLoading(false);
                                                                } else {
                                                                    Toast.makeText(getContext(), "Failed to remove stock record: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    showLoading(false);
                                                                }
                                                            });
                                                }
                                            } else {
                                                Toast.makeText(getContext(), "Failed to retrieve today's record: " + todayTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                showLoading(false);
                                            }
                                        });
                            } else {
                                Toast.makeText(getContext(), "No stock records found", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to retrieve records: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }

    private void showAddQuantityDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quantity_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
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
                showLoading(true);
                addQuantityToFirestore(item, quantityToAdd); // Updated to include inUseToAdd
                dialog.dismiss();
            }
        });
    }
    //
    private void showLoading(boolean isLoading) {
        // This assumes you have a ProgressBar defined in your layout
        ProgressBar progressBar = getActivity().findViewById(R.id.progressBar);

        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE); // Show the progress bar
        } else {
            progressBar.setVisibility(View.GONE); // Hide the progress bar
        }
    }

    private void showUpdateQuantityDialog(Map<String, Object> item) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quantity_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
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
                // Show loading indicator
                showLoading(true);

                // Choose action (subtract from stocks and add to in_use)
                updateStocksAndInUse(item, quantityToUpdate, dialog);
                dialog.dismiss();
            }
        });
    }

    // code to use stocks
    private void updateStocksAndInUse(Map<String, Object> item, int quantityToUpdate, AlertDialog dialog) {
        String documentId = (String) item.get("id");
        String todayDate = getTodayDate();

        // Retrieve all daily records
        db.collection("inventory").document(documentId)
                .collection("dailyRecords")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();

                        // Sort documents by date (descending)
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                        documents.sort((doc1, doc2) -> {
                            try {
                                return dateFormat.parse(doc2.getString("date"))
                                        .compareTo(dateFormat.parse(doc1.getString("date")));
                            } catch (Exception e) {
                                Log.e("Date Sorting Error", "Error parsing dates: " + e.getMessage());
                                return 0;
                            }
                        });

                        // Process the latest document
                        if (!documents.isEmpty()) {
                            DocumentSnapshot latestDoc = documents.get(0);
                            int latestStocks = latestDoc.getLong("stocks") != null ? latestDoc.getLong("stocks").intValue() : 0;
                            int latestInUse = latestDoc.getLong("in_use") != null ? latestDoc.getLong("in_use").intValue() : 0;
                            Log.d("Latest Date: ", "Date: " + latestDoc + ", Stocks: " + latestStocks + ", In Use: " + latestInUse);
                            // Ensure there is enough stock to update


                            // Retrieve today's record
                            db.collection("inventory").document(documentId)
                                    .collection("dailyRecords").document(todayDate)
                                    .get()
                                    .addOnCompleteListener(todayTask -> {
                                        if (todayTask.isSuccessful() && todayTask.getResult() != null) {
                                            DocumentSnapshot todaySnapshot = todayTask.getResult();

                                            int todayStocks = todaySnapshot.getLong("stocks") != null ? todaySnapshot.getLong("stocks").intValue() : 0;
                                            int todayInUse = todaySnapshot.getLong("in_use") != null ? todaySnapshot.getLong("in_use").intValue() : 0;
                                            int newTodayStocks2 = todayStocks - quantityToUpdate;
                                            int newInUse2 = todayInUse + quantityToUpdate;
                                            // If there's a record for today
                                            if (todaySnapshot.exists()) {
                                                // Prepare the update fields for today's record
                                                Map<String, Object> updateFields = new HashMap<>();
                                                updateFields.put("stocks", newTodayStocks2); // Update stocks
                                                updateFields.put("in_use", newInUse2); // Update in_use quantity

                                                // Update today's record in Firestore
                                                db.collection("inventory").document(documentId)
                                                        .collection("dailyRecords")
                                                        .document(todayDate).update(updateFields)
                                                        .addOnCompleteListener(updateTask -> {
                                                            if (updateTask.isSuccessful()) {
                                                                Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                // Update the item in the filtered list
                                                                for (Map<String, Object> inventoryItem : filteredList) {
                                                                    if (inventoryItem.get("id").equals(documentId)) {
                                                                        inventoryItem.put("quantity", newTodayStocks2);
                                                                        inventoryItem.put("used", newInUse2);
                                                                        break;
                                                                    }
                                                                }

                                                                // Notify the adapter about the item change
                                                                adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                showLoading(false);
                                                            } else {
                                                                Toast.makeText(getContext(), "Failed to update stock: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                showLoading(false);
                                                            }
                                                        });
                                            } else {
                                                if (quantityToUpdate > latestStocks) {
                                                    Toast.makeText(getContext(), "Not enough stock available", Toast.LENGTH_SHORT).show();
                                                    showLoading(false);
                                                    return;
                                                }

                                                // Calculate new stocks and in_use
                                                int newTodayStocks = latestStocks - quantityToUpdate;
                                                int newInUse = latestInUse + quantityToUpdate;
                                                // If no record for today, create a new one
                                                Map<String, Object> newRecord = new HashMap<>();
                                                newRecord.put("date", todayDate);
                                                newRecord.put("stocks", newTodayStocks); // Use the latest stocks
                                                newRecord.put("in_use", newInUse); // Use the new in-use value

                                                // Set the new document in Firestore
                                                db.collection("inventory").document(documentId)
                                                        .collection("dailyRecords")
                                                        .document(todayDate).set(newRecord)
                                                        .addOnCompleteListener(createTask -> {
                                                            if (createTask.isSuccessful()) {
                                                                Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                // Update the item in the filtered list
                                                                for (Map<String, Object> inventoryItem : filteredList) {
                                                                    if (inventoryItem.get("id").equals(documentId)) {
                                                                        inventoryItem.put("quantity", newTodayStocks);
                                                                        inventoryItem.put("used", newInUse);
                                                                        break;
                                                                    }
                                                                }

                                                                // Notify the adapter about the item change
                                                                adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                showLoading(false);
                                                            } else {
                                                                Toast.makeText(getContext(), "Failed to create stock record: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                showLoading(false);
                                                            }
                                                        });
                                            }
                                        } else {
                                            Toast.makeText(getContext(), "Failed to retrieve today's record: " + todayTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            showLoading(false);
                                        }
                                    });
                        } else {
                            Toast.makeText(getContext(), "No stock records found", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to retrieve records: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }


    // add stocks i guess
    private void addQuantityToFirestore(Map<String, Object> item, int quantityToAdd) {
        String documentId = (String) item.get("id");
        String todayDate = getTodayDate();
        String yesterdayDate = getYesterdayDate();

        if (documentId != null) {
            // Retrieve all daily records
            db.collection("inventory").document(documentId)
                    .collection("dailyRecords")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();

                            // Sort documents by date (descending) in code
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                            documents.sort((doc1, doc2) -> {
                                try {
                                    return dateFormat.parse(doc2.getString("date"))
                                            .compareTo(dateFormat.parse(doc1.getString("date")));
                                } catch (Exception e) {
                                    Log.e("Date Sorting Error", "Error parsing dates: " + e.getMessage());
                                    return 0;
                                }
                            });

                            // Process the latest document (first document after sorting)
                            if (!documents.isEmpty()) {
                                DocumentSnapshot latestDoc = documents.get(0);

                                // Get yesterday's stock and in_use values from the latest document
                                int yesterdayStocks = latestDoc.getLong("stocks") != null ? latestDoc.getLong("stocks").intValue() : 0;
                                int yesterdayInUse = latestDoc.getLong("in_use") != null ? latestDoc.getLong("in_use").intValue() : 0;

                                // Retrieve today's record
                                db.collection("inventory").document(documentId)
                                        .collection("dailyRecords").document(todayDate)
                                        .get().addOnCompleteListener(todayTask -> {
                                            if (todayTask.isSuccessful() && todayTask.getResult() != null) {
                                                DocumentSnapshot todaySnapshot = todayTask.getResult();

                                                // If there's a record for today
                                                if (todaySnapshot.exists()) {
                                                    // Get today's stock and in_use values
                                                    int todayStocks = todaySnapshot.getLong("stocks") != null ? todaySnapshot.getLong("stocks").intValue() : 0;
                                                    int todayInUse = todaySnapshot.getLong("in_use") != null ? todaySnapshot.getLong("in_use").intValue() : 0;

                                                    // Check if the date is today before modifying
                                                    if (todayDate.equals(getTodayDate())) {
                                                        // Update today's stock
                                                        int updatedTodayStocks = todayStocks + quantityToAdd;

                                                        // Prepare the update
                                                        Map<String, Object> updateFields = new HashMap<>();
                                                        updateFields.put("stocks", updatedTodayStocks);


                                                        // Update Firestore document for today
                                                        db.collection("inventory").document(documentId).collection("dailyRecords")
                                                                .document(todayDate).update(updateFields)
                                                                .addOnCompleteListener(updateTask -> {
                                                                    if (updateTask.isSuccessful()) {
                                                                        Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                        // Update the item in the filtered list
                                                                        for (Map<String, Object> inventoryItem : filteredList) {
                                                                            if (inventoryItem.get("id").equals(documentId)) {
                                                                                inventoryItem.put("quantity", updatedTodayStocks);
                                                                                break;
                                                                            }
                                                                        }

                                                                        // Notify the adapter about the item change
                                                                        adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                        showLoading(false);
                                                                    } else {
                                                                        Toast.makeText(getContext(), "Failed to update stock: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                        showLoading(false);
                                                                    }
                                                                });
                                                    } else {
                                                        Toast.makeText(getContext(), "Cannot edit data for previous dates", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    // If no record for today, create a new one based on the latest available values
                                                    int updatedTodayStocks = yesterdayStocks + quantityToAdd; // Use yesterday's stock as the base

                                                    // Create a new record for today
                                                    Map<String, Object> newRecord = new HashMap<>();
                                                    newRecord.put("date", todayDate);
                                                    newRecord.put("stocks", updatedTodayStocks);
                                                    newRecord.put("in_use", yesterdayInUse);

                                                    // Set the new document in Firestore
                                                    db.collection("inventory").document(documentId).collection("dailyRecords")
                                                            .document(todayDate).set(newRecord)
                                                            .addOnCompleteListener(createTask -> {
                                                                if (createTask.isSuccessful()) {
                                                                    Toast.makeText(getContext(), "Used removed for today successfully", Toast.LENGTH_SHORT).show();

                                                                    // Update the item in the filtered list
                                                                    for (Map<String, Object> inventoryItem : filteredList) {
                                                                        if (inventoryItem.get("id").equals(documentId)) {
                                                                            inventoryItem.put("quantity", updatedTodayStocks);
                                                                            break;
                                                                        }
                                                                    }

                                                                    // Notify the adapter about the item change
                                                                    adapter.notifyItemChanged(filteredList.indexOf(item));
                                                                    showLoading(false);
                                                                } else {
                                                                    Toast.makeText(getContext(), "Failed to create stock record: " + createTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                    showLoading(false);
                                                                }
                                                            });
                                                }
                                            } else {
                                                Toast.makeText(getContext(), "Failed to retrieve today's record: " + todayTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                showLoading(false);
                                            }
                                        });
                            } else {
                                Toast.makeText(getContext(), "No stock records found", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to retrieve records: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }


    private void showAddProductDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_product, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
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
            // Resize the image
            try {
                // Load the image as a Bitmap
                Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);

                // Calculate the desired width and height
                int desiredWidth = 200; // Set your desired width
                int desiredHeight = 200; // Set your desired height

                // Resize the Bitmap
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, desiredWidth, desiredHeight, false);

                // Convert the Bitmap to a ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // You can adjust the quality (0-100)
                byte[] data = baos.toByteArray();

                // Upload the resized image to Firebase Storage
                StorageReference ref = FirebaseStorage.getInstance().getReference("images/" + UUID.randomUUID().toString());
                UploadTask uploadTask = ref.putBytes(data);

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
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error resizing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

                            // Track the initial stocks in the dailyRecords collection
                            Map<String, Object> dailyRecord = new HashMap<>();
                            dailyRecord.put("date", getTodayDate());
                            dailyRecord.put("stocks", Long.parseLong(stocks));
                            dailyRecord.put("in_use", 0);

                            // Create a document for today in the dailyRecords subcollection
                            productRef.collection("dailyRecords").document(getTodayDate()).set(dailyRecord)
                                    .addOnCompleteListener(recordTask -> {
                                        if (recordTask.isSuccessful()) {
                                            // If the record is successfully added to Firestore
                                            Toast.makeText(getContext(), "Product added successfully", Toast.LENGTH_SHORT).show();

                                            // Update the local list with the new product
                                            Map<String, Object> newProduct = new HashMap<>(product);
                                            newProduct.put("id", productRef.getId());
                                            newProduct.put("quantity", Long.parseLong(stocks));
                                            newProduct.put("used", 0);
                                            newProduct.put("date", getTodayDate());

                                            // Add the new product to filteredList
                                            filteredList.add(newProduct);

                                            filterInventoryByDate(filteredList);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
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
                showLoading(true);
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
                            // Update the local list with the new name
                            item.put("name", newProductName);

                            // Notify the adapter that the name has changed
                            adapter.updateList(filteredList);
                            adapter.notifyDataSetChanged();

                            Toast.makeText(getContext(), "Product name updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to update name: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "Document ID is missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog(Map<String, Object> item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme);
        builder.setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + item.get("name") + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete the product from Firestore
                    showLoading(true);
                    deleteProductFromFirestore(item);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteProductFromFirestore(Map<String, Object> item) {
        String documentId = (String) item.get("id");
        if (documentId != null) {
            db.collection("inventory").document(documentId).collection("dailyRecords")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot subDoc : task.getResult()) {
                                db.collection("inventory").document(documentId)
                                        .collection("dailyRecords").document(subDoc.getId()).delete();
                            }
                            db.collection("inventory").document(documentId)
                                    .delete()
                                    .addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            // Remove the item from local list
                                            filteredList.remove(item);

                                            // Notify the adapter to refresh the UI
                                            adapter.updateList(filteredList);
                                            adapter.notifyDataSetChanged();

                                            Toast.makeText(getContext(), "Deleted: " + item.get("name"), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to delete: " + deleteTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(getContext(), "Failed to retrieve sub-collection: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
