package com.meow.utaract.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Event>> displayedEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeFilters = new MutableLiveData<>();
    private List<Event> allEvents = new ArrayList<>();
    private String currentSearchQuery = "";
    private final EventCreationStorage eventCreationStorage;

    public HomeViewModel() {
        eventCreationStorage = new EventCreationStorage();
        activeFilters.setValue(new ArrayList<>());
    }

    public LiveData<List<Event>> getEvents() {
        return displayedEvents;
    }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    public LiveData<List<String>> getActiveFilters() {
        return activeFilters;
    }

    /**
     * Fetches all events and optionally applies an initial category filter upon success.
     * @param initialCategories The list of categories to apply after fetching. Can be null or empty.
     */
    public void fetchEvents(List<String> initialCategories) {
        isLoading.setValue(true);
        eventCreationStorage.getAllEvents(new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> eventList) {
                allEvents = eventList;
                // This is the key fix: Apply the initial filter only AFTER data has arrived.
                if (initialCategories != null && !initialCategories.isEmpty()) {
                    setCategoryFilters(initialCategories);
                } else {
                    applyFilters(); // If no initial filters, just apply the current search query
                }
                isLoading.setValue(false);
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

        displayedEvents.setValue(filteredList);
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
}