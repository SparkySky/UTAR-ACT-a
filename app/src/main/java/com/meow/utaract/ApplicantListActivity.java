package com.meow.utaract;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.meow.utaract.utils.GuestProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicantListActivity extends AppCompatActivity {

    private ApplicantListViewModel viewModel;
    private ApplicantAdapter adapter;
    private List<Applicant> allApplicants = new ArrayList<>();
    private String eventId;
    private String organizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicant_list);

        eventId = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");
        if (eventId == null) {
            finish(); // Cannot function without an event ID
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.applicantsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create the adapter only ONCE
        adapter = new ApplicantAdapter(new ArrayList<>(), new ApplicantAdapter.OnApplicantActionListener() {
            @Override
            public void onAccept(Applicant applicant) {
                viewModel.updateApplicantStatus(eventId, applicant.getUserId(), "accepted", organizerId);
            }
            @Override
            public void onReject(Applicant applicant) {
                viewModel.updateApplicantStatus(eventId, applicant.getUserId(), "rejected", organizerId);
            }
            @Override
            public void onViewDetails(Applicant applicant) {
                fetchAndShowApplicantDetails(applicant);
            }
        });

        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ApplicantListViewModel.class);
        viewModel.getApplicants().observe(this, applicants -> {
            allApplicants = applicants;
            filterAndDisplayApplicants(); // Initial display
        });
        viewModel.fetchApplicants(eventId, organizerId);

        setupFilters();
    }

    private void setupFilters() {
        SearchView searchView = findViewById(R.id.searchView);
        ChipGroup chipGroup = findViewById(R.id.filterChipGroup);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filterAndDisplayApplicants();
                return true;
            }
        });

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> filterAndDisplayApplicants());
    }

    private void filterAndDisplayApplicants() {
        SearchView searchView = findViewById(R.id.searchView);
        ChipGroup chipGroup = findViewById(R.id.filterChipGroup);
        String query = searchView.getQuery().toString().toLowerCase().trim();
        int checkedChipId = chipGroup.getCheckedChipId();

        List<Applicant> filteredList = new ArrayList<>(allApplicants);

        // Filter by status
        if (checkedChipId == R.id.chipPending) {
            filteredList = filteredList.stream().filter(a -> "pending".equals(a.getStatus())).collect(Collectors.toList());
        } else if (checkedChipId == R.id.chipAccepted) {
            filteredList = filteredList.stream().filter(a -> "accepted".equals(a.getStatus())).collect(Collectors.toList());
        } else if (checkedChipId == R.id.chipRejected) {
            filteredList = filteredList.stream().filter(a -> "rejected".equals(a.getStatus())).collect(Collectors.toList());
        }

        // Filter by name
        if (!query.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(a -> a.getUserName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        // Update the existing adapter's list instead of creating a new one
        adapter.updateList(filteredList);
    }

    private void fetchAndShowApplicantDetails(Applicant applicant) {
        FirebaseFirestore.getInstance().collection("guest_profiles").document(applicant.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String json = documentSnapshot.getString("profile_json");
                        GuestProfile profile = new Gson().fromJson(json, GuestProfile.class);
                        if (profile != null) {
                            String details = "Name: " + profile.getName() + "\n" +
                                    "Email: " + profile.getEmail() + "\n" +
                                    "Phone: " + profile.getPhone();
                            new AlertDialog.Builder(this)
                                    .setTitle("Applicant Details")
                                    .setMessage(details)
                                    .setPositiveButton("Close", null)
                                    .show();
                        }
                    } else {
                        Toast.makeText(this, "Could not find profile for this applicant.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}