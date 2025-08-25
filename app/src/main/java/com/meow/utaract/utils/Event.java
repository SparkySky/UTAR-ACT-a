package com.meow.utaract.utils;

public class Event {
    private String id;
    private String title;
    private String description;
    private String date;
    private String time;
    private String location;
    private String audience;
    private String category;
    private String organizer;
    private String bannerImageUrl;
    private String dateBadge;
    private String categoryTag;

    public Event() {
        // Required empty constructor for Firebase
    }

    public Event(String id, String title, String description, String date, String time, 
                 String location, String audience, String category, String organizer, 
                 String bannerImageUrl, String dateBadge, String categoryTag) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.location = location;
        this.audience = audience;
        this.category = category;
        this.organizer = organizer;
        this.bannerImageUrl = bannerImageUrl;
        this.dateBadge = dateBadge;
        this.categoryTag = categoryTag;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getLocation() { return location; }
    public String getAudience() { return audience; }
    public String getCategory() { return category; }
    public String getOrganizer() { return organizer; }
    public String getBannerImageUrl() { return bannerImageUrl; }
    public String getDateBadge() { return dateBadge; }
    public String getCategoryTag() { return categoryTag; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setLocation(String location) { this.location = location; }
    public void setAudience(String audience) { this.audience = audience; }
    public void setCategory(String category) { this.category = category; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }
    public void setDateBadge(String dateBadge) { this.dateBadge = dateBadge; }
    public void setCategoryTag(String categoryTag) { this.categoryTag = categoryTag; }
}
