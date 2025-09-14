package com.meow.utaract.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.meow.utaract.activities.services.AiService;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ChatActivity - Handles chat interface between user and UTARACT AI assistant.
 * Supports two modes:
 *  - GENERAL: Suggests events based on user preferences.
 *  - EVENT:   Answers questions about a specific event.
 */
public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText chatInput;
    private Button sendButton;

    private String mode; // GENERAL or EVENT
    private String eventId;
    private FirebaseFirestore db;
    // Chat history for maintaining conversation context
    private List<AiService.ChatMessage> chatHistory;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatContainer = findViewById(R.id.chat_container);
        chatInput = findViewById(R.id.chat_input);
        sendButton = findViewById(R.id.send_button);

        mode = getIntent().getStringExtra("MODE");
        eventId = getIntent().getStringExtra("EVENT_ID");

        db = FirebaseFirestore.getInstance();

        // Initialize chat history
        chatHistory = new ArrayList<>();

        // Send button click → trigger AI response
        sendButton.setOnClickListener(v -> onSend());

        // Set up toolbar navigation with exit confirmation
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> showExitDialog());

        // Add welcome message based on mode
        String welcomeMessage;
        if ("EVENT".equals(mode)) {
            welcomeMessage = "Hi! I can help you with questions about this specific event. What would you like to know?";
        } else {
            welcomeMessage = "Hi! I'm your UTARACT assistant. I can help you find events that match your interests and preferences. What kind of activities are you looking for?";
        }
        appendBubble(welcomeMessage, false);

        // Add welcome message to chat history
        chatHistory.add(new AiService.ChatMessage("model", welcomeMessage));
    }

    /**
     * Handles sending user message:
     *  1. Show user bubble.
     *  2. Disable button and show "Thinking...".
     *  3. Query Firebase for event(s).
     *  4. Send context + user message to AI service.
     */
    private void onSend() {
        String userMessage = chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(userMessage)) return;

        // Add user message to chat history
        chatHistory.add(new AiService.ChatMessage("user", userMessage));

        appendBubble(userMessage, true);
        chatInput.setText("");

        // Disable send button while AI is processing
        sendButton.setEnabled(false);
        sendButton.setText("...");

        // Hide keyboard
        closeKeyboard();

        // Temporary loading bubble
        appendBubble("Thinking...", false);

        String apiKey = getString(R.string.gemini_api_key);
        AiService ai = new AiService(apiKey);

        // EVENT mode → build context from one event
        if ("EVENT".equals(mode) && !TextUtils.isEmpty(eventId)) {
            db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
                Event event = doc.toObject(Event.class);
                String context = buildEventContext(event);
                ai.chatWithHistory(context, chatHistory, userMessage, new UiCallback());
            });
        }
        // GENERAL mode → build context from all events
        else {
            db.collection("events").get().addOnSuccessListener(qs -> {
                List<Event> events = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot d : qs.getDocuments()) {
                    Event e = d.toObject(Event.class);
                    if (e != null) events.add(e);
                }
                String context = buildGeneralContext(events);
                ai.chatWithHistory(context, chatHistory, userMessage, new UiCallback());
            });
        }
    }

    /**
     * Builds AI context string for a specific event.
     *
     * @param e Event object from Firestore
     * @return Context string with details and uploaded document text
     */
    private String buildEventContext(@Nullable Event e) {
        if (e == null) return "You are UTARACT assistant. Event not found.";
        String desc = e.getSummary() != null && !e.getSummary().isEmpty() ? e.getSummary() : e.getDescription();

        StringBuilder context = new StringBuilder();
        context.append("You are UTARACT event assistant. Answer questions about this specific event.\n\n")
                .append("EVENT DETAILS:\n")
                .append("Title: ").append(e.getEventName()).append("\n")
                .append("Date & Time: ").append(e.getDate()).append(" at ").append(e.getTime()).append("\n")
                .append("Location: ").append(e.getLocation()).append("\n")
                .append("Category: ").append(e.getCategory()).append("\n")
                .append("Fee: RM").append(e.getFee()).append(e.getFee() == 0 ? " (Free)" : "").append("\n")
                .append("Max Guests: ").append(e.getMaxGuests()).append("\n\n")
                .append("EVENT SUMMARY:\n").append(desc).append("\n\n");

        // Add uploaded document text if available
        if (e.getUploadedDocumentText() != null && !e.getUploadedDocumentText().isEmpty()) {
            String docName = e.getUploadedDocumentName() != null ? e.getUploadedDocumentName() : "Uploaded Document";
            context.append("UPLOADED DOCUMENT (").append(docName).append(") CONTENT:\n")
                    .append(e.getUploadedDocumentText()).append("\n\n");
        }

        context.append("Answer user questions about this event. Redirect if unrelated.");
        return context.toString();
    }

    /**
     * Builds AI context string with a list of available events.
     *
     * @param events List of events from Firestore
     * @return Context string with summary of up to 15 events
     */
    private String buildGeneralContext(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are UTARACT assistant. Help users find suitable events.\n\nAVAILABLE EVENTS:\n");
        int count = 0;
        for (Event e : events) {
            if (count++ >= 15) break; // limit context
            String desc = e.getSummary() != null && !e.getSummary().isEmpty() ? e.getSummary() : e.getDescription();
            sb.append("• ").append(e.getEventName())
                    .append("\n  Date: ").append(e.getDate()).append(" at ").append(e.getTime())
                    .append("\n  Location: ").append(e.getLocation())
                    .append("\n  Category: ").append(e.getCategory())
                    .append("\n  Fee: RM").append(e.getFee()).append(e.getFee() == 0 ? " (Free)" : "")
                    .append("\n  Max Guests: ").append(e.getMaxGuests())
                    .append("\n  Details: ").append(desc);
            if (e.getUploadedDocumentText() != null && !e.getUploadedDocumentText().isEmpty()) {
                sb.append("\n  📄 Has uploaded document");
            }
            sb.append("\n\n");
        }
        sb.append("Recommend events based on user preferences.");
        return sb.toString();
    }

    /**
     * Custom AI callback implementation to update UI.
     * Handles success (replace "Thinking..." with AI response) and error states.
     */
    private class UiCallback implements AiService.AiCallback {
        @Override
        public void onSuccess(String text) {
            runOnUiThread(() -> {
                // Remove "Thinking..." placeholder
                if (chatContainer.getChildCount() > 0) {
                    View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
                    if (lastChild instanceof TextView) {
                        TextView lastTv = (TextView) lastChild;
                        if ("Thinking...".equals(lastTv.getText().toString())) {
                            chatContainer.removeView(lastChild);
                        }
                    }
                }
                sendButton.setEnabled(true);
                sendButton.setText("Send");
                // Add AI response to chat history
                chatHistory.add(new AiService.ChatMessage("model", text));
                appendBubble(formatAiText(text), false);
            });
        }
        @Override
        public void onError(Exception e) {
            runOnUiThread(() -> {
                // Remove "Thinking..." placeholder
                if (chatContainer.getChildCount() > 0) {
                    View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
                    if (lastChild instanceof TextView) {
                        TextView lastTv = (TextView) lastChild;
                        if ("Thinking...".equals(lastTv.getText().toString())) {
                            chatContainer.removeView(lastChild);
                        }
                    }
                }
                sendButton.setEnabled(true);
                sendButton.setText("Send");
                appendBubble("Sorry, I encountered an error. Please try again.", false);
            });
        }
    }

    /**
     * Appends a chat bubble (user or bot).
     *
     * @param text   Message text
     * @param isUser True if bubble is from user, false for AI
     */
    private void appendBubble(CharSequence text, boolean isUser) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        params.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        params.leftMargin = isUser ? (int) (48 * getResources().getDisplayMetrics().density) : (int) (12 * getResources().getDisplayMetrics().density);
        params.rightMargin = isUser ? (int) (12 * getResources().getDisplayMetrics().density) : (int) (48 * getResources().getDisplayMetrics().density);
        params.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
        tv.setLayoutParams(params);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setPadding(16, 12, 16, 12);
        tv.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.7));

        // User vs Bot bubble style
        if (isUser) {
            tv.setBackgroundResource(R.drawable.chat_bubble_user);
            tv.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.chat_bubble_bot);
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                tv.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                tv.setTextColor(getResources().getColor(android.R.color.black));
            }
        }

        chatContainer.addView(tv);
    }

    /**
     * Formats raw AI text with markdown-like syntax:
     *  - *italic* or **bold** → styled text
     *  - * bullet points → proper list
     */
    private CharSequence formatAiText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = raw.split("\\r?\\n");
        Pattern inlinePattern = Pattern.compile("(\\*\\*|\\*)(.+?)\\1");

        for (String line : lines) {
            if (line.trim().startsWith("* ")) {
                builder.append("\u2022 "); // bullet point
                String content = line.substring(line.indexOf("*") + 1).trim();
                applyInlineStyles(builder, content, inlinePattern);
            } else {
                applyInlineStyles(builder, line, inlinePattern);
            }
            builder.append("\n");
        }
        return builder;
    }

    /**
     * Apply inline markdown styles (bold/heading).
     */
    private void applyInlineStyles(SpannableStringBuilder builder, String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        int lastEnd = 0;
        while (matcher.find()) {
            builder.append(line.substring(lastEnd, matcher.start()));
            String marker = matcher.group(1);
            String content = matcher.group(2);
            int start = builder.length();
            builder.append(content);
            int end = builder.length();
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if ("**".equals(marker)) {
                builder.setSpan(new RelativeSizeSpan(1.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < line.length()) {
            builder.append(line.substring(lastEnd));
        }
    }

    /**
     * Shows confirmation dialog before exiting chat.
     */
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Chat")
                .setMessage("Closing the chat will clear your conversation history. Are you sure you want to exit?")
                .setPositiveButton("Exit", (dialog, which) -> {
                    clearChatHistory();
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /**
     * Utility: Closes soft keyboard if open.
     */
    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void clearChatHistory() {
        if (chatHistory != null) {
            chatHistory.clear();
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        clearChatHistory();
    }
}
