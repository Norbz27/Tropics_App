package com.example.tropics_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Appointmentadapter extends RecyclerView.Adapter<Appointmentadapter.AppointmentViewHolder> {

    private List<Appointment> appointmentList;

    public Appointmentadapter(List<Appointment> appointmentList) {
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false); // Ensure you have an appropriate layout file
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointmentList.get(position);

        // Set fullName, date, and time
        holder.tvFullName.setText(appointment.getFullName());
        holder.tvDate.setText(appointment.getDate());
        holder.tvTime.setText(appointment.getTime());

        // Format and display createdDateTime
        Date createdDate = appointment.getCreatedDateTimeAsDate();
        if (createdDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.tvCreatedDateTime.setText(dateFormat.format(createdDate));
        } else {
            holder.tvCreatedDateTime.setText("Invalid date"); // Handle invalid date case
        }

        // Set totalPrice
        holder.tvTotalPrice.setText(String.format(Locale.getDefault(), "₱%.2f", appointment.getTotalPrice()));
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    // ViewHolder class to hold and bind views
    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvDate, tvTime, tvCreatedDateTime, tvTotalPrice;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Bind views to their corresponding IDs from the layout file
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCreatedDateTime = itemView.findViewById(R.id.tvCreatedDateTime); // Make sure this ID exists in your layout
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice); // Make sure this ID exists in your layout
        }
    }
}
