package com.meow.utaract.ui.home;

public class Event {
    private String title;
    private String date;
    private String location;
    private String audience;
    private String category;
    private String dateBadge;
    private int bannerResId;

    public Event(String title, String date, String location, String audience,
                 String category, String dateBadge, int bannerResId) {
        this.title = title;
        this.date = date;
        this.location = location;
        this.audience = audience;
        this.category = category;
        this.dateBadge = dateBadge;
        this.bannerResId = bannerResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getAudience() {
        return audience;
    }

    public String getCategory() {
        return category;
    }

    public String getDateBadge() {
        return dateBadge;
    }

    public int getBannerResId() {
        return bannerResId;
    }
}