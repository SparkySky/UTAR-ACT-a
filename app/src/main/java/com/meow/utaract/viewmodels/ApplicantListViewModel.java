package com.meow.utaract.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentChange; // Import DocumentChange
import com.google.firebase.firestore.FirebaseFirestore;
import com.meow.utaract.utils.Applicant;
import com.meow.utaract.utils.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ApplicantListViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Applicant>> applicants = new MutableLiveData<>();
    private final List<Applicant> applicantListCache = new ArrayList<>();

    public LiveData<List<Applicant>> getApplicants() {
        return applicants;
    }

    public void fetchApplicants(String eventId, String organizerId) {
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDocument -> {
            if (!eventDocument.exists()) return;
            Event event = eventDocument.toObject(Event.class);
            String eventName = (event != null) ? event.getEventName() : "your event";

            db.collection("events").document(eventId).collection("registrations")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null || snapshots == null) return;

                        // Use DocumentChanges to reliably detect new applicants
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    // A new document has been added, this is a new applicant
                                    Applicant newApplicant = dc.getDocument().toObject(Applicant.class);
                                    sendOrganizerNotificationForNewApplicant(organizerId, eventId, eventName, newApplicant.getUserName());
                                    applicantListCache.add(newApplicant);
                                    break;
                                case MODIFIED:
                                    // An existing document has been modified
                                    Applicant modifiedApplicant = dc.getDocument().toObject(Applicant.class);
                                    int index = findApplicantIndex(modifiedApplicant.getUserId());
                                    if (index != -1) {
                                        applicantListCache.set(index, modifiedApplicant);
                                    }
                                    break;
                                case REMOVED:
                                    // A document has been removed
                                    Applicant removedApplicant = dc.getDocument().toObject(Applicant.class);
                                    int removeIndex = findApplicantIndex(removedApplicant.getUserId());
                                    if (removeIndex != -1) {
                                        applicantListCache.remove(removeIndex);
                                    }
                                    break;
                            }
                        }
                        // Update the LiveData with the full, updated list
                        applicants.setValue(new ArrayList<>(applicantListCache));
                    });
        });
    }

    // Helper method to find an applicant in the cache
    private int findApplicantIndex(String userId) {
        for (int i = 0; i < applicantListCache.size(); i++) {
            if (applicantListCache.get(i).getUserId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }

    public void updateApplicantStatus(String eventId, String userId, String newStatus, String organizerId) {
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDocument -> {
            if (!eventDocument.exists()) return;
            Event event = eventDocument.toObject(Event.class);
            String eventName = (event != null) ? event.getEventName() : "an event";

            db.collection("events").document(eventId).collection("registrations").document(userId)
                    .get().addOnSuccessListener(snapshot -> {
                        if (!snapshot.exists()) return;
                        String oldStatus = snapshot.getString("status");
                        String applicantName = snapshot.getString("userName");

                        String ticketCode;
                        if ("accepted".equals(newStatus)) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd", Locale.getDefault());
                            String datePrefix = sdf.format(new Date());
                            ticketCode = datePrefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                        } else {
                            ticketCode = null;
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", newStatus);
                        updates.put("ticketCode", ticketCode);

                        snapshot.getReference().update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    createUserNotification(userId, eventId, eventName, newStatus, ticketCode);
                                    createOrganizerNotification(organizerId, eventId, applicantName, newStatus);
                                    saveStatusHistory(eventId, userId, oldStatus, newStatus, organizerId);
                                });
                    });
        });
    }

    private void createUserNotification(String userId, String eventId, String eventName, String status, String ticketCode) {
        String message;
        if ("accepted".equals(status)) {
            message = "Your registration for <b>" + eventName + "</b> has been accepted!";
        } else if ("rejected".equals(status)) {
            message = "Your registration for <b>" + eventName + "</b> has been rejected.";
        } else {
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        if (ticketCode != null) {
            notification.put("ticketCode", ticketCode);
        }

        db.collection("users").document(userId).collection("notifications").add(notification);
    }

    private void sendOrganizerNotificationForNewApplicant(String organizerId, String eventId, String eventName, String applicantName) {
        String message = "New applicant " + applicantName + " has registered for <b>" + eventName + "</b>";

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("organizerId", organizerId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("users").document(organizerId).collection("notifications").add(notification);
    }

    private void createOrganizerNotification(String organizerId, String eventId, String applicantName, String status) {
        String message = "Applicant " + applicantName + "'s status changed to " + status;

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("users").document(organizerId).collection("notifications").add(notification);
    }

    private void saveStatusHistory(String eventId, String userId, String oldStatus, String newStatus, String operatorId) {
        Map<String, Object> history = new HashMap<>();
        history.put("userId", userId);
        history.put("oldStatus", oldStatus);
        history.put("newStatus", newStatus);
        history.put("operatorId", operatorId);
        history.put("timestamp", System.currentTimeMillis());

        db.collection("events").document(eventId).collection("history").add(history);
    }
}