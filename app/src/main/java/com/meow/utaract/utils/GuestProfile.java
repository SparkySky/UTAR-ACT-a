package com.meow.utaract.utils;

import java.util.List;

public class GuestProfile {
    private String name;
    private String email;
    private String phone;
    private List<String> preferences;
    private String profileImageUrl; // New field for profile picture URL

    // Constructor
    public GuestProfile(String name, String email, String phone, List<String> preferences) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.profileImageUrl = ""; // Default to empty string
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<String> getPreferences() { return preferences; }
    public void setPreferences(List<String> preferences) { this.preferences = preferences; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}