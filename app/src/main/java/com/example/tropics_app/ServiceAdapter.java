package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private final Context context;
    private List<Map<String, Object>> serviceList;
    private final OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener; // Custom long click listener

    public ServiceAdapter(Context context, List<Map<String, Object>> inventoryList, OnItemClickListener listener) {
        this.context = context;
        this.serviceList = new ArrayList<>(inventoryList);
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.onItemLongClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_service_layout, parent, false);
        return new ServiceViewHolder(view, onItemClickListener);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Map<String, Object> item = serviceList.get(position);
        String name = (String) item.get("service_name");
        holder.tvService.setText(name);

        // Handle click events
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(item));

        // Handle long click events to show the popup menu
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                showPopupMenu(v, holder.getAdapterPosition());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void updateList(List<Map<String, Object>> newList) {
        serviceList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView tvService;

        public ServiceViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            tvService = itemView.findViewById(R.id.tvService);
        }
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(context, view);

        // Set the gravity to the right
        popupMenu.setGravity(Gravity.END); // Use Gravity.RIGHT or Gravity.END based on your needs

        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.inventory_item_menu, popupMenu.getMenu());

        // Get the menu object and hide specific items
        Menu menu = popupMenu.getMenu();

        // Hide the 'add' and 'subtract' menu items if needed
        menu.findItem(R.id.action_add).setVisible(false);  // To hide 'add'
        menu.findItem(R.id.action_subtract).setVisible(false);  // To hide 'subtract'
        menu.findItem(R.id.action_removestocks).setVisible(false);  // To hide 'subtract'
        menu.findItem(R.id.action_remove).setVisible(false);  // To hide ''

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> permissions = (Map<String, Object>) documentSnapshot.get("permissions");

                    boolean editService = permissions != null && (boolean) permissions.getOrDefault("editService", false);
                    boolean deleteService = permissions != null && (boolean) permissions.getOrDefault("deleteService", false);

                    menu.findItem(R.id.action_edit).setEnabled(editService);
                    menu.findItem(R.id.action_delete).setEnabled(deleteService);

                    popupMenu.setOnMenuItemClickListener(menuItem -> {
                        int id = menuItem.getItemId();

                        if (id == R.id.action_edit) {
                            // Call the edit action
                            if (onItemLongClickListener != null) {
                                onItemLongClickListener.onEditClick(serviceList.get(position));
                            }
                            return true;
                        } else if (id == R.id.action_delete) {
                            // Call the delete action
                            if (onItemLongClickListener != null) {
                                onItemLongClickListener.onDeleteClick(serviceList.get(position));
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

    // Custom interface for long click listener
    public interface OnItemLongClickListener {
        void onEditClick(Map<String, Object> item);
        void onDeleteClick(Map<String, Object> item);
    }

    // Interface for handling regular item clicks
    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> item);
    }
}
