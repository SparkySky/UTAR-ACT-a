package com.meow.utaract.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.meow.utaract.R;
import com.meow.utaract.adapters.MyEventsAdapter;
import com.meow.utaract.activities.fragments.FilterBottomSheetDialogFragment;
import com.meow.utaract.viewmodels.ManageEventsViewModel;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

/**
 * Activity for managing events created by the user.
 * This screen allows users to:
 * - View their created events
 * - Search through events
 * - Apply filters to narrow down events
 * - Navigate to other sections using the drawer
 */
public class ManageEventsActivity extends AppCompatActivity implements FilterBottomSheetDialogFragment.FilterListener {

    private RecyclerView myEventsRecyclerView;
    private MyEventsAdapter myEventsAdapter;
    private TextView emptyView;
    private ManageEventsViewModel viewModel;
    private EditText searchInput;
    private DrawerLayout drawerLayout;
    private ImageView menuIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish()); // Close activity when back arrow pressed

        // Initialize drawer layout & menu
        drawerLayout = findViewById(R.id.drawer_layout_manage);
        menuIcon = findViewById(R.id.menu_icon);
        NavigationView navigationView = findViewById(R.id.nav_view_manage);

        // Open the navigation drawer when the menu icon is clicked
        menuIcon.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Get organiser flag (used to maintain role context across navigation)
        boolean isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);

        // Handle navigation drawer item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Navigate to home
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_news) {
                // Navigate to news
                drawerLayout.closeDrawer(GravityCompat.START);
                Intent intent = new Intent(this, NewsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_joined_events) {
                // Navigate to joined events
                Intent intent = new Intent(this, JoinedEventsActivity.class);
                intent.putExtra("IS_ORGANISER", isOrganiser);
                startActivity(intent);
            }
            // Always close the drawer after a selection
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Initialize views
        myEventsRecyclerView = findViewById(R.id.myEventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        searchInput = findViewById(R.id.search_input);

        // Setup RecyclerView and search/filter logic
        setupRecyclerView();
        setupSearchAndFilter();

        // Initialize ViewModel for managing events
        viewModel = new ViewModelProvider(this).get(ManageEventsViewModel.class);

        // Observe the list of events and update UI accordingly
        viewModel.getMyEvents().observe(this, managedEventItems -> {
            boolean hasEvents = managedEventItems != null && !managedEventItems.isEmpty();
            if (hasEvents) {
                // Update adapter with new events
                myEventsAdapter.updateEvents(managedEventItems);
            }
            // Update visibility of RecyclerView and empty view
            updateUI(hasEvents);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch latest events when the activity is resumed
        viewModel.fetchMyEvents();
    }

    /**
     * Sets up RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        myEventsAdapter = new MyEventsAdapter(new ArrayList<>(), this, new EventCreationStorage());
        myEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myEventsRecyclerView.setAdapter(myEventsAdapter);
    }

    /**
     * Sets up search functionality and filter button
     */
    private void setupSearchAndFilter() {
        // TextWatcher for search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action required before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update ViewModel search query on every input change
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action required after text changes
            }
        });

        // Show filter dialog when filter button clicked
        findViewById(R.id.filter_button).setOnClickListener(v -> showFilterDialog());
    }

    /**
     * Displays filter dialog for selecting event categories
     */
    private void showFilterDialog() {
        String[] categories = getResources().getStringArray(R.array.event_categories);
        viewModel.showFilterDialog(this, categories, this);
    }

    /**
     * Callback when filters are applied in the bottom sheet dialog
     */
    @Override
    public void onFilterApplied(java.util.List<String> selectedCategories) {
        viewModel.setCategoryFilters(selectedCategories);
    }

    /**
     * Updates UI visibility depending on whether events exist
     */
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
