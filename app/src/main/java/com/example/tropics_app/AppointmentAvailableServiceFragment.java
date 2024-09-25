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
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AppointmentAvailableServiceFragment extends Fragment {

    private FirebaseFirestore db;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_available_service, container, false);

        // Setup Back and Next buttons
        Button backButton = view.findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        });

        Button nextButton = view.findViewById(R.id.btnNext);
        nextButton.setOnClickListener(v -> {
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        });

        // Load services into the accordion layout
        LinearLayout servicesContainer = view.findViewById(R.id.servicesContainer);
        servicesContainer.removeAllViews(); // Clear existing views
        loadServices(servicesContainer);

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

        TextView tvServiceName = serviceView.findViewById(R.id.tvServiceName);
        LinearLayout subServiceContainer = serviceView.findViewById(R.id.subServiceContainer);
        ImageView ivExpandIcon = serviceView.findViewById(R.id.ivExpandIcon);

        tvServiceName.setText(serviceName);
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

                    // Create sub-service view with a checkbox
                    View subServiceView = createSubServiceView(subServiceName, subServiceId);
                    subServiceContainer.addView(subServiceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load sub-services", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View createSubServiceView(String subServiceName, String subServiceId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subServiceView = inflater.inflate(R.layout.accordion_sub_service_item, null);

        TextView tvSubServiceName = subServiceView.findViewById(R.id.tvSubServiceName);
        CheckBox cbSubService = subServiceView.findViewById(R.id.cbSubService);
        LinearLayout subSubServiceContainer = subServiceView.findViewById(R.id.subSubServiceContainer);
        ImageView ivExpandIcon = subServiceView.findViewById(R.id.ivExpandSubIcon);

        tvSubServiceName.setText(subServiceName);
        subSubServiceContainer.setVisibility(View.GONE); // Set to GONE initially

        // Toggle sub-sub-services visibility
        ivExpandIcon.setOnClickListener(v -> {
            if (subSubServiceContainer.getVisibility() == View.GONE) {
                subSubServiceContainer.setVisibility(View.VISIBLE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_up); // Expand icon
                loadSubSubServices(subSubServiceContainer, subServiceId);
            } else {
                subSubServiceContainer.setVisibility(View.GONE);
                ivExpandIcon.setImageResource(R.drawable.ic_arrow_down); // Collapse icon
            }
        });

        return subServiceView;
    }

    private void loadSubSubServices(LinearLayout subSubServiceContainer, String subServiceId) {
        db.collection("sub_sub_services").whereEqualTo("sub_service_id", subServiceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                subSubServiceContainer.removeAllViews(); // Clear existing sub-sub-services

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String subSubServiceName = document.getString("sub_service_name");

                    // Create sub-sub-service view with a checkbox
                    View subSubServiceView = createSubSubServiceView(subSubServiceName);
                    subSubServiceContainer.addView(subSubServiceView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to load sub-sub-services", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View createSubSubServiceView(String subSubServiceName) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View subSubServiceView = inflater.inflate(R.layout.accordion_sub_sub_service_item, null);

        TextView tvSubSubServiceName = subSubServiceView.findViewById(R.id.tvSubSubServiceName);
        CheckBox cbSubSubService = subSubServiceView.findViewById(R.id.cbSubSubService);

        tvSubSubServiceName.setText(subSubServiceName);

        return subSubServiceView;
    }
}
