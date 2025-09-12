package com.meow.utaract.utils;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Keep
public class GuestProfile implements Serializable {
    @SerializedName("name")
    private String name;
    @SerializedName("email")
    private String email;
    @SerializedName("phone")
    private String phone;
    @SerializedName("profileImageUrl")
    private String profileImageUrl;
    @SerializedName("preferences")
    private List<String> preferences;
    @SerializedName("following")
    private List<String> following;
    public List<String> getFollowing() { return following; }

    public GuestProfile() {
        // You can initialize lists here to prevent them from being null.
        this.following = new ArrayList<>();
        this.preferences = new ArrayList<>();
    }

    public GuestProfile(String name, String email, String phone, List<String> preferences) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.profileImageUrl = "";
        this.following = new ArrayList<>();
    }

/*    public boolean isFollowing(String organizerId) {
        return following != null && following.contains(organizerId);
    }

    public void setFollowing(List<String> following) { this.following = following; }*/

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
