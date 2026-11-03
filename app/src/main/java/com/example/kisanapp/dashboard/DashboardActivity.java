package com.example.kisanapp.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kisanapp.R;
import com.example.kisanapp.crops.CropEntryActivity;
import com.example.kisanapp.profile.FarmerProfileActivity;
import com.example.kisanapp.marketplace.MarketplaceActivity;
import com.example.kisanapp.analysis.CropAnalysisActivity;

public class DashboardActivity extends AppCompatActivity {

    private long farmerId = -1L;
    private String farmerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        farmerId = getIntent().getLongExtra("farmerId", -1L);

        String receivedName = getIntent().getStringExtra("farmerName");
        farmerName = receivedName == null ? "" : receivedName;

        findViewById(R.id.btnAddCrop).setOnClickListener(v -> {
            if (!checkLogin()) return;

            Intent intent = createFarmerIntent(CropEntryActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMyAccount).setOnClickListener(v -> {
            if (!checkLogin()) return;

            Intent intent = createFarmerIntent(FarmerProfileActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMarketing).setOnClickListener(v -> {
            if (!checkLogin()) return;

            Intent intent = createFarmerIntent(MarketplaceActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnCropAnalysis).setOnClickListener(v -> {
            Intent intent = createFarmerIntent(CropAnalysisActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> finish());
    }

    private boolean checkLogin() {
        if (farmerId <= 0) {
            Toast.makeText(
                    this,
                    "Farmer ID nahi mili. Logout karke dobara login karein.",
                    Toast.LENGTH_LONG
            ).show();
            return false;
        }

        return true;
    }

    private Intent createFarmerIntent(Class<?> destination) {
        Intent intent = new Intent(this, destination);
        intent.putExtra("farmerId", farmerId);
        intent.putExtra("farmerName", farmerName);
        return intent;
    }
}
