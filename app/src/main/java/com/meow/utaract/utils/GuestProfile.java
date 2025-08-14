package com.meow.utaract.utils;

import java.util.List;

public class GuestProfile {
    private String name;
    private String email;
    private String phone;
    private List<String> preferences; // Changed from String to List<String>

    // Constructor
    public GuestProfile(String name, String email, String phone, List<String> preferences) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
    }

    // Getters (and setters if needed)
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    // You might also need setters if you plan to modify profiles after creation
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }
}