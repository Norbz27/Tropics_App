package com.example.tropics_app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AppointmentFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_appointment, container, false);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getActivity());
        viewPager.setAdapter(adapter);

        return view;
    }
}