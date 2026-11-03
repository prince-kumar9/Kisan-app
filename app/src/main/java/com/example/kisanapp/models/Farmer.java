package com.example.kisanapp.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Farmer {

    private String name;
    private String mobile;
    private String email;
    private String password;
    private String state;
    private String district;
    private String block;
    private String panchayat;
    private String village;

    public Farmer() {
    }

    public Farmer(String name, String mobile, String email,
                  String password, String state, String district,
                  String block, String panchayat, String village) {
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.password = password;
        this.state = state;
        this.district = district;
        this.block = block;
        this.panchayat = panchayat;
        this.village = village;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public String getPanchayat() {
        return panchayat;
    }

    public void setPanchayat(String panchayat) {
        this.panchayat = panchayat;
    }

    public String getVillage() {
        return village;
    }

    public void setVillage(String village) {
        this.village = village;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("name", name);
        json.put("mobile", mobile);
        json.put("email", email);
        json.put("password", password);
        json.put("state", state);
        json.put("district", district);
        json.put("block", block);
        json.put("panchayat", panchayat);
        json.put("village", village);

        return json;
    }

    public static Farmer fromJson(JSONObject json) throws JSONException {
        Farmer farmer = new Farmer();

        farmer.setName(json.optString("name", ""));
        farmer.setMobile(json.optString("mobile", ""));
        farmer.setEmail(json.optString("email", ""));
        farmer.setPassword(json.optString("password", ""));
        farmer.setState(json.optString("state", ""));
        farmer.setDistrict(json.optString("district", ""));
        farmer.setBlock(json.optString("block", ""));
        farmer.setPanchayat(json.optString("panchayat", ""));
        farmer.setVillage(json.optString("village", ""));

        return farmer;
    }
}