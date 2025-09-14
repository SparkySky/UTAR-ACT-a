package com.meow.utaract.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // For pull-to-refresh

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // For Firestore real-time listener
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.firebase.firestore.DocumentSnapshot;
import com.meow.utaract.utils.Notification;
import com.meow.utaract.adapters.NotificationAdapter;
import com.meow.utaract.R;

public class NotificationActivity extends AppCompatActivity {

    // UI components
    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>(); // Holds the list of notifications
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firestore listener (keeps listening for changes)
    private ListenerRegistration notificationListener;

    private boolean isOrganiser; // Tracks if the user is an organiser

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Check if user is organiser (passed from previous activity)
        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);

        // Toolbar back button closes the activity
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // Initialize views
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // RecyclerView setup
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, this, isOrganiser);
        notificationsRecyclerView.setAdapter(adapter);

        // Swipe-to-refresh reloads notifications
        swipeRefreshLayout.setOnRefreshListener(this::fetchNotifications);

        // Initial fetch
        fetchNotifications();
    }

    /**
     * Fetches notifications from Firestore for the logged-in user.
     * Uses a snapshot listener to receive real-time updates.
     */
    private void fetchNotifications() {
        swipeRefreshLayout.setRefreshing(true);

        // Get the logged-in user's ID
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            // User not logged in, stop refreshing
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Remove previous listener to avoid duplicates
        if (notificationListener != null) {
            notificationListener.remove();
        }

        // Listen for notifications under the current user
        notificationListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sort by most recent
                .addSnapshotListener((snapshots, e) -> {
                    swipeRefreshLayout.setRefreshing(false);

                    if (e != null) {
                        // Error occurred, just return
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // Notifications exist, show them
                        emptyView.setVisibility(View.GONE);
                        notificationsRecyclerView.setVisibility(View.VISIBLE);

                        // Build new list of notifications manually
                        List<Notification> newNotifications = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Notification notification = new Notification();

                            // Safely extract each field
                            notification.setMessage(doc.getString("message"));
                            notification.setEventId(doc.getString("eventId"));
                            notification.setRead(Boolean.TRUE.equals(doc.getBoolean("isRead")));
                            notification.setTicketCode(doc.getString("ticketCode"));
                            notification.setOrganizerId(doc.getString("organizerId"));

                            // Handle timestamp that may be stored as Firestore Timestamp or Long
                            Object timestampObject = doc.get("timestamp");
                            if (timestampObject instanceof Timestamp) {
                                notification.setTimestamp(((Timestamp) timestampObject).toDate());
                            } else if (timestampObject instanceof Long) {
                                notification.setTimestamp(new Date((Long) timestampObject));
                            }

                            newNotifications.add(notification);
                        }

                        // Replace old list with fresh data
                        notificationList.clear();
                        notificationList.addAll(newNotifications);

                        adapter.notifyDataSetChanged();
                    } else {
                        // No notifications found
                        notificationList.clear();
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(View.VISIBLE);
                        notificationsRecyclerView.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener to avoid memory leaks
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}
