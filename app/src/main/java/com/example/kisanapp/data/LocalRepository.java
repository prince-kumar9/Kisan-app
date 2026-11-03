
package com.example.kisanapp.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.kisanapp.models.Crop;
import com.example.kisanapp.models.Farmer;
import com.example.kisanapp.models.Listing;

import java.util.ArrayList;
import java.util.List;

/**
 * LocalRepository
 *
 * Legacy local storage is disabled.
 * Farmer, crop and listing data must be loaded and saved
 * through the Spring Boot backend API.
 *
 * This class intentionally does not save or return old local records.
 */
public class LocalRepository {

    private static final String PREF_NAME = "KisanAppData";

    private final SharedPreferences preferences;

    public LocalRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    /**
     * Run once if you want to remove all legacy local data.
     * Does not affect SQL/backend data.
     */
    public void clearAllLocalData() {
        preferences.edit().clear().apply();
    }

    // ================= FARMER =================

    public boolean registerFarmer(Farmer farmer) {
        // Register through backend API instead.
        return false;
    }

    public Farmer getFarmerByMobile(String mobile) {
        // Fetch farmer from backend API instead.
        return null;
    }

    public Farmer loginFarmer(String mobile, String password) {
        // Authenticate through backend API instead.
        return null;
    }

    public boolean updateFarmer(Farmer updatedFarmer) {
        // Update through backend API instead.
        return false;
    }

    public void setCurrentFarmerMobile(String mobile) {
        // Do not persist the farmer locally.
        // Use backend authentication/session handling instead.
    }

    public String getCurrentFarmerMobile() {
        return null;
    }

    public Farmer getCurrentFarmer() {
        return null;
    }

    public void logout() {
        // Clear the actual backend authentication session/token here,
        // when your app's authentication implementation supports it.
    }

    // ================= CROPS =================

    public boolean saveCrop(Crop crop) {
        // Save through POST/PUT /api/crops.
        return false;
    }

    public List<Crop> getAllCrops() {
        // Load through the backend API.
        return new ArrayList<>();
    }

    public List<Crop> getCropsByFarmer(String farmerMobile) {
        // Load the current farmer's crops through the backend API.
        return new ArrayList<>();
    }

    public boolean deleteCrop(String cropId) {
        // Delete through the backend API.
        return false;
    }

    // ================= MARKETPLACE LISTINGS =================

    public boolean saveListing(Listing listing) {
        // Save through the backend API.
        return false;
    }

    public List<Listing> getAllListings() {
        // Load through the backend API.
        return new ArrayList<>();
    }

    public List<Listing> getListingsByFarmer(String farmerMobile) {
        // Load the farmer's listings through the backend API.
        return new ArrayList<>();
    }

    public boolean deleteListing(String listingId) {
        // Delete through the backend API.
        return false;
    }
}
