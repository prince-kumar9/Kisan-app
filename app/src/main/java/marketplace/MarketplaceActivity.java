package com.example.kisanapp.marketplace;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MarketplaceActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080";
    private static final int GREEN = 0xFF2E7D32;
    private static final int DARK_GREEN = 0xFF1B5E20;
    private static final int CARD_GREEN = 0xFFE8F5E9;

    // Buy section ki search state save rahegi.
    private static final String MARKETPLACE_PREFS =
            "marketplace_preferences";
    private static final String KEY_LAST_AREA = "last_searched_area";
    private static final String KEY_LAST_CROP = "last_searched_crop";
    private static final String KEY_HAS_SEARCHED = "has_searched";
    private static final String KEY_SEARCH_CLEARED = "search_cleared";

    // Purani API request ko naye search ke results overwrite karne se rokta hai.
    private int buyListingsRequestVersion = 0;

    private Button btnBuy, btnSell, btnBack;
    private LinearLayout layoutMarketplaceContent;
    private TextView tvMarketplaceTitle, tvMarketplaceInfo;

    private long farmerId = -1;
    private String farmerName = "Farmer";
    private String farmerMobile = "";
    private String farmerState = "";
    private String farmerDistrict = "";
    private String farmerBlock = "";
    private String farmerPanchayat = "";

    private JSONArray farmerCrops = new JSONArray();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marketplace);

        farmerId = getIntent().getLongExtra("farmerId", -1L);
        farmerName = getIntent().getStringExtra("farmerName");

        if (farmerName == null || farmerName.trim().isEmpty()) {
            farmerName = "Farmer";
        }

        initializeViews();
        setupButtons();
        showBuySection();

        if (farmerId > 0) {
            loadFarmerAndCrops();
        } else {
            Toast.makeText(
                    this,
                    "Farmer ID nahi mili. Dobara login karein.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void initializeViews() {
        btnBuy = findViewById(R.id.btnBuy);
        btnSell = findViewById(R.id.btnSell);
        btnBack = findViewById(R.id.btnBack);

        layoutMarketplaceContent =
                findViewById(R.id.layoutMarketplaceContent);

        tvMarketplaceTitle =
                findViewById(R.id.tvMarketplaceTitle);

        tvMarketplaceInfo =
                findViewById(R.id.tvMarketplaceInfo);
    }

    private void setupButtons() {
        btnBuy.setOnClickListener(v -> showBuySection());
        btnSell.setOnClickListener(v -> showSellSection());
        btnBack.setOnClickListener(v -> finish());
    }

    // LOAD FARMER PROFILE AND CROPS

    private void loadFarmerAndCrops() {
        executor.execute(() -> {
            JSONObject farmer = null;
            JSONArray crops = new JSONArray();
            String error = null;

            try {
                farmer = getObject(
                        BASE_URL + "/api/farmers/" + farmerId
                );

                crops = getArray(
                        BASE_URL + "/api/crops/farmer/" + farmerId
                );

            } catch (Exception e) {
                error = e.getMessage();
            }

            JSONObject finalFarmer = farmer;
            JSONArray finalCrops = crops;
            String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalError != null) {
                    Toast.makeText(
                            this,
                            "Farmer data load error: " + finalError,
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                if (finalFarmer != null) {
                    farmerName = finalFarmer.optString(
                            "name", farmerName
                    );
                    farmerMobile = finalFarmer.optString(
                            "mobile", ""
                    );
                    farmerState = finalFarmer.optString(
                            "state", ""
                    );
                    farmerDistrict = finalFarmer.optString(
                            "district", ""
                    );
                    farmerBlock = finalFarmer.optString(
                            "block", ""
                    );
                    farmerPanchayat = finalFarmer.optString(
                            "panchayat", ""
                    );
                }

                farmerCrops = finalCrops;

                if (isSellSectionVisible()) {
                    showSellSection();
                }
            });
        });
    }

    private boolean isSellSectionVisible() {
        return tvMarketplaceTitle.getText()
                .toString()
                .toLowerCase(Locale.ROOT)
                .contains("sell");
    }

    // BUY SECTION

    private void showBuySection() {
        tvMarketplaceTitle.setText("Buy Crops");
        tvMarketplaceInfo.setText(
                "Search by crop, location, or both. Type or select suggestions."
        );

        layoutMarketplaceContent.removeAllViews();
        setBuyButtonStyle();

        SharedPreferences preferences = getSharedPreferences(
                MARKETPLACE_PREFS,
                MODE_PRIVATE
        );

        addSectionHeading("Search Available Crops");

        layoutMarketplaceContent.addView(
                createLabel("Search by Area")
        );

        AutoCompleteTextView etArea =
                createAutoCompleteInput("Type or select an area");

        etArea.setText(
                preferences.getString(KEY_LAST_AREA, ""),
                false
        );

        layoutMarketplaceContent.addView(etArea);
        // Area suggestions ko Buy screen khulte hi load karo,
        // search button dabane ka wait nahi karna hai.
        loadAreaSuggestions(etArea);
        addBigGap();

        layoutMarketplaceContent.addView(
                createLabel("Search by Crop")
        );

        AutoCompleteTextView etCrop =
                createAutoCompleteInput("Type or select a crop");

        etCrop.setText(
                preferences.getString(KEY_LAST_CROP, ""),
                false
        );

        layoutMarketplaceContent.addView(etCrop);
        // Buy screen khulte hi latest crop list load karo.
        loadAllCropSuggestions(etCrop);
        // Agar baad mein naya crop add hua ho, crop field par focus karte hi
        // backend se latest list dobara load hogi.
        etCrop.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                loadAllCropSuggestions(etCrop);
            }
        });
        addBigGap();

        Button search = makeButton(
                "Search Crops", GREEN, 0xFFFFFFFF
        );

        layoutMarketplaceContent.addView(search, matchParams());
        addBigGap();

        Button clear = makeButton(
                "Clear Search", 0xFFDDEBDD, DARK_GREEN
        );

        layoutMarketplaceContent.addView(clear, matchParams());
        addBigGap();

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        layoutMarketplaceContent.addView(results);

        boolean hasSearched = preferences.getBoolean(
                KEY_HAS_SEARCHED, false
        );

        boolean searchCleared = preferences.getBoolean(
                KEY_SEARCH_CLEARED, false
        );

        if (searchCleared) {
            addMessageToLayout(
                    results,
                    "Search cleared. Available crops dekhne ke liye Search Crops dabayein."
            );
        } else if (hasSearched) {
            String savedArea = preferences.getString(
                    KEY_LAST_AREA, ""
            );

            String savedCrop = preferences.getString(
                    KEY_LAST_CROP, ""
            );

            loadAvailableListings(
                    results,
                    etArea,
                    etCrop,
                    savedArea,
                    savedCrop
            );
        } else {
            addMessageToLayout(
                    results,
                    "Area ya Crop enter karke Search Crops dabayein."
            );
        }

        search.setOnClickListener(v -> {
            String area = etArea.getText().toString().trim();
            String crop = etCrop.getText().toString().trim();

            if (area.isEmpty() && crop.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please enter Area or Crop before searching",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            preferences.edit()
                    .putString(KEY_LAST_AREA, area)
                    .putString(KEY_LAST_CROP, crop)
                    .putBoolean(KEY_HAS_SEARCHED, true)
                    .putBoolean(KEY_SEARCH_CLEARED, false)
                    .apply();

            loadAvailableListings(
                    results,
                    etArea,
                    etCrop,
                    area,
                    crop
            );
        });

        clear.setOnClickListener(v -> {
            // Purani request ke response ko results restore nahi karne dena.
            buyListingsRequestVersion++;

            etArea.setText("", false);
            etCrop.setText("", false);

            etArea.dismissDropDown();
            etCrop.dismissDropDown();

            preferences.edit()
                    .remove(KEY_LAST_AREA)
                    .remove(KEY_LAST_CROP)
                    .putBoolean(KEY_HAS_SEARCHED, false)
                    .putBoolean(KEY_SEARCH_CLEARED, true)
                    .apply();

            results.removeAllViews();

            TextView message = createLabel(
                    "Search cleared. Available crops dekhne ke liye Search Crops dabayein."
            );

            message.setPadding(4, 10, 4, 18);
            results.addView(message);
        });
    }
    private AutoCompleteTextView createAutoCompleteInput(String hint) {
        AutoCompleteTextView input =
                new AutoCompleteTextView(this);

        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setThreshold(1);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setPadding(20, 0, 20, 0);
        input.setBackgroundResource(R.drawable.bg_spinner_box);

        input.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        110
                )
        );

        return input;
    }

    private void setSuggestions(
            AutoCompleteTextView input,
            List<String> suggestions
    ) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        suggestions
                );

        input.setAdapter(adapter);
        input.setThreshold(1);

        input.setOnClickListener(v -> {
            if (input.getAdapter() != null
                    && input.getAdapter().getCount() > 0) {
                input.showDropDown();
            }
        });

        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus
                    && input.getAdapter() != null
                    && input.getAdapter().getCount() > 0) {
                input.post(input::showDropDown);
            }
        });
    }

    // Buy section ke liye available listings se area suggestions preload karo.
    // Backend listing mein location ya farmer ke address fields hon to unhe use karo.
    private void loadAreaSuggestions(AutoCompleteTextView etArea) {
        executor.execute(() -> {
            JSONArray listings = null;
            String error = null;

            try {
                listings = getArray(BASE_URL + "/api/listings");
            } catch (Exception e) {
                error = e.getMessage();
            }

            final JSONArray finalListings = listings;
            final String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || etArea.getParent() == null) {
                    return;
                }

                if (finalError != null || finalListings == null) {
                    // Listings loadAvailableListings() mein bhi check hongi.
                    return;
                }

                LinkedHashSet<String> areas = new LinkedHashSet<>();
                String[] fields = {
                        "location", "village", "panchayat", "block",
                        "district", "state", "area", "address", "locality"
                };

                for (int i = 0; i < finalListings.length(); i++) {
                    JSONObject listing = finalListings.optJSONObject(i);
                    if (listing == null) continue;

                    addAreaValues(areas, listing, fields);

                    // Kuch backend responses mein location farmer object ke andar ho sakti hai.
                    JSONObject farmer = listing.optJSONObject("farmer");
                    if (farmer != null) {
                        addAreaValues(areas, farmer, fields);
                        JSONObject profile = farmer.optJSONObject("profile");
                        if (profile != null) {
                            addAreaValues(areas, profile, fields);
                        }
                    }

                    JSONObject profile = listing.optJSONObject("farmerProfile");
                    if (profile != null) {
                        addAreaValues(areas, profile, fields);
                    }
                }

                setSuggestions(etArea, new ArrayList<>(areas));
            });
        });
    }

    private void addAreaValues(
            LinkedHashSet<String> areas,
            JSONObject object,
            String[] fields
    ) {
        for (String field : fields) {
            String value = object.optString(field, "").trim();
            if (value.isEmpty() || value.equalsIgnoreCase("null")
                    || value.equalsIgnoreCase("Not available")) {
                continue;
            }

            areas.add(value);
            // A combined location like "Village, Block, District" ko alag suggestions mein bhi dikhao.
            if (field.equals("location") || field.equals("address") || field.equals("area")) {
                for (String part : value.split(",")) {
                    String clean = part.trim();
                    if (!clean.isEmpty()) areas.add(clean);
                }
            }
        }
    }

    // Buy section ke liye sabhi registered crops backend se load karo.
    // Ye suggestions active listings par depend nahi karte.
    private void loadAllCropSuggestions(AutoCompleteTextView etCrop) {
        executor.execute(() -> {
            JSONArray crops = null;
            String error = null;

            try {
                crops = getArray(BASE_URL + "/api/crops/all");
            } catch (Exception e) {
                error = e.getMessage();
            }

            final JSONArray finalCrops = crops;
            final String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalError != null || finalCrops == null) {
                    Toast.makeText(
                            this,
                            "Crops load nahi hue: " + finalError,
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                LinkedHashSet<String> cropNames = new LinkedHashSet<>();

                for (int i = 0; i < finalCrops.length(); i++) {
                    JSONObject item = finalCrops.optJSONObject(i);

                    if (item == null) {
                        continue;
                    }

                    String name = item.optString("cropName", "").trim();

                    if (!name.isEmpty()) {
                        cropNames.add(name);
                    }
                }

                setSuggestions(etCrop, new ArrayList<>(cropNames));
            });
        });
    }

    // LOAD AVAILABLE LISTINGS

    private void loadAvailableListings(
            LinearLayout results,
            AutoCompleteTextView etArea,
            AutoCompleteTextView etCrop,
            String area,
            String crop
    ) {
        // Har request ko unique version dete hain.
        final int requestVersion = ++buyListingsRequestVersion;

        results.removeAllViews();

        addMessageToLayout(
                results,
                "Loading available listings from server..."
        );

        executor.execute(() -> {
            JSONArray listings = null;
            String error = null;

            try {
                listings = getArray(
                        BASE_URL + "/api/listings"
                );
            } catch (Exception e) {
                error = e.getMessage();
            }

            JSONArray finalListings = listings;
            String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Agar is beech naya search ya Clear Search hua,
                // to purani request ka result ignore hoga.
                if (requestVersion != buyListingsRequestVersion) {
                    return;
                }

                results.removeAllViews();

                if (finalError != null) {
                    addMessageToLayout(
                            results,
                            "Listings load nahi hui: " + finalError
                    );
                    return;
                }

                if (finalListings == null) {
                    addMessageToLayout(
                            results,
                            "Backend se response nahi mila."
                    );
                    return;
                }

                LinkedHashSet<String> areaSet =
                        new LinkedHashSet<>();

                // Available listings se suggestions banao.
                for (int i = 0;
                     i < finalListings.length();
                     i++) {

                    JSONObject item =
                            finalListings.optJSONObject(i);

                    if (item == null || !isAvailable(item)) {
                        continue;
                    }

                    String loc = item.optString(
                            "location", ""
                    ).trim();

                    String name = item.optString(
                            "cropName", ""
                    ).trim();

                    if (!loc.isEmpty()) {
                        areaSet.add(loc);

                        for (String part : loc.split(",")) {
                            String clean = part.trim();

                            if (!clean.isEmpty()) {
                                areaSet.add(clean);
                            }
                        }
                    }

                }

                setSuggestions(
                        etArea,
                        new ArrayList<>(areaSet)
                );

                int count = 0;

                String a = area.toLowerCase(Locale.ROOT);
                String c = crop.toLowerCase(Locale.ROOT);

                // Area aur crop ke hisaab se filtering.
                for (int i = 0;
                     i < finalListings.length();
                     i++) {

                    JSONObject item =
                            finalListings.optJSONObject(i);

                    if (item == null || !isAvailable(item)) {
                        continue;
                    }

                    String loc = item.optString(
                            "location", ""
                    ).toLowerCase(Locale.ROOT);

                    String name = item.optString(
                            "cropName", ""
                    ).toLowerCase(Locale.ROOT);

                    // Dono fields di hain to dono match honi chahiye.
                    if ((!a.isEmpty() && !loc.contains(a))
                            || (!c.isEmpty() && !name.contains(c))) {
                        continue;
                    }

                    addBuyListingCard(item, results);
                    count++;
                }

                TextView summary = createLabel(
                        count == 0
                                ? "No matching available listings found."
                                : "Showing " + count
                                  + " available listing(s)"
                );

                summary.setPadding(4, 10, 4, 18);
                results.addView(summary, 0);
            });
        });
    }

    private boolean isAvailable(JSONObject item) {
        String status = item.optString(
                "status",
                item.optString("availability", "Available")
        );

        return "Available".equalsIgnoreCase(status);
    }

    // BUY LISTING CARD

    private void addBuyListingCard(
            JSONObject item,
            LinearLayout target
    ) {
        LinearLayout card = createCard();

        card.addView(
                createCropTitle(
                        item.optString("cropName", "Crop")
                )
        );

        TextView details = new TextView(this);

        String unit = item.optString("quantityUnit", "kg");

        details.setText(
                "Quantity: "
                        + formatNumber(
                        item.optDouble("quantity", 0)
                )
                        + " " + unit

                        + "\n\nPrice: ₹"
                        + formatNumber(
                        item.optDouble("rate", 0)
                )
                        + " per " + unit

                        + "\n\nFarmer: "
                        + item.optString(
                        "farmerName", "Not available"
                )

                        + "\nMobile: "
                        + item.optString(
                        "farmerMobile", "Not available"
                )

                        + "\nLocation: "
                        + item.optString(
                        "location", "Not available"
                )

                        + "\n\nStart Date: "
                        + getDisplayDate(item, "startDate")

                        + "\nExpiry Date: "
                        + getDisplayDate(item, "endDate")

                        + "\n\nStatus: Available"
        );

        details.setTextSize(14);
        details.setTextColor(0xFF263A2B);
        details.setPadding(0, 8, 0, 10);

        card.addView(details);

        Button contact = makeButton(
                "Contact Farmer",
                GREEN,
                0xFFFFFFFF
        );

        card.addView(contact, matchParams());

        contact.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Farmer Contact")
                        .setMessage(
                                "Farmer: "
                                        + item.optString(
                                        "farmerName", "Not available"
                                )

                                        + "\n\nMobile: "
                                        + item.optString(
                                        "farmerMobile", "Not available"
                                )

                                        + "\n\nCrop: "
                                        + item.optString("cropName", "")

                                        + "\nQuantity: "
                                        + formatNumber(
                                        item.optDouble("quantity", 0)
                                )
                                        + " " + unit

                                        + "\nPrice: ₹"
                                        + formatNumber(
                                        item.optDouble("rate", 0)
                                )
                        )
                        .setPositiveButton("OK", null)
                        .show()
        );

        target.addView(card);
    }

    // SELL SECTION

    private void showSellSection() {
        tvMarketplaceTitle.setText("Sell Your Crops");

        tvMarketplaceInfo.setText(
                "Select your crop or choose Other to enter a crop name."
        );

        layoutMarketplaceContent.removeAllViews();
        setSellButtonStyle();

        if (farmerId <= 0 || farmerMobile.trim().isEmpty()) {
            addMessage(
                    "Farmer login details load nahi hue. "
                            + "Dashboard se Marketplace dobara open karein."
            );
            return;
        }

        addSectionHeading("Create New Listing");

        layoutMarketplaceContent.addView(
                createLabel("Select Crop")
        );

        Spinner cropSpinner = new Spinner(this);

        List<String> cropNames = new ArrayList<>();
        cropNames.add("Select Crop");

        for (int i = 0; i < farmerCrops.length(); i++) {
            JSONObject crop = farmerCrops.optJSONObject(i);

            if (crop != null) {
                String name = crop.optString(
                        "cropName", ""
                ).trim();

                if (!name.isEmpty()
                        && !cropNames.contains(name)) {
                    cropNames.add(name);
                }
            }
        }

        if (!cropNames.contains("Other")) {
            cropNames.add("Other");
        }

        ArrayAdapter<String> cropAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        cropNames
                );

        cropAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        cropSpinner.setAdapter(cropAdapter);

        layoutMarketplaceContent.addView(
                createSpinnerBox(cropSpinner)
        );

        addBigGap();

        EditText otherCrop =
                createInput("Enter your crop name");

        otherCrop.setVisibility(View.GONE);

        otherCrop.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );

        layoutMarketplaceContent.addView(otherCrop);

        cropSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        boolean other = position >= 0
                                && "Other".equals(
                                parent.getItemAtPosition(position)
                        );

                        otherCrop.setVisibility(
                                other ? View.VISIBLE : View.GONE
                        );

                        if (!other) {
                            otherCrop.setText("");
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        addBigGap();

        layoutMarketplaceContent.addView(
                createLabel("Quantity to Sell")
        );

        EditText quantity = createInput("Example: 100");

        quantity.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layoutMarketplaceContent.addView(quantity);
        addBigGap();

        layoutMarketplaceContent.addView(
                createLabel("Quantity Unit")
        );

        Spinner unitSpinner = new Spinner(this);

        String[] units = {
                "kg", "quintal", "ton", "bag"
        };

        ArrayAdapter<String> unitAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        units
                );

        unitAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        unitSpinner.setAdapter(unitAdapter);

        layoutMarketplaceContent.addView(
                createSpinnerBox(unitSpinner)
        );

        addBigGap();

        layoutMarketplaceContent.addView(
                createLabel("Price per Unit (₹)")
        );

        EditText rate = createInput("Example: 2500");

        rate.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layoutMarketplaceContent.addView(rate);
        addBigGap();

        Button create = makeButton(
                "Create Crop Listing",
                GREEN,
                0xFFFFFFFF
        );

        layoutMarketplaceContent.addView(
                create,
                matchParams()
        );

        create.setOnClickListener(v -> {
            String selectedCrop =
                    cropSpinner.getSelectedItem() == null
                            ? ""
                            : cropSpinner.getSelectedItem()
                            .toString().trim();

            if (selectedCrop.isEmpty()
                    || "Select Crop".equals(selectedCrop)) {
                toast("Please select a crop");
                return;
            }

            if ("Other".equalsIgnoreCase(selectedCrop)) {
                selectedCrop = otherCrop.getText()
                        .toString()
                        .trim();
            }

            if (selectedCrop.isEmpty()) {
                otherCrop.setError("Enter your crop name");
                return;
            }

            final String cropName = selectedCrop;

            try {
                double q = Double.parseDouble(
                        quantity.getText().toString().trim()
                );

                double r = Double.parseDouble(
                        rate.getText().toString().trim()
                );

                if (q <= 0 || r <= 0) {
                    toast(
                            "Quantity and price must be greater than zero"
                    );
                    return;
                }

                JSONObject body = new JSONObject();

                body.put("farmerMobile", farmerMobile);
                body.put("farmerName", farmerName);
                body.put("cropName", cropName);
                body.put("quantity", q);

                body.put(
                        "quantityUnit",
                        unitSpinner.getSelectedItem().toString()
                );

                body.put("rate", r);
                body.put("location", buildFarmerLocation());
                body.put("availability", "Available");
                body.put("status", "Available");

                postListing(body);

            } catch (NumberFormatException e) {
                toast("Enter valid quantity and price");

            } catch (Exception e) {
                toast("Listing data error: " + e.getMessage());
            }
        });

        addSectionHeading("My Listings");
        loadMyListings();
    }
    // CREATE LISTING

    private void postListing(JSONObject body) {
        executor.execute(() -> {
            String error = null;

            try {
                sendJson(
                        "POST",
                        BASE_URL + "/api/listings",
                        body
                );
            } catch (Exception e) {
                error = e.getMessage();
            }

            String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalError == null) {
                    toast("Crop listing created successfully");
                    showSellSection();
                } else {
                    toast("Unable to create listing: " + finalError);
                }
            });
        });
    }

    // MY LISTINGS

    private void loadMyListings() {
        if (farmerMobile.trim().isEmpty()) {
            addMessage("Farmer mobile number not available.");
            return;
        }

        addMessage("Loading your listings...");

        executor.execute(() -> {
            JSONArray arr = null;
            String error = null;

            try {
                arr = getArray(
                        BASE_URL + "/api/listings/farmer/" + farmerMobile
                );
            } catch (Exception e) {
                error = e.getMessage();
            }

            JSONArray finalArr = arr;
            String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                int start = Math.max(
                        0,
                        layoutMarketplaceContent.getChildCount() - 1
                );

                View last = layoutMarketplaceContent.getChildAt(start);

                if (last instanceof TextView
                        && ((TextView) last).getText()
                        .toString()
                        .startsWith("Loading your listings")) {
                    layoutMarketplaceContent.removeView(last);
                }

                if (finalError != null) {
                    addMessage(
                            "My listings load nahi hui: " + finalError
                    );
                    return;
                }

                if (finalArr == null || finalArr.length() == 0) {
                    addMessage(
                            "You have not created any listings yet."
                    );
                    return;
                }

                for (int i = 0; i < finalArr.length(); i++) {
                    JSONObject item = finalArr.optJSONObject(i);

                    if (item != null) {
                        addOwnListingCard(item);
                    }
                }
            });
        });
    }

    private void addOwnListingCard(JSONObject item) {
        LinearLayout card = createCard();

        card.addView(
                createCropTitle(
                        item.optString("cropName", "Crop")
                )
        );

        String unit = item.optString("quantityUnit", "kg");
        String status = item.optString("status", "Available");

        TextView details = new TextView(this);

        details.setText(
                "Quantity: "
                        + formatNumber(item.optDouble("quantity", 0))
                        + " " + unit

                        + "\n\nPrice: ₹"
                        + formatNumber(item.optDouble("rate", 0))
                        + " per " + unit

                        + "\n\nLocation: "
                        + item.optString("location", "Not available")

                        + "\n\nStart Date: "
                        + getDisplayDate(item, "startDate")

                        + "\nExpiry Date: "
                        + getDisplayDate(item, "endDate")

                        + "\n\nStatus: " + status
        );

        details.setTextSize(14);
        details.setTextColor(0xFF263A2B);
        details.setPadding(0, 8, 0, 8);

        card.addView(details);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button edit = makeButton(
                "Edit", 0xFFDDEBDD, DARK_GREEN
        );

        Button toggle = makeButton(
                "Available".equalsIgnoreCase(status)
                        ? "Mark Sold"
                        : "Mark Available",
                GREEN,
                0xFFFFFFFF
        );

        Button delete = makeButton(
                "Delete", 0xFFFFE0E0, 0xFFB71C1C
        );

        actions.addView(edit, createWeightParams());
        actions.addView(toggle, createWeightParams());
        actions.addView(delete, createWeightParams());

        card.addView(actions);

        edit.setOnClickListener(v -> showEditDialog(item));

        toggle.setOnClickListener(v -> {
            String next = "Available".equalsIgnoreCase(status)
                    ? "Sold"
                    : "Available";

            updateListing(
                    item,
                    item.optString("cropName", ""),
                    item.optDouble("quantity", 0),
                    unit,
                    item.optDouble("rate", 0),
                    item.optString("location", ""),
                    next
            );
        });

        delete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Listing")
                        .setMessage(
                                "Are you sure you want to delete this listing?"
                        )
                        .setPositiveButton(
                                "Delete",
                                (dialog, which) ->
                                        deleteListing(
                                                item.optLong("id", -1)
                                        )
                        )
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        layoutMarketplaceContent.addView(card);
    }

    private String getDisplayDate(JSONObject item, String key) {
        String date = item.optString(key, "");

        if (date.isEmpty() || "null".equalsIgnoreCase(date)) {
            return "Not available";
        }

        return date;
    }

    // EDIT LISTING

    private void showEditDialog(JSONObject item) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 8, 24, 8);

        EditText crop = createInput("Crop name");
        crop.setText(item.optString("cropName", ""));

        EditText quantity = createInput("Quantity");
        quantity.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        quantity.setText(
                String.valueOf(item.optDouble("quantity", 0))
        );

        EditText unit = createInput(
                "Quantity unit (kg/quintal/ton/bag)"
        );
        unit.setText(item.optString("quantityUnit", "kg"));

        EditText rate = createInput("Price per unit");
        rate.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        rate.setText(
                String.valueOf(item.optDouble("rate", 0))
        );

        EditText location = createInput("Location");
        location.setText(item.optString("location", ""));

        form.addView(crop);
        form.addView(quantity);
        form.addView(unit);
        form.addView(rate);
        form.addView(location);

        new AlertDialog.Builder(this)
                .setTitle("Edit Listing")
                .setView(form)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String name = crop.getText()
                                .toString()
                                .trim();

                        double q = Double.parseDouble(
                                quantity.getText().toString().trim()
                        );

                        String u = unit.getText()
                                .toString()
                                .trim();

                        double r = Double.parseDouble(
                                rate.getText().toString().trim()
                        );

                        if (name.isEmpty() || u.isEmpty()
                                || q <= 0 || r <= 0) {
                            toast(
                                    "Enter valid crop, quantity, unit and price"
                            );
                            return;
                        }

                        updateListing(
                                item,
                                name,
                                q,
                                u,
                                r,
                                location.getText().toString().trim(),
                                item.optString("status", "Available")
                        );

                    } catch (Exception e) {
                        toast("Enter valid quantity and price");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateListing(
            JSONObject old,
            String crop,
            double quantity,
            String unit,
            double rate,
            String location,
            String status
    ) {
        long id = old.optLong("id", -1);

        if (id <= 0) {
            toast("Invalid listing ID from backend");
            return;
        }

        executor.execute(() -> {
            String error = null;

            try {
                JSONObject body = new JSONObject();

                body.put("cropName", crop);
                body.put("quantity", quantity);
                body.put("quantityUnit", unit);
                body.put("rate", rate);
                body.put("location", location);
                body.put("farmerName", farmerName);
                body.put("availability", status);
                body.put("status", status);

                sendJson(
                        "PUT",
                        BASE_URL + "/api/listings/" + id,
                        body
                );

            } catch (Exception e) {
                error = e.getMessage();
            }

            String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalError == null) {
                    toast("Listing updated successfully");
                    showSellSection();
                } else {
                    toast("Update failed: " + finalError);
                }
            });
        });
    }

    // DELETE LISTING

    private void deleteListing(long id) {
        if (id <= 0) {
            toast("Invalid listing ID from backend");
            return;
        }

        executor.execute(() -> {
            String error = null;

            try {
                sendJson(
                        "DELETE",
                        BASE_URL + "/api/listings/" + id,
                        null
                );
            } catch (Exception e) {
                error = e.getMessage();
            }

            String finalError = error;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (finalError == null) {
                    toast("Listing deleted");
                    showSellSection();
                } else {
                    toast("Delete failed: " + finalError);
                }
            });
        });
    }

    // HTTP HELPERS

    private JSONObject getObject(String url) throws Exception {
        return new JSONObject(request("GET", url, null));
    }

    private JSONArray getArray(String url) throws Exception {
        return new JSONArray(request("GET", url, null));
    }

    private void sendJson(
            String method,
            String url,
            JSONObject body
    ) throws Exception {
        request(method, url, body);
    }

    private String request(
            String method,
            String urlString,
            JSONObject body
    ) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection =
                    (HttpURLConnection) new URL(urlString)
                            .openConnection();

            connection.setRequestMethod(method);
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty(
                    "Accept", "application/json"
            );

            if (body != null) {
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                try (OutputStream out = connection.getOutputStream()) {
                    out.write(
                            body.toString()
                                    .getBytes(StandardCharsets.UTF_8)
                    );
                }
            }

            int code = connection.getResponseCode();

            InputStream stream = (code >= 200 && code < 400)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String response = stream == null
                    ? ""
                    : readStream(stream);

            if (code < 200 || code >= 300) {
                String message = response;

                try {
                    JSONObject obj = new JSONObject(response);
                    message = obj.optString("message", response);
                } catch (Exception ignored) {
                    // Response JSON format mein nahi hai.
                }

                throw new Exception(
                        "HTTP " + code
                                + (message.isEmpty()
                                ? ""
                                : ": " + message)
                );
            }

            return response;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readStream(InputStream stream) throws Exception {
        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        stream,
                        StandardCharsets.UTF_8
                ))) {

            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    // LOCATION HELPERS

    private String buildFarmerLocation() {
        StringBuilder b = new StringBuilder();

        appendLocation(b, farmerPanchayat);
        appendLocation(b, farmerBlock);
        appendLocation(b, farmerDistrict);
        appendLocation(b, farmerState);

        return b.length() == 0
                ? "Location not available"
                : b.toString();
    }

    private void appendLocation(StringBuilder b, String part) {
        if (part != null && !part.trim().isEmpty()) {
            if (b.length() > 0) {
                b.append(", ");
            }

            b.append(part.trim());
        }
    }

    // UI HELPERS

    private FrameLayout createSpinnerBox(Spinner spinner) {
        FrameLayout frame = new FrameLayout(this);

        frame.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        110
                )
        );

        spinner.setBackgroundResource(R.drawable.bg_spinner_box);
        spinner.setPadding(20, 0, 65, 0);
        spinner.setGravity(Gravity.CENTER_VERTICAL);

        frame.addView(
                spinner,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        110
                )
        );

        TextView arrow = new TextView(this);
        arrow.setText("▼");
        arrow.setTextSize(16);
        arrow.setTextColor(GREEN);
        arrow.setGravity(Gravity.CENTER);
        arrow.setClickable(false);
        arrow.setFocusable(false);

        FrameLayout.LayoutParams ap =
                new FrameLayout.LayoutParams(
                        60,
                        110,
                        Gravity.END | Gravity.CENTER_VERTICAL
                );

        frame.addView(arrow, ap);

        return frame;
    }

    private EditText createInput(String hint) {
        EditText input = new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setPadding(20, 0, 20, 0);
        input.setBackgroundResource(R.drawable.bg_spinner_box);

        input.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        110
                )
        );

        return input;
    }

    private TextView createLabel(String text) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(DARK_GREEN);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, 0, 0, 8);

        return t;
    }

    private void addSectionHeading(String title) {
        TextView t = new TextView(this);

        t.setText(title);
        t.setTextSize(20);
        t.setTextColor(DARK_GREEN);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, 20, 0, 16);

        layoutMarketplaceContent.addView(t);
    }

    private void addMessage(String message) {
        addMessageToLayout(layoutMarketplaceContent, message);
    }

    private void addMessageToLayout(
            LinearLayout target,
            String message
    ) {
        TextView t = new TextView(this);

        t.setText(message);
        t.setTextSize(15);
        t.setTextColor(0xFF455A49);
        t.setPadding(12, 20, 12, 20);

        target.addView(t);
    }

    private LinearLayout createCard() {
        LinearLayout c = new LinearLayout(this);

        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(18, 16, 18, 16);
        c.setBackgroundColor(CARD_GREEN);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        p.setMargins(0, 0, 0, 14);
        c.setLayoutParams(p);

        return c;
    }

    private TextView createCropTitle(String name) {
        TextView t = new TextView(this);

        t.setText(
                name == null || name.trim().isEmpty()
                        ? "Crop"
                        : name
        );

        t.setTextSize(19);
        t.setTextColor(DARK_GREEN);
        t.setTypeface(null, android.graphics.Typeface.BOLD);

        return t;
    }

    private Button makeButton(
            String text,
            int background,
            int foreground
    ) {
        Button b = new Button(this);

        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(foreground);

        b.setBackgroundTintList(
                ColorStateList.valueOf(background)
        );

        return b;
    }

    private LinearLayout.LayoutParams matchParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams createWeightParams() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        p.setMargins(2, 0, 2, 0);

        return p;
    }

    private void addBigGap() {
        View gap = new View(this);

        gap.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        38
                )
        );

        layoutMarketplaceContent.addView(gap);
    }

    private String formatNumber(double value) {
        return value == (long) value
                ? String.valueOf((long) value)
                : String.valueOf(value);
    }

    private void toast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void setBuyButtonStyle() {
        btnBuy.setTextColor(0xFFFFFFFF);

        btnBuy.setBackgroundTintList(
                ColorStateList.valueOf(GREEN)
        );

        btnSell.setTextColor(0xFFFFFFFF);

        btnSell.setBackgroundTintList(
                ColorStateList.valueOf(DARK_GREEN)
        );
    }

    private void setSellButtonStyle() {
        btnBuy.setTextColor(0xFFFFFFFF);

        btnBuy.setBackgroundTintList(
                ColorStateList.valueOf(DARK_GREEN)
        );

        btnSell.setTextColor(0xFFFFFFFF);

        btnSell.setBackgroundTintList(
                ColorStateList.valueOf(GREEN)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
