package com.meow.utaract.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GuestProfile implements Serializable {

    private String name; // Store Name
    private String email;// Store Email
    private String phone; // Store Phone Number
    private String profileImageUrl; // Store Profile Image URL
    private List<String> preferences; // Store Food Preferences
    private List<String> following; // Store Follower

    public List<String> getFollowing() { return following; }

    public void setFollowing(List<String> following) { this.following = following; }

    public void addFollowing(String organizerId) {
        if (following == null) {
            following = new ArrayList<>();
        }
        if (!following.contains(organizerId)) {
            following.add(organizerId);
        }
    }

    public void removeFollowing(String organizerId) {
        if (following != null) {
            following.remove(organizerId);
        }
    }
    private List<String> likedNews; // News IDs that this guest has liked

    public List<String> getLikedNews() { return likedNews; }
    public void setLikedNews(List<String> likedNews) { this.likedNews = likedNews; }
    // Constructor

    public GuestProfile(String name, String email, String phone, List<String> preferences) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.profileImageUrl = "";
        this.following = new ArrayList<>();
        this.socialMediaLink = "";
        this.socialMediaPlatform = "None";
    }

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

