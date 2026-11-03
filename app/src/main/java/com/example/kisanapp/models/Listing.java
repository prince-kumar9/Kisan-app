package com.example.kisanapp.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Listing {

    private String id;
    private String farmerMobile;
    private String farmerName;
    private String cropName;
    private double quantity;
    private String quantityUnit;
    private double rate;
    private String location;
    private String photoUri;
    private String availability;
    private String status;

    public Listing() {
    }

    public Listing(String id, String farmerMobile, String farmerName,
                   String cropName, double quantity, String quantityUnit,
                   double rate, String location, String photoUri,
                   String availability, String status) {
        this.id = id;
        this.farmerMobile = farmerMobile;
        this.farmerName = farmerName;
        this.cropName = cropName;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.rate = rate;
        this.location = location;
        this.photoUri = photoUri;
        this.availability = availability;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerMobile() {
        return farmerMobile;
    }

    public void setFarmerMobile(String farmerMobile) {
        this.farmerMobile = farmerMobile;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("id", id);
        json.put("farmerMobile", farmerMobile);
        json.put("farmerName", farmerName);
        json.put("cropName", cropName);
        json.put("quantity", quantity);
        json.put("quantityUnit", quantityUnit);
        json.put("rate", rate);
        json.put("location", location);
        json.put("photoUri", photoUri);
        json.put("availability", availability);
        json.put("status", status);

        return json;
    }

    public static Listing fromJson(JSONObject json) throws JSONException {
        Listing listing = new Listing();

        listing.setId(json.optString("id", ""));
        listing.setFarmerMobile(json.optString("farmerMobile", ""));
        listing.setFarmerName(json.optString("farmerName", ""));
        listing.setCropName(json.optString("cropName", ""));
        listing.setQuantity(json.optDouble("quantity", 0.0));
        listing.setQuantityUnit(json.optString("quantityUnit", "kg"));
        listing.setRate(json.optDouble("rate", 0.0));
        listing.setLocation(json.optString("location", ""));
        listing.setPhotoUri(json.optString("photoUri", ""));
        listing.setAvailability(json.optString("availability", ""));
        listing.setStatus(json.optString("status", "Available"));

        return listing;
    }
}