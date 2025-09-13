package com.meow.utaract;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // Import
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>(); // Use a list of Notification objects
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListenerRegistration notificationListener;
    private boolean isOrganiser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // The adapter now gets a clean list of Notification objects
        adapter = new NotificationAdapter(notificationList, this, isOrganiser);
        notificationsRecyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::fetchNotifications);
        fetchNotifications();
    }

    private void fetchNotifications() {
        swipeRefreshLayout.setRefreshing(true);
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = FirebaseFirestore.getInstance().collection("users").document(userId).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (e != null) { return; }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        emptyView.setVisibility(View.GONE);
                        notificationsRecyclerView.setVisibility(View.VISIBLE);

                        // --- ROBUST MANUAL DESERIALIZATION ---
                        List<Notification> newNotifications = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            // Manually create the object to avoid the toObject() crash
                            Notification notification = new Notification();
                            notification.setMessage(doc.getString("message"));
                            notification.setEventId(doc.getString("eventId"));
                            notification.setRead(Boolean.TRUE.equals(doc.getBoolean("isRead")));
                            notification.setTicketCode(doc.getString("ticketCode"));
                            notification.setOrganizerId(doc.getString("organizerId"));

                            // Handle both old (Long) and new (Timestamp) formats
                            Object timestampObject = doc.get("timestamp");
                            if (timestampObject instanceof Timestamp) {
                                notification.setTimestamp(((Timestamp) timestampObject).toDate());
                            } else if (timestampObject instanceof Long) {
                                notification.setTimestamp(new Date((Long) timestampObject));
                            }

                            newNotifications.add(notification);
                        }
                        notificationList.clear();
                        notificationList.addAll(newNotifications);
                        // ------------------------------------

                        adapter.notifyDataSetChanged();
                    } else {
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
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}