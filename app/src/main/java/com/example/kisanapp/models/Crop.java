package com.example.kisanapp.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Crop {

    private String id;
    private String farmerMobile;
    private String season;
    private String cropName;
    private double area;
    private String areaUnit;

    public Crop() {
    }

    public Crop(
            String id,
            String farmerMobile,
            String season,
            String cropName,
            double area,
            String areaUnit
    ) {
        this.id = id;
        this.farmerMobile = farmerMobile;
        this.season = season;
        this.cropName = cropName;
        this.area = area;
        this.areaUnit = areaUnit;
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

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getAreaUnit() {
        return areaUnit;
    }

    public void setAreaUnit(String areaUnit) {
        this.areaUnit = areaUnit;
    }

    // ================= JSON =================

    public JSONObject toJson() throws JSONException {

        JSONObject json = new JSONObject();

        json.put("id", id);
        json.put("farmerMobile", farmerMobile);
        json.put("season", season);
        json.put("cropName", cropName);
        json.put("area", area);
        json.put("areaUnit", areaUnit);

        return json;
    }

    public static Crop fromJson(JSONObject json) throws JSONException {

        Crop crop = new Crop();

        crop.setId(json.optString("id", ""));
        crop.setFarmerMobile(
                json.optString("farmerMobile", "")
        );
        crop.setSeason(
                json.optString("season", "")
        );
        crop.setCropName(
                json.optString("cropName", "")
        );
        crop.setArea(
                json.optDouble("area", 0.0)
        );
        crop.setAreaUnit(
                json.optString("areaUnit", "acre")
        );

        return crop;
    }
}