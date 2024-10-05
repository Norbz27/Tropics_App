package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private final Context context;
    private List<Map<String, Object>> inventoryList;
    private List<Map<String, Object>> filteredList; // Declare the filteredList here
    private OnItemLongClickListener longClickListener;

    public InventoryAdapter(Context context, List<Map<String, Object>> inventoryList) {
        this.context = context;
        this.inventoryList = new ArrayList<>(inventoryList);
        this.filteredList = new ArrayList<>(inventoryList); // Initialize filteredList here
    }

    // Setter for the long click listener
    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
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
        Map<String, Object> item = filteredList.get(position); // Use filteredList instead of inventoryList
        String name = (String) item.get("name");
        String stocks = (String) item.get("stocks");
        String in_use = (String) item.get("in_use");
        String imageUrl = (String) item.get("imageUrl");

        holder.tvName.setText(name);
        holder.tvStocks.setText("Stocks: " + stocks);

        if (stocks.equals("0")) {
            holder.tvStocks.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        } else {
            holder.tvStocks.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        }

        holder.tvInUse.setText("Used: " + in_use);

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgProduct);

        // Check the canEdit property to enable/disable the edit button

        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, holder.getAdapterPosition());
            return true;
        });
    }


    @Override
    public int getItemCount() {
        return filteredList.size(); // Return size of filteredList
    }

    public void updateList(List<Map<String, Object>> newList) {
        filteredList.clear();
        filteredList.addAll(newList);
        notifyDataSetChanged();
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
        popupMenu.setGravity(Gravity.END);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.inventory_item_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.action_edit) {
                if (longClickListener != null) {
                    longClickListener.onEditClick(filteredList.get(position)); // Use filteredList
                }
                return true;
            } else if (id == R.id.action_add) {
                if (longClickListener != null) {
                    longClickListener.onAddClick(filteredList.get(position)); // Use filteredList
                }
                return true;
            } else if (id == R.id.action_subtract) {
                if (longClickListener != null) {
                    longClickListener.onUseClick(filteredList.get(position)); // Use filteredList
                }
                return true;
            } else if (id == R.id.action_delete) {
                if (longClickListener != null) {
                    longClickListener.onDeleteClick(filteredList.get(position)); // Use filteredList
                }
                return true;
            } else {
                return false;
            }
        });

        popupMenu.show();
    }

    // Interface for handling long-clicks with edit and delete actions
    public interface OnItemLongClickListener {
        void onAddClick(Map<String, Object> item);
        void onEditClick(Map<String, Object> item);
        void onUseClick(Map<String, Object> item);
        void onDeleteClick(Map<String, Object> item);
    }
}
