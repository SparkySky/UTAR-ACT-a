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

        sendButton.setOnClickListener(v -> onSend());

        // Set up toolbar navigation
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> showExitDialog());
        
        // Add welcome message
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

    private void onSend() {
        String userMessage = chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(userMessage)) return;
        
        // Add user message to chat history
        chatHistory.add(new AiService.ChatMessage("user", userMessage));
        
        appendBubble(userMessage, true);
        chatInput.setText("");

        // 1. Disable the send button immediately
        sendButton.setEnabled(false);
        sendButton.setText("..."); // Optional: change text to indicate loading

        // 2. Close the keyboard
        closeKeyboard();

        // Show loading indicator
        appendBubble("Thinking...", false);

        String apiKey = getString(R.string.gemini_api_key);
        AiService ai = new AiService(apiKey);

        if ("EVENT".equals(mode) && !TextUtils.isEmpty(eventId)) {
            db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
                Event event = doc.toObject(Event.class);
                String context = buildEventContext(event);
                ai.chatWithHistory(context, chatHistory, userMessage, new UiCallback());
            });
        } else {
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

    private String buildEventContext(@Nullable Event e) {
        if (e == null) return "You are UTARACT assistant. Event not found.";
        String desc = e.getSummary() != null && !e.getSummary().isEmpty() ? e.getSummary() : e.getDescription();
        
        StringBuilder context = new StringBuilder();
        context.append("You are UTARACT event assistant. Answer questions about this specific event. Be helpful and concise.\n\n");
        context.append("EVENT DETAILS:\n");
        context.append("Title: ").append(e.getEventName()).append("\n");
        context.append("Date & Time: ").append(e.getDate()).append(" at ").append(e.getTime()).append("\n");
        context.append("Location: ").append(e.getLocation()).append("\n");
        context.append("Category: ").append(e.getCategory()).append("\n");
        context.append("Fee: RM").append(e.getFee()).append(e.getFee() == 0 ? " (Free)" : "").append("\n");
        context.append("Max Guests: ").append(e.getMaxGuests()).append("\n\n");
        context.append("EVENT SUMMARY:\n").append(desc).append("\n\n");
        
        // Include uploaded document text if available
        if (e.getUploadedDocumentText() != null && !e.getUploadedDocumentText().isEmpty()) {
            String docName = e.getUploadedDocumentName() != null ? e.getUploadedDocumentName() : "Uploaded Document";
            context.append("UPLOADED DOCUMENT (").append(docName).append(") CONTENT:\n");
            context.append(e.getUploadedDocumentText()).append("\n\n");
            context.append("You can reference the uploaded document content to provide more detailed answers about the event.\n\n");
        }
        
        context.append("Answer user questions about this event. If asked about other events, politely redirect to the general chat.");
        return context.toString();
    }

    private String buildGeneralContext(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are UTARACT assistant. Help users find suitable events based on their preferences and needs. " +
                "Consider categories, dates, locations, fees, and event descriptions. Be friendly and helpful.\n\n");
        sb.append("AVAILABLE EVENTS:\n");
        int count = 0;
        for (Event e : events) {
            if (count++ >= 15) break; // bound context
            String desc = e.getSummary() != null && !e.getSummary().isEmpty() ? e.getSummary() : e.getDescription();
            sb.append("• ").append(e.getEventName())
              .append("\n  Date: ").append(e.getDate()).append(" at ").append(e.getTime())
              .append("\n  Location: ").append(e.getLocation())
              .append("\n  Category: ").append(e.getCategory())
              .append("\n  Fee: RM").append(e.getFee()).append(e.getFee() == 0 ? " (Free)" : "")
              .append("\n  Max Guests: ").append(e.getMaxGuests())
              .append("\n  Details: ").append(desc);
            
            // Add note if document is available
            if (e.getUploadedDocumentText() != null && !e.getUploadedDocumentText().isEmpty()) {
                sb.append("\n  📄 Has uploaded document with additional details");
            }
            sb.append("\n\n");
        }
        sb.append("Recommend events based on user preferences. Ask clarifying questions if needed to provide better suggestions.");
        return sb.toString();
    }

    private class UiCallback implements AiService.AiCallback {
        @Override
        public void onSuccess(String text) { 
            runOnUiThread(() -> {
                // Remove the "Thinking..." message and add the actual response
                if (chatContainer.getChildCount() > 0) {
                    View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
                    if (lastChild instanceof TextView) {
                        TextView lastTv = (TextView) lastChild;
                        if ("Thinking...".equals(lastTv.getText().toString())) {
                            chatContainer.removeView(lastChild);
                        }
                    }
                }
                // 3. Re-enable the send button after getting a response
                sendButton.setEnabled(true);
                sendButton.setText("Send"); // Restore original text
                
                // Add AI response to chat history
                chatHistory.add(new AiService.ChatMessage("model", text));
                
                appendBubble(formatAiText(text), false);
            });
        }
        @Override
        public void onError(Exception e) { 
            runOnUiThread(() -> {
                // Remove the "Thinking..." message and add error
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

    private void appendBubble(String text, boolean isUser) {
        appendBubble((CharSequence) text, isUser);
    }

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
        
        // Set proper Material Design bubble background
        if (isUser) {
            tv.setBackgroundResource(R.drawable.chat_bubble_user);
            tv.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tv.setBackgroundResource(R.drawable.chat_bubble_bot);
            // Use theme-aware text color for bot messages: white in dark mode, black in light mode
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // Dark mode - use white text
                tv.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                // Light mode - use black text
                tv.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
        
        chatContainer.addView(tv);
        
        // Auto-scroll to bottom
        chatContainer.post(() -> {
            View lastChild = chatContainer.getChildAt(chatContainer.getChildCount() - 1);
            if (lastChild != null) {
                lastChild.requestFocus();
            }
        });
    }

    /**
     * Formats a raw string with simple markdown into a styled CharSequence.
     * This version uses a more efficient regex approach to handle inline styles.
     *
     * @param raw The raw string from the AI, which may contain markdown.
     * @return A CharSequence with bold and list styles applied.
     */
    private CharSequence formatAiText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = raw.split("\\r?\\n"); // Split the text into lines

        // Regex to find **bold** and *italic* text.
        // It captures the markers (like **) and the content separately.
        Pattern inlinePattern = Pattern.compile("(\\*\\*|\\*)(.+?)\\1");

        for (String line : lines) {
            // Handle bullet points first
            if (line.trim().startsWith("* ")) {
                builder.append("\u2022 "); // Append a bullet character
                String content = line.substring(line.indexOf("*") + 1).trim();
                applyInlineStyles(builder, content, inlinePattern);
            } else {
                applyInlineStyles(builder, line, inlinePattern);
            }
            builder.append("\n"); // Add a newline character after each line
        }

        return builder;
    }

    /**
     * A helper method to apply inline markdown styles (like bold) to a line of text.
     *
     * @param builder The SpannableStringBuilder to append to.
     * @param line The line of text to process.
     * @param pattern The regex pattern for finding inline styles.
     */
    private void applyInlineStyles(SpannableStringBuilder builder, String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append the text before the current match
            builder.append(line.substring(lastEnd, matcher.start()));

            String marker = matcher.group(1); // The markdown characters (* or **)
            String content = matcher.group(2); // The text inside the markers

            int start = builder.length();
            builder.append(content);
            int end = builder.length();

            // Apply a bold style for both * and **
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Apply a larger text size for ** (acting as a heading)
            if ("**".equals(marker)) {
                builder.setSpan(new RelativeSizeSpan(1.2f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            lastEnd = matcher.end();
        }

        // Append any remaining text after the last match
        if (lastEnd < line.length()) {
            builder.append(line.substring(lastEnd));
        }
    }

    private void appendWithInlineMarkdown(SpannableStringBuilder out, String text) {
        // First handle bold **...**
        int idx = 0;
        while (true) {
            int open = text.indexOf("**", idx);
            if (open < 0) break;
            int close = text.indexOf("**", open + 2);
            if (close < 0) break;
            // append before
            if (open > idx) out.append(text, idx, open);
            String inner = text.substring(open + 2, close);
            int spanStart = out.length();
            out.append(inner);
            int spanEnd = out.length();
            out.setSpan(new StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            idx = close + 2;
        }
        if (idx < text.length()) out.append(text.substring(idx));

        // Now handle single-* emphasis for H2-like emphasis in remaining text (avoid bullets)
        String current = out.toString();
        int searchFrom = Math.max(0, out.length() - text.length());
        int pos = searchFrom;
        while (pos < out.length()) {
            int open = current.indexOf('*', pos);
            if (open < 0 || open + 1 >= out.length()) break;
            // skip if it's part of ** (already handled)
            if (open + 1 < current.length() && current.charAt(open + 1) == '*') { pos = open + 2; continue; }
            int close = current.indexOf('*', open + 1);
            if (close < 0) break;
            // ensure not **
            if (close + 1 < current.length() && current.charAt(close + 1) == '*') { pos = close + 2; continue; }
            // Apply span and remove markers by replacing them with nothing
            int spanStart = open;
            int spanEnd = close - 1; // after removal offset adjusts; we'll compute carefully using builder ops

            // Recompute against builder live: remove closing then opening, then apply span
            // Remove closing '*'
            out.delete(close, close + 1);
            // Remove opening '*'
            out.delete(open, open + 1);
            // Apply bold over the inner text
            out.setSpan(new StyleSpan(Typeface.BOLD), open, close - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Update current string and continue after the span
            current = out.toString();
            pos = open + 1;
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Chat")
                .setMessage("Closing the chat will clear your conversation history. Are you sure you want to exit?")
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearChatHistory();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }
    
    private void clearChatHistory() {
        if (chatHistory != null) {
            chatHistory.clear();
        }
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearChatHistory();
    }
}



