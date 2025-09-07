package com.meow.utaract;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View; // Import View
import android.widget.Button;
import android.widget.ImageView;
// import android.widget.LinearLayout; // This is no longer needed
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TicketActivity extends AppCompatActivity {

    private View ticketLayout;
    private String eventName;
    private String ticketCode;

    // Get permission to save the QR and details
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveTicket();
                } else {
                    Toast.makeText(this, "Permission denied. Cannot save ticket.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        // Initialize views
        ticketLayout = findViewById(R.id.ticketLayout);
        Button saveTicketButton = findViewById(R.id.saveTicketButton);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // ... (the rest of your onCreate method remains exactly the same)
        eventName = getIntent().getStringExtra("EVENT_NAME");
        ticketCode = getIntent().getStringExtra("TICKET_CODE");
        String attendeeName = getIntent().getStringExtra("ATTENDEE_NAME");
        String attendeeEmail = getIntent().getStringExtra("ATTENDEE_EMAIL");
        String attendeePhone = getIntent().getStringExtra("ATTENDEE_PHONE");

        ((TextView) findViewById(R.id.eventNameText)).setText(eventName);
        ((TextView) findViewById(R.id.verificationCodeText)).setText("Verification Code: " + ticketCode);
        ((TextView) findViewById(R.id.attendeeNameText)).setText(attendeeName);
        ((TextView) findViewById(R.id.attendeeEmailText)).setText(attendeeEmail);
        ((TextView) findViewById(R.id.attendeePhoneText)).setText(attendeePhone);

        ImageView qrCodeImageView = findViewById(R.id.qrCodeImageView);
        try {
            Bitmap qrBitmap = generateQrCode(ticketCode);
            qrCodeImageView.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        saveTicketButton.setOnClickListener(v -> checkPermissionAndSave());
    }

    private void checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveTicket();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                saveTicket();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void saveTicket() {
        Bitmap bitmap = Bitmap.createBitmap(ticketLayout.getWidth(), ticketLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        ticketLayout.draw(canvas);

        String filename = "Ticket-" + eventName.replaceAll("[^a-zA-Z0-9]", "") + "-" + ticketCode + ".jpg";

        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UTARACT Tickets");
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/UTARACT Tickets";
                File dir = new File(imagesDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File imageFile = new File(dir, filename);
                fos = new FileOutputStream(imageFile);
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            Toast.makeText(this, "Ticket saved to Pictures/UTARACT Tickets", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving ticket: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap generateQrCode(String text) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }
}