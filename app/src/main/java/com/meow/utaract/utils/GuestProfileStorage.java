package com.meow.utaract.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.meow.utaract.firebase.AuthService;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Keep
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            return;
        }
        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // --- THIS IS THE FIX ---
        // Save the GuestProfile object directly to the document.
        // Firestore will handle the field mapping correctly.
        db.collection("guest_profiles").document(userId)
                .set(profile, SetOptions.merge()) // Pass the object directly
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Profile successfully written!"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error writing profile", e));
        // ----------------------
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

    public void getProfilesForUserIds(List<String> userIds, ProfilesCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onSuccess(new HashMap<>()); // Return empty map if no IDs
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("guest_profiles")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), userIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, GuestProfile> profiles = new HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String json = document.getString("profile_json");
                        if (json != null) {
                            GuestProfile profile = gson.fromJson(json, GuestProfile.class);
                            profiles.put(document.getId(), profile);
                        }
                    }
                    callback.onSuccess(profiles);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // New Callback interface for multiple profiles
    public interface ProfilesCallback {
        void onSuccess(Map<String, GuestProfile> profiles);
        void onFailure(Exception e);
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
}
