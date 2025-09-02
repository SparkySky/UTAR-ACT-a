package com.meow.utaract.utils;

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.meow.utaract.firebase.AuthService;

import java.io.*;
import java.util.HashMap;

public class GuestProfileStorage {
    private static final String FILE_NAME = "profile.json";
    private final Context context;
    private final Gson gson = new Gson();

    public GuestProfileStorage(Context context) {
        this.context = context;
    }

    // Store JSON locally
    public void saveProfile(GuestProfile profile) {
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), FILE_NAME))) {
            gson.toJson(profile, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load profile from JSON file
    public GuestProfile loadProfile() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, GuestProfile.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Check if profile.json existed
    public boolean profileExists() {
        return new File(context.getFilesDir(), FILE_NAME).exists();
    }

    // Clean remove profile.json
    public void clearProfile() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
    }


    // Upload profile to Firestore - For organiser only
    public void uploadProfileToFirestore(GuestProfile profile) {
        FirebaseUser user = new AuthService().getAuth().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String json = gson.toJson(profile);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("guest_profiles").document(uid)
                .set(new HashMap<String, Object>() {{
                    put("profile_json", json);
                }})
                .addOnSuccessListener(aVoid -> {
                    // Optional: show success message
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    // Download profile from Firestore
    public void downloadProfileFromFirestore(FirestoreCallback callback) {
        FirebaseUser user = new AuthService().getAuth().getCurrentUser();
        if (user == null) {
            callback.onFailure(new Exception("User not logged in"));
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("guest_profiles").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String json = document.getString("profile_json");
                        GuestProfile profile = gson.fromJson(json, GuestProfile.class);
                        callback.onSuccess(profile);
                    } else {
                        callback.onFailure(new Exception("Profile not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Callback interface
    // After (Functional Interface - if onSuccess is the only abstract method)
    @FunctionalInterface
    public interface FirestoreCallback {
        void onSuccess(GuestProfile profile);
        // Optional: Provide default implementations for other methods if needed
        default void onFailure(Exception e) {
            // Default error handling
        }
    }

    public void createNotification(String organiserId, String guestId, String eventId,
                                   String message, String targetRole) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> notif = new HashMap<>();
        notif.put("organiserId", organiserId);
        notif.put("guestId", guestId);
        notif.put("eventId", eventId);
        notif.put("message", message);
        notif.put("targetRole", targetRole);
        notif.put("read", false);
        notif.put("timestamp", System.currentTimeMillis());

        db.collection("notifications").add(notif)
                .addOnSuccessListener(ref -> {
                    System.out.println("Notification created: " + ref.getId());
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }
}
