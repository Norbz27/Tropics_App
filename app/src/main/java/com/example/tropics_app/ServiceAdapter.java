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
public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private Context context;
    private List<Map<String, Object>> serviceList;
    private OnItemClickListener onItemClickListener;

    public ServiceAdapter(Context context, List<Map<String, Object>> inventoryList, OnItemClickListener listener) {
        this.context = context;
        this.serviceList = new ArrayList<>(inventoryList);
        this.onItemClickListener = listener;
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

        // You can pass the entire item map here to the listener if needed
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(item));
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

    // Interface for handling clicks
    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> item);
    }
}
