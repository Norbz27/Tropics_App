package com.example.tropics_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class CustomerAppointmentAdapter extends RecyclerView.Adapter<CustomerAppointmentAdapter.CustomerAppointmentAdapterViewHolder> {

    private List<Map<String, Object>> appointmentList;

    public CustomerAppointmentAdapter(List<Map<String, Object>> appointmentList) {
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public CustomerAppointmentAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_customer_rep, parent, false);
        return new CustomerAppointmentAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerAppointmentAdapterViewHolder holder, int position) {
        Map<String, Object> appointment = appointmentList.get(position);

        // Get the services array
        List<Map<String, Object>> services = (List<Map<String, Object>>) appointment.get("services");
        StringBuilder servicesString = new StringBuilder();

        // Extract the parentServiceName from each service
        if (services != null && !services.isEmpty()) {
            for (Map<String, Object> service : services) {
                String parentServiceName = (String) service.get("parentServiceName");
                if (parentServiceName != null) {
                    if (servicesString.length() > 0) {
                        servicesString.append(", "); // Add a comma for separation
                    }
                    servicesString.append(parentServiceName);
                }
            }
        } else {
            servicesString.append("No services availed"); // Fallback message
        }
        Double totalPrice = (Double) appointment.get("totalPrice");
        holder.tvAppointmentDetails.setText(servicesString.toString());
        holder.tvTotalPrice.setText(String.format("₱%.2f", totalPrice));
        holder.tvAppointmentDate.setText((String) appointment.get("date"));
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class CustomerAppointmentAdapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppointmentDetails;
        TextView tvAppointmentDate;
        TextView tvTotalPrice;

        public CustomerAppointmentAdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppointmentDetails = itemView.findViewById(R.id.tvAppointmentDetails);
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
        }
    }
}
