package com.meow.utaract.utils;

import androidx.annotation.Keep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Keep
public class Event implements Serializable {
    private String eventId;
    private String eventName;
    private String description;
    private String summary;
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
    private long publishAt; // The ONLY field for visibility control
    private String socialMediaLink;
    private String uploadedDocumentText; // Store the text content of uploaded documents
    private String uploadedDocumentName; // Store the original name of uploaded documents

    public String getSocialMediaLink() {
        return socialMediaLink;
    }
    public void setSocialMediaLink(String socialMediaLink) {
        this.socialMediaLink = socialMediaLink;
    }
    public String getUploadedDocumentText() {
        return uploadedDocumentText;
    }
    public void setUploadedDocumentText(String uploadedDocumentText) {
        this.uploadedDocumentText = uploadedDocumentText;
    }
    public String getUploadedDocumentName() {
        return uploadedDocumentName;
    }
    public void setUploadedDocumentName(String uploadedDocumentName) {
        this.uploadedDocumentName = uploadedDocumentName;
    }

    public Event() {
        this.additionalImageUrls = new ArrayList<>();
    }

    // Constructor is now simpler, without isVisible
    public Event(String eventName, String description, String category, String date,
                 String time, String location, String organizerId, int maxGuests, double fee, long publishAt) {
        this.eventName = eventName;
        this.description = description;
        this.summary = "";
        this.category = category;
        this.date = date;
        this.time = time;
        this.location = location;
        this.organizerId = organizerId;
        this.createdAt = System.currentTimeMillis();
        this.maxGuests = maxGuests;
        this.fee = fee;
        this.publishAt = publishAt;
        this.coverImageUrl = "";
        this.additionalImageUrls = new ArrayList<>();
    }

    // --- Getters and Setters ---
    public long getPublishAt() { return publishAt; }
    public void setPublishAt(long publishAt) { this.publishAt = publishAt; }

    // (All other getters and setters remain the same)
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
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