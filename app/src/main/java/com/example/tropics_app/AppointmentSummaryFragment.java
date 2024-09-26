package com.example.tropics_app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;

public class AppointmentSummaryFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private TextView tvFullName, tvAddress, tvPhone, tvEmail;
    private View view;
    private String parentname = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_appointment_summary, container, false);

        LinearLayout summaryContainer = view.findViewById(R.id.summaryContainer);

        viewModel.getSelectedServices().observe(getViewLifecycleOwner(), services -> {
            summaryContainer.removeAllViews();
            Log.d("AppointmentSummaryFragment", "Selected Services: " + services);

            if (services.isEmpty()) {
                TextView emptyTextView = new TextView(getContext());
                emptyTextView.setText("No services selected.");
                emptyTextView.setTextColor(getResources().getColor(android.R.color.white));
                summaryContainer.addView(emptyTextView);
            } else {
                // Reset parentname for each load
                parentname = "";
                for (SelectedService service : services) {
                    Log.d("Service", "Parent Service: " + service.getParentServiceName());
                    addServiceToSummary(summaryContainer, service, 0); // Start at indentation level 0
                }
            }
        });

        // Initialize TextViews
        tvFullName = view.findViewById(R.id.tvFname);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);

        Button backButton = view.findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        });

        Button nextButton = view.findViewById(R.id.btnSubmitAppointment);
        nextButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        });

        // Call the update method to display the latest data
        updateSummary();

        return view;
    }

    private void updateSummary() {
        tvFullName.setText(viewModel.getFullName());
        tvAddress.setText(viewModel.getAddress());
        tvPhone.setText(viewModel.getPhone());
        tvEmail.setText(viewModel.getEmail());
    }

    private void addServiceToSummary(LinearLayout parent, SelectedService service, int indentLevel) {
        Log.d("addServiceToSummary", "Adding service: " + service.getName() + " at level: " + indentLevel);

        // Add Parent Service only if it's not already displayed
        if (!parentname.equals(service.getParentServiceName())) {
            TextView parentTextView = new TextView(getContext());
            parentTextView.setText(service.getParentServiceName());
            parentTextView.setPadding(indentLevel * 30, 0, 0, 0); // Indent based on level
            parentTextView.setTextColor(getResources().getColor(android.R.color.white));
            parent.addView(parentTextView);
            parentname = service.getParentServiceName(); // Update parentname to the current parent
        }

        // Add SubService
        TextView subServiceTextView = new TextView(getContext());
        subServiceTextView.setText(service.getName());
        subServiceTextView.setPadding((indentLevel + 1) * 30, 0, 0, 0); // One more level of indentation
        subServiceTextView.setTextColor(getResources().getColor(android.R.color.white));
        parent.addView(subServiceTextView);

        // Recursively add sub-sub-services
        for (SelectedService subService : service.getSubServices()) {
            TextView subSubServiceTextView = new TextView(getContext());
            subSubServiceTextView.setText(subService.getName());
            subSubServiceTextView.setPadding((indentLevel + 2) * 30, 0, 0, 0); // Two more levels of indentation
            subSubServiceTextView.setTextColor(getResources().getColor(android.R.color.white));
            parent.addView(subSubServiceTextView);
        }
    }
}
