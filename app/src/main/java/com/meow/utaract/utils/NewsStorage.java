package com.meow.utaract.utils;

import android.util.Log;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class NewsStorage {
    private static final String NEWS_COLLECTION = "news";
    private final FirebaseFirestore firestore;

    public NewsStorage() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void createNews(News news, NewsCallback callback) {
        firestore.collection(NEWS_COLLECTION)
                .add(news)
                .addOnSuccessListener(documentReference -> {
                    String newsId = documentReference.getId();
                    news.setNewsId(newsId);

                    // Update with the ID
                    firestore.collection(NEWS_COLLECTION).document(newsId)
                            .set(news)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(newsId))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateNews(String newsId, News news, NewsCallback callback) {
        news.setUpdatedAt(System.currentTimeMillis());
        firestore.collection(NEWS_COLLECTION).document(newsId)
                .set(news, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(newsId))
                .addOnFailureListener(callback::onFailure);
    }

    public void deleteNews(String newsId, NewsCallback callback) {
        firestore.collection(NEWS_COLLECTION).document(newsId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(newsId))
                .addOnFailureListener(callback::onFailure);
    }

    public void getNewsForGuest(List<String> followedOrganizerIds, NewsListCallback callback) {
        if (followedOrganizerIds == null || followedOrganizerIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // Get news from organizers that the guest is following
        firestore.collection(NEWS_COLLECTION)
                .whereIn("organizerId", followedOrganizerIds)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<News> newsList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        News news = document.toObject(News.class);
                        newsList.add(news);
                    }
                    callback.onSuccess(newsList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getNewsForOrganizerWithFollowing(String organizerId, List<String> followedOrganizerIds, NewsListCallback callback) {
        List<String> allOrganizerIds = new ArrayList<>();
        allOrganizerIds.add(organizerId); // Include own news
        if (followedOrganizerIds != null) {
            allOrganizerIds.addAll(followedOrganizerIds); // Include followed organizers
        }

        // Remove duplicates
        allOrganizerIds = new ArrayList<>(new HashSet<>(allOrganizerIds));

        if (allOrganizerIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // Get news from all organizers (own + followed)
        firestore.collection(NEWS_COLLECTION)
                .whereIn("organizerId", allOrganizerIds)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<News> newsList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        News news = document.toObject(News.class);
                        newsList.add(news);
                    }
                    callback.onSuccess(newsList);
                })
                .addOnFailureListener(callback::onFailure);
    }


    public interface NewsCallback {
        void onSuccess(String newsId);
        void onFailure(Exception e);
    }

    public interface NewsListCallback {
        void onSuccess(List<News> newsList);
        void onFailure(Exception e);
    }
}