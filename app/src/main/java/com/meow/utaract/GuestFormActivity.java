package com.meow.utaract;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import de.hdodenhof.circleimageview.CircleImageView;

public class GuestFormActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, phoneInput;
    private Button saveButton, changeProfilePicButton;
    private CircleImageView profileImageView;
    private ChipGroup preferencesChipGroup;
    private GuestProfileStorage storage;
    private boolean isOrganiser;
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

        initializeViews();
        initializeImagePicker();

        // Load all the preferences categories from res/value/arrays.xml
        preferenceList.addAll(Arrays.asList(getResources().getStringArray(R.array.event_categories)));

        populatePreferences();
        loadExistingProfile();



        if (isOrganiser) {
            profileImageView.setVisibility(View.VISIBLE);
            changeProfilePicButton.setVisibility(View.VISIBLE);
            changeProfilePicButton.setOnClickListener(v -> openImagePicker());
        } else {
            profileImageView.setVisibility(View.GONE);
            changeProfilePicButton.setVisibility(View.GONE);
        }

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.etName);
        emailInput = findViewById(R.id.etEmail);
        phoneInput = findViewById(R.id.etPhone);
        saveButton = findViewById(R.id.btnSubmitGuest);
        profileImageView = findViewById(R.id.profileImageView);
        changeProfilePicButton = findViewById(R.id.changeProfilePicButton);
        preferencesChipGroup = findViewById(R.id.preferencesChipGroup);
    }

    private void populatePreferences() {
        for (String preference : preferenceList) {
            Chip chip = new Chip(this);
            chip.setText(preference);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.preference_chip_background);
            chip.setTextColor(getResources().getColorStateList(R.color.preference_chip_text_color, getTheme()));
            chip.setChipStrokeWidth(0); // Hide the default stroke
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

        List<String> selectedPreferences = getSelectedPreferences();
        if (selectedPreferences.isEmpty()) {
            Toast.makeText(this, "Please select at least one preference", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("IS_ORGANISER", isOrganiser);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    // --- Image Picker and Upload Logic (no changes needed from previous version) ---
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