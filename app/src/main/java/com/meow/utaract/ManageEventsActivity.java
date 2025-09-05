package com.meow.utaract;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.meow.utaract.ui.event.MyEventsAdapter;
import com.meow.utaract.ui.home.FilterBottomSheetDialogFragment;
import com.meow.utaract.ui.manage.ManageEventsViewModel;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;

public class ManageEventsActivity extends AppCompatActivity implements FilterBottomSheetDialogFragment.FilterListener {

    private RecyclerView myEventsRecyclerView;
    private MyEventsAdapter myEventsAdapter;
    private TextView emptyView;
    private ManageEventsViewModel viewModel;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_events);

        myEventsRecyclerView = findViewById(R.id.myEventsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        searchInput = findViewById(R.id.search_input);

        setupRecyclerView();
        setupSearchAndFilter();

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

    private void setupSearchAndFilter() {
        // Set up search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set up filter button click listener
        findViewById(R.id.filter_button).setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        String[] categories = getResources().getStringArray(R.array.event_categories);
        viewModel.showFilterDialog(this, categories, this);
    }

    @Override
    public void onFilterApplied(java.util.List<String> selectedCategories) {
        viewModel.setCategoryFilters(selectedCategories);
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