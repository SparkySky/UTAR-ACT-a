package com.meow.utaract;

import android.os.Bundle;
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

    private RecyclerView myEventsRecyclerView;
    private MyEventsAdapter myEventsAdapter;
    private final List<Event> eventList = new ArrayList<>();
    private EventCreationStorage eventStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // FIX: Pointing to the renamed layout file
        setContentView(R.layout.activity_manage_events);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        myEventsRecyclerView = findViewById(R.id.myEventsRecyclerView);
        eventStorage = new EventCreationStorage();

        setupRecyclerView();
        fetchMyEvents();
    }

    private void setupRecyclerView() {
        myEventsAdapter = new MyEventsAdapter(eventList, this, eventStorage);
        myEventsRecyclerView.setAdapter(myEventsAdapter);
    }

    private void fetchMyEvents() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            return;
        }

        eventStorage.getEventsByOrganizer(currentUserId, new EventCreationStorage.EventsFetchCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                eventList.clear();
                eventList.addAll(events);
                myEventsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from the edit screen
        fetchMyEvents();
    }
}