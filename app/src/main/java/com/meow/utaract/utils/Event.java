package com.meow.utaract.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Implement Serializable to allow passing Event objects between activities
public class Event implements Serializable {
    private String eventId;
    private String eventName;
    private String description;
    private String category;
    private String date;
    private String time;
    private String location;
    private String coverImageUrl;
    private List<String> additionalImageUrls;
    private String organizerId;
    private long createdAt;
    private int maxGuests;
    private double fee;

    // New fields for visibility and scheduling
    private boolean isVisible;
    private long publishAt;

    public Event() {
        // Default constructor
        this.additionalImageUrls = new ArrayList<>();
    }

    public Event(String eventName, String description, String category, String date,
                 String time, String location, String organizerId, int maxGuests, double fee, boolean isVisible, long publishAt) {
        this.eventName = eventName;
        this.description = description;
        this.category = category;
        this.date = date;
        this.time = time;
        this.location = location;
        this.organizerId = organizerId;
        this.createdAt = System.currentTimeMillis();
        this.maxGuests = maxGuests;
        this.fee = fee;
        this.isVisible = isVisible;
        this.publishAt = publishAt;
        this.coverImageUrl = "";
        this.additionalImageUrls = new ArrayList<>();
    }

    // --- Getters and Setters for all fields (including new ones) ---

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public long getPublishAt() { return publishAt; }
    public void setPublishAt(long publishAt) { this.publishAt = publishAt; }

    // (All other getters and setters remain)
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public List<String> getAdditionalImageUrls() { return additionalImageUrls; }
    public void setAdditionalImageUrls(List<String> additionalImageUrls) { this.additionalImageUrls = additionalImageUrls; }
    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }
    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }
}