// sparkysky/utar-act-a/UTAR-ACT-a-CP10/app/src/main/java/com/meow/utaract/MainViewModel.java
package com.meow.utaract;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.meow.utaract.utils.GuestProfile;

public class MainViewModel extends AndroidViewModel {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final MutableLiveData<Boolean> isOrganiser = new MutableLiveData<>();
    private final MutableLiveData<GuestProfile> userProfile = new MutableLiveData<>();
    private final Gson gson = new Gson(); // Add Gson instance

    public MainViewModel(@NonNull Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fetchUserRole();
        fetchUserProfile();
    }

    public LiveData<Boolean> isOrganiser() {
        return isOrganiser;
    }

    public LiveData<GuestProfile> getUserProfile() {
        return userProfile;
    }

    public void fetchUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Boolean organiser = task.getResult().getClaims().containsKey("organiser");
                    isOrganiser.postValue(organiser);
                }
            });
        }
    }

    public void fetchUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("guest_profiles").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // --- THIS IS THE FIX ---
                            // Get the JSON string from the "profile_json" field
                            String json = documentSnapshot.getString("profile_json");
                            if (json != null) {
                                // Deserialize the JSON string into a GuestProfile object
                                GuestProfile profile = gson.fromJson(json, GuestProfile.class);
                                userProfile.postValue(profile);
                            }
                            // ----------------------
                        }
                    });
        }
    }

    public void setOrganiser(boolean isOrganiser) {
        this.isOrganiser.setValue(isOrganiser);
    }

}