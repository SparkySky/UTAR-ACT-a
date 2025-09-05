package com.meow.utaract;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicantListViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Applicant>> applicants = new MutableLiveData<>();
    private List<String> existingApplicantIds = new ArrayList<>(); // 用于检测新申请

    public LiveData<List<Applicant>> getApplicants() {
        return applicants;
    }

    // Obtain the list of applicants
    public void fetchApplicants(String eventId, String organizerId) {
        db.collection("events").document(eventId).collection("registrations")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    List<Applicant> applicantList = new ArrayList<>();
                    List<String> newApplicantIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Applicant applicant = doc.toObject(Applicant.class);
                        applicantList.add(applicant);
                        newApplicantIds.add(applicant.getUserId());

                        // Test new applications
                        if (!existingApplicantIds.contains(applicant.getUserId())) {
                            sendOrganizerNotificationForNewApplicant(organizerId, eventId, applicant.getUserName());
                        }
                    }

                    existingApplicantIds = newApplicantIds;
                    applicants.setValue(applicantList);
                });
    }

    // Update the application status
    public void updateApplicantStatus(String eventId, String userId, String newStatus, String organizerId) {
        db.collection("events").document(eventId).collection("registrations").document(userId)
                .get().addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;
                    String oldStatus = snapshot.getString("status");

                    // Update status
                    snapshot.getReference().update("status", newStatus)
                            .addOnSuccessListener(aVoid -> {
                                // Create user notifications
                                createUserNotification(userId, eventId, newStatus);
                                // Create Organizer Notice
                                createOrganizerNotification(organizerId, eventId, userId, newStatus);
                                // Save status history
                                saveStatusHistory(eventId, userId, oldStatus, newStatus, organizerId);
                            });
                });
    }

    // User Notification
    private void createUserNotification(String userId, String eventId, String status) {
        String message;
        if ("accepted".equals(status)) {
            message = "Your registration for an event has been accepted!";
        } else if ("rejected".equals(status)) {
            message = "Your registration for an event has been rejected.";
        } else {
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("users").document(userId).collection("notifications").add(notification);
    }

    // Organizer Notice (Status Change)
    private void createOrganizerNotification(String organizerId, String eventId, String userId, String status) {
        String message = "Applicant " + userId + " status changed to " + status;

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("users").document(organizerId).collection("notifications").add(notification);
    }

    // Notify the organizer of the new application
    private void sendOrganizerNotificationForNewApplicant(String organizerId, String eventId, String applicantName) {
        String message = "New applicant: " + applicantName + " has registered for your event";

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        db.collection("users").document(organizerId).collection("notifications").add(notification);
    }

    // Save historical records
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
