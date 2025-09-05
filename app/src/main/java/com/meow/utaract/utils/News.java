package com.meow.utaract.utils;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class News implements Serializable {
    private String newsId;
    private String organizerId;
    private String organizerName;
    private String title;
    private String message;
    private List<String> imageUrls;
    private long createdAt;
    private long updatedAt;
    private Map<String, Boolean> likes; // userId -> true if liked

    public News() {
        this.imageUrls = new ArrayList<>();
        this.likes = new HashMap<>();
    }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public News(String organizerId, String organizerName, String title, String message) {
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.title = title;
        this.message = message;
        this.imageUrls = new ArrayList<>();
        this.likes = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getNewsId() { return newsId; }
    public void setNewsId(String newsId) { this.newsId = newsId; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Boolean> getLikes() { return likes; }
    public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }

    public int getLikeCount() { return likes != null ? likes.size() : 0; }

    public boolean isLikedByUser(String userId) {
        return likes != null && likes.containsKey(userId) && likes.get(userId);
    }

    public int getTotalLikeCount(Context context) {
        int firestoreLikes = getLikeCount(); // From Firestore

        // Get local guest likes
        GuestProfileStorage storage = new GuestProfileStorage(context);
        GuestProfile profile = storage.loadProfile();
        int localLikes = 0;

        if (profile != null && profile.getLikedNews() != null &&
                profile.getLikedNews().contains(getNewsId())) {
            localLikes = 1; // This guest has liked it
        }

        return firestoreLikes + localLikes;
    }

    // Add this method to check if current user liked (both types)
    public boolean isLikedByCurrentUser(Context context, boolean isOrganiser) {
        if (isOrganiser) {
            // Organizers: check Firestore
            String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            return isLikedByUser(userId);
        } else {
            // Guests: check local storage
            GuestProfileStorage storage = new GuestProfileStorage(context);
            GuestProfile profile = storage.loadProfile();
            return profile != null && profile.getLikedNews() != null &&
                    profile.getLikedNews().contains(getNewsId());
        }
    }
}