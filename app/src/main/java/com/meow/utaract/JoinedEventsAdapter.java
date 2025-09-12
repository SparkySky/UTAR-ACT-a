package com.meow.utaract;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import java.util.List;

public class JoinedEventsAdapter extends RecyclerView.Adapter<JoinedEventsAdapter.JoinedEventViewHolder> {

    final List<JoinedEvent> joinedEventList;
    private final Context context;

    public JoinedEventsAdapter(List<JoinedEvent> joinedEventList, Context context) {
        this.joinedEventList = joinedEventList;
        this.context = context;
    }

    @NonNull
    @Override
    public JoinedEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_joined_event, parent, false);
        return new JoinedEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JoinedEventViewHolder holder, int position) {
        JoinedEvent joinedEvent = joinedEventList.get(position);
        holder.bind(joinedEvent);
    }

    @Override
    public int getItemCount() {
        return joinedEventList.size();
    }

    class JoinedEventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImageView;
        TextView eventNameTextView;
        TextView registrationStatusTextView;
        Button viewTicketButton;

        public JoinedEventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImageView = itemView.findViewById(R.id.eventImageView);
            eventNameTextView = itemView.findViewById(R.id.eventNameTextView);
            registrationStatusTextView = itemView.findViewById(R.id.registrationStatusTextView);
            viewTicketButton = itemView.findViewById(R.id.viewTicketButton);
        }

        void bind(JoinedEvent joinedEvent) {
            eventNameTextView.setText(joinedEvent.getEvent().getEventName());
            registrationStatusTextView.setText(joinedEvent.getRegistrationStatus().toUpperCase());

            // Set status color
            switch (joinedEvent.getRegistrationStatus()) {
                case "accepted":
                    registrationStatusTextView.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                    break;
                case "rejected":
                    registrationStatusTextView.setBackgroundColor(Color.parseColor("#F44336")); // Red
                    break;
                default:
                    registrationStatusTextView.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                    break;
            }

            Glide.with(context)
                    .load(joinedEvent.getEvent().getCoverImageUrl())
                    .override(Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.event_banner_placeholder)
                    .into(eventImageView);

            // Show and handle the "View Ticket" button
            if ("accepted".equals(joinedEvent.getRegistrationStatus())) {
                viewTicketButton.setVisibility(View.VISIBLE);
                viewTicketButton.setOnClickListener(v -> {
                    GuestProfile userProfile = new GuestProfileStorage(context).loadProfile();
                    if (userProfile != null) {
                        Intent intent = new Intent(context, TicketActivity.class);
                        intent.putExtra("EVENT_NAME", joinedEvent.getEvent().getEventName());
                        intent.putExtra("TICKET_CODE", joinedEvent.getTicketCode());
                        intent.putExtra("ATTENDEE_NAME", userProfile.getName());
                        intent.putExtra("ATTENDEE_EMAIL", userProfile.getEmail());
                        intent.putExtra("ATTENDEE_PHONE", userProfile.getPhone());
                        context.startActivity(intent);
                    }
                });
            } else {
                viewTicketButton.setVisibility(View.GONE);
            }

            // Set a click listener for the entire list item view
            itemView.setOnClickListener(v -> {
                // Create an intent to open EventDetailActivity
                Intent intent = new Intent(context, EventDetailActivity.class);
                // Use the correct key: "EVENT_ID" instead of "eventId"
                intent.putExtra("EVENT_ID", joinedEvent.getEvent().getEventId());
                context.startActivity(intent);
            });
        }
    }
}