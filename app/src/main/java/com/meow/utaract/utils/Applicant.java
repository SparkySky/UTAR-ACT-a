package com.meow.utaract.utils;

import androidx.annotation.Keep;

@Keep
public class Applicant {
    private String userId;
    private String userName;
    private String status;
    private String email; // Add this line
    private String phone; // Add this line

    public Applicant() {} // Needed for Firestore

    // The constructor is not strictly needed for Firestore, but good practice
    public Applicant(String userId, String userName, String status, String email, String phone) {
        this.userId = userId;
        this.userName = userName;
        this.status = status;
        this.email = email; // Add this line
        this.phone = phone; // Add this line
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getStatus() { return status; }
    public String getEmail() { return email; } // Add this getter
    public String getPhone() { return phone; } // Add this getter
}