package com.meow.utaract.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.meow.utaract.activities.fragments.FullScreenImageDialogFragment;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.LinearLayout;

import java.io.OutputStream;
import java.util.Objects;

public class EventDetailActivity extends AppCompatActivity {

    private Event event;
    private GuestProfile organizerProfile;
    private GuestProfile userProfile;
    private GuestProfileStorage profileStorage;
    private Button followButton, registerButton;
    private FloatingActionButton qrCodeButton, askButton;
    private ProgressBar buttonProgressBar, pageProgressBar;
    private ScrollView contentScrollView;
    private RelativeLayout registrationLayout;
    private String registrationStatus = "not_registered";
    private String eventId;
    private Bitmap qrCodeToSave;
    private LinearLayout catalogueImagesLayout;
    private LinearLayout catalogueSection;

    // Permission Launcher: handles storage permission requests
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted → save QR code
                    if (qrCodeToSave != null) {
                        saveQrCodeToGallery(qrCodeToSave);
                    }
                } else {
                    // Permission denied → show error message
                    Toast.makeText(this, "Permission denied. Cannot save QR Code.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Get event ID passed via Intent
        eventId = getIntent().getStringExtra("EVENT_ID");

        // Validate event ID
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Load user profile from local storage
        profileStorage = new GuestProfileStorage(this);
        userProfile = profileStorage.loadProfile();

        // Fetch event details from Firestore
        fetchEventDetails();
    }

    // Initialize all views from layout
    private void initializeViews() {
        followButton = findViewById(R.id.follow_button);
        registerButton = findViewById(R.id.registerButton);
        qrCodeButton = findViewById(R.id.qrCodeButton);
        askButton = findViewById(R.id.askButton);
        buttonProgressBar = findViewById(R.id.buttonProgressBar);
        pageProgressBar = findViewById(R.id.pageProgressBar);
        contentScrollView = findViewById(R.id.contentScrollView);
        registrationLayout = findViewById(R.id.registration_layout);
        catalogueImagesLayout = findViewById(R.id.catalogueImagesLayout);
        catalogueSection = findViewById(R.id.catalogueSection);
    }

    // Fetch event details from Firestore
    private void fetchEventDetails() {
        pageProgressBar.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.INVISIBLE);
        registrationLayout.setVisibility(View.INVISIBLE);

        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            // Populate UI and fetch more info
                            populateViews();
                            fetchOrganizerProfile();
                            setupListeners();
                            checkRegistrationStatus();
                        } else {
                            showErrorAndFinish("Failed to parse event data.");
                        }
                    } else {
                        showErrorAndFinish("Event not found.");
                    }
                })
                .addOnFailureListener(e -> showErrorAndFinish("Failed to load event: " + e.getMessage()));
    }

    // Fetch organizer profile from Firestore
    private void fetchOrganizerProfile() {
        FirebaseFirestore.getInstance().collection("guest_profiles").document(event.getOrganizerId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Organizer profile stored as JSON
                        String json = documentSnapshot.getString("profile_json");
                        if (json != null) {
                            organizerProfile = new Gson().fromJson(json, GuestProfile.class);
                        }
                    }
                    displayOrganizerInfo();
                })
                .addOnFailureListener(e -> {
                    // If failure → still show UI
                    displayOrganizerInfo();
                });
    }

    // Toggle follow/unfollow organizer
    private void toggleFollowStatus() {
        if (userProfile == null) return;

        String organizerId = event.getOrganizerId();
        String organizerName = (organizerProfile != null) ? organizerProfile.getName() : "the organizer";

        // Prevent user from following themselves
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(organizerId)) {
            Toast.makeText(this, "You cannot follow yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        GuestProfileStorage storage = new GuestProfileStorage(this);

        if (userProfile.getFollowing() != null && userProfile.getFollowing().contains(organizerId)) {
            // Already following → unfollow
            userProfile.removeFollowing(organizerId);
            Toast.makeText(this, "Unfollowed " + organizerName, Toast.LENGTH_SHORT).show();
        } else {
            // Not following → follow
            userProfile.addFollowing(organizerId);
            Toast.makeText(this, "Followed " + organizerName, Toast.LENGTH_SHORT).show();
        }

        // Save locally
        storage.saveProfile(userProfile);

        // If user logged in with Firebase, update Firestore too
        if (currentUser != null && !currentUser.isAnonymous()) {
            storage.uploadProfileToFirestore(userProfile);
        }

        updateFollowButtonState();
    }

    // Populate event details in UI
    private void populateViews() {
        ImageView eventPosterImage = findViewById(R.id.event_poster_image);
        TextView eventTitleText = findViewById(R.id.event_title_text);
        TextView eventDateTimeText = findViewById(R.id.event_date_time_text);
        TextView eventLocationText = findViewById(R.id.event_location_text);
        TextView eventDescriptionText = findViewById(R.id.event_description_text);

        // Load poster image
        Glide.with(this)
                .load(event.getCoverImageUrl())
                .override(Target.SIZE_ORIGINAL)
                .placeholder(R.drawable.event_banner_placeholder)
                .into(eventPosterImage);

        // Set event details
        eventTitleText.setText(event.getEventName());
        eventDateTimeText.setText(String.format("%s\n%s", event.getDate(), event.getTime()));
        eventLocationText.setText(event.getLocation());
        eventDescriptionText.setText(event.getDescription());

        // Show catalogue images
        populateCatalogueImages();
    }

    // Display organizer info in UI
    private void displayOrganizerInfo() {
        CircleImageView organizerAvatarImage = findViewById(R.id.organizer_avatar_image);
        TextView organizerNameText = findViewById(R.id.organizer_name_text);

        if (organizerProfile != null) {
            organizerNameText.setText(organizerProfile.getName());

            // Load avatar
            Glide.with(this)
                    .load(organizerProfile.getProfileImageUrl())
                    .override(Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.ic_person)
                    .into(organizerAvatarImage);
        } else {
            // Default info if missing
            organizerNameText.setText("Unknown Organizer");
            organizerAvatarImage.setImageResource(R.drawable.ic_person);
        }

        updateFollowButtonState();

        // Show content after data loaded
        pageProgressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        registrationLayout.setVisibility(View.VISIBLE);
    }

    // Attach listeners to buttons
    private void setupListeners() {
        followButton.setOnClickListener(v -> toggleFollowStatus());
        qrCodeButton.setOnClickListener(v -> generateAndShowQrCode());
        registerButton.setOnClickListener(v -> handleRegistrationClick());
        askButton.setOnClickListener(v -> {
            // Open chat activity
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("MODE", "EVENT");
            intent.putExtra("EVENT_ID", event.getEventId());
            startActivity(intent);
        });
    }

    // Populate additional event images
    private void populateCatalogueImages() {
        catalogueImagesLayout.removeAllViews();

        if (event.getAdditionalImageUrls() != null && !event.getAdditionalImageUrls().isEmpty()) {
            catalogueSection.setVisibility(View.VISIBLE);

            for (String imageUrl : event.getAdditionalImageUrls()) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
                params.setMarginEnd(16);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Load image
                Glide.with(this)
                        .load(imageUrl)
                        .override(Target.SIZE_ORIGINAL)
                        .into(imageView);

                // Open fullscreen on click
                imageView.setOnClickListener(v -> {
                    FullScreenImageDialogFragment dialog = FullScreenImageDialogFragment.newInstance(imageUrl);
                    dialog.show(getSupportFragmentManager(), "FullScreenImageDialog");
                });

                catalogueImagesLayout.addView(imageView);
            }
        } else {
            catalogueSection.setVisibility(View.GONE);
        }
    }

    // Check if current user is registered for event
    private void checkRegistrationStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            updateRegistrationButtonUI();
            return;
        }

        buttonProgressBar.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.INVISIBLE);

        FirebaseFirestore.getInstance()
                .collection("events").document(event.getEventId())
                .collection("registrations").document(currentUser.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        registrationStatus = "not_registered";
                    } else if (snapshot != null && snapshot.exists()) {
                        registrationStatus = snapshot.getString("status");
                    } else {
                        registrationStatus = "not_registered";
                    }
                    updateRegistrationButtonUI();
                });
    }

    // Update UI of registration button
    private void updateRegistrationButtonUI() {
        buttonProgressBar.setVisibility(View.GONE);
        registerButton.setVisibility(View.VISIBLE);

        switch (registrationStatus) {
            case "pending":
                registerButton.setText("Pending\n(Click to Cancel)");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_pending_background));
                registerButton.setEnabled(true);
                break;
            case "accepted":
                registerButton.setText("Accepted");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_confirmed_background));
                registerButton.setEnabled(false); // Cannot cancel
                break;
            case "rejected":
                registerButton.setText("Rejected");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rejected_background));
                registerButton.setEnabled(false); // Cannot re-register
                break;
            default: // not_registered
                registerButton.setText("Register Now");
                registerButton.setBackgroundColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
                registerButton.setEnabled(true);
                break;
        }
    }

    // Handle click on registration button
    private void handleRegistrationClick() {
        if ("pending".equals(registrationStatus)) {
            // Ask user to confirm cancellation
            showCancelConfirmationDialog();
        } else if ("not_registered".equals(registrationStatus)) {
            // Go to registration form
            Intent intent = new Intent(this, GuestFormActivity.class);
            intent.putExtra("IS_REGISTRATION_MODE", true);
            intent.putExtra("EVENT_ID", event.getEventId());
            startActivity(intent);
        }
        // If accepted or rejected → no action
    }

    // Show confirmation dialog before canceling registration
    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Registration")
                .setMessage("Are you sure you want to cancel your registration for this event?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelRegistration())
                .setNegativeButton("No", null)
                .show();
    }

    // Cancel event registration in Firestore
    private void cancelRegistration() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("events").document(event.getEventId())
                .collection("registrations").document(currentUser.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Registration cancelled.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to cancel registration.", Toast.LENGTH_SHORT).show());
    }

    // Generate QR code and display in dialog
    private void generateAndShowQrCode() {
        String deepLink = "https://utaract.page.link/event?id=" + event.getEventId();
        QRCodeWriter writer = new QRCodeWriter();
        try {
            // Encode link as QR code
            BitMatrix bitMatrix = writer.encode(deepLink, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Show QR in dialog
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bmp);

            new AlertDialog.Builder(this)
                    .setTitle("Event QR Code")
                    .setView(imageView)
                    .setPositiveButton("Save", (dialog, which) -> initiateSaveProcess(bmp))
                    .setNegativeButton("Close", null)
                    .show();

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }

    // Start process to save QR code (check permission)
    private void initiateSaveProcess(Bitmap bitmap) {
        this.qrCodeToSave = bitmap; // Save temporarily

        // Check storage permission for older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            // Already allowed or new Android version
            saveQrCodeToGallery(bitmap);
        }
    }

    // Save QR code bitmap to gallery
    private void saveQrCodeToGallery(Bitmap bitmap) {
        String fileName = "UTAR_ACT_Event_" + System.currentTimeMillis() + ".jpg";
        OutputStream fos;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            // Save into UTAR-ACT folder (modern Android)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UTAR-ACT");
            }

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            fos = getContentResolver().openOutputStream(Objects.requireNonNull(imageUri));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Objects.requireNonNull(fos).close();

            Toast.makeText(this, "QR Code saved to Gallery", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save QR Code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Update Follow button text and visibility
    private void updateFollowButtonState() {
        if (userProfile != null && userProfile.getFollowing() != null &&
                userProfile.getFollowing().contains(event.getOrganizerId())) {
            followButton.setText("Following");
        } else {
            followButton.setText("Follow");
        }

        // Hide follow button if current user is organizer
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(event.getOrganizerId())) {
            followButton.setVisibility(View.GONE);
        } else {
            followButton.setVisibility(View.VISIBLE);
        }
    }

    // Show error and finish activity
    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
