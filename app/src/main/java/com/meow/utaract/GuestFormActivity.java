package com.meow.utaract;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import de.hdodenhof.circleimageview.CircleImageView;

public class GuestFormActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, phoneInput;
    private Button saveButton, changeProfilePicButton, confirmRegisterButton;
    private CircleImageView profileImageView;
    private ChipGroup preferencesChipGroup;
    private TextView titleGuestForm, tvPreferences;
    private GuestProfileStorage storage;
    private boolean isOrganiser, isRegistrationMode;
    private String eventId;
    private Uri selectedImageUri;
    private String downloadedImageUrl;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private final List<String> preferenceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_form);

        storage = new GuestProfileStorage(this);
        isOrganiser = getIntent().getBooleanExtra("IS_ORGANISER", false);
        isRegistrationMode = getIntent().getBooleanExtra("IS_REGISTRATION_MODE", false);
        eventId = getIntent().getStringExtra("EVENT_ID");

        initializeViews();
        initializeImagePicker();

        preferenceList.addAll(Arrays.asList(getResources().getStringArray(R.array.event_categories)));

        populatePreferences();
        loadExistingProfile();

        if (isRegistrationMode) {
            setupRegistrationMode();
        } else {
            setupProfileMode();
        }
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.etName);
        emailInput = findViewById(R.id.etEmail);
        phoneInput = findViewById(R.id.etPhone);
        saveButton = findViewById(R.id.btnSubmitGuest);
        confirmRegisterButton = findViewById(R.id.btnConfirmRegister);
        profileImageView = findViewById(R.id.profileImageView);
        changeProfilePicButton = findViewById(R.id.changeProfilePicButton);
        preferencesChipGroup = findViewById(R.id.preferencesChipGroup);
        titleGuestForm = findViewById(R.id.titleGuestForm);
        tvPreferences = findViewById(R.id.tvPreferences);
    }

    private void setupRegistrationMode() {
        titleGuestForm.setText("Confirm Your Details");
        saveButton.setVisibility(View.GONE);
        confirmRegisterButton.setVisibility(View.VISIBLE);
        changeProfilePicButton.setVisibility(View.GONE);
        preferencesChipGroup.setVisibility(View.GONE);
        tvPreferences.setVisibility(View.GONE);

        nameInput.setEnabled(false);
        emailInput.setEnabled(false);
        phoneInput.setEnabled(false);

        confirmRegisterButton.setOnClickListener(v -> showConfirmationDialog());
    }

    private void setupProfileMode() {
        saveButton.setOnClickListener(v -> saveProfile());
        if (isOrganiser) {
            profileImageView.setVisibility(View.VISIBLE);
            changeProfilePicButton.setVisibility(View.VISIBLE);
            changeProfilePicButton.setOnClickListener(v -> openImagePicker());
        } else {
            profileImageView.setVisibility(View.GONE);
            changeProfilePicButton.setVisibility(View.GONE);
        }
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Registration")
                .setMessage("Are you sure you want to register for this event?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    saveProfile(); // Save the profile first
                    submitRegistration();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void submitRegistration() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to register.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Load the user profile to get their name, email, and phone
        GuestProfile userProfile = storage.loadProfile();
        if (userProfile == null) {
            Toast.makeText(this, "Error: Could not find user profile.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userName = userProfile.getName();
        String userEmail = userProfile.getEmail(); // Get email
        String userPhone = userProfile.getPhone(); // Get phone

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("userId", userId);
        registrationData.put("userName", userName);
        registrationData.put("email", userEmail); //  <- ADD THIS LINE
        registrationData.put("phone", userPhone); //  <- ADD THIS LINE
        registrationData.put("timestamp", System.currentTimeMillis());
        registrationData.put("status", "pending");

        db.collection("events").document(eventId).collection("registrations").document(userId)
                .set(registrationData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    sendRegistrationNotification(eventId, userId);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendRegistrationNotification(String eventId, String userId) {
        System.out.println("Placeholder: Sending notification to organizer for event " + eventId + " from user " + userId);
    }

    private void populatePreferences() {
        for (String preference : preferenceList) {
            Chip chip = new Chip(this);
            chip.setText(preference);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.preference_chip_background);
            chip.setTextColor(getResources().getColorStateList(R.color.preference_chip_text_color, getTheme()));
            chip.setChipStrokeWidth(0);
            preferencesChipGroup.addView(chip);
        }
    }

    private void loadExistingProfile() {
        GuestProfile existingProfile = storage.loadProfile();
        if (existingProfile != null) {
            nameInput.setText(existingProfile.getName());
            emailInput.setText(existingProfile.getEmail());
            phoneInput.setText(existingProfile.getPhone());

            downloadedImageUrl = existingProfile.getProfileImageUrl();
            if (downloadedImageUrl != null && !downloadedImageUrl.isEmpty()) {
                Glide.with(this).load(downloadedImageUrl).into(profileImageView);
            }

            List<String> savedPrefs = existingProfile.getPreferences();
            if (savedPrefs != null) {
                for (int i = 0; i < preferencesChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) preferencesChipGroup.getChildAt(i);
                    if (savedPrefs.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        }
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^0\\d{2}-\\d{7,8}$")) {
            Toast.makeText(this, "Invalid phone number format (e.g., 012-1234567)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedPreferences = getSelectedPreferences();
        if (!isRegistrationMode && selectedPreferences.size() < 3) {
            Toast.makeText(this, "Please select at least 3 preferences", Toast.LENGTH_SHORT).show();
            return;
        }

        GuestProfile profile = new GuestProfile(name, email, phone, selectedPreferences);
        if (downloadedImageUrl != null && !downloadedImageUrl.isEmpty()) {
            profile.setProfileImageUrl(downloadedImageUrl);
        }

        storage.saveProfile(profile);

        if (isOrganiser) {
            storage.uploadProfileToFirestore(profile);
            Toast.makeText(this, "Online profile updated", Toast.LENGTH_SHORT).show();
        }

        if (!isRegistrationMode) {
            Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("IS_ORGANISER", isOrganiser);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private List<String> getSelectedPreferences() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < preferencesChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) preferencesChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }
        return selected;
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        uploadProfileImageToStorage(selectedImageUri);
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImageToStorage(Uri imageUri) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (imageUri == null || !isOrganiser || currentUser == null) return;
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        String userId = currentUser.getUid();
        String fileName = "profile_images/" + userId + "/" + UUID.randomUUID().toString();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            downloadedImageUrl = uri.toString();
                            Glide.with(this).load(downloadedImageUrl).into(profileImageView);
                            Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}