package com.example.tropics_app;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private final List<Employee> employeeList;
    private final OnEmployeeClickListener listener; // Listener for item clicks

    // Nested interface for click listener
    public interface OnEmployeeClickListener {
        void onEmployeeClick(Employee employee);

        void onEmployeeLongClick(View view, Employee employee);
    }

    public EmployeeAdapter(OnEmployeeClickListener listener, List<Employee> employeeList) {
        this.listener = listener;
        this.employeeList = employeeList;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your custom_salary_layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_salary_layout, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = employeeList.get(position);
        holder.bind(employee); // Bind the employee data to the holder

        // Get the commission as a String
        Double coms = employee.getComs();
        if (coms != null) {
            // Log the commission for debugging
            Log.d("Employee Coms", "Commission: " + coms);
        }

        // Set an OnClickListener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeClick(employee); // Notify the listener that an item was clicked
            }
        });

        // Set an OnLongClickListener for the entire item
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeLongClick(v,employee); // Notify the listener that an item was long-pressed
            }
            return true; // Return true to indicate that the long click event was handled
        });
    }

    public void updateEmployeeList(List<Employee> newList) {
        employeeList.clear(); // Clear current list
        employeeList.addAll(newList); // Add new list
        notifyDataSetChanged(); // Notify the adapter of data changes
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView imageViewLetter; // Add the ImageView referenced

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvEmpName); // Updated ID to match your layout
            imageViewLetter = itemView.findViewById(R.id.imageViewLetter); // Initialize the ImageView
        }

        public void bind(Employee employee) {
            // Bind employee data to your views
            name.setText(employee.getName());

            // Load the employee's image into the ImageView using Glide
            Glide.with(itemView.getContext())
                    .load(employee.getImage()) // Use the getImage method to get the URL
                    .placeholder(R.drawable.ic_image_placeholder) // Optional: Placeholder image while loading
                    .into(imageViewLetter); // Set the image to the ImageView
        }
    }
}
