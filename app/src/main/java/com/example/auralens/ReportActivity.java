package com.example.auralens;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document); // Use your provided XML layout

        // Receive data from FormActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String pressure = extras.getString("PRESSURE", "N/A");
            String flow = extras.getString("FLOW", "N/A");
            String inTime = extras.getString("IN_TIME", "N/A");
            String outTime = extras.getString("OUT_TIME", "N/A");
            String tankerDetails = extras.getString("TANKER_DETAILS", "N/A");
            String weight = extras.getString("WEIGHT", "N/A");

            // Initialize views
            TextView tvPressure = findViewById(R.id.tvProductName);
            TextView tvFlow = findViewById(R.id.tvBatchSize);
            TextView tvInTime = findViewById(R.id.tvDateOfProduction);
            TextView tvOutTime = findViewById(R.id.tvBatchNumber);
            TextView tvTankerDetails = findViewById(R.id.tvQualitySpecs);
            TextView tvWeight = findViewById(R.id.tvActualProduced);

            // Set formatted values
            tvPressure.setText(String.format(Locale.getDefault(), "Pressure(psi): %s", getFormattedValue(pressure, "psi")));
            tvFlow.setText(String.format(Locale.getDefault(), "Flow(L/min): %s", getFormattedValue(flow, "L/min")));
            tvInTime.setText(String.format(Locale.getDefault(), "In-Time(HH:mm): %s", inTime));
            tvOutTime.setText(String.format(Locale.getDefault(), "Out-Time(HH:mm): %s", outTime));
            tvTankerDetails.setText(String.format(Locale.getDefault(), "Tanker Details: %s", tankerDetails));
            tvWeight.setText(String.format(Locale.getDefault(), "Weight(Kg): %s", getFormattedValue(weight, "kg")));
        }
    }

    private String getFormattedValue(String value, String unit) {
        return value.equals("N/A") ? "N/A" : value + " " + unit;
    }
}