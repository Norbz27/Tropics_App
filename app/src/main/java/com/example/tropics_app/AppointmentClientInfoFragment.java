package com.example.tropics_app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class AppointmentClientInfoFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private EditText edFirstname, edLastname, edAddress, edPhone, edEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AppointmentViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment_client_info, container, false);

        edFirstname = view.findViewById(R.id.edFirstname);
        edLastname = view.findViewById(R.id.edLastname);
        edAddress = view.findViewById(R.id.edAddress);
        edPhone = view.findViewById(R.id.edPhone);
        edEmail = view.findViewById(R.id.edEmail);

        Button nextButton = view.findViewById(R.id.btnNext);
        nextButton.setOnClickListener(v -> {
            saveData(); // Save data before navigating
            ViewPager2 viewPager = getActivity().findViewById(R.id.viewPager);
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
        });

        return view;
    }

    private void saveData() {
        String fullName = edFirstname.getText().toString() + " " + edLastname.getText().toString();
        viewModel.setFullName(fullName);
        viewModel.setAddress(edAddress.getText().toString());
        viewModel.setPhone(edPhone.getText().toString());
        viewModel.setEmail(edEmail.getText().toString());
    }
}
