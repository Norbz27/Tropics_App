package com.example.tropics_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppointmentAvailableServiceFragment extends Fragment {

    private FirebaseFirestore db;
    private AppointmentViewModel viewModel;
    public AppointmentAvailableServiceFragment() {
        // Required empty public constructor
    }

    public static AppointmentAvailableServiceFragment newInstance(String param1, String param2) {
        AppointmentAvailableServiceFragment fragment = new AppointmentAvailableServiceFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_available_service, container, false);

        LinearLayout servicesContainer = view.findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews(); // Clear existing views
        loadServices(servicesContainer);

        // Setup Back and Next buttons
        Button backButton = view.findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        });

        Button nextButton = view.findViewById(R.id.btnNext);
        nextButton.setOnClickListener(v -> {
            List<SelectedService> selectedServices = new ArrayList<>();
            for (int i = 0; i < servicesContainer.getChildCount(); i++) {
                View serviceView = servicesContainer.getChildAt(i);
                LinearLayout subServiceContainer = serviceView.findViewById(R.id.subServiceContainer);

                for (int j = 0; j < subServiceContainer.getChildCount(); j++) {
                    View subServiceView = subServiceContainer.getChildAt(j);
                    CheckBox cbSubService = subServiceView.findViewById(R.id.cbSubService);
                    if (cbSubService.isChecked()) {
                        // Get the service name from the parent checkbox
                        String serviceName = cbSubService.getText().toString();
                        // Here, serviceName should be the top-level service name
                        String parentServiceName = ((TextView) serviceView.findViewById(R.id.tvServiceName2)).getText().toString();
                        SelectedService selectedService = new SelectedService(cbSubService.getText().toString(), parentServiceName, serviceName); // Pass the service name

                        // Check for sub-sub-services
                        LinearLayout subSubServiceContainer = subServiceView.findViewById(R.id.subSubServiceContainer);
                        for (int k = 0; k < subSubServiceContainer.getChildCount(); k++) {
                            View subSubServiceView = subSubServiceContainer.getChildAt(k);
                            CheckBox cbSubSubService = subSubServiceView.findViewById(R.id.cbSubSubService);
                            if (cbSubSubService.isChecked()) {
                                selectedService.addSubService(new SelectedService(cbSubSubService.getText().toString(), parentServiceName, serviceName)); // Pass the service name
                            }
                        }

                        selectedServices.add(selectedService);
                    }
                }
            }

            // Update ViewModel
            for (SelectedService service : selectedServices) {
                viewModel.addSelectedService(service);
            }

            // Navigate to the next fragment
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        });

        return view;
    }

    private void loadServices(LinearLayout servicesContainer) {
        db.collection("service").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                servicesContainer.removeAllViews(); // Clear existing services

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String serviceName = document.getString("service_name");
                    String serviceId = document.getId();

                    // Create the parent service view
                    View serviceView = createServiceView(serviceName, serviceId);
                    servicesContainer.addView(serviceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load services", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View createServiceView(String serviceName, String serviceId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View serviceView = inflater.inflate(R.layout.accordion_service_item, null);

        TextView cbSubService = serviceView.findViewById(R.id.tvServiceName2); // parent checkbox
        LinearLayout subServiceContainer = serviceView.findViewById(R.id.subServiceContainer);
        ImageView ivExpandIcon = serviceView.findViewById(R.id.ivExpandIcon);

        cbSubService.setText(serviceName);
        subServiceContainer.setVisibility(View.GONE); // Set to GONE initially

        // Toggle sub-services visibility
        ivExpandIcon.setOnClickListener(v -> {
            if (subServiceContainer.getVisibility() == View.GONE) {
                subServiceContainer.setVisibility(View.VISIBLE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Expand icon
                loadSubServices(subServiceContainer, serviceId);
            } else {
                subServiceContainer.setVisibility(View.GONE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Collapse icon
            }
        });

        return serviceView;
    }

    private void loadSubServices(LinearLayout subServiceContainer, String serviceId) {
        db.collection("sub_services").whereEqualTo("main_service_id", serviceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                subServiceContainer.removeAllViews(); // Clear existing sub-services

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String subServiceName = document.getString("sub_service_name");
                    String subServiceId = document.getId();
                    Number priceNumber = document.getDouble("price"); // Retrieve price as Number
                    String price = priceNumber != null ? String.format("₱%.2f", priceNumber.doubleValue()) : "N/A"; // Format as string
                    if(price.equals("₱0.00")){
                        price = "";
                    }
                    // Create sub-service view with a checkbox and price
                    View subServiceView = createSubServiceView(subServiceName, price, subServiceId);
                    subServiceContainer.addView(subServiceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load sub-services", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View createSubServiceView(String subServiceName, String price, String subServiceId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subServiceView = inflater.inflate(R.layout.accordion_sub_service_item, null);

        TextView tvSubServicePrice = subServiceView.findViewById(R.id.tvSubServicePrice); // Price TextView
        CheckBox cbSubService = subServiceView.findViewById(R.id.cbSubService); // sub service checkbox
        LinearLayout subSubServiceContainer = subServiceView.findViewById(R.id.subSubServiceContainer);
        ImageView ivExpandIcon = subServiceView.findViewById(R.id.ivExpandSubIcon);

        cbSubService.setText(subServiceName);
        tvSubServicePrice.setText(price); // Set price
        subSubServiceContainer.setVisibility(View.GONE); // Set to GONE initially

        // Toggle sub-sub-services visibility
        ivExpandIcon.setOnClickListener(v -> {
            if (subSubServiceContainer.getVisibility() == View.GONE) {
                subSubServiceContainer.setVisibility(View.VISIBLE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Expand icon
                loadSubSubServices(subSubServiceContainer, subServiceId, cbSubService); // Pass parent checkbox
            } else {
                subSubServiceContainer.setVisibility(View.GONE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Collapse icon
            }
        });

        tvSubServicePrice.setOnClickListener(v -> {
            if (subSubServiceContainer.getVisibility() == View.GONE) {
                subSubServiceContainer.setVisibility(View.VISIBLE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Expand icon
                loadSubSubServices(subSubServiceContainer, subServiceId, cbSubService); // Pass parent checkbox
            } else {
                subSubServiceContainer.setVisibility(View.GONE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Collapse icon
            }
        });
        return subServiceView;
    }

    private void loadSubSubServices(LinearLayout subSubServiceContainer, String subServiceId, CheckBox parentCheckBox) {
        db.collection("sub_sub_services").whereEqualTo("sub_service_id", subServiceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                subSubServiceContainer.removeAllViews(); // Clear existing sub-sub-services

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String subSubServiceName = document.getString("sub_service_name");
                    Number priceNumber = document.getDouble("price"); // Retrieve price as Number
                    String price = priceNumber != null ? String.format("₱%.2f", priceNumber.doubleValue()) : "N/A"; // Format as string

                    // Create sub-sub-service view with a checkbox and price
                    View subSubServiceView = createSubSubServiceView(subSubServiceName, price, parentCheckBox, subSubServiceContainer); // Pass parent checkbox and container
                    subSubServiceContainer.addView(subSubServiceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load sub-sub-services", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private View createSubSubServiceView(String subSubServiceName, String price, CheckBox parentCheckBox, LinearLayout subSubServiceContainer) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subSubServiceView = inflater.inflate(R.layout.accordion_sub_sub_service_item, null);

        TextView tvSubSubServicePrice = subSubServiceView.findViewById(R.id.tvSubSubServicePrice); // Price TextView
        CheckBox cbSubSubService = subSubServiceView.findViewById(R.id.cbSubSubService); // Sub-sub-service checkbox

        cbSubSubService.setText(subSubServiceName);
        tvSubSubServicePrice.setText(price); // Set price

        // Listen for checkbox state changes
        cbSubSubService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Automatically check the parent sub-service if any sub-sub-service is checked
                parentCheckBox.setChecked(true);
            } else {
                // If none of the sub-sub-services are checked, uncheck the parent checkbox
                boolean anyChecked = false;
                for (int i = 0; i < subSubServiceContainer.getChildCount(); i++) {
                    View childView = subSubServiceContainer.getChildAt(i);
                    CheckBox cbChild = childView.findViewById(R.id.cbSubSubService);
                    if (cbChild.isChecked()) {
                        anyChecked = true;
                        break;
                    }
                }
                if (!anyChecked) {
                    parentCheckBox.setChecked(false);
                }
            }
        });

        return subSubServiceView;
    }


}