package com.meow.utaract.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";
    private final MutableLiveData<List<Event>> events;
    private final MutableLiveData<GuestProfile> userProfile = new MutableLiveData<>();
    private final MutableLiveData<List<EventItem>> displayedEventItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeFilters = new MutableLiveData<>(new ArrayList<>());

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final Gson gson = new Gson(); // Add Gson instance

    private List<Event> allEvents = new ArrayList<>();
    private Map<String, GuestProfile> organizerProfiles = new HashMap<>();
    private String currentSearchQuery = "";

    private final EventCreationStorage eventStorage;
    private final GuestProfileStorage profileStorage;

    public void fetchEvents() {
        isLoading.setValue(true);
        eventStorage.getAllEvents(new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                allEvents = events;
                List<String> organizerIds = allEvents.stream()
                        .map(Event::getOrganizerId)
                        .distinct()
                        .collect(Collectors.toList());

                if (organizerIds.isEmpty()) {
                    applyFiltersAndCombine();
                    return;
                }

                profileStorage.getProfilesForUserIds(organizerIds, new GuestProfileStorage.ProfilesCallback() {
                    @Override
                    public void onSuccess(Map<String, GuestProfile> profiles) {
                        organizerProfiles = profiles;
                        applyFiltersAndCombine();
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to fetch organizer profiles", e);
                        organizerProfiles = new HashMap<>();
                        applyFiltersAndCombine();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                isLoading.setValue(false);
                displayedEventItems.setValue(new ArrayList<>());
            }
        });
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
/*    public void fetchUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Only fetch for logged-in, non-anonymous users
        if (currentUser != null && !currentUser.isAnonymous()) {
            FirebaseFirestore.getInstance().collection("guest_profiles").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            GuestProfile profile = documentSnapshot.toObject(GuestProfile.class);
                            userProfile.setValue(profile);
                        }
                    });
        }
    }*/



    public HomeViewModel() {
        events = new MutableLiveData<>();
        eventStorage = new EventCreationStorage();
        profileStorage = new GuestProfileStorage(null);
    }

    public void setSearchQuery(String query) {
        currentSearchQuery = query;
        applyFiltersAndCombine();
    }

    public void setCategoryFilters(List<String> categories) {
        activeFilters.setValue(categories);
        // After setting a new filter, we must re-apply it to the existing data.
        applyFiltersAndCombine();
    }

    private void applyFiltersAndCombine() {
        long currentTime = System.currentTimeMillis();

        List<Event> filteredEvents = allEvents.stream()
                .filter(event -> event.getPublishAt() <= currentTime)
                .filter(event -> {
                    List<String> categories = activeFilters.getValue();
                    return categories == null || categories.isEmpty() || categories.contains(event.getCategory());
                })
                .filter(event -> {
                    if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) return true;
                    String normalizedQuery = currentSearchQuery.toLowerCase().replaceAll("\\s", "");
                    return isSubsequence(normalizedQuery, event.getEventName().toLowerCase().replaceAll("\\s", ""));
                })
                .collect(Collectors.toList());

        List<EventItem> combinedList = new ArrayList<>();
        for (Event event : filteredEvents) {
            GuestProfile organizer = (organizerProfiles != null) ? organizerProfiles.get(event.getOrganizerId()) : null;
            combinedList.add(new EventItem(event, organizer));
        }
        displayedEventItems.setValue(combinedList);
        isLoading.setValue(false);
    }

    private boolean isSubsequence(String s1, String s2) {
        int i = 0, j = 0;
        while (i < s1.length() && j < s2.length()) {
            if (s1.charAt(i) == s2.charAt(j)) {
                i++;
            }
            j++;
        }
        return i == s1.length();
    }

    public static class EventItem {
        public final Event event;
        public final GuestProfile organizer;
        public EventItem(Event event, GuestProfile organizer) {
            this.event = event;
            this.organizer = organizer;
        }
    }


    public void setEvents(List<Event> eventList) {
        events.setValue(eventList);
    }

    public LiveData<List<EventItem>> getEventItems() {
        return displayedEventItems;
    }
    public LiveData<GuestProfile> getUserProfile() { return userProfile; }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<List<String>> getActiveFilters() {
        return activeFilters;
    }
    public LiveData<List<Event>> getEvents() { return events; }
}