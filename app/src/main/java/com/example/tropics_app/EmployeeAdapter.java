package com.example.tropics_app;

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

    private List<Employee> employeeList;
    private OnEmployeeClickListener listener; // Listener for item clicks

    // Nested interface for click listener
    public interface OnEmployeeClickListener {
        void onEmployeeClick(Employee employee);
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
        holder.bind(employee);

        // Set an OnClickListener for the entire item
        holder.itemView.setOnClickListener(v -> listener.onEmployeeClick(employee));
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView imageViewLetter; // Add the ImageView reference

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
