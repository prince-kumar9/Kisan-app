package com.example.kisanapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kisanapp.R;
import com.example.kisanapp.dashboard.DashboardActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etMobile, etEmail, etPassword;
    private Spinner spinnerState, spinnerDistrict;
    private Spinner spinnerBlock, spinnerPanchayat;
    private Button btnRegister;
    private ScrollView scrollRegister;

    private JSONObject locationData;

    private static final String REGISTER_URL =
            "http://10.0.2.2:8080/api/farmers/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        scrollRegister = findViewById(R.id.scrollRegister);

        etName = findViewById(R.id.etName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        spinnerState = findViewById(R.id.spinnerState);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerBlock = findViewById(R.id.spinnerBlock);
        spinnerPanchayat = findViewById(R.id.spinnerPanchayat);

        btnRegister = findViewById(R.id.btnRegister);

        loadLocationData();

        btnRegister.setOnClickListener(v -> registerFarmer());
    }

    // Load location JSON from assets
    private void loadLocationData() {
        try {
            InputStream inputStream =
                    getAssets().open("bihar_locations.json");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream)
            );

            StringBuilder jsonText = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }

            reader.close();
            inputStream.close();

            locationData = new JSONObject(jsonText.toString());
            setupStateDropdown();

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Location data load nahi hua: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private List<String> getKeys(JSONObject object) {
        List<String> keys = new ArrayList<>();

        if (object == null) {
            return keys;
        }

        Iterator<String> iterator = object.keys();

        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }

        return keys;
    }

    private void setSpinnerOptions(
            Spinner spinner,
            List<String> options,
            String placeholder
    ) {
        List<String> items = new ArrayList<>();
        items.add(placeholder);
        items.addAll(options);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }

    private void setupStateDropdown() {
        setSpinnerOptions(
                spinnerState,
                getKeys(locationData),
                "Select State"
        );

        setSpinnerOptions(
                spinnerDistrict,
                new ArrayList<>(),
                "Select District"
        );

        setSpinnerOptions(
                spinnerBlock,
                new ArrayList<>(),
                "Select Block"
        );

        setSpinnerOptions(
                spinnerPanchayat,
                new ArrayList<>(),
                "Select Panchayat"
        );

        spinnerState.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        String state = position == 0
                                ? ""
                                : parent.getItemAtPosition(position).toString();

                        JSONObject districts =
                                locationData.optJSONObject(state);

                        setSpinnerOptions(
                                spinnerDistrict,
                                getKeys(districts),
                                "Select District"
                        );

                        resetBlockAndPanchayat();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        setupDistrictDropdown();
    }

    private void setupDistrictDropdown() {
        spinnerDistrict.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        String state = getSelected(spinnerState);

                        String district = position == 0
                                ? ""
                                : parent.getItemAtPosition(position).toString();

                        JSONObject districts =
                                locationData.optJSONObject(state);

                        JSONObject blocks = districts == null
                                ? null
                                : districts.optJSONObject(district);

                        setSpinnerOptions(
                                spinnerBlock,
                                getKeys(blocks),
                                "Select Block"
                        );

                        resetPanchayat();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        setupBlockDropdown();
    }

    private void setupBlockDropdown() {
        spinnerBlock.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        String state = getSelected(spinnerState);
                        String district = getSelected(spinnerDistrict);

                        String block = position == 0
                                ? ""
                                : parent.getItemAtPosition(position).toString();

                        JSONObject districts =
                                locationData.optJSONObject(state);

                        JSONObject blocks = districts == null
                                ? null
                                : districts.optJSONObject(district);

                        JSONObject panchayats = blocks == null
                                ? null
                                : blocks.optJSONObject(block);

                        setSpinnerOptions(
                                spinnerPanchayat,
                                getKeys(panchayats),
                                "Select Panchayat"
                        );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    private void resetBlockAndPanchayat() {
        setSpinnerOptions(
                spinnerBlock,
                new ArrayList<>(),
                "Select Block"
        );

        resetPanchayat();
    }

    private void resetPanchayat() {
        setSpinnerOptions(
                spinnerPanchayat,
                new ArrayList<>(),
                "Select Panchayat"
        );
    }

    private String getSelected(Spinner spinner) {
        if (spinner.getSelectedItemPosition() <= 0
                || spinner.getSelectedItem() == null) {
            return "";
        }

        return spinner.getSelectedItem().toString();
    }

    private void scrollToField(View field) {
        field.requestFocus();

        scrollRegister.post(() ->
                scrollRegister.smoothScrollTo(0, field.getTop())
        );
    }

    // Validate form and send registration to Spring Boot
    private void registerFarmer() {

        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        String state = getSelected(spinnerState);
        String district = getSelected(spinnerDistrict);
        String block = getSelected(spinnerBlock);
        String panchayat = getSelected(spinnerPanchayat);

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name enter karein");
            scrollToField(etName);
            return;
        }

        if (!mobile.matches("[0-9]{10}")) {
            etMobile.setError("10 digit mobile number enter karein");
            scrollToField(etMobile);
            return;
        }

        if (TextUtils.isEmpty(email)
                || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            etEmail.setError("Valid email enter karein");
            scrollToField(etEmail);
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(
                    "Password kam se kam 6 characters ka ho"
            );
            scrollToField(etPassword);
            return;
        }

        if (TextUtils.isEmpty(state)) {
            Toast.makeText(this, "State select karein",
                    Toast.LENGTH_SHORT).show();
            scrollToField(spinnerState);
            return;
        }

        if (TextUtils.isEmpty(district)) {
            Toast.makeText(this, "District select karein",
                    Toast.LENGTH_SHORT).show();
            scrollToField(spinnerDistrict);
            return;
        }

        if (TextUtils.isEmpty(block)) {
            Toast.makeText(this, "Block select karein",
                    Toast.LENGTH_SHORT).show();
            scrollToField(spinnerBlock);
            return;
        }

        if (TextUtils.isEmpty(panchayat)) {
            Toast.makeText(this, "Panchayat select karein",
                    Toast.LENGTH_SHORT).show();
            scrollToField(spinnerPanchayat);
            return;
        }

        // Prevent repeated registration taps
        btnRegister.setEnabled(false);

        // Send request in background thread
        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                JSONObject requestData = new JSONObject();

                requestData.put("name", name);
                requestData.put("mobile", mobile);
                requestData.put("email", email);
                requestData.put("password", password);
                requestData.put("state", state);
                requestData.put("district", district);
                requestData.put("block", block);
                requestData.put("panchayat", panchayat);
                requestData.put("village", "");

                URL url = new URL(REGISTER_URL);

                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                byte[] requestBytes = requestData.toString()
                        .getBytes(StandardCharsets.UTF_8);

                try (OutputStream outputStream =
                             connection.getOutputStream()) {
                    outputStream.write(requestBytes);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream responseStream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder responseText = new StringBuilder();

                if (responseStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    responseStream,
                                    StandardCharsets.UTF_8
                            ))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            responseText.append(line);
                        }
                    }
                }

                final int resultCode = responseCode;
                final String resultText = responseText.toString();

                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);

                    if (resultCode >= 200 && resultCode < 300) {
                        Toast.makeText(
                                RegisterActivity.this,
                                "Registration successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        // Return to Login so the new account can be tested.
                        Intent intent = new Intent(
                                RegisterActivity.this,
                                LoginActivity.class
                        );

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();

                    } else {
                        String message =
                                "Registration failed. HTTP "
                                        + resultCode;

                        try {
                            JSONObject errorJson =
                                    new JSONObject(resultText);

                            if (errorJson.has("message")) {
                                message = errorJson.optString(
                                        "message", message
                                );
                            } else if (errorJson.has("error")) {
                                message = errorJson.optString(
                                        "error", message
                                );
                            }
                        } catch (Exception ignored) {
                        }

                        Toast.makeText(
                                RegisterActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnRegister.setEnabled(true);

                    Toast.makeText(
                            RegisterActivity.this,
                            "Backend connection failed. "
                                    + "Backend running hai check karein.",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}
