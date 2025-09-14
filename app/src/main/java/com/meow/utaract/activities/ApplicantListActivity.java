package com.meow.utaract.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.meow.utaract.utils.Applicant;
import com.meow.utaract.adapters.ApplicantAdapter;
import com.meow.utaract.viewmodels.ApplicantListViewModel;
import com.meow.utaract.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Activity to display and manage a list of applicants for an event.
 * Supports filtering, searching, QR scanning, and updating applicant status.
 */
public class ApplicantListActivity extends AppCompatActivity {
    private ApplicantListViewModel viewModel;
    private ApplicantAdapter adapter;
    private String eventId;
    private String organizerId;
    private List<Applicant> allApplicants = new ArrayList<>();
    private ActivityResultLauncher<ScanOptions> qrScannerLauncher;
    private FirebaseFirestore firebase;

    /**
     * Called when the activity is created.
     * Sets up UI, initializes ViewModel, RecyclerView, QR scanner, and filter controls.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicant_list);

        firebase = FirebaseFirestore.getInstance();

        // Get event and organizer IDs passed from previous activity
        eventId = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");
        if (eventId == null) {
            finish();
            return;
        }

        // Setup toolbar with back navigation
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView for displaying applicants
        RecyclerView recyclerView = findViewById(R.id.applicantsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup adapter with applicant actions
        adapter = new ApplicantAdapter(new ArrayList<>(), new ApplicantAdapter.OnApplicantActionListener() {
            @Override
            public void onAccept(Applicant applicant) {
                // Update status to "accepted"
                viewModel.updateApplicantStatus(eventId, applicant.getUserId(), "accepted", organizerId);
            }
            @Override
            public void onReject(Applicant applicant) {
                // Update status to "rejected"
                viewModel.updateApplicantStatus(eventId, applicant.getUserId(), "rejected", organizerId);
            }
            @Override
            public void onViewDetails(Applicant applicant) {
                // Show details dialog (from list, no "Scan Next" option)
                showApplicantDetailsDialog(applicant, false);
            }
        });

        recyclerView.setAdapter(adapter);

        // Initialize ViewModel and observe applicant list
        viewModel = new ViewModelProvider(this).get(ApplicantListViewModel.class);
        viewModel.getApplicants().observe(this, applicants -> {
            allApplicants = applicants;
            updateChipCounts();
            filterAndDisplayApplicants();
        });
        viewModel.fetchApplicants(eventId, organizerId);

        // Setup QR scanner callback
        qrScannerLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                verifyTicket(result.getContents());
            }
        });

        // Setup QR scanner button
        ImageView qrScannerButton = findViewById(R.id.qrScannerButton);
        qrScannerButton.setOnClickListener(v -> launchScanner());

        // Setup search and filter chips
        setupFilters();
    }

    /**
     * Updates the chip labels with the current applicant counts
     * (All, Pending, Accepted, Rejected).
     */
    private void updateChipCounts() {
        long all = allApplicants.size();
        long pending = allApplicants.stream().filter(a -> "pending".equals(a.getStatus())).count();
        long accepted = allApplicants.stream().filter(a -> "accepted".equals(a.getStatus())).count();
        long rejected = allApplicants.stream().filter(a -> "rejected".equals(a.getStatus())).count();

        ((Chip) findViewById(R.id.chipAll)).setText(String.format(Locale.getDefault(), "All (%d)", all));
        ((Chip) findViewById(R.id.chipPending)).setText(String.format(Locale.getDefault(), "Pending (%d)", pending));
        ((Chip) findViewById(R.id.chipAccepted)).setText(String.format(Locale.getDefault(), "Accepted (%d)", accepted));
        ((Chip) findViewById(R.id.chipRejected)).setText(String.format(Locale.getDefault(), "Rejected (%d)", rejected));
    }

    /**
     * Sets up search bar and filter chip group listeners
     * to filter the applicant list dynamically.
     */
    private void setupFilters() {
        SearchView searchView = findViewById(R.id.searchView);
        ChipGroup chipGroup = findViewById(R.id.filterChipGroup);

        // Search filtering
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filterAndDisplayApplicants();
                return true;
            }
        });

        // Chip selection filtering
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> filterAndDisplayApplicants());
    }

    /**
     * Applies search and filter criteria to the applicant list
     * and updates the RecyclerView adapter with the results.
     */
    private void filterAndDisplayApplicants() {
        SearchView searchView = findViewById(R.id.searchView);
        ChipGroup chipGroup = findViewById(R.id.filterChipGroup);
        String query = searchView.getQuery().toString().toLowerCase().trim();
        int checkedChipId = chipGroup.getCheckedChipId();

        List<Applicant> filteredList = new ArrayList<>(allApplicants);

        // Apply chip filter
        if (checkedChipId == R.id.chipPending) {
            filteredList = filteredList.stream().filter(a -> "pending".equals(a.getStatus())).collect(Collectors.toList());
        } else if (checkedChipId == R.id.chipAccepted) {
            filteredList = filteredList.stream().filter(a -> "accepted".equals(a.getStatus())).collect(Collectors.toList());
        } else if (checkedChipId == R.id.chipRejected) {
            filteredList = filteredList.stream().filter(a -> "rejected".equals(a.getStatus())).collect(Collectors.toList());
        }

        // Apply search filter
        if (!query.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(a -> a.getUserName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        // Update list in RecyclerView
        adapter.updateList(filteredList);
    }

    /**
     * Shows a dialog with applicant details.
     * If triggered by the scanner, also provides a "Scan Next" button.
     */
    private void showApplicantDetailsDialog(Applicant applicant, boolean isFromScanner) {
        String title;
        String message;

        // Build dialog message depending on status
        if (applicant.getStatus() != null && applicant.getStatus().equals("accepted")) {
            title = "Ticket Verified";
            message = "Name: " + applicant.getUserName() + "\n" +
                    "Email: " + applicant.getEmail() + "\n" +
                    "Phone: " + applicant.getPhone() + "\n" +
                    "Status: " + applicant.getStatus().toUpperCase();
        } else {
            title = "Applicant Details";
            message = "Name: " + applicant.getUserName() + "\n" +
                    "Email: " + applicant.getEmail() + "\n" +
                    "Phone: " + applicant.getPhone();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Add "Scan Next" if opened from scanner
        if (isFromScanner) {
            builder.setNeutralButton("Scan Next", (dialog, which) -> {
                dialog.dismiss();
                launchScanner();
            });
        }

        builder.show();
    }

    /**
     * Launches the QR code scanner with custom options.
     */
    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan Attendee's QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrScannerLauncher.launch(options);
    }

    /**
     * Verifies a scanned ticket code against Firestore registrations.
     * Shows applicant details if valid, or an "Invalid Ticket" dialog if not.
     */
    private void verifyTicket(String ticketCode) {
        firebase.collection("events").document(eventId).collection("registrations")
                .whereEqualTo("ticketCode", ticketCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Applicant applicant = queryDocumentSnapshots.getDocuments().get(0).toObject(Applicant.class);
                        if (applicant != null) {
                            // Show details with continuous scanning option
                            showApplicantDetailsDialog(applicant, true);
                        }
                    } else {
                        // Invalid ticket
                        new AlertDialog.Builder(this)
                                .setTitle("Invalid Ticket")
                                .setMessage("This QR code is not valid for this event.")
                                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                                .setNeutralButton("Scan Next", (dialog, which) -> {
                                    dialog.dismiss();
                                    launchScanner();
                                })
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error verifying ticket. Please try again.", Toast.LENGTH_SHORT).show();
                    Log.e("ApplicantListActivity", "Ticket verification failed", e);
                });
    }

    /**
     * Convenience method to show applicant details when not using the scanner.
     * Calls the main dialog function with isFromScanner = false.
     */
    private void showApplicantDetailsDialog(Applicant applicant) {
        showApplicantDetailsDialog(applicant, false);
    }
}
