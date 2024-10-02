package com.example.tropics_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
public class AssignedServicesAdapter extends RecyclerView.Adapter<AssignedServicesAdapter.WeekViewHolder> {
    private List<WeekServices> weekServicesList;
    private Set<Integer> expandedWeeks = new HashSet<>();
    private Context context; // Add context as a member variable

    public AssignedServicesAdapter(Context context, List<WeekServices> weekServicesList) {
        this.context = context; // Initialize context
        this.weekServicesList = weekServicesList;
    }

    @Override
    public WeekViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.week_header, parent, false); // Use context here
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WeekViewHolder holder, int position) {
        WeekServices weekServices = weekServicesList.get(position);
        holder.weekLabel.setText(weekServices.getWeekLabel());
        String formattedCommission = String.format(Locale.getDefault(), "₱%.2f", weekServices.getTotalCommission());
        holder.totalCommision.setText("Commission: " + formattedCommission);
        // Check if this week is expanded
        boolean isExpanded = expandedWeeks.contains(position);
        holder.servicesContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Toggle sub-services visibility
        holder.ivExpandIcon.setOnClickListener(v -> {
            if (isExpanded) {
                holder.ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Expand icon
                expandedWeeks.remove(position);
            } else {

                holder.ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Collapse icon
                expandedWeeks.add(position);
            }
            notifyItemChanged(position);
        });
        // Clear previous views
        holder.servicesContainer.removeAllViews();

        // If expanded, add all services
        if (isExpanded) {
            for (AssignedService service : weekServices.getServices()) {
                View serviceView = LayoutInflater.from(context).inflate(R.layout.item_assigned_service, holder.servicesContainer, false);
                ((TextView) serviceView.findViewById(R.id.service_name)).setText(service.getServiceName());
                ((TextView) serviceView.findViewById(R.id.client_name)).setText(service.getClientName());
                String appointmentDateString = service.getAppointmentDate(); // This returns a String

                SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Adjust to match the format of your input date string
                SimpleDateFormat sdf2 = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // Desired output format

                Date appointmentDate = null;
                try {
                    // Parse the string to Date
                    appointmentDate = originalFormat.parse(appointmentDateString);
                } catch (ParseException e) {
                    e.printStackTrace(); // Handle the exception
                }

                if (appointmentDate != null) {
                    // Format the date
                    String formattedDate = sdf2.format(appointmentDate);
                    // Set the formatted date to the TextView
                    ((TextView) serviceView.findViewById(R.id.appointment_date)).setText(formattedDate);
                } else {
                    // Handle the case where the date could not be parsed
                    ((TextView) serviceView.findViewById(R.id.appointment_date)).setText("Invalid date");
                }

                holder.servicesContainer.addView(serviceView);
            }
        }

        // Set a click listener to toggle visibility
        holder.weekLayout.setOnClickListener(v -> {
            if (isExpanded) {
                expandedWeeks.remove(position); // Collapse
            } else {
                expandedWeeks.add(position); // Expand
            }
            notifyItemChanged(position); // Notify that this item has changed
        });
    }


    @Override
    public int getItemCount() {
        return weekServicesList.size();
    }

    static class WeekViewHolder extends RecyclerView.ViewHolder {
        TextView weekLabel, totalCommision;
        LinearLayout servicesContainer; // Container for services
        LinearLayout weekLayout; // Root layout for click events
        ImageView ivExpandIcon;
        WeekViewHolder(View itemView) {
            super(itemView);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);
            weekLabel = itemView.findViewById(R.id.week_label);
            totalCommision = itemView.findViewById(R.id.totalCommision);
            servicesContainer = itemView.findViewById(R.id.services_container);
            weekLayout = itemView.findViewById(R.id.week_layout); // Ensure you have this in your week_header.xml
        }
    }
}
