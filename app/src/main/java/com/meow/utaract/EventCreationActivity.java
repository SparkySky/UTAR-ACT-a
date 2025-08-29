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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.EventCreationStorage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class EventCreationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private TextInputEditText etEventName, etDescription, etDate, etTime, etLocation, etMaxGuests, etFee, etPublishDate, etPublishTime;
    private Spinner spinnerCategory;
    private DrawerLayout drawerLayout;
    private EventCreationStorage eventStorage;
    private Button btnUploadPoster, btnUploadCatalog, btnCreate;
    private ImageView ivPosterPreview;
    private LinearLayout layoutCatalogPreview;
    private ActivityResultLauncher<Intent> posterImageLauncher, catalogImageLauncher;
    private MaterialSwitch publishSwitch;
    private LinearLayout scheduleLayout;

    private String posterImageUrl = "";
    private List<String> catalogImageUrls = new ArrayList<>();
    private boolean isEditMode = false;
    private Event eventToEdit;
    private final Calendar publishCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_creation);

        initializeViews();
        setupNavigationDrawer();
        setupCategorySpinner();
        setupDateTimePickers();
        setupButtonListeners();
        initializeImageLaunchers();
        eventStorage = new EventCreationStorage();

        if (getIntent().hasExtra("IS_EDIT_MODE")) {
            isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
            eventToEdit = (Event) getIntent().getSerializableExtra("EDIT_EVENT_DATA");
            if (isEditMode && eventToEdit != null) {
                setupEditMode();
            }
        }
    }

    private void initializeViews() {
        etEventName = findViewById(R.id.etEventName);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etMaxGuests = findViewById(R.id.etMaxGuests);
        etFee = findViewById(R.id.etFee);
        publishSwitch = findViewById(R.id.publishSwitch);
        scheduleLayout = findViewById(R.id.scheduleLayout);
        etPublishDate = findViewById(R.id.etPublishDate);
        etPublishTime = findViewById(R.id.etPublishTime);
        btnCreate = findViewById(R.id.btnCreate);
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

    private void setupCategorySpinner() {
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

        // Pickers for the scheduling feature
        etPublishDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                publishCalendar.set(Calendar.YEAR, year);
                publishCalendar.set(Calendar.MONTH, month);
                publishCalendar.set(Calendar.DAY_OF_MONTH, day);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etPublishDate.setText(sdf.format(publishCalendar.getTime()));
            }, publishCalendar.get(Calendar.YEAR), publishCalendar.get(Calendar.MONTH), publishCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        etPublishTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                publishCalendar.set(Calendar.HOUR_OF_DAY, hour);
                publishCalendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                etPublishTime.setText(sdf.format(publishCalendar.getTime()));
            }, publishCalendar.get(Calendar.HOUR_OF_DAY), publishCalendar.get(Calendar.MINUTE), false).show();
        });
    }

    private void setupButtonListeners() {
        findViewById(R.id.btnReset).setOnClickListener(v -> resetForm());
        btnUploadPoster.setOnClickListener(v -> openImagePicker(posterImageLauncher, false));
        btnUploadCatalog.setOnClickListener(v -> openImagePicker(catalogImageLauncher, true));
        publishSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            scheduleLayout.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
        btnCreate.setOnClickListener(v -> saveEvent());
    }

    private void setupEditMode() {
        ((TextView) findViewById(R.id.event_creation_title)).setText("Update Event");
        btnCreate.setText("Update Event");

        // Populate all fields from the event object passed to the activity
        etEventName.setText(eventToEdit.getEventName());
        etDescription.setText(eventToEdit.getDescription());
        etDate.setText(eventToEdit.getDate());
        etTime.setText(eventToEdit.getTime());
        etLocation.setText(eventToEdit.getLocation());
        etMaxGuests.setText(String.valueOf(eventToEdit.getMaxGuests()));
        etFee.setText(String.valueOf(eventToEdit.getFee()));

        // Set spinner selection
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(eventToEdit.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Set image previews
        posterImageUrl = eventToEdit.getCoverImageUrl();
        if (posterImageUrl != null && !posterImageUrl.isEmpty()) {
            Glide.with(this).load(posterImageUrl).into(ivPosterPreview);
            ivPosterPreview.setVisibility(View.VISIBLE);
        }
        catalogImageUrls = new ArrayList<>(eventToEdit.getAdditionalImageUrls());
        for (String url : catalogImageUrls) {
            addCatalogImageToPreview(Uri.parse(url));
        }

        // Handle visibility and scheduling UI
        publishSwitch.setChecked(eventToEdit.isVisible());
        if (eventToEdit.getPublishAt() > System.currentTimeMillis() && !eventToEdit.isVisible()) {
            scheduleLayout.setVisibility(View.VISIBLE);
            publishCalendar.setTimeInMillis(eventToEdit.getPublishAt());
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etPublishDate.setText(sdfDate.format(publishCalendar.getTime()));
            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            etPublishTime.setText(sdfTime.format(publishCalendar.getTime()));
        } else {
            scheduleLayout.setVisibility(View.GONE);
        }
    }

    private void saveEvent() {
        if (!isFormValid()) return;

        String eventName = Objects.requireNonNull(etEventName.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        String date = Objects.requireNonNull(etDate.getText()).toString().trim();
        String time = Objects.requireNonNull(etTime.getText()).toString().trim();
        String location = Objects.requireNonNull(etLocation.getText()).toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        int maxGuests = Integer.parseInt(Objects.requireNonNull(etMaxGuests.getText()).toString().trim());
        double fee = Double.parseDouble(Objects.requireNonNull(etFee.getText()).toString().trim());

        boolean isVisible;
        long publishAt;

        if (publishSwitch.isChecked()) {
            isVisible = true;
            publishAt = System.currentTimeMillis();
        } else {
            isVisible = false;
            publishAt = publishCalendar.getTimeInMillis();
            if (publishAt <= System.currentTimeMillis()) {
                Toast.makeText(this, "Scheduled publish time must be in the future.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String organizerId = isEditMode ? eventToEdit.getOrganizerId() : FirebaseAuth.getInstance().getUid();

        Event event = new Event(eventName, description, category, date, time, location, organizerId, maxGuests, fee, isVisible, publishAt);
        event.setCoverImageUrl(posterImageUrl);
        event.setAdditionalImageUrls(catalogImageUrls);

        if (isEditMode) {
            event.setEventId(eventToEdit.getEventId());
            event.setCreatedAt(eventToEdit.getCreatedAt()); // Preserve original creation date
            eventStorage.updateEvent(event.getEventId(), event, getEventCreationCallback());
        } else {
            eventStorage.createEvent(event, getEventCreationCallback());
        }
    }

    private EventCreationStorage.EventCreationCallback getEventCreationCallback() {
        return new EventCreationStorage.EventCreationCallback() {
            @Override
            public void onSuccess(String eventId) {
                runOnUiThread(() -> {
                    String message = isEditMode ? "Event updated successfully!" : "Event created successfully!";
                    Toast.makeText(EventCreationActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    String message = isEditMode ? "Failed to update event: " : "Failed to create event: ";
                    Toast.makeText(EventCreationActivity.this, message + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("EventCreation", "Error saving event", e);
                });
            }
        };
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