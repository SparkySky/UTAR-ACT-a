package com.meow.utaract;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import java.util.ArrayList;
import java.util.List;

public class GuestFormActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, phoneInput;
    private CheckBox cbSports, cbMusic, cbVolunteer, cbWorkshops, cbExhibition, cbSeminar, cbNetworking, cbCulturalFest, cbTalk;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_form);

        String organiserId = getIntent().getStringExtra("organiserId");
        String eventId = getIntent().getStringExtra("eventId");
        String guestId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        nameInput = findViewById(R.id.etName);
        emailInput = findViewById(R.id.etEmail);
        phoneInput = findViewById(R.id.etPhone);
        saveButton = findViewById(R.id.btnSubmitGuest);

        cbMusic = findViewById(R.id.cbMusic);
        cbSports = findViewById(R.id.cbSports);
        cbVolunteer = findViewById(R.id.cbVolunteer);
        cbWorkshops = findViewById(R.id.cbWorkshops);
        cbExhibition = findViewById(R.id.cbExhibition);
        cbSeminar = findViewById(R.id.cbSeminar);
        cbNetworking = findViewById(R.id.cbNetworking);
        cbCulturalFest = findViewById(R.id.cbCulturalFest);
        cbTalk = findViewById(R.id.cbTalk);

        // Load existing profile if available
        GuestProfileStorage storage = new GuestProfileStorage(this);
        GuestProfile existingProfile = storage.loadProfile();
        if (existingProfile != null) {
            nameInput.setText(existingProfile.getName());
            emailInput.setText(existingProfile.getEmail());
            phoneInput.setText(existingProfile.getPhone());

            List<String> prefs = existingProfile.getPreferences();
            if (prefs != null) {
                cbMusic.setChecked(prefs.contains("Music"));
                cbSports.setChecked(prefs.contains("Sports"));
                cbVolunteer.setChecked(prefs.contains("Volunteer"));
                cbWorkshops.setChecked(prefs.contains("Workshops"));
                cbExhibition.setChecked(prefs.contains("Exhibition"));
                cbSeminar.setChecked(prefs.contains("Seminar"));
                cbNetworking.setChecked(prefs.contains("Networking"));
                cbCulturalFest.setChecked(prefs.contains("Cultural Festival"));
                cbTalk.setChecked(prefs.contains("Talk"));
            }
        }

        saveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Enter a valid email (e.g. example@gmail.com)");
                emailInput.requestFocus();
                return;
            }

            if (!phone.matches("^01\\d-\\d{7,8}$")) {
                phoneInput.setError("Enter a valid phone number (e.g. 012-1234567 or 012-12345678)");
                phoneInput.requestFocus();
                return;
            }

            List<String> preferences = new ArrayList<>();
            if (cbSports.isChecked()) preferences.add("Sports");
            if (cbMusic.isChecked()) preferences.add("Music");
            if (cbVolunteer.isChecked()) preferences.add("Volunteer");
            if (cbWorkshops.isChecked()) preferences.add("Workshops");
            if (cbExhibition.isChecked()) preferences.add("Exhibition");
            if (cbSeminar.isChecked()) preferences.add("Seminar");
            if (cbNetworking.isChecked()) preferences.add("Networking");
            if (cbCulturalFest.isChecked()) preferences.add("Cultural Festival");
            if (cbTalk.isChecked()) preferences.add("Talk");

            if (preferences.size() < 3) {
                Toast.makeText(this, "Please select at least 3 preferences", Toast.LENGTH_SHORT).show();
                return;
            }

            GuestProfile profile = new GuestProfile(name, email, phone, preferences);
            storage.saveProfile(profile);

            // Get "IS_EDIT" from MainActivity intent
            Intent intent = getIntent();

            // Update or create guest profile - Local
            if (getIntent().getBooleanExtra("IS_EDIT", false)) {
                Toast.makeText(this, "Update Successful!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Guest Profile Stored Successfully!", Toast.LENGTH_SHORT).show();
            }

            // Update or create organiser profile - Firestore
            if (getIntent().getBooleanExtra("IS_ORGANISER", false)) {
                storage.uploadProfileToFirestore(profile);
                Toast.makeText(this, "Updated online profile", Toast.LENGTH_SHORT).show();
            }

            if (storage.profileExists()) {
                intent = new Intent(GuestFormActivity.this, MainActivity.class);
                intent.putExtra("IS_GUEST_USER", true);
                startActivity(intent);
            }

            storage.createNotification(
                    organiserId,
                    guestId,
                    eventId,
                    "New applicant " + name + " has applied for your event!",
                    "organiser"
            );

            finish();
        });
    }
}
