package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final Context context;
    private List<Map<String, Object>> inventoryList;
    private List<Map<String, Object>> filteredList;
    private OnItemLongClickListener longClickListener;
    private String selectedDate;
    public InventoryAdapter(Context context, List<Map<String, Object>> inventoryList) {
        this.context = context;
        this.inventoryList = new ArrayList<>(inventoryList);
    }

    // Setter for the long click listener
    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }
    public void setSelectedDate(String selectedDate){
        this.selectedDate = selectedDate;
    }
    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_inventory_layout, parent, false);
        return new InventoryViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Map<String, Object> item = inventoryList.get(position);
        String name = (String) item.get("name");
        String stocks = String.valueOf(item.get("quantity")); // Changed to get "quantity"
        String in_use = String.valueOf(item.get("used")); // Changed to get "used"
        String imageUrl = (String) item.get("imageUrl");

        holder.tvName.setText(name);

        // Set the stock text with null check
        holder.tvStocks.setText("Stocks: " + (stocks != null ? stocks : "N/A"));

        // Set color based on stocks value
        if (Integer.parseInt(stocks) <= 3) {
            holder.tvStocks.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark)); // Set to red
        } else {
            holder.tvStocks.setTextColor(ContextCompat.getColor(context, android.R.color.white)); // Set to default color
        }

        // Set the in_use text with null check
        holder.tvInUse.setText("In Use: " + (in_use != null ? in_use : "N/A"));

        // Load the image using Glide with null check for imageUrl
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgProduct);

        // Set the long click listener for each item
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, holder.getAdapterPosition());
            return true;
        });
    }


    @Override
    public int getItemCount() {
        return inventoryList.size();
    }


    public void updateList(List<Map<String, Object>> newList) {
        inventoryList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }
    public String getSelectedDate() {
        return selectedDate;
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStocks, tvInUse;
        ImageView imgProduct;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvStocks = itemView.findViewById(R.id.tvStocks);
            tvInUse = itemView.findViewById(R.id.tvInUse);
            imgProduct = itemView.findViewById(R.id.imageViewLetter);
        }
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);

        // Set the gravity to the right
        popupMenu.setGravity(Gravity.END);

        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.inventory_item_menu, popupMenu.getMenu());
        Menu menu = popupMenu.getMenu();
        Date currentDate = new Date(); // Get current date
        SimpleDateFormat sdf = new SimpleDateFormat("MM/d/yyyy"); // Define the date format
        String formattedDate = sdf.format(currentDate); // Format current date
        Log.d("InventoryAdapter", "Selected Date: " + selectedDate);
        Log.d("InventoryAdapter", "Formatted Current Date: " + formattedDate);
        boolean iTrue = !formattedDate.equals(selectedDate);
        Log.d("InventoryAdapter", "das" + iTrue);

        if (selectedDate != null && !formattedDate.equals(selectedDate)) {
            menu.findItem(R.id.action_edit).setEnabled(false);
            menu.findItem(R.id.action_add).setEnabled(false);
            menu.findItem(R.id.action_subtract).setEnabled(false);
            menu.findItem(R.id.action_delete).setEnabled(false);
            menu.findItem(R.id.action_removestocks).setEnabled(false);
            menu.findItem(R.id.action_remove).setEnabled(false);
        } else {
            menu.findItem(R.id.action_edit).setEnabled(true);
            menu.findItem(R.id.action_add).setEnabled(true);
            menu.findItem(R.id.action_subtract).setEnabled(true);
            menu.findItem(R.id.action_delete).setEnabled(true);
            menu.findItem(R.id.action_removestocks).setEnabled(true);
            menu.findItem(R.id.action_remove).setEnabled(true);

            Log.d("InventoryAdapter", "Trues");
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> permissions = (Map<String, Object>) documentSnapshot.get("permissions");

                    boolean editInventory = permissions != null && (boolean) permissions.getOrDefault("editInventory", false);
                    boolean deleteInventory = permissions != null && (boolean) permissions.getOrDefault("deleteInventory", false);

                    menu.findItem(R.id.action_edit).setEnabled(editInventory);
                    menu.findItem(R.id.action_add).setEnabled(editInventory);
                    menu.findItem(R.id.action_remove).setEnabled(editInventory);
                    menu.findItem(R.id.action_subtract).setEnabled(editInventory);
                    menu.findItem(R.id.action_removestocks).setEnabled(editInventory);
                    menu.findItem(R.id.action_delete).setEnabled(deleteInventory);

                    popupMenu.setOnMenuItemClickListener(menuItem -> {
                        int id = menuItem.getItemId();

                        if (id == R.id.action_edit) {
                            // Call the edit action
                            if (longClickListener != null) {
                                longClickListener.onEditClick(inventoryList.get(position));
                            }
                            return true;
                        } else if (id == R.id.action_add) {
                            // Add stocks
                            if (longClickListener != null) {
                                longClickListener.onAddClick(inventoryList.get(position));
                            }
                            return true;
                        } else if (id == R.id.action_remove) {
                            // Remove stocks
                            if (longClickListener != null) {
                                longClickListener.onRemoveInUsedClick(inventoryList.get(position));
                            }
                            return true;
                        } else if (id == R.id.action_subtract) {
                            // Subtract stocks and add on in use
                            if (longClickListener != null) {
                                longClickListener.onUseClick(inventoryList.get(position));
                            }
                            return true;
                        } else if (id == R.id.action_delete) {
                            // Call the delete action
                            if (longClickListener != null) {
                                longClickListener.onDeleteClick(inventoryList.get(position));
                            }
                            return true;
                        } else if (id == R.id.action_removestocks) {
                            // Call the remove stocks action
                            if (longClickListener != null) {
                                longClickListener.onRemoveStockClick(inventoryList.get(position)); // Assuming you want to call onRemoveinUsed here
                            }
                            return true;
                        } else {
                            return false;
                        }
                    });

                    popupMenu.show();
                }
            });
        }
    }


    // Interface for handling long-clicks with edit and delete actions
    public interface OnItemLongClickListener {
        void onAddClick(Map<String, Object> item);
        void onEditClick(Map<String, Object> item);
        void onRemoveInUsedClick(Map<String, Object> item);
        void onUseClick(Map<String, Object> item);
        void onDeleteClick(Map<String, Object> item);
        void onRemoveStockClick(Map<String, Object> item);
    }
}
