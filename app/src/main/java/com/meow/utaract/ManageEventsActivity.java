package com.meow.utaract;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.meow.utaract.ui.event.MyEventsAdapter;
import com.meow.utaract.ui.manage.ManageEventsViewModel;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;

public class ManageEventsActivity extends AppCompatActivity {

    private RecyclerView myEventsRecyclerView;
    private MyEventsAdapter myEventsAdapter;
    private TextView emptyView;
    private ManageEventsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        myEventsRecyclerView = findViewById(R.id.myEventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);

        setupRecyclerView();

        // Initialize the ViewModel
        viewModel = new ViewModelProvider(this).get(ManageEventsViewModel.class);

        // Observe the final, correctly-typed list from the ViewModel
        viewModel.getMyEvents().observe(this, managedEventItems -> {
            boolean hasEvents = managedEventItems != null && !managedEventItems.isEmpty();
            if (hasEvents) {
                // The adapter's update method receives the correct list type
                myEventsAdapter.updateEvents(managedEventItems);
            }
            updateUI(hasEvents);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tell the ViewModel to fetch the data
        viewModel.fetchMyEvents();
    }

    private void setupRecyclerView() {
        // Initialize the adapter with an empty list of the correct type.
        // The old `eventList` variable is no longer used here.
        myEventsAdapter = new MyEventsAdapter(new ArrayList<>(), this, new EventCreationStorage());
        myEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myEventsRecyclerView.setAdapter(myEventsAdapter);
    }

    private void updateUI(boolean hasEvents) {
        if (hasEvents) {
            myEventsRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        } else {
            myEventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }
}