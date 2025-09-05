package com.meow.utaract.ui.manage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.meow.utaract.ManagedEventItem;
import com.meow.utaract.ui.home.FilterBottomSheetDialogFragment;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ManageEventsViewModel extends ViewModel {

    private final MutableLiveData<List<ManagedEventItem>> myEvents = new MutableLiveData<>();
    private final MutableLiveData<List<ManagedEventItem>> filteredEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<List<String>> activeFilters = new MutableLiveData<>(new ArrayList<>());
    private final EventCreationStorage eventStorage;

    private List<ManagedEventItem> allEvents = new ArrayList<>();
    private String currentSearchQuery = "";

    public ManageEventsViewModel() {
        eventStorage = new EventCreationStorage();
    }

    public LiveData<List<ManagedEventItem>> getMyEvents() {
        return filteredEvents;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<String>> getActiveFilters() {
        return activeFilters;
    }

    public void fetchMyEvents() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            myEvents.setValue(new ArrayList<>());
            filteredEvents.setValue(new ArrayList<>());
            return;
        }

        isLoading.setValue(true);
        eventStorage.getEventsByOrganizer(currentUserId, new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (events.isEmpty()) {
                    allEvents = new ArrayList<>();
                    myEvents.setValue(allEvents);
                    filteredEvents.setValue(allEvents);
                    isLoading.setValue(false);
                    return;
                }
                fetchApplicantCountsForEvents(events);
            }

            @Override
            public void onFailure(Exception e) {
                allEvents = new ArrayList<>();
                myEvents.setValue(null);
                filteredEvents.setValue(null);
                isLoading.setValue(false);
            }
        });
    }

    private void fetchApplicantCountsForEvents(List<Event> events) {
        List<ManagedEventItem> managedEventItems = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicInteger tasksCompleted = new AtomicInteger(0);

        if (events.isEmpty()) {
            allEvents = managedEventItems;
            myEvents.setValue(managedEventItems);
            filteredEvents.setValue(managedEventItems);
            isLoading.setValue(false);
            return;
        }

        for (Event event : events) {
            ManagedEventItem managedEvent = new ManagedEventItem(event);
            managedEventItems.add(managedEvent);

            db.collection("events").document(event.getEventId()).collection("registrations").get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            int pending = 0, accepted = 0, rejected = 0;
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String status = doc.getString("status");
                                if (status != null) {
                                    switch (status) {
                                        case "pending": pending++; break;
                                        case "accepted": accepted++; break;
                                        case "rejected": rejected++; break;
                                    }
                                }
                            }
                            managedEvent.setPendingCount(pending);
                            managedEvent.setAcceptedCount(accepted);
                            managedEvent.setRejectedCount(rejected);
                        }

                        if (tasksCompleted.incrementAndGet() == events.size()) {
                            allEvents = managedEventItems;
                            myEvents.setValue(managedEventItems);
                            applyFilters();
                            isLoading.setValue(false);
                        }
                    });
        }
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
        if (allEvents == null) {
            filteredEvents.setValue(new ArrayList<>());
            return;
        }

        List<ManagedEventItem> filteredList = allEvents.stream()
                .filter(eventItem -> {
                    List<String> categories = activeFilters.getValue();
                    return categories == null || categories.isEmpty() ||
                            categories.contains(eventItem.getEvent().getCategory());
                })
                .filter(eventItem -> {
                    if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) return true;
                    String normalizedQuery = currentSearchQuery.toLowerCase().replaceAll("\\s", "");
                    return isSubsequence(normalizedQuery, eventItem.getEvent().getEventName().toLowerCase().replaceAll("\\s", ""));
                })
                .collect(Collectors.toList());

        filteredEvents.setValue(filteredList);
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

    public void showFilterDialog(Context context, String[] categories, FilterBottomSheetDialogFragment.FilterListener listener) {
        List<String> currentFilters = activeFilters.getValue();
        ArrayList<String> selected = (currentFilters != null) ? new ArrayList<>(currentFilters) : new ArrayList<>();

        FilterBottomSheetDialogFragment bottomSheet = FilterBottomSheetDialogFragment.newInstance(categories, selected);
        bottomSheet.setFilterListener(listener);
        bottomSheet.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "FilterBottomSheet");
    }
}