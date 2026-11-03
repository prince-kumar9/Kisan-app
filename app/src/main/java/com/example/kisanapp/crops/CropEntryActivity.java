package com.example.kisanapp.crops;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kisanapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CropEntryActivity extends AppCompatActivity {

    // Android Emulator se Spring Boot backend
    private static final String BASE_URL = "http://10.0.2.2:8080";

    private Spinner spinnerSeason;
    private Spinner spinnerCrop;
    private Spinner spinnerAreaUnit;

    private EditText etArea;
    private EditText etOtherCrop;
    private EditText etStartDate;
    private EditText etEndDate;

    private Button btnSaveCrop;
    private Button btnBack;

    private LinearLayout layoutCropList;

    private long farmerId = -1;
    private Long editingCropId = null;
    private boolean loading = false;

    private final List<JSONObject> crops = new ArrayList<>();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final String[] seasons = {
            "Kharif",
            "Rabi",
            "Zaid"
    };

    private final String[] cropNames = {
            "Rice / Paddy",
            "Wheat",
            "Maize",
            "Potato",
            "Tomato",
            "Onion",
            "Mustard",
            "Sugarcane",
            "Vegetables",
            "Other"
    };

    private final String[] areaUnits = {
            "acre",
            "katha"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_entry);

        farmerId = getIntent().getLongExtra("farmerId", -1);

        initializeViews();
        setupSpinners();
        setupClickListeners();

        if (farmerId <= 0) {
            Toast.makeText(
                    this,
                    "Farmer ID nahi mila. Dobara login karein.",
                    Toast.LENGTH_LONG
            ).show();

            btnSaveCrop.setEnabled(false);
            return;
        }

        refreshCropList();
    }

    private void initializeViews() {
        spinnerSeason = findViewById(R.id.spinnerSeason);
        spinnerCrop = findViewById(R.id.spinnerCrop);
        spinnerAreaUnit = findViewById(R.id.spinnerAreaUnit);

        etArea = findViewById(R.id.etArea);
        etOtherCrop = findViewById(R.id.etOtherCrop);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);

        btnSaveCrop = findViewById(R.id.btnSaveCrop);
        btnBack = findViewById(R.id.btnBack);

        layoutCropList = findViewById(R.id.layoutCropList);

        if (etOtherCrop != null) {
            etOtherCrop.setVisibility(View.GONE);
        }
    }

    private void setupSpinners() {
        setSpinner(spinnerSeason, seasons);
        setSpinner(spinnerCrop, cropNames);
        setSpinner(spinnerAreaUnit, areaUnits);

        spinnerCrop.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        String selected =
                                parent.getItemAtPosition(position).toString();

                        boolean isOther = "Other".equals(selected);

                        if (etOtherCrop != null) {
                            etOtherCrop.setVisibility(
                                    isOther ? View.VISIBLE : View.GONE
                            );

                            if (!isOther && editingCropId == null) {
                                etOtherCrop.setText("");
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void setSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSaveCrop.setOnClickListener(v -> saveCrop());
        btnBack.setOnClickListener(v -> finish());

        etStartDate.setOnClickListener(
                v -> showDatePicker(etStartDate)
        );

        etEndDate.setOnClickListener(
                v -> showDatePicker(etEndDate)
        );
    }

    // Calendar: dates 2026 se 2050 tak select hongi.
    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();

        String currentValue = target.getText().toString().trim();

        if (!currentValue.isEmpty()) {
            try {
                String[] parts = currentValue.split("-");

                calendar.set(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2])
                );
            } catch (Exception ignored) {
                // Invalid value: current date use hogi.
            }
        }

        if (calendar.get(Calendar.YEAR) < 2026) {
            calendar.set(2026, Calendar.JANUARY, 1);
        } else if (calendar.get(Calendar.YEAR) > 2050) {
            calendar.set(2050, Calendar.DECEMBER, 31);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    target.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        Calendar minimum = Calendar.getInstance();
        minimum.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        minimum.set(Calendar.MILLISECOND, 0);

        Calendar maximum = Calendar.getInstance();
        maximum.set(2050, Calendar.DECEMBER, 31, 23, 59, 59);
        maximum.set(Calendar.MILLISECOND, 999);

        dialog.getDatePicker().setMinDate(
                minimum.getTimeInMillis()
        );

        dialog.getDatePicker().setMaxDate(
                maximum.getTimeInMillis()
        );

        dialog.show();
    }

    // ADD aur UPDATE dono handle karta hai.
    private void saveCrop() {
        if (loading) {
            return;
        }

        String areaText = etArea.getText().toString().trim();

        if (areaText.isEmpty()) {
            etArea.setError("Area enter karein");
            etArea.requestFocus();
            return;
        }

        final double area;

        try {
            area = Double.parseDouble(areaText);
        } catch (NumberFormatException e) {
            etArea.setError("Valid area enter karein");
            etArea.requestFocus();
            return;
        }

        if (Double.isNaN(area)
                || Double.isInfinite(area)
                || area <= 0) {

            etArea.setError("Area zero se zyada hona chahiye");
            etArea.requestFocus();
            return;
        }

        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();

        if (startDate.isEmpty()) {
            etStartDate.setError("Start date select karein");
            etStartDate.requestFocus();
            return;
        }

        if (endDate.isEmpty()) {
            etEndDate.setError("End date select karein");
            etEndDate.requestFocus();
            return;
        }

        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            Toast.makeText(
                    this,
                    "Date format sahi nahi hai. Calendar se date select karein.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (endDate.compareTo(startDate) < 0) {
            etEndDate.setError(
                    "End date, start date se pehle nahi ho sakti"
            );
            etEndDate.requestFocus();
            return;
        }

        String season = spinnerSeason.getSelectedItem().toString();
        String cropName = spinnerCrop.getSelectedItem().toString();
        String areaUnit = spinnerAreaUnit.getSelectedItem().toString();

        if ("Other".equals(cropName)) {
            if (etOtherCrop == null
                    || etOtherCrop.getText().toString().trim().isEmpty()) {

                if (etOtherCrop != null) {
                    etOtherCrop.setError("Crop ka naam enter karein");
                    etOtherCrop.requestFocus();
                }
                return;
            }

            cropName = etOtherCrop.getText().toString().trim();
        }

        final String finalCropName = cropName;
        final String finalSeason = season;
        final String finalAreaUnit = areaUnit;
        final String finalStartDate = startDate;
        final String finalEndDate = endDate;
        final Long cropId = editingCropId;

        try {
            JSONObject body = new JSONObject();

            body.put("farmerId", farmerId);
            body.put("cropName", finalCropName);
            body.put("season", finalSeason);
            body.put("area", area);
            body.put("areaUnit", finalAreaUnit);
            body.put("startDate", finalStartDate);
            body.put("endDate", finalEndDate);

            if ("acre".equalsIgnoreCase(finalAreaUnit)) {
                body.put("areaInAcres", area);
            } else {
                // Backend area unit ke according conversion karega.
                body.put("areaInAcres", JSONObject.NULL);
            }

            String path = cropId == null
                    ? "/api/crops"
                    : "/api/crops/" + cropId;

            String method = cropId == null ? "POST" : "PUT";

            setLoading(true);

            executor.execute(() -> {
                ApiResult result = request(method, path, body);

                runOnUiThread(() -> {
                    setLoading(false);

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (result.code >= 200 && result.code < 300) {
                        Toast.makeText(
                                this,
                                cropId == null
                                        ? "Crop successfully added"
                                        : "Crop successfully updated",
                                Toast.LENGTH_SHORT
                        ).show();

                        resetForm();
                        refreshCropList();
                    } else {
                        Toast.makeText(
                                this,
                                "Save failed (" + result.code + "): "
                                        + result.message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            });

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Crop data prepare nahi hua: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private boolean isValidDate(String value) {
        if (value == null
                || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }

        try {
            String[] parts = value.split("-");

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            if (year < 2026 || year > 2050) {
                return false;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setLenient(false);
            calendar.clear();
            calendar.set(year, month - 1, day);
            calendar.getTime();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    // Backend status use karta hai.
    // PREVIOUS crops hide hoti hain, delete nahi.
    private void refreshCropList() {
        if (farmerId <= 0 || loading) {
            return;
        }

        setLoading(true);

        executor.execute(() -> {
            ApiResult result = request(
                    "GET",
                    "/api/crops/farmer/" + farmerId,
                    null
            );

            List<JSONObject> loadedCrops = new ArrayList<>();

            try {
                if (result.code >= 200 && result.code < 300) {
                    JSONArray array = new JSONArray(result.message);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject crop = array.optJSONObject(i);

                        if (crop == null) {
                            continue;
                        }

                        String status = crop.optString(
                                "status",
                                "UNKNOWN"
                        );

                        if (!"PREVIOUS".equalsIgnoreCase(status)) {
                            loadedCrops.add(crop);
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e(
                        "CropEntryActivity",
                        "Crop response parse error",
                        e
                );
            }

            runOnUiThread(() -> {
                setLoading(false);

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (result.code < 200 || result.code >= 300) {
                    Toast.makeText(
                            this,
                            "Crops load nahi hui (" + result.code + "): "
                                    + result.message,
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                crops.clear();
                crops.addAll(loadedCrops);
                showCropCards();
            });
        });
    }

    private void showCropCards() {
        layoutCropList.removeAllViews();

        if (crops.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(
                    "Abhi koi current ya upcoming crop add nahi ki gayi hai."
            );
            empty.setTextSize(15);
            empty.setTextColor(0xFF607D66);
            empty.setPadding(8, 20, 8, 20);

            layoutCropList.addView(empty);
            return;
        }

        for (JSONObject crop : crops) {
            addCropCard(crop);
        }
    }

    private void addCropCard(JSONObject crop) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 16, 18, 16);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setElevation(4);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        String cropName = crop.optString("cropName", "Crop");
        String season = crop.optString("season", "");
        String unit = getCropUnit(crop);
        double area = getCropArea(crop);
        String startDate = crop.optString("startDate", "");
        String endDate = crop.optString("endDate", "");
        String status = crop.optString("status", "UNKNOWN");

        StringBuilder text = new StringBuilder();

        text.append("🌱  ").append(cropName)
                .append("\n\nSeason: ").append(season)
                .append("\nArea: ").append(formatNumber(area))
                .append(" ").append(unit)
                .append("\nStatus: ").append(status);

        if (!startDate.isEmpty()
                && !"null".equals(startDate)
                && !endDate.isEmpty()
                && !"null".equals(endDate)) {

            text.append("\nStart Date: ").append(startDate)
                    .append("\nEnd Date: ").append(endDate);

        } else {
            text.append("\nDates not set");
        }

        TextView details = new TextView(this);
        details.setText(text.toString());
        details.setTextSize(16);
        details.setTextColor(0xFF263528);

        card.addView(details);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, 10, 0, 0);

        Button edit = new Button(this);
        edit.setText("EDIT");
        edit.setOnClickListener(v -> editCrop(crop));

        Button delete = new Button(this);
        delete.setText("DELETE");
        delete.setTextColor(0xFFB3261E);
        delete.setOnClickListener(v -> deleteCrop(crop));

        actions.addView(edit);
        actions.addView(delete);

        card.addView(actions);
        layoutCropList.addView(card);
    }

    private double getCropArea(JSONObject crop) {
        if (crop.has("area") && !crop.isNull("area")) {
            return crop.optDouble("area", 0);
        }

        return crop.optDouble("areaInAcres", 0);
    }

    private String getCropUnit(JSONObject crop) {
        String unit = crop.optString("areaUnit", "acre");

        if (unit == null || unit.trim().isEmpty()) {
            return "acre";
        }

        return unit.trim();
    }

    private void editCrop(JSONObject crop) {
        long id = crop.optLong("id", -1);

        if (id <= 0) {
            Toast.makeText(
                    this,
                    "Crop ID nahi mila",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        editingCropId = id;

        setSpinnerSelection(
                spinnerSeason,
                crop.optString("season", "Kharif")
        );

        String name = crop.optString("cropName", "");
        boolean predefined = false;
        String predefinedSelection = name;

        for (String item : cropNames) {
            if (!"Other".equals(item)
                    && item.equalsIgnoreCase(name)) {
                predefined = true;
                predefinedSelection = item;
                break;
            }
        }

        if (predefined) {
            setSpinnerSelection(spinnerCrop, predefinedSelection);

            if (etOtherCrop != null) {
                etOtherCrop.setText("");
                etOtherCrop.setVisibility(View.GONE);
            }

        } else {
            setSpinnerSelection(spinnerCrop, "Other");

            if (etOtherCrop != null) {
                etOtherCrop.setVisibility(View.VISIBLE);
                etOtherCrop.setText(name);
            }
        }

        setSpinnerSelection(spinnerAreaUnit, getCropUnit(crop));
        etArea.setText(formatNumber(getCropArea(crop)));

        String startDate = crop.optString("startDate", "");
        String endDate = crop.optString("endDate", "");

        etStartDate.setText(
                "null".equals(startDate) ? "" : startDate
        );

        etEndDate.setText(
                "null".equals(endDate) ? "" : endDate
        );

        btnSaveCrop.setText("UPDATE CROP");

        Toast.makeText(
                this,
                "Details edit karke UPDATE CROP dabayein",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void deleteCrop(JSONObject crop) {
        if (loading) {
            return;
        }

        long id = crop.optLong("id", -1);

        if (id <= 0) {
            Toast.makeText(
                    this,
                    "Crop ID nahi mila",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        setLoading(true);

        executor.execute(() -> {
            ApiResult result = request(
                    "DELETE",
                    "/api/crops/" + id,
                    null
            );

            runOnUiThread(() -> {
                setLoading(false);

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (result.code >= 200 && result.code < 300) {
                    if (editingCropId != null
                            && editingCropId.longValue() == id) {
                        resetForm();
                    }

                    Toast.makeText(
                            this,
                            "Crop deleted",
                            Toast.LENGTH_SHORT
                    ).show();

                    refreshCropList();

                } else {
                    Toast.makeText(
                            this,
                            "Delete failed (" + result.code + "): "
                                    + result.message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });
    }

    private void setSpinnerSelection(
            Spinner spinner,
            String value) {

        if (value == null) {
            return;
        }

        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);

            if (item != null
                    && item.toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void resetForm() {
        editingCropId = null;

        btnSaveCrop.setText("ADD CROP");

        etArea.setText("");
        etStartDate.setText("");
        etEndDate.setText("");

        spinnerSeason.setSelection(0);
        spinnerCrop.setSelection(0);
        spinnerAreaUnit.setSelection(0);

        if (etOtherCrop != null) {
            etOtherCrop.setText("");
            etOtherCrop.setVisibility(View.GONE);
        }
    }

    private void setLoading(boolean value) {
        loading = value;
        btnSaveCrop.setEnabled(!value);
    }

    private ApiResult request(
            String method,
            String path,
            JSONObject body) {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(BASE_URL + path);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            if (body != null) {
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setDoOutput(true);

                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(
                            body.toString().getBytes(StandardCharsets.UTF_8)
                    );
                }
            }

            int code = connection.getResponseCode();

            InputStream stream =
                    (code >= 200 && code < 400)
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            StringBuilder response = new StringBuilder();

            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        ))) {

                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }

            return new ApiResult(code, response.toString());

        } catch (Exception e) {
            return new ApiResult(
                    -1,
                    e.getMessage() == null
                            ? "Network error"
                            : e.getMessage()
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String formatNumber(double value) {
        return new DecimalFormat("0.##").format(value);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private static class ApiResult {
        final int code;
        final String message;

        ApiResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
