package com.meow.utaract.ui.home;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";
    private final MutableLiveData<List<EventItem>> displayedEventItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeFilters = new MutableLiveData<>(new ArrayList<>());

    private List<Event> allEvents = new ArrayList<>();
    private Map<String, GuestProfile> organizerProfiles;
    private String currentSearchQuery = "";

    private final EventCreationStorage eventStorage;
    private final GuestProfileStorage profileStorage;

    public HomeViewModel() {
        eventStorage = new EventCreationStorage();
        profileStorage = new GuestProfileStorage(null);
    }

    public LiveData<List<EventItem>> getEventItems() {
        return displayedEventItems;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<String>> getActiveFilters() {
        return activeFilters;
    }

    public void fetchEvents(List<String> initialCategoryPreferences) {
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
                        organizerProfiles = null;
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

    public void setSearchQuery(String query) {
        currentSearchQuery = query;
        applyFiltersAndCombine();
    }

    public void setCategoryFilters(List<String> categories) {
        activeFilters.setValue(categories);
        applyFiltersAndCombine();
    }

    private void applyFiltersAndCombine() {
        long currentTime = System.currentTimeMillis();

        List<Event> filteredEvents = allEvents.stream()
                // THE ONLY VISIBILITY RULE:
                .filter(event -> event.getPublishAt() <= currentTime)
                .filter(event -> { // User preference filter
                    List<String> categories = activeFilters.getValue();
                    return categories == null || categories.isEmpty() || categories.contains(event.getCategory());
                })
                .filter(event -> { // Search filter
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
}