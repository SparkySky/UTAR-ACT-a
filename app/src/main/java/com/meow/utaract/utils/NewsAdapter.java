package com.meow.utaract.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.R;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import com.meow.utaract.utils.News;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<News> newsList;
    private final NewsItemClickListener listener;
    private final boolean showFullList;
    private boolean isOrganiser;

    public interface NewsItemClickListener {
        void onLikeClicked(News news, int position);
        void onEditClicked(News news, int position);
        void onDeleteClicked(News news, int position);
    }

    public NewsAdapter(List<News> newsList, NewsItemClickListener listener,
                       boolean showFullList, boolean isOrganiser) {
        this.newsList = newsList != null ? newsList : new ArrayList<>();
        this.listener = listener;
        this.showFullList = showFullList;
        this.isOrganiser = isOrganiser; // INITIALIZE IT
    }

    public void updateNews(List<News> newNews) {
        this.newsList.clear();
        this.newsList.addAll(newNews);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);
        holder.bind(news, listener, showFullList, isOrganiser);
    }

    @Override
    public int getItemCount() {
        if (showFullList) {
            return newsList.size();
        } else {
            return Math.min(newsList.size(), 5); // Show only first 5 items
        }
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView organizerName, newsTitle, newsMessage, likeCount, newsDate;
        ImageView newsImage;
        ImageButton likeButton;
        ViewGroup imageContainer;
        Button editButton, deleteButton;
        LinearLayout organizerActions;
        ImageButton menuButton;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            organizerName = itemView.findViewById(R.id.organizerName);
            newsTitle = itemView.findViewById(R.id.newsTitle);
            newsMessage = itemView.findViewById(R.id.newsMessage);
            likeCount = itemView.findViewById(R.id.likeCount);
            newsDate = itemView.findViewById(R.id.newsDate);
            newsImage = itemView.findViewById(R.id.newsImage);
            likeButton = itemView.findViewById(R.id.likeButton);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            menuButton = itemView.findViewById(R.id.menuButton);
            organizerActions = itemView.findViewById(R.id.organizerActions);
            imageContainer = itemView.findViewById(R.id.imageContainer);
        }

        void bind(News news, NewsItemClickListener listener, boolean showFullContent, boolean isOrganiser) {
            // Set news data
            newsTitle.setText(news.getTitle());

            if (showFullContent) {
                newsMessage.setText(news.getMessage());
                newsMessage.setMaxLines(Integer.MAX_VALUE);
            } else {
                // Truncate message for preview
                String message = news.getMessage();
                if (message.length() > 100) {
                    message = message.substring(0, 100) + "...";
                }
                newsMessage.setText(message);
                newsMessage.setMaxLines(3);
            }

            if (news.getOrganizerName() != null && !news.getOrganizerName().isEmpty()) {
                organizerName.setText(news.getOrganizerName());
            } else {
                organizerName.setText("Organizer Name");
            }

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());

            String dateString = sdf.format(new Date(news.getCreatedAt()));
            newsDate.setText(dateString);

            // Set like count and state
            int totalLikes = news.getTotalLikeCount(itemView.getContext());
            likeCount.setText(String.valueOf(totalLikes));

            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

            boolean isLiked = news.isLikedByCurrentUser(itemView.getContext(), isOrganiser);
            likeButton.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            boolean isOwner = currentUserId.equals(news.getOrganizerId());
            if (isOrganiser && isOwner) {
                organizerActions.setVisibility(View.VISIBLE);
                menuButton.setVisibility(View.VISIBLE);
            } else {
                organizerActions.setVisibility(View.GONE);
                menuButton.setVisibility(View.GONE);
            }

            // Load images
            if (news.getImageUrls() != null && !news.getImageUrls().isEmpty()) {
                imageContainer.setVisibility(View.VISIBLE);
                imageContainer.removeAllViews(); // Clear previous images

                for (String imageUrl : news.getImageUrls()) {
                    ImageView imageView = new ImageView(itemView.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            300 // Fixed height or adjust as needed
                    );
                    params.setMargins(0, 0, 0, 16); // Add spacing between images
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .into(imageView);

                    imageContainer.addView(imageView);
                }
            } else {
                imageContainer.setVisibility(View.GONE);
            }

            // Set click listeners
            likeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClicked(news, getAdapterPosition());
                }
            });

            // Edit button
            editButton.setOnClickListener(v -> {
                if (listener != null && isOrganiser && isOwner) {
                    listener.onEditClicked(news, getAdapterPosition());
                }
            });

            // Delete button
            deleteButton.setOnClickListener(v -> {
                if (listener != null && isOrganiser && isOwner) {
                    listener.onDeleteClicked(news, getAdapterPosition());
                }
            });

            // Menu button (for overflow menu)
            menuButton.setOnClickListener(v -> showOptionsMenu(news, listener, isOrganiser, isOwner));
        }
        private void showOptionsMenu(News news, NewsItemClickListener listener, boolean isOrganiser, boolean isOwner) {
            PopupMenu popupMenu = new PopupMenu(itemView.getContext(), menuButton);
            popupMenu.getMenuInflater().inflate(R.menu.news_options_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_edit && isOrganiser && isOwner) {
                    listener.onEditClicked(news, getAdapterPosition());
                    return true;
                } else if (id == R.id.menu_delete && isOrganiser && isOwner) {
                    listener.onDeleteClicked(news, getAdapterPosition());
                    return true;
                }
                return false;
            });

            popupMenu.show();
        }
    }
}