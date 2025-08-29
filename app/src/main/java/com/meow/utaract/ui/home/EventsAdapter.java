package com.meow.utaract.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.meow.utaract.R;
import com.meow.utaract.utils.Event;
import com.meow.utaract.utils.GuestProfile;
import java.util.List;
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
        Event event = eventItem.event;
        GuestProfile organizer = eventItem.organizer;

        holder.eventTitle.setText(event.getEventName());
        holder.eventDate.setText(String.format("%s, %s", event.getDate(), event.getTime()));
        holder.eventLocation.setText(event.getLocation());
        holder.categoryTag.setText(event.getCategory());

        String feeText = (event.getFee() == 0) ? "Free" : "RM" + String.format("%.2f", event.getFee());
        holder.eventAudience.setText("Fee: " + feeText + " | Max Guests: " + event.getMaxGuests());

        if (organizer != null) {
            holder.organizerName.setText(organizer.getName());
            if (organizer.getProfileImageUrl() != null && !organizer.getProfileImageUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(organizer.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person)
                        .into(holder.organizerAvatar);
            } else {
                holder.organizerAvatar.setImageResource(R.drawable.ic_person);
            }
        } else {
            holder.organizerName.setText("Unknown Organizer");
            holder.organizerAvatar.setImageResource(R.drawable.ic_person);
        }

        try {
            holder.dateBadge.setText(event.getDate().substring(0, event.getDate().lastIndexOf('/')));
        } catch (Exception e) {
            holder.dateBadge.setText(event.getDate());
        }

        if (event.getCoverImageUrl() != null && !event.getCoverImageUrl().isEmpty()) {
            holder.bannerContainer.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(event.getCoverImageUrl())
                    .placeholder(R.drawable.event_banner_placeholder)
                    .into(holder.eventBanner);

            holder.eventBanner.setOnClickListener(v -> {
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                FullScreenImageDialogFragment dialog = FullScreenImageDialogFragment.newInstance(event.getCoverImageUrl());
                dialog.show(activity.getSupportFragmentManager(), "FullScreenImageDialog");
            });
        } else {
            holder.bannerContainer.setVisibility(View.GONE);
        }
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

        public EventViewHolder(@NonNull View itemView) {
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
        }
    }
}