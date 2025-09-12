package com.meow.utaract;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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
    private Button btnUploadDoc;
    private ImageView ivPosterPreview;
    private LinearLayout layoutCatalogPreview;
    private ActivityResultLauncher<Intent> posterImageLauncher, catalogImageLauncher;
    private ActivityResultLauncher<Intent> docPickerLauncher;
    private MaterialSwitch scheduleSwitch;
    private LinearLayout scheduleLayout;
    private ProgressBar progressBar;
    private FrameLayout posterFrame;
    private ImageView removePosterButton;

    private String posterImageUrl = "";
    private String uploadedDocText = "";
    private String uploadedDocName = "";
    private List<String> catalogImageUrls = new ArrayList<>();
    private boolean isEditMode = false;
    private Event eventToEdit;
    private final Calendar publishCalendar = Calendar.getInstance();
    private HorizontalScrollView catalogScrollView;
    private Uri newPosterUri = null;
    private List<Uri> newCatalogUris = new ArrayList<>();
    private TextInputEditText etSocialMediaLink;
    private LinearLayout uploadedDocumentsLayout;

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
        etSocialMediaLink = findViewById(R.id.etSocialMediaLink);
        eventStorage = new EventCreationStorage();

        updateUploadedDocumentsDisplay();

        if (getIntent().hasExtra("eventId")) {
            isEditMode = true;
            String eventId = getIntent().getStringExtra("eventId");
            loadEventDetails(eventId);
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
        scheduleSwitch = findViewById(R.id.scheduleSwitch);
        scheduleLayout = findViewById(R.id.scheduleLayout);
        etPublishDate = findViewById(R.id.etPublishDate);
        etPublishTime = findViewById(R.id.etPublishTime);
        btnCreate = findViewById(R.id.btnCreate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnUploadPoster = findViewById(R.id.buttonSelectPoster);
        btnUploadCatalog = findViewById(R.id.buttonAddCatalogImage);
        btnUploadDoc = findViewById(R.id.btnUploadDoc);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);
        posterFrame = findViewById(R.id.posterFrame);
        removePosterButton = findViewById(R.id.removePosterButton);
        catalogScrollView = findViewById(R.id.catalogScrollView);
        layoutCatalogPreview = findViewById(R.id.layoutCatalogPreview);
        progressBar = findViewById(R.id.progressBar);
        uploadedDocumentsLayout = findViewById(R.id.uploadedDocumentsLayout);
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
        btnUploadDoc.setOnClickListener(v -> openDocPicker());
        scheduleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            scheduleLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        btnCreate.setOnClickListener(v -> saveEvent());
        removePosterButton.setOnClickListener(v -> {
            newPosterUri = null;
            posterImageUrl = "";
            updatePosterPreview();
        });
    }

    private void saveEvent() {
        if (!isFormValid()) return;

        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);

        uploadAllImagesAndSaveEvent();
    }

    private void uploadAllImagesAndSaveEvent() {
        final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        final List<Task<Uri>> uploadTasks = new ArrayList<>();
        final List<String> finalCatalogUrls = new ArrayList<>(catalogImageUrls);

        if (newPosterUri != null) {
            final StorageReference posterRef = storageRef.child("event_images/" + UUID.randomUUID().toString());
            UploadTask posterUploadTask = posterRef.putFile(newPosterUri);
            Task<Uri> posterUrlTask = posterUploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                return posterRef.getDownloadUrl();
            });
            uploadTasks.add(posterUrlTask);
        }

        for (Uri uri : newCatalogUris) {
            final StorageReference catalogRef = storageRef.child("event_images/" + UUID.randomUUID().toString());
            UploadTask catalogUploadTask = catalogRef.putFile(uri);
            Task<Uri> catalogUrlTask = catalogUploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                return catalogRef.getDownloadUrl();
            });
            uploadTasks.add(catalogUrlTask);
        }

        Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(results -> {
            int urlIndex = 0;
            if (newPosterUri != null) {
                posterImageUrl = results.get(urlIndex++).toString();
            }

            for (; urlIndex < results.size(); urlIndex++) {
                finalCatalogUrls.add(results.get(urlIndex).toString());
            }

            saveEventToFirestore(posterImageUrl, finalCatalogUrls);

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            btnCreate.setEnabled(true);
            Toast.makeText(EventCreationActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });

        if (uploadTasks.isEmpty()) {
            saveEventToFirestore(posterImageUrl, finalCatalogUrls);
        }
    }

    private void saveEventToFirestore(String finalPosterUrl, List<String> finalCatalogUrls) {
        String eventName = Objects.requireNonNull(etEventName.getText()).toString().trim();
        long publishAt = scheduleSwitch.isChecked() ? publishCalendar.getTimeInMillis() : System.currentTimeMillis();
        String organizerId = isEditMode ? eventToEdit.getOrganizerId() : Objects.requireNonNull(FirebaseAuth.getInstance().getUid());

        Event event = new Event(eventName, etDescription.getText().toString(), spinnerCategory.getSelectedItem().toString(),
                etDate.getText().toString(), etTime.getText().toString(), etLocation.getText().toString(),
                organizerId, Integer.parseInt(etMaxGuests.getText().toString()), Double.parseDouble(etFee.getText().toString()),
                publishAt);

        event.setCoverImageUrl(finalPosterUrl);
        event.setAdditionalImageUrls(finalCatalogUrls);

        String socialMedia = Objects.requireNonNull(etSocialMediaLink.getText()).toString().trim();
        event.setSocialMediaLink(socialMedia);

        if (uploadedDocText != null && !uploadedDocText.trim().isEmpty() &&
                !uploadedDocText.contains("Error extracting") && !uploadedDocText.contains("Could not extract") &&
                !uploadedDocText.contains("not supported") && !uploadedDocText.contains("requires additional library setup")) {
            event.setUploadedDocumentText(uploadedDocText);
            event.setUploadedDocumentName(uploadedDocName != null ? uploadedDocName : "Uploaded Document");
        }

        if (isEditMode && eventToEdit.getSummary() != null && !eventToEdit.getSummary().isEmpty()) {
            event.setSummary(eventToEdit.getSummary());
        }

        EventCreationStorage.EventCreationCallback callback = new EventCreationStorage.EventCreationCallback() {
            @Override
            public void onSuccess(String eventId) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EventCreationActivity.this, isEditMode ? "Event updated!" : "Event created!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnCreate.setEnabled(true);
                Toast.makeText(EventCreationActivity.this, "Failed to save event: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        if (isEditMode) {
            event.setEventId(eventToEdit.getEventId());
            event.setCreatedAt(eventToEdit.getCreatedAt());
            eventStorage.updateEvent(event.getEventId(), event, callback);
        } else {
            eventStorage.createEvent(event, callback);
        }
    }
    private void setupEditMode() {
        ((TextView) findViewById(R.id.event_creation_title)).setText("Update Event");
        btnCreate.setText("Update Event");

        etEventName.setText(eventToEdit.getEventName());
        etDescription.setText(eventToEdit.getDescription());
        etDate.setText(eventToEdit.getDate());
        etTime.setText(eventToEdit.getTime());
        etLocation.setText(eventToEdit.getLocation());
        etMaxGuests.setText(String.valueOf(eventToEdit.getMaxGuests()));
        etFee.setText(String.valueOf(eventToEdit.getFee()));

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(eventToEdit.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        posterImageUrl = eventToEdit.getCoverImageUrl();

        catalogImageUrls = new ArrayList<>(eventToEdit.getAdditionalImageUrls());
        updatePosterPreview();
        updateCatalogPreview();

        String savedDocText = eventToEdit.getUploadedDocumentText();
        String savedDocName = eventToEdit.getUploadedDocumentName();

        if (savedDocText != null && !savedDocText.trim().isEmpty()) {
            uploadedDocText = savedDocText;
            uploadedDocName = (savedDocName != null && !savedDocName.trim().isEmpty()) ? savedDocName : "Uploaded Document";
        } else {
            uploadedDocText = "";
            uploadedDocName = "";
        }

        if (eventToEdit.getSocialMediaLink() != null && !eventToEdit.getSocialMediaLink().isEmpty()) {
            etSocialMediaLink.setText(eventToEdit.getSocialMediaLink());
        }

        updateUploadedDocumentsDisplay();

        if (eventToEdit.getPublishAt() > System.currentTimeMillis()) {
            scheduleSwitch.setChecked(true);
            scheduleLayout.setVisibility(View.VISIBLE);
            publishCalendar.setTimeInMillis(eventToEdit.getPublishAt());
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etPublishDate.setText(sdfDate.format(publishCalendar.getTime()));
            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            etPublishTime.setText(sdfTime.format(publishCalendar.getTime()));
        } else {
            scheduleSwitch.setChecked(false);
            scheduleLayout.setVisibility(View.GONE);
        }
    }

    private void persistEvent(Event event) {
        if (isEditMode) {
            event.setEventId(eventToEdit.getEventId());
            event.setCreatedAt(eventToEdit.getCreatedAt());
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
                newPosterUri = result.getData().getData();
                posterImageUrl = "";
                updatePosterPreview();
            }
        });

        catalogImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        newCatalogUris.add(result.getData().getClipData().getItemAt(i).getUri());
                    }
                } else if (result.getData().getData() != null) {
                    newCatalogUris.add(result.getData().getData());
                }
                updateCatalogPreview();
            }
        });

        docPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                uploadedDocName = getFileName(uri);
                uploadedDocText = readTextFromUri(uri);
                if (uploadedDocText != null) {
                    if (uploadedDocText.contains("Error extracting") || uploadedDocText.contains("Could not extract") || uploadedDocText.contains("not supported")) {
                        Toast.makeText(this, uploadedDocText, Toast.LENGTH_LONG).show();
                    } else if (uploadedDocText.trim().isEmpty()) {
                        Toast.makeText(this, "Document is empty or contains no readable text", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Document ready for summarization (" + uploadedDocText.length() + " characters)", Toast.LENGTH_SHORT).show();
                        updateUploadedDocumentsDisplay();
                    }
                } else {
                    Toast.makeText(this, "Failed to read document", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openDocPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/plain",
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        docPickerLauncher.launch(intent);
    }

    private String readTextFromUri(Uri uri) {
        try {
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) {
                String fileName = getFileName(uri);
                if (fileName != null) {
                    if (fileName.toLowerCase().endsWith(".pdf")) {
                        mimeType = "application/pdf";
                    } else if (fileName.toLowerCase().endsWith(".doc") || fileName.toLowerCase().endsWith(".docx")) {
                        mimeType = "application/msword";
                    } else if (fileName.toLowerCase().endsWith(".txt")) {
                        mimeType = "text/plain";
                    }
                }
            }

            if (mimeType != null && mimeType.startsWith("text/")) {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(getContentResolver().openInputStream(uri)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(getContentResolver().openInputStream(uri)))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            Log.e("DocumentReader", "Error reading document", e);
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void openImagePicker(ActivityResultLauncher<Intent> launcher, boolean allowMultiple) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        launcher.launch(intent);
    }

    private void updateCatalogPreview() {
        layoutCatalogPreview.removeAllViews();

        for (String url : catalogImageUrls) {
            addSingleCatalogItem(url);
        }

        for (Uri uri : newCatalogUris) {
            addSingleCatalogItem(uri);
        }

        catalogScrollView.setVisibility(catalogImageUrls.isEmpty() && newCatalogUris.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void addSingleCatalogItem(Object imageSource) {
        FrameLayout itemFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(250, 250);
        frameParams.setMarginEnd(16);
        itemFrame.setLayoutParams(frameParams);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this)
                .load(imageSource)
                .override(Target.SIZE_ORIGINAL)
                .into(imageView);

        ImageView removeBtn = new ImageView(this);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(48, 48, Gravity.TOP | Gravity.END);
        removeBtn.setLayoutParams(btnParams);
        removeBtn.setPadding(8, 8, 8, 8);
        removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeBtn.setBackgroundResource(R.drawable.round_red_background);
        removeBtn.setOnClickListener(v -> {
            if (imageSource instanceof Uri) {
                newCatalogUris.remove(imageSource);
            } else if (imageSource instanceof String) {
                catalogImageUrls.remove(imageSource);
            }
            updateCatalogPreview();
        });

        itemFrame.addView(imageView);
        itemFrame.addView(removeBtn);
        layoutCatalogPreview.addView(itemFrame);
    }

    private void updatePosterPreview() {
        if (newPosterUri != null) {
            posterFrame.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(newPosterUri)
                    .override(Target.SIZE_ORIGINAL)
                    .into(ivPosterPreview);
        } else if (posterImageUrl != null && !posterImageUrl.isEmpty()) {
            posterFrame.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(posterImageUrl)
                    .override(Target.SIZE_ORIGINAL)
                    .into(ivPosterPreview);
        } else {
            posterFrame.setVisibility(View.GONE);
        }
    }

    private void updateUploadedDocumentsDisplay() {
        uploadedDocumentsLayout.removeAllViews();

        if (uploadedDocName != null && !uploadedDocName.trim().isEmpty() &&
                uploadedDocText != null && !uploadedDocText.trim().isEmpty()) {

            LinearLayout documentCard = new LinearLayout(this);
            documentCard.setOrientation(LinearLayout.HORIZONTAL);
            documentCard.setPadding(16, 12, 16, 12);
            documentCard.setBackgroundResource(R.drawable.rounded_card_background);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 8);
            documentCard.setLayoutParams(cardParams);

            ImageView docIcon = new ImageView(this);
            docIcon.setImageResource(android.R.drawable.ic_menu_upload);
            docIcon.setPadding(0, 0, 12, 0);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            iconParams.gravity = Gravity.CENTER_VERTICAL;
            docIcon.setLayoutParams(iconParams);

            LinearLayout docInfoLayout = new LinearLayout(this);
            docInfoLayout.setOrientation(LinearLayout.VERTICAL);
            docInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));

            TextView docNameText = new TextView(this);
            docNameText.setText(uploadedDocName);
            docNameText.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
            docNameText.setMaxLines(1);
            docNameText.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView docSizeText = new TextView(this);
            docSizeText.setText(uploadedDocText.length() + " characters");
            docSizeText.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            docSizeText.setTextColor(getResources().getColor(android.R.color.darker_gray, null));

            docInfoLayout.addView(docNameText);
            docInfoLayout.addView(docSizeText);

            ImageView deleteButton = new ImageView(this);
            deleteButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            deleteButton.setBackgroundResource(R.drawable.round_red_background);
            deleteButton.setPadding(8, 8, 8, 8);
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            deleteParams.gravity = Gravity.CENTER_VERTICAL;
            deleteButton.setLayoutParams(deleteParams);

            deleteButton.setOnClickListener(v -> {
                uploadedDocName = "";
                uploadedDocText = "";
                updateUploadedDocumentsDisplay();
                Toast.makeText(this, "Document removed", Toast.LENGTH_SHORT).show();
            });

            documentCard.addView(docIcon);
            documentCard.addView(docInfoLayout);
            documentCard.addView(deleteButton);

            uploadedDocumentsLayout.addView(documentCard);
            uploadedDocumentsLayout.setVisibility(View.VISIBLE);
        } else {
            uploadedDocumentsLayout.setVisibility(View.GONE);
        }
    }

    private void loadEventDetails(String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);

                        if (event != null) {
                            populateEventForm(event);
                        } else {
                            Toast.makeText(this, "Error: Could not parse event data.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateEventForm(Event event) {
        eventToEdit = event;
        ((TextView) findViewById(R.id.event_creation_title)).setText("Update Event");
        btnCreate.setText("Update Event");

        etEventName.setText(event.getEventName());
        etDescription.setText(event.getDescription());
        etDate.setText(event.getDate());
        etTime.setText(event.getTime());
        etLocation.setText(event.getLocation());
        etMaxGuests.setText(String.valueOf(event.getMaxGuests()));
        etFee.setText(String.valueOf(event.getFee()));
        if (event.getSocialMediaLink() != null) {
            etSocialMediaLink.setText(event.getSocialMediaLink());
        }

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerCategory.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(event.getCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        posterImageUrl = event.getCoverImageUrl();
        catalogImageUrls = new ArrayList<>(event.getAdditionalImageUrls());
        updatePosterPreview();
        updateCatalogPreview();

        uploadedDocText = event.getUploadedDocumentText();
        uploadedDocName = event.getUploadedDocumentName();
        updateUploadedDocumentsDisplay();

        if (event.getPublishAt() > System.currentTimeMillis()) {
            scheduleSwitch.setChecked(true);
            scheduleLayout.setVisibility(View.VISIBLE);
            publishCalendar.setTimeInMillis(event.getPublishAt());
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etPublishDate.setText(sdfDate.format(publishCalendar.getTime()));
            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            etPublishTime.setText(sdfTime.format(publishCalendar.getTime()));
        } else {
            scheduleSwitch.setChecked(false);
            scheduleLayout.setVisibility(View.GONE);
        }
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

        String eventDateStr = Objects.requireNonNull(etDate.getText()).toString().trim();
        if (Objects.requireNonNull(etDate.getText()).toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return false;
        }

        Calendar eventDate = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            eventDate.setTime(sdf.parse(eventDateStr));

            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.DAY_OF_MONTH, 3);

            if (eventDate.before(minDate)) {
                Toast.makeText(this, "Event start date must be at least 3 days from today", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid event date format", Toast.LENGTH_SHORT).show();
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
        if (scheduleSwitch.isChecked()) {
            String publishDateStr = Objects.requireNonNull(etPublishDate.getText()).toString().trim();
            if (publishDateStr.isEmpty()) {
                Toast.makeText(this, "Please select a publish date", Toast.LENGTH_SHORT).show();
                return false;
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Calendar publishDate = Calendar.getInstance();
                publishDate.setTime(sdf.parse(publishDateStr));

                if (!publishDate.before(eventDate)) {
                    Toast.makeText(this, "Publish date must be earlier than event start date", Toast.LENGTH_LONG).show();
                    return false;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid publish date format", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void resetForm() {
        etEventName.setText("");
        etDescription.setText("");
        etDate.setText("");
        etTime.setText("");
        etLocation.setText("");
        etMaxGuests.setText("");
        etFee.setText("");
        spinnerCategory.setSelection(0);

        uploadedDocName = "";
        uploadedDocText = "";
        updateUploadedDocumentsDisplay();

        newPosterUri = null;
        posterImageUrl = "";
        newCatalogUris.clear();
        catalogImageUrls.clear();
        updatePosterPreview();
        updateCatalogPreview();

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