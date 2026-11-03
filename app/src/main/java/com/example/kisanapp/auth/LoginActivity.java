
package com.example.kisanapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kisanapp.R;
import com.example.kisanapp.dashboard.DashboardActivity;
import com.example.kisanapp.models.Farmer;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private EditText etMobile, etPassword;
    private Button btnLogin;
    private TextView btnGoRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        btnLogin.setOnClickListener(v -> loginFarmer());

        btnGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    private void loginFarmer() {
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(mobile)) {
            etMobile.setError("Mobile number enter karein");
            etMobile.requestFocus();
            return;
        }

        if (!mobile.matches("[0-9]{10}")) {
            etMobile.setError("10 digit ka valid mobile number enter karein");
            etMobile.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password enter karein");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "http://10.0.2.2:8080/api/farmers/login"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty(
                        "Content-Type", "application/json"
                );
                connection.setRequestProperty(
                        "Accept", "application/json"
                );
                connection.setDoOutput(true);

                JSONObject requestBody = new JSONObject();
                requestBody.put("mobile", mobile);
                requestBody.put("password", password);

                try (OutputStream outputStream =
                             connection.getOutputStream()) {
                    outputStream.write(
                            requestBody.toString().getBytes("UTF-8")
                    );
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream =
                        responseCode >= 200 && responseCode < 300
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    JSONObject farmerJson =
                            new JSONObject(response.toString());

                    String farmerName =
                            farmerJson.optString("name", "Farmer");

                    long farmerId =
                            farmerJson.optLong("id", -1);

                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);

                        Toast.makeText(
                                LoginActivity.this,
                                "Login successful! Welcome " + farmerName,
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                LoginActivity.this,
                                DashboardActivity.class
                        );

                        intent.putExtra("farmerId", farmerId);
                        intent.putExtra("farmerName", farmerName);

                        intent.setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    });

                } else {
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);

                        Toast.makeText(
                                LoginActivity.this,
                                "Invalid mobile number or password",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);

                    Toast.makeText(
                            LoginActivity.this,
                            "Backend connection failed. Check server and try again.",
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
