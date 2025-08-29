package com.meow.utaract.ui.manage;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.List;

public class ManageEventsViewModel extends ViewModel {

    private final MutableLiveData<List<Event>> myEvents = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final EventCreationStorage eventStorage;

    public ManageEventsViewModel() {
        eventStorage = new EventCreationStorage();
    }

    public LiveData<List<Event>> getMyEvents() {
        return myEvents;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchMyEvents() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            return; // Not logged in
        }
        isLoading.setValue(true);
        eventStorage.getEventsByOrganizer(currentUserId, new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                myEvents.setValue(events);
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error, post empty list
                myEvents.setValue(null);
                isLoading.setValue(false);
            }
        });
    }
}