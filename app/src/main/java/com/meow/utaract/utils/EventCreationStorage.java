package com.meow.utaract.utils;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.meow.utaract.utils.Event;

import java.util.List;

public class EventCreationStorage {
    private static final String EVENTS_COLLECTION = "events";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    public EventCreationStorage() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // Create a new event in Firestore
    public void createEvent(Event event, EventCreationCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            callback.onFailure(new Exception("User must be logged in to create events"));
            return;
        }

        // Set organizer ID from current user
        event.setOrganizerId(currentUser.getUid());

        firestore.collection(EVENTS_COLLECTION)
                .add(event)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String eventId = documentReference.getId();
                        event.setEventId(eventId);

                        // Update the document with the event ID
                        firestore.collection(EVENTS_COLLECTION).document(eventId)
                                .set(event)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        callback.onSuccess(eventId);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        callback.onFailure(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    // Get event by ID
    public void getEvent(String eventId, EventFetchCallback callback) {
        firestore.collection(EVENTS_COLLECTION).document(eventId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Event event = documentSnapshot.toObject(Event.class);
                            callback.onSuccess(event);
                        } else {
                            callback.onFailure(new Exception("Event not found"));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    // Get all events
    public void getAllEvents(EventsFetchCallback callback) {
        Log.d(TAG, "Attempting to fetch all events from Firestore.");
        firestore.collection("events")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    Log.d(TAG, "Successfully fetched " + events.size() + " total events.");
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching events", e);
                    callback.onFailure(e);
                });
    }

    // Update event
    public void updateEvent(String eventId, Event event, EventCreationCallback callback) {
        firestore.collection(EVENTS_COLLECTION).document(eventId)
                .set(event)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        callback.onSuccess(eventId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    // Delete event
    public void deleteEvent(String eventId, EventDeletionCallback callback) {
        firestore.collection(EVENTS_COLLECTION).document(eventId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    // Method to get events by a specific organizer
    public void getEventsByOrganizer(String organizerId, EventsFetchCallback callback) {
        firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            events.add(event);
                        }
                    }
                    callback.onSuccess(events);
                })
                .addOnFailureListener(callback::onFailure);
    }





    // Callback interfaces
    public interface EventCreationCallback {
        void onSuccess(String eventId);
        void onFailure(Exception e);
    }

    public interface EventFetchCallback {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }

    public interface EventsFetchCallback {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }

    public interface EventDeletionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}