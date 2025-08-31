package com.meow.utaract.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.EventCreationActivity;
import com.meow.utaract.EventDetailActivity;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {
    private List<HomeViewModel.EventItem> eventItemList;
    // Jus placeholder image colours
    private final int[] placeholderColors;
    private final Random random = new Random();
    public EventsAdapter(List<HomeViewModel.EventItem> eventItemList, View context) {
        this.eventItemList = eventItemList;
        // Define a palette of colors for the image placeholders
        placeholderColors = new int[]{
                Color.parseColor("#E0BBE4"),
                Color.parseColor("#957DAD"),
                Color.parseColor("#D291BC"),
                Color.parseColor("#FEC8D8"),
                Color.parseColor("#B2EBF2"), // Light Cyan
                Color.parseColor("#FFCCBC"), // Light Coral
                Color.parseColor("#D1C4E9"), // Light Lavender
                Color.parseColor("#C8E6C9"), // Light Green
                Color.parseColor("#FFF9C4"), // Light Yellow
                Color.parseColor("#F8BBD0"), // Light Pink
                Color.parseColor("#80DEEA"), // Vibrant Cyan
                Color.parseColor("#FFAB91"), // Vibrant Coral
                Color.parseColor("#B39DDB"), // Vibrant Lavender
                Color.parseColor("#A5D6A7"), // Vibrant Green
                Color.parseColor("#FFF59D"), // Vibrant Yellow
                Color.parseColor("#F48FB1"), // Vibrant Pink
                Color.parseColor("#A7FFEB"), // Light Teal
                Color.parseColor("#CFD8DC"), // Blue Grey
                Color.parseColor("#FFD180"), // Light Orange
                Color.parseColor("#B2FF59"), // Light Lime
                Color.parseColor("#81D4FA"), // Light Blue
                Color.parseColor("#FF8A80")  // Light Red
        };
    }

    public void updateEvents(List<HomeViewModel.EventItem> newEventItems) {
        this.eventItemList.clear();
        this.eventItemList.addAll(newEventItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        HomeViewModel.EventItem eventItem = eventItemList.get(position);
        holder.bind(eventItem, placeholderColors[random.nextInt(placeholderColors.length)]);
    }

    @Override
    public int getItemCount() {
        return eventItemList.size();
    }

    // Class for the view holder
    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventBanner;
        TextView dateBadge, categoryTag, eventTitle, eventAudience, eventDate, eventLocation, organizerName;
        View bannerContainer;
        CircleImageView organizerAvatar;
        Button editEventButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventBanner = itemView.findViewById(R.id.event_banner);
            dateBadge = itemView.findViewById(R.id.date_badge);
            categoryTag = itemView.findViewById(R.id.category_tag);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventAudience = itemView.findViewById(R.id.event_audience);
            eventDate = itemView.findViewById(R.id.event_date);
            eventLocation = itemView.findViewById(R.id.event_location);
            bannerContainer = itemView.findViewById(R.id.banner_container);
            organizerAvatar = itemView.findViewById(R.id.organizer_avatar);
            organizerName = itemView.findViewById(R.id.organizer_name);
            editEventButton = itemView.findViewById(R.id.editEventButton);
        }

        void bind(final HomeViewModel.EventItem eventItem, int placeholderColor) {
            final Event event = eventItem.event;
            GuestProfile organizer = eventItem.organizer;
            String currentUserId = FirebaseAuth.getInstance().getUid();

            // Set basic event details
            eventTitle.setText(event.getEventName());
            eventDate.setText(String.format("%s, %s", event.getDate(), event.getTime()));
            eventLocation.setText(event.getLocation());
            categoryTag.setText(event.getCategory());

            // Set Fee and Max Guests text
            String feeText = (event.getFee() == 0) ? "Free" : "RM" + String.format(Locale.US, "%.2f", event.getFee());
            eventAudience.setText("Fee: " + feeText + " | Max Guests: " + event.getMaxGuests());

            // Set Date Badge
            try {
                dateBadge.setText(event.getDate().substring(0, event.getDate().lastIndexOf('/')));
            } catch (Exception e) {
                dateBadge.setText(event.getDate());
            }

            // Set Organizer Name and Profile Picture
            if (organizer != null) {
                organizerName.setText(organizer.getName());
                if (organizer.getProfileImageUrl() != null && !organizer.getProfileImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(organizer.getProfileImageUrl())
                            .placeholder(R.drawable.ic_person)
                            .into(organizerAvatar);
                } else {
                    organizerAvatar.setImageResource(R.drawable.ic_person);
                }
            } else {
                organizerName.setText("Unknown Organizer");
                organizerAvatar.setImageResource(R.drawable.ic_person);
            }

            // Set Event Banner
            if (event.getCoverImageUrl() != null && !event.getCoverImageUrl().isEmpty()) {
                // If there IS a poster, load it with Glide
                bannerContainer.setOnClickListener(null); // Remove placeholder listener
                Glide.with(itemView.getContext())
                        .load(event.getCoverImageUrl())
                        .into(eventBanner);
                eventBanner.setOnClickListener(v -> { // Set click listener on the image itself
                    AppCompatActivity activity = (AppCompatActivity) v.getContext();
                    FullScreenImageDialogFragment dialog = FullScreenImageDialogFragment.newInstance(event.getCoverImageUrl());
                    dialog.show(activity.getSupportFragmentManager(), "FullScreenImageDialog");
                });
            } else {
                // If there is NO poster, apply the colored placeholder
                eventBanner.setOnClickListener(null); // Remove image listener
                GradientDrawable newDrawable = (GradientDrawable) ContextCompat.getDrawable(itemView.getContext(), R.drawable.event_banner_gradient_placeholder).mutate();
                newDrawable.setColor(placeholderColor);
                eventBanner.setImageDrawable(newDrawable); // Set the drawable on the ImageView
            }

            // Set color for placeholder image
            if (event.getCoverImageUrl() != null && !event.getCoverImageUrl().isEmpty()) {
                // If there is a poster, load it with Glide
                eventBanner.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(event.getCoverImageUrl())
                        .into(eventBanner);
                bannerContainer.setBackgroundColor(Color.TRANSPARENT); // Remove any placeholder color
                eventBanner.setOnClickListener(v -> {
                    AppCompatActivity activity = (AppCompatActivity) v.getContext();
                    FullScreenImageDialogFragment dialog = FullScreenImageDialogFragment.newInstance(event.getCoverImageUrl());
                    dialog.show(activity.getSupportFragmentManager(), "FullScreenImageDialog");
                });
            } else {
                // If there is NO poster, hide the ImageView and set a random color on the container
                eventBanner.setVisibility(View.GONE); // Hide the ImageView
                GradientDrawable newDrawable = (GradientDrawable) ContextCompat.getDrawable(itemView.getContext(), R.drawable.event_banner_gradient_placeholder).mutate();
                newDrawable.setColor(placeholderColor);
                bannerContainer.setBackground(newDrawable);
            }

            // Show/Hide and set listener for the Edit Button
            if (currentUserId != null && currentUserId.equals(event.getOrganizerId())) {
                editEventButton.setVisibility(View.VISIBLE);
                editEventButton.setOnClickListener(v -> {
                    Intent intent = new Intent(itemView.getContext(), EventCreationActivity.class);
                    intent.putExtra("IS_EDIT_MODE", true);
                    intent.putExtra("EDIT_EVENT_DATA", event);
                    itemView.getContext().startActivity(intent);
                });
            } else {
                editEventButton.setVisibility(View.GONE);
            }

            // Set a click listener on the entire card
            itemView.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, EventDetailActivity.class);
                intent.putExtra("event_details", eventItem.event);
                intent.putExtra("organizer_details", eventItem.organizer);
                context.startActivity(intent);
            });
        }
    }
}