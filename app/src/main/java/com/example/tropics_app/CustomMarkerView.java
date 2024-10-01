package com.example.tropics_app;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

public class CustomMarkerView extends MarkerView implements CustomMarkerView2 {

    private TextView tvContent;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent); // Assuming you have a TextView with this ID in your layout
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // Get the y-value for the entry
        float sales = e.getY();
        tvContent.setText("₱" + String.format("%.2f", sales));
        super.refreshContent(e, highlight);
    }

    @Override
    public int getXOffset(float xpos) {
        return -getWidth() / 2; // Center the marker horizontally
    }

    @Override
    public int getYOffset(float ypos) {
        return -getHeight(); // Adjust vertical offset
    }
}
