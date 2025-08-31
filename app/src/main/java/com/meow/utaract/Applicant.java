package com.meow.utaract;

public class Applicant {
    private String userId;
    private String userName;
    private String status;

    public Applicant() {} // Needed for Firestore

    public Applicant(String userId, String userName, String status) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getStatus() { return status; }
}