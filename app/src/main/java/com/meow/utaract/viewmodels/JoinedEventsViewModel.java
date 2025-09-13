package com.meow.utaract.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.meow.utaract.utils.JoinedEvent;
import com.meow.utaract.utils.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class JoinedEventsViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final List<JoinedEvent> masterJoinedEvents = new ArrayList<>();
    private final MutableLiveData<List<JoinedEvent>> displayedEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    private String currentStatusFilter = "all";
    private String currentSortOrder = "latest";

    public LiveData<List<JoinedEvent>> getDisplayedEvents() {
        return displayedEvents;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public String getCurrentSortOrder() {
        return currentSortOrder;
    }

    public void setFilter(String status) {
        currentStatusFilter = status;
        applyFiltersAndSort();
    }

    public void setSort(String sortOrder) {
        currentSortOrder = sortOrder;
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        List<JoinedEvent> processedList = new ArrayList<>(masterJoinedEvents);

        if (!"all".equals(currentStatusFilter)) {
            processedList = processedList.stream()
                    .filter(e -> currentStatusFilter.equals(e.getRegistrationStatus()))
                    .collect(Collectors.toList());
        }

        if ("latest".equals(currentSortOrder)) {
            Collections.sort(processedList, Comparator.comparing(e -> e.getEvent().getCreatedAt(), Comparator.reverseOrder()));
        } else {
            Collections.sort(processedList, Comparator.comparing(e -> e.getEvent().getCreatedAt()));
        }

        displayedEvents.setValue(processedList);
    }

    public void fetchJoinedEvents() {
        isLoading.setValue(true);
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            isLoading.setValue(false);
            return;
        }

        db.collection("events").get().addOnSuccessListener(eventSnapshots -> {
            masterJoinedEvents.clear();
            if (eventSnapshots.isEmpty()) {
                applyFiltersAndSort();
                isLoading.setValue(false);
                return;
            }

            AtomicInteger eventsToProcess = new AtomicInteger(eventSnapshots.size());

            for (DocumentSnapshot eventDoc : eventSnapshots.getDocuments()) {
                eventDoc.getReference().collection("registrations").document(currentUserId).get()
                        .addOnSuccessListener(registrationDoc -> {
                            if (registrationDoc.exists()) {
                                Event event = eventDoc.toObject(Event.class);
                                if (event != null) {
                                    // Manually set the event ID from the document ID
                                    event.setEventId(eventDoc.getId());

                                    String status = registrationDoc.getString("status");
                                    String ticketCode = registrationDoc.getString("ticketCode");
                                    masterJoinedEvents.add(new JoinedEvent(event, status, ticketCode));
                                }
                            }
                            if (eventsToProcess.decrementAndGet() == 0) {
                                applyFiltersAndSort();
                                isLoading.setValue(false);
                            }
                        });
            }
        });
    }
}