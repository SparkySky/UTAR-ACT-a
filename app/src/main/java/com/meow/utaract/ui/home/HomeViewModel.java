package com.meow.utaract.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<List<Event>> displayedEvents;
    private final MutableLiveData<Boolean> isLoading;
    private final EventCreationStorage eventCreationStorage;
    private List<Event> allEvents; // Cache for the full list of events

    public HomeViewModel() {
        displayedEvents = new MutableLiveData<>();
        isLoading = new MutableLiveData<>();
        allEvents = new ArrayList<>();
        eventCreationStorage = new EventCreationStorage();
    }

    public LiveData<List<Event>> getEvents() {
        return displayedEvents;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchEvents() {
        isLoading.setValue(true);
        eventCreationStorage.getAllEvents(new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> eventList) {
                allEvents = eventList;
                displayedEvents.setValue(allEvents); // Initially, display all events
                isLoading.setValue(false);
            }
            @Override
            public void onFailure(Exception e) {
                isLoading.setValue(false);
            }
        });
    }

    /**
     * Filters the cached list of events based on a search query.
     * This performs a fuzzy search that is tolerant of spacing, case, and character sequence.
     * @param query The user's search input.
     */
    public void filterEvents(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayedEvents.setValue(allEvents); // If query is empty, show all events
            return;
        }

        List<Event> filteredList = new ArrayList<>();
        // Prepare the query for matching: lowercase and no spaces.
        String normalizedQuery = query.toLowerCase().replaceAll("\\s", "");

        for (Event event : allEvents) {
            // Prepare the event name for matching.
            String eventName = event.getEventName().toLowerCase().replaceAll("\\s", "");
            if (isSubsequence(normalizedQuery, eventName)) {
                filteredList.add(event);
            }
        }
        displayedEvents.setValue(filteredList);
    }

    /**
     * Checks if one string is a subsequence of another.
     * This allows for fuzzy matching where characters appear in order but are not necessarily contiguous.
     * e.g., "dep" is a subsequence of "depression talk".
     */
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