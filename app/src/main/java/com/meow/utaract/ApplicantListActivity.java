package com.meow.utaract;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ApplicantListActivity extends AppCompatActivity {
    private ApplicantListViewModel viewModel;
    private ApplicantAdapter adapter;
    private String eventId;
    private String organizerId;
    private List<Applicant> allApplicants = new ArrayList<>();
    private ActivityResultLauncher<ScanOptions> qrScannerLauncher;
    private FirebaseFirestore firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicant_list);

        firebase = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("EVENT_ID");
        organizerId = getIntent().getStringExtra("ORGANIZER_ID");
        if (eventId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.applicantsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
                // When viewing details from the list, we don't need the "Scan Next" option.
                showApplicantDetailsDialog(applicant, false);
            }
        });

        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ApplicantListViewModel.class);
        viewModel.getApplicants().observe(this, applicants -> {
            allApplicants = applicants;
            updateChipCounts();
            filterAndDisplayApplicants();
        });
        viewModel.fetchApplicants(eventId, organizerId);

        qrScannerLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                verifyTicket(result.getContents());
            }
        });

        ImageView qrScannerButton = findViewById(R.id.qrScannerButton);
        qrScannerButton.setOnClickListener(v -> launchScanner());

        setupFilters();
    }

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

        if (checkedChipId == R.id.chipPending) {
            filteredList = filteredList.stream().filter(a -> "pending".equals(a.getStatus())).collect(Collectors.toList());
        } else if (checkedChipId == R.id.chipAccepted) {
            filteredList = filteredList.stream().filter(a -> "accepted".equals(a.getStatus())).collect(Collectors.toList());
        } else if (checkedChipId == R.id.chipRejected) {
            filteredList = filteredList.stream().filter(a -> "rejected".equals(a.getStatus())).collect(Collectors.toList());
        }

        if (!query.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(a -> a.getUserName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        adapter.updateList(filteredList);
    }

    private void showApplicantDetailsDialog(Applicant applicant, boolean isFromScanner) {
        String title;
        String message;

        // Determine the title and message based on the verification context
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

        // Only show the "Scan Next" button if the dialog was triggered by the scanner
        if (isFromScanner) {
            builder.setNeutralButton("Scan Next", (dialog, which) -> {
                dialog.dismiss();
                launchScanner(); // Relaunch the scanner for the next guest
            });
        }

        builder.show();
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan Attendee's QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrScannerLauncher.launch(options);
    }

    private void verifyTicket(String ticketCode) {
        firebase.collection("events").document(eventId).collection("registrations")
                .whereEqualTo("ticketCode", ticketCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Applicant applicant = queryDocumentSnapshots.getDocuments().get(0).toObject(Applicant.class);
                        if (applicant != null) {
                            // Show details with the continuous scanning option
                            showApplicantDetailsDialog(applicant, true);
                        }
                    } else {
                        // Show invalid ticket dialog with the continuous scanning option
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

    // Renamed from fetchAndShowApplicantDetails to avoid confusion
    private void showApplicantDetailsDialog(Applicant applicant) {
        // This is the original method, now we call the new one with isFromScanner = false
        showApplicantDetailsDialog(applicant, false);
    }
}