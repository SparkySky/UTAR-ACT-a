package com.meow.utaract.utils;

import java.util.ArrayList;
import java.util.List;

public class Event {
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
    private String organizerName;
    private long createdAt;
    private int maxGuests;
    private double fee;

    public Event() {
        // Default constructor required for Firestore
        this.additionalImageUrls = new ArrayList<>();
    }

    public Event(String eventName, String description, String category, String date,
                 String time, String location, String organizerId, String organizerName, int maxGuests, double fee) {
        this.eventName = eventName;
        this.description = description;
        this.category = category;
        this.date = date;
        this.time = time;
        this.location = location;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.additionalImageUrls = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.maxGuests = maxGuests;
        this.fee = fee;
    }

    // --- Getters and Setters for all fields ---

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

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }
}