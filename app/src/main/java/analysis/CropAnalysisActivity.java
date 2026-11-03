package com.example.kisanapp.analysis;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CropAnalysisActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080";

    private static final double SQ_FT_PER_ACRE = 43560.0;
    private static final double SQ_FT_PER_KATHA = 1361.25;

    private Spinner spinnerAnalysisType;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;

    private Spinner spinnerState;
    private Spinner spinnerDistrict;
    private Spinner spinnerBlock;
    private Spinner spinnerPanchayat;

    private Button btnViewAnalysis;
    private Button btnBackAnalysis;

    private TextView tvAreaName;
    private TextView tvTotalArea;
    private TextView tvAnalysisInfo;
    private LinearLayout layoutCropGraph;

    private JSONObject locationData;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final List<String> states = new ArrayList<>();
    private final List<String> districts = new ArrayList<>();
    private final List<String> blocks = new ArrayList<>();
    private final List<String> panchayats = new ArrayList<>();

    private final List<String> analysisTypes = new ArrayList<>();
    private final List<String> months = new ArrayList<>();
    private final List<String> years = new ArrayList<>();

    private final List<JSONObject> allCrops = new ArrayList<>();
    private final List<JSONObject> allFarmers = new ArrayList<>();

    private ArrayAdapter<String> stateAdapter;
    private ArrayAdapter<String> districtAdapter;
    private ArrayAdapter<String> blockAdapter;
    private ArrayAdapter<String> panchayatAdapter;
    private ArrayAdapter<String> analysisAdapter;
    private ArrayAdapter<String> monthAdapter;
    private ArrayAdapter<String> yearAdapter;

    private boolean changingLocations = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_analysis);

        initializeViews();
        loadLocationJson();
        setupAdapters();
        setupLocationDropdowns();
        setupButtons();
        loadStates();
        loadCropData();
    }

    private void initializeViews() {
        spinnerAnalysisType = findViewById(R.id.spinnerAnalysisType);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);

        spinnerState = findViewById(R.id.spinnerState);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerBlock = findViewById(R.id.spinnerBlock);
        spinnerPanchayat = findViewById(R.id.spinnerPanchayat);

        btnViewAnalysis = findViewById(R.id.btnViewAnalysis);
        btnBackAnalysis = findViewById(R.id.btnBackAnalysis);

        tvAreaName = findViewById(R.id.tvAreaName);
        tvTotalArea = findViewById(R.id.tvTotalArea);
        tvAnalysisInfo = findViewById(R.id.tvAnalysisInfo);
        layoutCropGraph = findViewById(R.id.layoutCropGraph);
    }

    private void loadLocationJson() {
        try (InputStream input =
                     getAssets().open("bihar_locations.json");
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {

            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            locationData = new JSONObject(builder.toString());

            JSONObject nestedData = locationData.optJSONObject("data");
            if (nestedData != null) {
                locationData = nestedData;
            }

        } catch (Exception e) {
            locationData = new JSONObject();

            Toast.makeText(
                    this,
                    "Location JSON error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private ArrayAdapter<String> createAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {
                TextView text = (TextView) super.getView(
                        position, convertView, parent
                );

                text.setTextSize(15);
                text.setTextColor(0xFF344638);
                text.setGravity(Gravity.CENTER_VERTICAL);
                text.setPadding(8, 0, 8, 0);

                return text;
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {
                TextView text = (TextView) super.getDropDownView(
                        position, convertView, parent
                );

                text.setTextSize(15);
                text.setTextColor(0xFF263629);
                text.setPadding(20, 16, 20, 16);

                return text;
            }
        };

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        return adapter;
    }

    private void setupAdapters() {
        states.add("All States");
        districts.add("All Districts");
        blocks.add("All Blocks");
        panchayats.add("All Panchayats");

        stateAdapter = createAdapter(states);
        districtAdapter = createAdapter(districts);
        blockAdapter = createAdapter(blocks);
        panchayatAdapter = createAdapter(panchayats);

        spinnerState.setAdapter(stateAdapter);
        spinnerDistrict.setAdapter(districtAdapter);
        spinnerBlock.setAdapter(blockAdapter);
        spinnerPanchayat.setAdapter(panchayatAdapter);

        analysisTypes.add("Current Crops");
        analysisTypes.add("All Previous Crops");
        analysisTypes.add("Previous Crops (Selected Month/Year)");

        analysisAdapter = createAdapter(analysisTypes);
        spinnerAnalysisType.setAdapter(analysisAdapter);

        String[] monthNames = {
                "January", "February", "March", "April",
                "May", "June", "July", "August",
                "September", "October", "November", "December"
        };

        Collections.addAll(months, monthNames);
        monthAdapter = createAdapter(months);
        spinnerMonth.setAdapter(monthAdapter);

        int currentYear = LocalDate.now().getYear();

        for (int y = currentYear - 5; y <= currentYear + 2; y++) {
            years.add(String.valueOf(y));
        }

        yearAdapter = createAdapter(years);
        spinnerYear.setAdapter(yearAdapter);

        spinnerMonth.setSelection(LocalDate.now().getMonthValue() - 1);

        int yearIndex = years.indexOf(String.valueOf(currentYear));
        if (yearIndex >= 0) {
            spinnerYear.setSelection(yearIndex);
        }
    }

    private void setupLocationDropdowns() {
        spinnerState.setOnItemSelectedListener(
                new SimpleSelectionListener(() -> {
                    if (!changingLocations) {
                        loadDistricts();
                    }
                })
        );

        spinnerDistrict.setOnItemSelectedListener(
                new SimpleSelectionListener(() -> {
                    if (!changingLocations) {
                        loadBlocks();
                    }
                })
        );

        spinnerBlock.setOnItemSelectedListener(
                new SimpleSelectionListener(() -> {
                    if (!changingLocations) {
                        loadPanchayats();
                    }
                })
        );

    }

    private class SimpleSelectionListener
            implements android.widget.AdapterView.OnItemSelectedListener {

        private final Runnable action;

        SimpleSelectionListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void onItemSelected(
                android.widget.AdapterView<?> parent,
                View view,
                int position,
                long id
        ) {
            action.run();
        }

        @Override
        public void onNothingSelected(
                android.widget.AdapterView<?> parent
        ) {
        }
    }

    private void loadStates() {
        states.clear();
        states.add("All States");

        if (locationData != null) {
            Iterator<String> keys = locationData.keys();

            while (keys.hasNext()) {
                String key = keys.next();

                if (locationData.opt(key) instanceof JSONObject) {
                    states.add(key);
                }
            }
        }

        sortAfterAll(states);
        stateAdapter.notifyDataSetChanged();

        changingLocations = true;
        spinnerState.setSelection(0);
        changingLocations = false;

        loadDistricts();
    }

    private void loadDistricts() {
        districts.clear();
        districts.add("All Districts");

        blocks.clear();
        blocks.add("All Blocks");

        panchayats.clear();
        panchayats.add("All Panchayats");

        String state = selected(spinnerState);

        if (!isAll(state) && locationData != null) {
            JSONObject stateObject = locationData.optJSONObject(state);

            if (stateObject != null) {
                addKeys(districts, stateObject);
            }
        }

        sortAfterAll(districts);
        notifyLocationAdapters();

        changingLocations = true;
        spinnerDistrict.setSelection(0);
        spinnerBlock.setSelection(0);
        spinnerPanchayat.setSelection(0);
        changingLocations = false;
    }

    private void loadBlocks() {
        blocks.clear();
        blocks.add("All Blocks");

        panchayats.clear();
        panchayats.add("All Panchayats");

        String state = selected(spinnerState);
        String district = selected(spinnerDistrict);

        JSONObject districtObject = getDistrictObject(state, district);

        if (districtObject != null) {
            addKeys(blocks, districtObject);
        }

        sortAfterAll(blocks);
        notifyLocationAdapters();

        changingLocations = true;
        spinnerBlock.setSelection(0);
        spinnerPanchayat.setSelection(0);
        changingLocations = false;
    }

    private void loadPanchayats() {
        panchayats.clear();
        panchayats.add("All Panchayats");

        String state = selected(spinnerState);
        String district = selected(spinnerDistrict);
        String block = selected(spinnerBlock);

        JSONObject blockObject = getBlockObject(state, district, block);

        if (blockObject != null) {
            addKeys(panchayats, blockObject);
        }

        sortAfterAll(panchayats);
        notifyLocationAdapters();

        changingLocations = true;
        spinnerPanchayat.setSelection(0);
        changingLocations = false;
    }

    private JSONObject getDistrictObject(
            String state,
            String district
    ) {
        if (isAll(state) || isAll(district) || locationData == null) {
            return null;
        }

        JSONObject stateObject = locationData.optJSONObject(state);

        if (stateObject == null) {
            return null;
        }

        return stateObject.optJSONObject(district);
    }

    private JSONObject getBlockObject(
            String state,
            String district,
            String block
    ) {
        if (isAll(block)) {
            return null;
        }

        JSONObject districtObject = getDistrictObject(state, district);

        if (districtObject == null) {
            return null;
        }

        return districtObject.optJSONObject(block);
    }

    private Object findValueIgnoreCase(JSONObject object, String key) {
        if (object == null || key == null || isAll(key)) {
            return null;
        }

        if (object.has(key)) {
            return object.opt(key);
        }

        Iterator<String> keys = object.keys();

        while (keys.hasNext()) {
            String candidate = keys.next();

            if (candidate.equalsIgnoreCase(key)) {
                return object.opt(candidate);
            }
        }

        return null;
    }

    private void addKeys(List<String> destination, JSONObject object) {
        if (object == null) {
            return;
        }

        Iterator<String> keys = object.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            if (!key.equalsIgnoreCase("name")
                    && !key.equalsIgnoreCase("id")
                    && !key.equalsIgnoreCase("code")) {
                destination.add(key);
            }
        }
    }

    private void sortAfterAll(List<String> items) {
        if (items.size() > 2) {
            Collections.sort(
                    items.subList(1, items.size()),
                    String.CASE_INSENSITIVE_ORDER
            );
        }
    }

    private void notifyLocationAdapters() {
        stateAdapter.notifyDataSetChanged();
        districtAdapter.notifyDataSetChanged();
        blockAdapter.notifyDataSetChanged();
        panchayatAdapter.notifyDataSetChanged();
    }

    private String selected(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) {
            return "";
        }

        return spinner.getSelectedItem().toString().trim();
    }

    private boolean isAll(String value) {
        return value == null
                || value.trim().isEmpty()
                || value.startsWith("All ")
                || value.startsWith("Select ");
    }

    private String firstString(JSONObject obj, String... keys) {
        for (String key : keys) {
            String value = obj.optString(key, "").trim();

            if (!value.isEmpty() && !value.equalsIgnoreCase("null")) {
                return value;
            }
        }

        return "";
    }

    private void setupButtons() {
        btnViewAnalysis.setOnClickListener(v -> showAnalysis());
        btnBackAnalysis.setOnClickListener(v -> finish());
    }

    private void showAnalysis() {
        String state = selected(spinnerState);
        String district = selected(spinnerDistrict);
        String block = selected(spinnerBlock);
        String panchayat = selected(spinnerPanchayat);

        String analysisType = selected(spinnerAnalysisType);
        String monthName = selected(spinnerMonth);
        String yearText = selected(spinnerYear);

        if (isAll(state)) {
            showToast("Please select State");
            return;
        }

        if (isAll(district)) {
            showToast("Please select District");
            return;
        }

        if (isAll(block)) {
            showToast("Please select Block");
            return;
        }

        if (isAll(panchayat)) {
            showToast("Please select Panchayat");
            return;
        }

        int month = months.indexOf(monthName) + 1;
        int year;

        try {
            year = Integer.parseInt(yearText);
        } catch (Exception e) {
            showToast("Please select Year");
            return;
        }

        if (month < 1 || month > 12) {
            showToast("Please select Month");
            return;
        }

        tvAreaName.setText(
                panchayat
                        + ", " + block
                        + ", " + district
                        + ", " + state
        );

        renderAnalysis(
                state, district, block, panchayat,
                analysisType, month, year
        );
    }

    private void loadCropData() {
        executor.execute(() -> {
            try {
                JSONArray crops = getArrayFromApi(
                        BASE_URL + "/api/crops/all"
                );

                List<JSONObject> loadedCrops = new ArrayList<>();

                for (int i = 0; i < crops.length(); i++) {
                    JSONObject crop = crops.optJSONObject(i);

                    if (crop != null) {
                        loadedCrops.add(crop);
                    }
                }

                List<JSONObject> loadedFarmers = new ArrayList<>();

                for (JSONObject crop : loadedCrops) {
                    String farmerId = crop.optString("farmerId", "");

                    if (farmerId.isEmpty() || farmerId.equals("null")) {
                        continue;
                    }

                    boolean exists = false;

                    for (JSONObject farmer : loadedFarmers) {
                        String id = farmer.optString("id", "");
                        String alternateId = farmer.optString("farmerId", "");

                        if (id.equals(farmerId)
                                || alternateId.equals(farmerId)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        try {
                            loadedFarmers.add(
                                    getObjectFromApi(
                                            BASE_URL + "/api/farmers/" + farmerId
                                    )
                            );
                        } catch (Exception ignored) {
                            // Continue loading the remaining crop records.
                        }
                    }
                }

                runOnUiThread(() -> {
                    allCrops.clear();
                    allCrops.addAll(loadedCrops);

                    allFarmers.clear();
                    allFarmers.addAll(loadedFarmers);

                    tvAnalysisInfo.setText(
                            "Data loaded. Select location and View Analysis."
                    );
                });

            } catch (Exception e) {
                runOnUiThread(() -> tvAnalysisInfo.setText(
                        "Crop API load failed: " + e.getMessage()
                ));
            }
        });
    }

    private JSONArray getArrayFromApi(String address) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    new URL(address).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();

            if (status < 200 || status >= 300) {
                throw new Exception("HTTP " + status);
            }

            return new JSONArray(readResponse(connection.getInputStream()));

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject getObjectFromApi(String address) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)
                    new URL(address).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int status = connection.getResponseCode();

            if (status < 200 || status >= 300) {
                throw new Exception("HTTP " + status);
            }

            return new JSONObject(readResponse(connection.getInputStream()));

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    private void renderAnalysis(
            String state,
            String district,
            String block,
            String panchayat,
            String analysisType,
            int month,
            int year
    ) {
        layoutCropGraph.removeAllViews();

        LocalDate today = LocalDate.now();
        YearMonth selectedPeriod = YearMonth.of(year, month);

        Map<String, Double> cropAreas = new LinkedHashMap<>();

        double totalAcres = 0;
        double totalKatha = 0;
        int matchingRecords = 0;

        for (JSONObject crop : allCrops) {
            String farmerId = crop.optString("farmerId", "");
            JSONObject farmer = findFarmer(farmerId);

            if (farmer == null) {
                continue;
            }

            if (!matchesLocation(
                    farmer, state, district, block, panchayat
            )) {
                continue;
            }

            LocalDate start = parseDate(crop.optString("startDate", ""));
            LocalDate end = parseDate(crop.optString("endDate", ""));

            if (start == null || end == null || end.isBefore(start)) {
                continue;
            }

            /*
             * Crop status:
             * Current Crops = crop ki start date aa chuki hai aur end date
             * aaj ya future mein hai.
             * Previous Crops = crop ki end date aaj se pehle hai.
             */
            boolean current =
                    !today.isBefore(start) && !today.isAfter(end);

            boolean previous = end.isBefore(today);

            if ("Current Crops".equalsIgnoreCase(analysisType)) {
                boolean overlapsSelectedPeriod =
                        !start.isAfter(selectedPeriod.atEndOfMonth())
                                && !end.isBefore(selectedPeriod.atDay(1));

                if (!current || !overlapsSelectedPeriod) {
                    continue;
                }
            } else if ("All Previous Crops".equalsIgnoreCase(analysisType)) {
                // Is option mein selected month/year ko ignore karna hai.
                if (!previous) {
                    continue;
                }
            } else if ("Previous Crops (Selected Month/Year)"
                    .equalsIgnoreCase(analysisType)) {
                boolean endedInSelectedPeriod =
                        !end.isBefore(selectedPeriod.atDay(1))
                                && !end.isAfter(selectedPeriod.atEndOfMonth());

                if (!previous || !endedInSelectedPeriod) {
                    continue;
                }
            }

            String cropName = crop.optString("cropName", "Unknown Crop");

            double areaInAcres = crop.optDouble("areaInAcres", Double.NaN);
            double rawArea = crop.optDouble("area", Double.NaN);
            String unit = crop.optString("areaUnit", "ACRE");

            /*
             * Backend areaInAcres already acres mein hota hai.
             * Agar value missing ho, raw area aur unit se conversion karo.
             */
            if (!Double.isNaN(areaInAcres) && areaInAcres > 0) {
                totalAcres += areaInAcres;
                totalKatha += areaInAcres * SQ_FT_PER_ACRE
                        / SQ_FT_PER_KATHA;
            } else if (!Double.isNaN(rawArea) && rawArea > 0) {
                if (unit.equalsIgnoreCase("KATHA")) {
                    totalKatha += rawArea;
                    totalAcres += rawArea * SQ_FT_PER_KATHA
                            / SQ_FT_PER_ACRE;
                } else {
                    totalAcres += rawArea;
                    totalKatha += rawArea * SQ_FT_PER_ACRE
                            / SQ_FT_PER_KATHA;
                }
            } else {
                continue;
            }

            /*
             * Graph aur percentage hamesha acres ke basis par calculate honge.
             */
            double areaForGraph;

            if (!Double.isNaN(areaInAcres) && areaInAcres > 0) {
                areaForGraph = areaInAcres;
            } else if (unit.equalsIgnoreCase("KATHA")) {
                areaForGraph = rawArea * SQ_FT_PER_KATHA
                        / SQ_FT_PER_ACRE;
            } else {
                areaForGraph = rawArea;
            }

            cropAreas.put(
                    cropName,
                    cropAreas.getOrDefault(cropName, 0.0) + areaForGraph
            );

            matchingRecords++;
        }

        tvTotalArea.setText(String.format(
                Locale.getDefault(),
                "Total area: %.2f acres | %.2f katha",
                totalAcres, totalKatha
        ));

        String periodInfo;
        if ("All Previous Crops".equalsIgnoreCase(analysisType)) {
            periodInfo = "Period: All past dates (month/year filter ignored)";
        } else {
            periodInfo = "Period: " + selectedPeriod;
        }

        tvAnalysisInfo.setText(
                "Matching crop records: " + matchingRecords
                        + "\n" + periodInfo
                        + "\nAnalysis: " + analysisType
                        + "\nCurrent date: " + today
                        + "\nExpired crops are kept in the database."
        );

        if (cropAreas.isEmpty()) {
            addResultText(
                    "Selected location/period ke liye "
                            + analysisType.toLowerCase(Locale.ROOT)
                            + " ka crop data nahi mila.",
                    true
            );
            return;
        }

        double totalForPercent = 0;

        for (double area : cropAreas.values()) {
            totalForPercent += area;
        }

        for (Map.Entry<String, Double> entry : cropAreas.entrySet()) {
            double percentage = totalForPercent > 0
                    ? entry.getValue() * 100.0 / totalForPercent
                    : 0;

            addCropCard(entry.getKey(), entry.getValue(), percentage);
        }
    }

    private JSONObject findFarmer(String farmerId) {
        for (JSONObject farmer : allFarmers) {
            if (farmer.optString("id", "").equals(farmerId)
                    || farmer.optString("farmerId", "").equals(farmerId)) {
                return farmer;
            }
        }

        return null;
    }

    private boolean matchesLocation(
            JSONObject farmer,
            String state,
            String district,
            String block,
            String panchayat
    ) {
        return matchesField(farmer, "state", state)
                && matchesField(farmer, "district", district)
                && matchesField(farmer, "block", block)
                && matchesField(farmer, "panchayat", panchayat);
    }

    private boolean matchesField(
            JSONObject farmer,
            String field,
            String selectedValue
    ) {
        if (isAll(selectedValue)) {
            return true;
        }

        return farmer.optString(field, "").trim()
                .equalsIgnoreCase(selectedValue.trim());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()
                || value.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void addCropCard(
            String cropName,
            double area,
            double percentage
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.bg_spinner_box);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setText(cropName);
        name.setTextSize(17);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(0xFF23482D);

        TextView details = new TextView(this);
        details.setText(String.format(
                Locale.getDefault(),
                "Area: %.2f acres\nShare: %.1f%%",
                area, percentage
        ));
        details.setTextSize(14);
        details.setTextColor(0xFF344638);

        card.addView(name);
        card.addView(details);

        layoutCropGraph.addView(card);
    }

    private void addResultText(String message, boolean bold) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(15);
        text.setTextColor(0xFF344638);
        text.setPadding(8, 16, 8, 16);

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        layoutCropGraph.addView(text);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
