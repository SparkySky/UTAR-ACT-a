package com.meow.utaract.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GuestProfile implements Serializable {
    private String name;
    private String email;
    private String phone;
    private String profileImageUrl;
    private List<String> preferences;
    private List<String> following;
    private String socialMediaLink;
    private String socialMediaPlatform;
    private boolean isOrganiser;
    private boolean isGuest;

    public GuestProfile(String name, String email, String phone, List<String> preferences, boolean isOrganiser) {
        this(name, email, phone, preferences, false, true);
    }

    public GuestProfile(String name, String email, String phone, List<String> preferences, boolean isOrganiser, boolean isGuest) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.preferences = preferences;
        this.profileImageUrl = "";
        this.following = new ArrayList<>();
        this.socialMediaLink = "";
        this.socialMediaPlatform = "None";
        this.isOrganiser = isOrganiser;
        this.isGuest = isGuest;
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

    public List<String> getFollowing() { return following; }
    public void setFollowing(List<String> following) { this.following = following; }

    public String getSocialMediaLink() { return socialMediaLink; }
    public void setSocialMediaLink(String socialMediaLink) { this.socialMediaLink = socialMediaLink; }
    public String getSocialMediaPlatform() { return socialMediaPlatform; }
    public void setSocialMediaPlatform(String socialMediaPlatform) { this.socialMediaPlatform = socialMediaPlatform; }
    public boolean isOrganiser() { return isOrganiser; }
    public void setOrganiser(boolean organiser) { isOrganiser = organiser; }
    public boolean isGuest() {
        return isGuest;
    }
    public void setGuest(boolean guest) {
        this.isGuest = guest;
    }
}
