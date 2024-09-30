package com.example.tropics_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.InventoryViewHolder> {

    private final Context context;
    private List<Map<String, Object>> customerList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> customerData);
    }

    public CustomerAdapter(Context context, List<Map<String, Object>> inventoryList, OnItemClickListener listener) {
        this.context = context;
        this.customerList = new ArrayList<>(inventoryList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_customer_layout, parent, false);
        return new InventoryViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Map<String, Object> item = customerList.get(position);
        String name = (String) item.get("fullName");

        holder.tvName.setText(name);

        // Set the click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item); // Pass the item data to the listener
            }
        });
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

    public void updateList(List<Map<String, Object>> newList) {
        customerList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
        }
    }
}
