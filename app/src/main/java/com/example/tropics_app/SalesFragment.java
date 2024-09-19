package com.example.tropics_app;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;

public class SalesFragment extends Fragment {

    private LineChart lineChart;

    public SalesFragment() {
        // Required empty public constructor
    }

    public static SalesFragment newInstance(String param1, String param2) {
        SalesFragment fragment = new SalesFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_sales, container, false);

        // Find LineChart and Buttons from layout
        lineChart = rootView.findViewById(R.id.lineChart);
        Button btnDaily = rootView.findViewById(R.id.btnDaily);
        Button btnMonthly = rootView.findViewById(R.id.btnMonthly);

        // Set default chart data (e.g., Daily)
        setDailyData();

        // Handle Daily button click
        btnDaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDailyData(); // Load daily data
                btnDaily.setBackgroundResource(R.drawable.button_daily_checked);
                btnMonthly.setBackgroundResource(R.drawable.button_monthly);
            }
        });

        // Handle Monthly button click
        btnMonthly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMonthlyData(); // Load monthly data
                btnDaily.setBackgroundResource(R.drawable.button_daily);
                btnMonthly.setBackgroundResource(R.drawable.button_monthly_checked);
            }
        });

        return rootView;
    }

    // Method to set Daily Data on the chart
    private void setDailyData() {
        ArrayList<Entry> dailyEntries = new ArrayList<>();
        dailyEntries.add(new Entry(0f, 2f));
        dailyEntries.add(new Entry(1f, 4f));
        dailyEntries.add(new Entry(2f, 1f));
        dailyEntries.add(new Entry(3f, 5f));
        dailyEntries.add(new Entry(4f, 3f));

        LineDataSet dataSet = new LineDataSet(dailyEntries, null); // Set label to null
        dataSet.setColor(Color.parseColor("#FFA500")); // Set line color to orange
        dataSet.setCircleColor(Color.parseColor("#FFA500")); // Set circle color to orange
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Set the fill drawable
        Drawable gradientDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(gradientDrawable);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Customize chart appearance
        lineChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray)); // Set chart background color

        // Hide the legend
        lineChart.getLegend().setEnabled(false);

        // Hide the description
        lineChart.getDescription().setEnabled(false);

        // Set X-axis labels to daily format
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "Day " + (int) value;
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE); // Set X-axis text color to white

        // Customize Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set Y-axis text color to white
        lineChart.getAxisRight().setEnabled(false);

        lineChart.invalidate(); // Refresh the chart
    }

    // Method to set Monthly Data on the chart
    private void setMonthlyData() {
        ArrayList<Entry> monthlyEntries = new ArrayList<>();
        monthlyEntries.add(new Entry(0f, 100f));
        monthlyEntries.add(new Entry(1f, 120f));
        monthlyEntries.add(new Entry(2f, 80f));
        monthlyEntries.add(new Entry(3f, 150f));
        monthlyEntries.add(new Entry(4f, 110f));

        LineDataSet dataSet = new LineDataSet(monthlyEntries, null); // Set label to null
        dataSet.setColor(Color.parseColor("#FFA500")); // Set line color to orange
        dataSet.setCircleColor(Color.parseColor("#FFA500")); // Set circle color to orange
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Set the fill drawable
        Drawable gradientDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(gradientDrawable);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Customize chart appearance
        lineChart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray)); // Set chart background color

        // Hide the legend
        lineChart.getLegend().setEnabled(false);

        // Hide the description
        lineChart.getDescription().setEnabled(false);

        // Set X-axis labels to monthly format
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int index = (int) value;
                if (index >= 0 && index < months.length) {
                    return months[index];
                } else {
                    return "";
                }
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE); // Set X-axis text color to white

        // Customize Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set Y-axis text color to white
        lineChart.getAxisRight().setEnabled(false);

        lineChart.invalidate(); // Refresh the chart
    }
}
