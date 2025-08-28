package com.meow.utaract;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EventCreationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private TextInputEditText etEventName, etDescription, etDate, etTime, etLocation, etMaxGuests;
    private Spinner spinnerCategory;
    private DrawerLayout drawerLayout;
    private EventCreationStorage eventStorage;
    private Button btnUploadPoster, btnUploadCatalog;
    private ImageView ivPosterPreview;
    private LinearLayout layoutCatalogPreview;
    private ActivityResultLauncher<Intent> posterImageLauncher, catalogImageLauncher;
    private String posterImageUrl = "";
    private List<String> catalogImageUrls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_creation);

        initializeViews();
        setupNavigationDrawer();
        setupCategorySpinner();
        setupDateTimePickers();
        setupButtonListeners();

        eventStorage = new EventCreationStorage();
        initializeImageLaunchers();
    }

    private void initializeViews() {
        etEventName = findViewById(R.id.etEventName);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etMaxGuests = findViewById(R.id.etMaxGuests);
        //etFee = findViewById(R.id.etFee);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnUploadPoster = findViewById(R.id.btnUploadPoster);
        btnUploadCatalog = findViewById(R.id.btnUploadCatalog);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        layoutCatalogPreview = findViewById(R.id.layoutCatalogPreview);
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ImageButton drawerButton = findViewById(R.id.drawerButton);
        drawerButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    // Retrieve the available category for spinners from the string array
    private void setupCategorySpinner() {
        // This is the corrected implementation that uses the centralized array
        String[] categories = getResources().getStringArray(R.array.event_categories);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupDateTimePickers() {
        final Calendar calendar = Calendar.getInstance();
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                String selectedDate = day + "/" + (month + 1) + "/" + year;
                etDate.setText(selectedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        etTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                @SuppressLint("DefaultLocale") String selectedTime = String.format("%02d:%02d", hour, minute);
                etTime.setText(selectedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });
    }

    private void setupButtonListeners() {
        findViewById(R.id.btnReset).setOnClickListener(v -> resetForm());
        findViewById(R.id.btnCreate).setOnClickListener(v -> createEvent());

        btnUploadPoster.setOnClickListener(v -> openImagePicker(posterImageLauncher, false));
        btnUploadCatalog.setOnClickListener(v -> openImagePicker(catalogImageLauncher, true));
    }

    private void initializeImageLaunchers() {
        posterImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                uploadImageToStorage(result.getData().getData(), true);
            }
        });

        catalogImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        uploadImageToStorage(result.getData().getClipData().getItemAt(i).getUri(), false);
                    }
                } else if (result.getData().getData() != null) {
                    uploadImageToStorage(result.getData().getData(), false);
                }
            }
        });
    }

    private void openImagePicker(ActivityResultLauncher<Intent> launcher, boolean allowMultiple) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        launcher.launch(intent);
    }

    private void uploadImageToStorage(Uri imageUri, boolean isPoster) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        String fileName = "event_images/" + UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            if (isPoster) {
                                posterImageUrl = uri.toString();
                                ivPosterPreview.setImageURI(imageUri);
                                ivPosterPreview.setVisibility(View.VISIBLE);
                            } else {
                                catalogImageUrls.add(uri.toString());
                                addCatalogImageToPreview(imageUri);
                            }
                            Toast.makeText(this, "Image uploaded.", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void addCatalogImageToPreview(Uri imageUri) {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
        params.setMarginEnd(16);
        imageView.setLayoutParams(params);
        imageView.setImageURI(imageUri);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        layoutCatalogPreview.addView(imageView);
    }

    private void createEvent() {
        if (!isFormValid()) {
            return;
        }

        String eventName = Objects.requireNonNull(etEventName.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        String date = Objects.requireNonNull(etDate.getText()).toString().trim();
        String time = Objects.requireNonNull(etTime.getText()).toString().trim();
        String location = Objects.requireNonNull(etLocation.getText()).toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        int maxGuests = Integer.parseInt(Objects.requireNonNull(etMaxGuests.getText()).toString().trim());
        //double fee = Double.parseDouble(Objects.requireNonNull(etFee.getText()).toString().trim());

        GuestProfile organizerProfile = new GuestProfileStorage(this).loadProfile();
        String organizerName = (organizerProfile != null) ? organizerProfile.getName() : "Unknown Organizer";

        Event event = new Event(eventName, description, category, date, time, location, "", organizerName, maxGuests);

        event.setCoverImageUrl(posterImageUrl);
        event.setAdditionalImageUrls(catalogImageUrls);

        Toast.makeText(this, "Creating event...", Toast.LENGTH_SHORT).show();
        eventStorage.createEvent(event, new EventCreationStorage.EventCreationCallback() {
            @Override
            public void onSuccess(String eventId) {
                runOnUiThread(() -> {
                    Toast.makeText(EventCreationActivity.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(EventCreationActivity.this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("EventCreation", "Error creating event", e);
                });
            }
        });
    }

    private boolean isFormValid() {
        if (Objects.requireNonNull(etEventName.getText()).toString().trim().isEmpty()) {
            etEventName.setError("Event name is required");
            etEventName.requestFocus();
            return false;
        }
        if (Objects.requireNonNull(etDescription.getText()).toString().trim().isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }
        if (Objects.requireNonNull(etDate.getText()).toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Objects.requireNonNull(etTime.getText()).toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Objects.requireNonNull(etLocation.getText()).toString().trim().isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return false;
        }
        if (Objects.requireNonNull(etMaxGuests.getText()).toString().trim().isEmpty()) {
            etMaxGuests.setError("Max guests is required");
            etMaxGuests.requestFocus();
            return false;
        }
/*        if (Objects.requireNonNull(etFee.getText()).toString().trim().isEmpty()) {
            etFee.setError("Fee is required (enter 0 for free events)");
            etFee.requestFocus();
            return false;
        }*/
        return true;
    }

    private void resetForm() {
        etEventName.setText("");
        etDescription.setText("");
        etDate.setText("");
        etTime.setText("");
        etLocation.setText("");
        etMaxGuests.setText("");
        //etFee.setText("");
        spinnerCategory.setSelection(0);
        Toast.makeText(this, "Form has been reset", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}