package com.meow.utaract.ui.home;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.EventCreationActivity;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {
    private List<HomeViewModel.EventItem> eventItemList;

    public EventsAdapter(List<HomeViewModel.EventItem> eventItemList) {
        this.eventItemList = eventItemList;
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
        holder.bind(eventItem);
    }

    @Override
    public int getItemCount() {
        return eventItemList.size();
    }

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

        void bind(final HomeViewModel.EventItem eventItem) {
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
                bannerContainer.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(event.getCoverImageUrl())
                        .placeholder(R.drawable.event_banner_placeholder)
                        .into(eventBanner);
                eventBanner.setOnClickListener(v -> {
                    AppCompatActivity activity = (AppCompatActivity) v.getContext();
                    FullScreenImageDialogFragment dialog = FullScreenImageDialogFragment.newInstance(event.getCoverImageUrl());
                    dialog.show(activity.getSupportFragmentManager(), "FullScreenImageDialog");
                });
            } else {
                bannerContainer.setVisibility(View.GONE);
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
        }
    }
}