package com.example.tropics_app;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private final List<Appointment> appointmentList;
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;

    // Constructor
    public AppointmentAdapter(List<Appointment> appointmentList) {
        this.appointmentList = appointmentList;
    }

    public AppointmentAdapter(List<Appointment> appointmentList, OnItemClickListener listener) {
        this.appointmentList = appointmentList;
        this.itemClickListener = listener; // Store the click listener
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

        // Set text values
        holder.tvFullName.setText(appointment.getFullName());
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Example: "2024-01-27"
        SimpleDateFormat inputTimeFormat = new SimpleDateFormat("HH:mm");      // Example: "15:30" (24-hour format)

        // Desired output formats
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy"); // Example: "Jan 27, 2024"
        SimpleDateFormat outputTimeFormat = new SimpleDateFormat("hh:mm a");      // Example: "03:30 PM"

        try {
            // Parse the date and time strings from the Appointment object
            String appointmentDateStr = appointment.getDate(); // "2024-01-27"
            String appointmentTimeStr = appointment.getTime(); // "15:30"

            Date appointmentDate = inputDateFormat.parse(appointmentDateStr);
            Date appointmentTime = inputTimeFormat.parse(appointmentTimeStr);

            // Format the Date objects to the desired format and set them to TextViews
            holder.tvDate.setText(outputDateFormat.format(appointmentDate));
            holder.tvTime.setText(outputTimeFormat.format(appointmentTime));

        } catch (ParseException e) {
            e.printStackTrace(); // Handle the exception if parsing fails
            holder.tvDate.setText("Invalid Date");
            holder.tvTime.setText("Invalid Time");
        }

        // Handle item long press event
        holder.itemView.setOnLongClickListener(v -> {
            if (itemLongClickListener != null) {
                Log.d("Adapter", "Item long pressed: " + appointment.getFullName()); // Debug log for long press
                itemLongClickListener.onItemLongClick(appointment); // Pass appointment to listener
                return true;
            }
            return false;
        });

        // Handle item click event
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                Log.d("Adapter", "Item clicked: " + appointment.getFullName()); // Debug log for click
                itemClickListener.onItemClick(appointment); // Pass appointment to listener
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Appointment appointment);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Appointment appointment);
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvDate, tvTime, tvCreatedDateTime, tvPhone;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
          //  tvCreatedDateTime = itemView.findViewById(R.id.timecreated); // Make sure this is defined in item_appointment.xml
            tvPhone = itemView.findViewById(R.id.tvPhone); // Make sure this is defined in item_appointment.xml
        }
    }
}
