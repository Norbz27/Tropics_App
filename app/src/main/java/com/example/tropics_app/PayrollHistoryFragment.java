package com.example.tropics_app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;


public class PayrollHistoryFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payroll_history, container, false);

        String[] names = {
                "ADORO, MATTHEW LOUIS E.",
                "BROCAL, VANNISA M.",
                "BRUZON, NORBERTO JR. J.",
                "CORTES, MARIELLA MAE R.",
                "DELFIN, KEVIN ART T.",
                "DENOSAPAL, IVY JOY Z.",
                "FANUNAL, JOSEPHEN",
                "LEOPARDAS, TIMOTEO P.",
                "MEDALLA, PEJIE Q.",
                "MONTEALTO, JAN ANGELIEKA",
                "MARIF V."
        };

        AutoCompleteTextView autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), // or getContext(), depending on your use case
                android.R.layout.simple_dropdown_item_1line,
                names
        );

        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);

        return view;
    }
}