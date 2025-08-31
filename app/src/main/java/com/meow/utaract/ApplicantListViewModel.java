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

    public LiveData<List<Applicant>> getApplicants() {
        return applicants;
    }

    public void fetchApplicants(String eventId) {
        db.collection("events").document(eventId).collection("registrations")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<Applicant> applicantList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        applicantList.add(doc.toObject(Applicant.class));
                    }
                    applicants.setValue(applicantList);
                });
    }

    public void updateApplicantStatus(String eventId, String userId, String newStatus) {
        db.collection("events").document(eventId).collection("registrations").document(userId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    // On success, create a notification for the user
                    createUserNotification(userId, eventId, newStatus);
                });
    }

    private void createUserNotification(String userId, String eventId, String status) {
        // This is a placeholder for a robust notification system, which would
        // ideally be handled by Cloud Functions for security and scalability.
        String message;
        if ("accepted".equals(status)) {
            message = "Your registration for an event has been accepted!";
        } else if ("rejected".equals(status)) {
            message = "Your registration for an event has been rejected.";
        } else {
            return; // Don't notify for other statuses
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("message", message);
        notification.put("eventId", eventId);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);

        // Add to a user's subcollection of notifications
        db.collection("users").document(userId).collection("notifications").add(notification);
    }
}