package com.example.kisanapp.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kisanapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FarmerProfileActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080";

    private TextView tvProfileName, tvProfileMobile, tvProfileEmail;
    private TextView tvProfileLocation, tvProfileCropCount;
    private LinearLayout layoutMyCrops;
    private Button btnBack;

    private long farmerId;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_profile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileMobile = findViewById(R.id.tvProfileMobile);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileLocation = findViewById(R.id.tvProfileLocation);
        tvProfileCropCount = findViewById(R.id.tvProfileCropCount);
        layoutMyCrops = findViewById(R.id.layoutMyCrops);
        btnBack = findViewById(R.id.btnBack);

        farmerId = getIntent().getLongExtra("farmerId", -1L);

        btnBack.setOnClickListener(v -> finish());

        if (farmerId <= 0) {
            tvProfileName.setText("Farmer ID nahi mili. Dobara login karein.");
            tvProfileCropCount.setText("My Crops");
            return;
        }

        tvProfileName.setText("Loading profile...");
        tvProfileCropCount.setText("Loading crops...");

        loadProfileAndCrops();
    }

    private void loadProfileAndCrops() {
        executor.execute(() -> {
            try {
                JSONObject farmer = getJsonObject(
                        BASE_URL + "/api/farmers/" + farmerId
                );

                JSONArray crops = getJsonArray(
                        BASE_URL + "/api/crops/farmer/" + farmerId
                );

                runOnUiThread(() -> {
                    showProfile(farmer);
                    showCrops(crops);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvProfileName.setText("Profile load nahi hui");
                    tvProfileCropCount.setText("Crops load nahi hui");

                    Toast.makeText(
                            FarmerProfileActivity.this,
                            "Backend se data nahi mila. Server aur internet permission check karein.",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private JSONObject getJsonObject(String urlString) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = openConnection(urlString);
            int code = connection.getResponseCode();
            String response = readResponse(connection, code);

            if (code < 200 || code >= 300) {
                throw new Exception(
                        "Profile API HTTP " + code + ": " + response
                );
            }

            return new JSONObject(response);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONArray getJsonArray(String urlString) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = openConnection(urlString);
            int code = connection.getResponseCode();
            String response = readResponse(connection, code);

            if (code < 200 || code >= 300) {
                throw new Exception(
                        "Crops API HTTP " + code + ": " + response
                );
            }

            return new JSONArray(response);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection openConnection(String urlString)
            throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(urlString).openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");

        return connection;
    }

    private String readResponse(
            HttpURLConnection connection, int code) throws Exception {

        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        StringBuilder response = new StringBuilder();

        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        return response.toString();
    }

    private void showProfile(JSONObject farmer) {
        tvProfileName.setText(farmer.optString("name", "Farmer"));

        tvProfileMobile.setText(
                "Mobile: " + farmer.optString("mobile", "Not available")
        );

        tvProfileEmail.setText(
                "Email: " + farmer.optString("email", "Not available")
        );

        String location =
                farmer.optString("village", "") + ", " +
                        farmer.optString("panchayat", "") + ", " +
                        farmer.optString("block", "") + ", " +
                        farmer.optString("district", "") + ", " +
                        farmer.optString("state", "");

        tvProfileLocation.setText("Location: " + location);
    }

    private void showCrops(JSONArray crops) {
        layoutMyCrops.removeAllViews();

        LocalDate today = LocalDate.now();

        List<JSONObject> currentCrops = new ArrayList<>();
        List<JSONObject> upcomingCrops = new ArrayList<>();
        List<JSONObject> previousCrops = new ArrayList<>();
        int validCropCount = 0;

        for (int i = 0; i < crops.length(); i++) {
            JSONObject crop = crops.optJSONObject(i);

            if (crop == null) {
                continue;
            }

            LocalDate startDate = parseDate(
                    crop.optString("startDate", "")
            );

            LocalDate endDate = parseDate(
                    crop.optString("endDate", "")
            );

            // Dates missing or invalid: classification possible nahi hai.
            if (startDate == null || endDate == null
                    || endDate.isBefore(startDate)) {
                continue;
            }

            validCropCount++;

            if (startDate.isAfter(today)) {
                upcomingCrops.add(crop);
            } else if (endDate.isBefore(today)) {
                previousCrops.add(crop);
            } else {
                currentCrops.add(crop);
            }
        }

        tvProfileCropCount.setText(
                "My Crops (" + validCropCount + ")"
        );

        addSectionHeading("Current Crops (" + currentCrops.size() + ")");
        addCropGroup(currentCrops);

        addSectionHeading("Upcoming Crops (" + upcomingCrops.size() + ")");
        addCropGroup(upcomingCrops);

        addSectionHeading("Previous Crops (" + previousCrops.size() + ")");
        addCropGroup(previousCrops);

        if (validCropCount == 0) {
            addCropText("Abhi koi valid crop record nahi mila.");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()
                || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void addSectionHeading(String title) {
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(17);
        heading.setTextColor(0xFF1B5E20);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setPadding(4, 16, 4, 10);
        layoutMyCrops.addView(heading);
    }

    private void addCropGroup(List<JSONObject> crops) {
        if (crops.isEmpty()) {
            addCropText("Koi crop nahi hai.");
            return;
        }

        for (JSONObject crop : crops) {
            String cropName = crop.optString("cropName", "Unknown crop");
            String season = crop.optString("season", "Not specified");

            double area = crop.optDouble(
                    "area",
                    crop.optDouble("areaInAcres", 0.0)
            );

            String unit = crop.optString("areaUnit", "acre");
            String startDate = crop.optString("startDate", "Not available");
            String endDate = crop.optString("endDate", "Not available");

            String details = "Crop: " + cropName +
                    "\nSeason: " + season +
                    "\nArea: " + area + " " + unit +
                    "\nStart Date: " + startDate +
                    "\nEnd Date: " + endDate;

            addCropText(details);
        }
    }

    private void addCropText(String text) {
        TextView cropView = new TextView(this);
        cropView.setText(text);
        cropView.setTextSize(15);
        cropView.setTextColor(0xFF263A2B);
        cropView.setPadding(14, 14, 14, 14);
        cropView.setBackgroundColor(0xFFE8F5E9);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 12);
        cropView.setLayoutParams(params);
        layoutMyCrops.addView(cropView);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
