package com.meow.utaract.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class News {
    private String newsId;
    private String organizerId;
    private String organizerName;
    private String organizerProfilePic;
    private String content;
    private Date postedDate;
    private int likeCount;
    private List<String> likedBy;

    public News() {
        // Default constructor required for Firestore
    }

    public News(String newsId, String organizerId, String organizerName, String organizerProfilePic,
                String content, Date postedDate) {
        this.newsId = newsId;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.organizerProfilePic = organizerProfilePic;
        this.content = content;
        this.postedDate = postedDate;
        this.likeCount = 0;
        this.likedBy = new ArrayList<>();
    }

    // Getters and setters
    public String getNewsId() { return newsId; }
    public void setNewsId(String newsId) { this.newsId = newsId; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getOrganizerProfilePic() { return organizerProfilePic; }
    public void setOrganizerProfilePic(String organizerProfilePic) { this.organizerProfilePic = organizerProfilePic; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getPostedDate() { return postedDate; }
    public void setPostedDate(Date postedDate) { this.postedDate = postedDate; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
}
