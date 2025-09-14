package com.meow.utaract.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast; // Import Toast
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar; // Import MaterialToolbar
import com.google.android.material.chip.ChipGroup;
import com.meow.utaract.adapters.JoinedEventsAdapter;
import com.meow.utaract.viewmodels.JoinedEventsViewModel;
import com.meow.utaract.R;

import java.util.ArrayList;

public class JoinedEventsActivity extends AppCompatActivity {

    private JoinedEventsViewModel viewModel;
    private JoinedEventsAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private ChipGroup filterChipGroup;
    private MaterialToolbar toolbar; // Add a variable for the toolbar

    /**
     * Lifecycle method called when activity is created.
     * Sets up the UI, initializes the RecyclerView, ViewModel, observers,
     * and event listeners for filtering, sorting, and refreshing.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_events_list);

        // Initialize Views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.joinedEventsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        // Setup Toolbar with back navigation and menu actions
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sort) {
                toggleSortOrder();
                return true;
            }
            return false;
        });

        // Setup RecyclerView with LinearLayoutManager and adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JoinedEventsAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(JoinedEventsViewModel.class);

        // Observe list of displayed events and update UI accordingly
        viewModel.getDisplayedEvents().observe(this, joinedEvents -> {
            if (joinedEvents == null || joinedEvents.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.joinedEventList.clear();
                adapter.joinedEventList.addAll(joinedEvents);
                adapter.notifyDataSetChanged();
            }
        });

        // Observe loading state to control swipe refresh animation
        viewModel.getIsLoading().observe(this, isLoading -> {
            swipeRefreshLayout.setRefreshing(isLoading);
        });

        // Setup listeners for swipe-to-refresh and filter chips
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.fetchJoinedEvents());
        setupFilterListener();

        // Fetch events initially when the activity is loaded
        viewModel.fetchJoinedEvents();
    }

    /**
     * Toggle sorting order of events between "latest" and "oldest".
     * Updates the ViewModel and shows a toast to inform the user.
     */
    private void toggleSortOrder() {
        String currentSort = viewModel.getCurrentSortOrder();
        String newSort;
        if ("latest".equals(currentSort)) {
            newSort = "oldest";
            Toast.makeText(this, "Sorted by Oldest", Toast.LENGTH_SHORT).show();
        } else {
            newSort = "latest";
            Toast.makeText(this, "Sorted by Latest", Toast.LENGTH_SHORT).show();
        }
        viewModel.setSort(newSort);
    }

    /**
     * Setup filter chip group listener to filter events by status:
     * - All, Pending, Accepted, or Rejected.
     * Updates the ViewModel filter state based on selected chip.
     */
    private void setupFilterListener() {
        filterChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                viewModel.setFilter("all");
            } else if (checkedId == R.id.chipPending) {
                viewModel.setFilter("pending");
            } else if (checkedId == R.id.chipAccepted) {
                viewModel.setFilter("accepted");
            } else if (checkedId == R.id.chipRejected) {
                viewModel.setFilter("rejected");
            }
        });
    }
}
