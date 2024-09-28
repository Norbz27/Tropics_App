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

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private List<Appointment> appointmentList;

    public AppointmentAdapter(List<Appointment> appointmentList) {
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
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

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvDate, tvTime, tvTotalPrice, tvCreatedDateTime;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvCreatedDateTime = itemView.findViewById(R.id.tvCreatedDateTime);
        }
    }
}
