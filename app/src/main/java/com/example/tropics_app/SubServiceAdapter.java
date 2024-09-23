package com.example.tropics_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class SubServiceAdapter extends RecyclerView.Adapter<SubServiceAdapter.ViewHolder> {
    private List<Map<String, Object>> serviceList;
    private OnItemClickListener listener;
    private boolean removeDrawable;

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> service);
    }

    public SubServiceAdapter(Context context, List<Map<String, Object>> serviceList, OnItemClickListener listener, boolean removeDrawable) {
        this.serviceList = serviceList;
        this.listener = listener;
        this.removeDrawable = removeDrawable;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_sub_service_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> service = serviceList.get(position);
        holder.bind(service, listener);
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    public void updateList(List<Map<String, Object>> newServiceList) {
        this.serviceList = newServiceList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tvService);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }

        void bind(Map<String, Object> service, OnItemClickListener listener) {
            tvServiceName.setText((String) service.get("sub_service_name"));
            double price = (double) service.get("price");

            if (removeDrawable) {
                tvPrice.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }

            if (price != 0.0) {
                tvPrice.setText(String.valueOf(price));
            } else {
                tvPrice.setText("");
            }

            itemView.setOnClickListener(v -> listener.onItemClick(service));
        }

    }
}
