package com.meow.utaract.ui.home;

import android.content.Context;

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

    private final MutableLiveData<List<EventItem>> displayedEventItems = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeFilters = new MutableLiveData<>();
    private List<Event> allEvents = new ArrayList<>();
    private Map<String, GuestProfile> organizerProfiles = null;
    private String currentSearchQuery = "";
    private final EventCreationStorage eventCreationStorage;
    private final GuestProfileStorage guestProfileStorage;

    public HomeViewModel() {
        eventCreationStorage = new EventCreationStorage();
        guestProfileStorage = new GuestProfileStorage(null);
        activeFilters.setValue(new ArrayList<>());
    }

    public LiveData<List<EventItem>> getEventItems() {
        return displayedEventItems;
    }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<String>> getActiveFilters() { return activeFilters; }

    public void fetchEvents(List<String> initialCategories) {
        isLoading.setValue(true);
        eventCreationStorage.getAllEvents(new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> eventList) {
                allEvents = eventList;
                List<String> organizerIds = allEvents.stream()
                        .map(Event::getOrganizerId)
                        .distinct()
                        .collect(Collectors.toList());

                guestProfileStorage.getProfilesForUserIds(organizerIds, new GuestProfileStorage.ProfilesCallback() {
                    @Override
                    public void onSuccess(Map<String, GuestProfile> profiles) {
                        organizerProfiles = profiles;
                        if (initialCategories != null && !initialCategories.isEmpty()) {
                            setCategoryFilters(initialCategories);
                        } else {
                            applyFilters();
                        }
                        isLoading.setValue(false);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        organizerProfiles = null;
                        applyFilters();
                        isLoading.setValue(false);
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                isLoading.setValue(false);
            }
        });
    }

    public void setSearchQuery(String query) {
        currentSearchQuery = query;
        applyFilters();
    }

    public void setCategoryFilters(List<String> categories) {
        activeFilters.setValue(categories);
        applyFilters();
    }

    private void applyFilters() {
        if (allEvents == null) return;
        List<Event> filteredList = new ArrayList<>(allEvents);
        List<String> currentCategories = activeFilters.getValue();

        if (currentCategories != null && !currentCategories.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(event -> currentCategories.contains(event.getCategory()))
                    .collect(Collectors.toList());
        }

        if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
            String normalizedQuery = currentSearchQuery.toLowerCase().replaceAll("\\s", "");
            filteredList = filteredList.stream()
                    .filter(event -> isSubsequence(normalizedQuery, event.getEventName().toLowerCase().replaceAll("\\s", "")))
                    .collect(Collectors.toList());
        }

        List<EventItem> eventItems = new ArrayList<>();
        for (Event event : filteredList) {
            GuestProfile organizer = (organizerProfiles != null) ? organizerProfiles.get(event.getOrganizerId()) : null;
            eventItems.add(new EventItem(event, organizer));
        }
        displayedEventItems.setValue(eventItems);
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