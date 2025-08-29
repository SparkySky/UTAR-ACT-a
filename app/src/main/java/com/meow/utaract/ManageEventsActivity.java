package com.meow.utaract;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.ui.home.MyEventsAdapter;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;
import java.util.List;

public class ManageEventsActivity extends AppCompatActivity {

    private static final String TAG = "ManageEventsActivity";
    private RecyclerView myEventsRecyclerView;
    private MyEventsAdapter myEventsAdapter;
    private final List<Event> eventList = new ArrayList<>();
    private EventCreationStorage eventStorage;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        myEventsRecyclerView = findViewById(R.id.myEventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        eventStorage = new EventCreationStorage();

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetching in onResume ensures the list is always up-to-date
        // when the user navigates back to this screen.
        fetchMyEvents();
    }

    private void setupRecyclerView() {
        myEventsAdapter = new MyEventsAdapter(eventList, this, eventStorage);
        myEventsRecyclerView.setAdapter(myEventsAdapter);
    }

    private void fetchMyEvents() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Log.e(TAG, "User is not logged in. Cannot fetch events.");
            updateUI(); // Show empty view
            return;
        }

        Log.d(TAG, "Fetching events for organizer: " + currentUserId);
        eventStorage.getEventsByOrganizer(currentUserId, new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                Log.d(TAG, "Successfully fetched " + events.size() + " events.");
                eventList.clear();
                eventList.addAll(events);
                myEventsAdapter.notifyDataSetChanged();
                updateUI();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch events.", e);
                eventList.clear();
                myEventsAdapter.notifyDataSetChanged();
                updateUI();
            }
        });
    }

    // This new method handles showing/hiding the list or the empty message
    private void updateUI() {
        if (eventList.isEmpty()) {
            myEventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            myEventsRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }
}