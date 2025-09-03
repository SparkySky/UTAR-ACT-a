package com.meow.utaract.utils;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.meow.utaract.NewsActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsStorage {
    private static final String TAG = "NewsStorage";
    private static final String NEWS_COLLECTION = "news";
    private static final String FOLLOWS_COLLECTION = "follows";
    private static final String GUEST_PROFILES_COLLECTION = "guest_profiles";

    private Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;

    public NewsStorage(NewsActivity newsActivity) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // Create a new news post
    public void createNews(String content, NewsCreationCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            callback.onFailure(new Exception("User must be logged in to create news"));
            return;
        }

        String userId = currentUser.getUid();

        // First, fetch user details from Firestore
        firestore.collection(GUEST_PROFILES_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot userDocument) {
                        String organizerName = "Unknown Organizer";
                        String organizerProfilePic = "";

                        if (userDocument.exists()) {
                            // Get organizer name - check multiple possible fields
                            if (userDocument.contains("name")) {
                                organizerName = userDocument.getString("name");
                            } else if (userDocument.contains("displayName")) {
                                organizerName = userDocument.getString("displayName");
                            } else if (userDocument.contains("username")) {
                                organizerName = userDocument.getString("username");
                            } else if (userDocument.contains("organizerName")) {
                                organizerName = userDocument.getString("organizerName");
                            } else if (currentUser.getDisplayName() != null) {
                                organizerName = currentUser.getDisplayName();
                            }

                            // Get profile picture
                            if (userDocument.contains("profilePicture")) {
                                organizerProfilePic = userDocument.getString("profilePicture");
                            } else if (userDocument.contains("profilePic")) {
                                organizerProfilePic = userDocument.getString("profilePic");
                            } else if (userDocument.contains("profileImage")) {
                                organizerProfilePic = userDocument.getString("profileImage");
                            } else if (userDocument.contains("photoUrl")) {
                                organizerProfilePic = userDocument.getString("photoUrl");
                            }
                        } else {
                            // If no user document, create one with basic info
                            createUserDocument(userId, currentUser.getDisplayName());
                            organizerName = currentUser.getDisplayName() != null ?
                                    currentUser.getDisplayName() : "Unknown Organizer";
                        }

                        // Continue with news creation...
                        createNewsWithDetails(content, callback, userId, organizerName, organizerProfilePic);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        // Fallback to basic info
                        String organizerName = currentUser.getDisplayName() != null ?
                                currentUser.getDisplayName() : "Unknown Organizer";
                        createNewsWithDetails(content, callback, userId, organizerName, "");
                    }
                });
    }

    private void createUserDocument(String userId, String displayName) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", displayName != null ? displayName : "Unknown Organizer");
        userData.put("userId", userId);
        userData.put("createdAt", new Date());

        firestore.collection(GUEST_PROFILES_COLLECTION).document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User document created"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user document", e));
    }

    private void createNewsWithDetails(String content, NewsCreationCallback callback,
                                       String userId, String organizerName, String organizerProfilePic) {
        News news = new News();
        news.setOrganizerId(userId);
        news.setOrganizerName(organizerName);
        news.setOrganizerProfilePic(organizerProfilePic);
        news.setContent(content);
        news.setPostedDate(new Date());
        news.setLikeCount(0);
        news.setLikedBy(new ArrayList<>());

        // Save to Firestore...
        firestore.collection(NEWS_COLLECTION)
                .add(news)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String newsId = documentReference.getId();
                        news.setNewsId(newsId);

                        // Update with newsId
                        firestore.collection(NEWS_COLLECTION).document(newsId)
                                .set(news)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "News created successfully with ID: " + newsId);
                                        callback.onSuccess(newsId);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Failed to update news with ID", e);
                                        callback.onFailure(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to create news", e);
                        callback.onFailure(e);
                    }
                });
    }

    private void createNewsWithBasicInfo(String content, NewsCreationCallback callback,
                                         String userId, FirebaseUser currentUser) {
        String organizerName = currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "Unknown Organizer";

        News news = new News();
        news.setOrganizerId(userId);
        news.setOrganizerName(organizerName);
        news.setOrganizerProfilePic("");
        news.setContent(content);
        news.setPostedDate(new Date());
        news.setLikeCount(0);
        news.setLikedBy(new ArrayList<>());

        firestore.collection(NEWS_COLLECTION)
                .add(news)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        String newsId = documentReference.getId();
                        news.setNewsId(newsId);

                        firestore.collection(NEWS_COLLECTION).document(newsId)
                                .set(news)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        callback.onSuccess(newsId);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(Exception e) {
                                        callback.onFailure(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    // Get news from followed organizers
    public void getNewsFromFollowedOrganizers(NewsFetchCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        // Use GuestProfileStorage to get followed organizers
        GuestProfileStorage guestProfileStorage = new GuestProfileStorage(context);
        guestProfileStorage.downloadProfileFromFirestore(new GuestProfileStorage.FirestoreCallback() {
            @Override
            public void onSuccess(GuestProfile profile) {
                if (profile != null && profile.getFollowing() != null
                        && !profile.getFollowing().isEmpty()) {

                    // Get news from followed organizers
                    firestore.collection(NEWS_COLLECTION)
                            .whereIn("organizerId", profile.getFollowing())
                            .orderBy("postedDate", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    List<News> newsList = new ArrayList<>();
                                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                        News news = document.toObject(News.class);
                                        if (news != null) {
                                            newsList.add(news);
                                        }
                                    }
                                    callback.onSuccess(newsList);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    callback.onFailure(e);
                                }
                            });
                } else {
                    // No followed organizers
                    callback.onSuccess(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private void createEmptyFollowsDocument(String userId) {
        Map<String, Object> followsData = new HashMap<>();
        followsData.put("followedOrganizers", new ArrayList<String>());

        firestore.collection(FOLLOWS_COLLECTION).document(userId)
                .set(followsData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Created empty follows document"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create follows document", e));
    }

    // Get all news (for organizers)
    public void getAllNews(NewsFetchCallback callback) {
        Log.d(TAG, "Fetching all news...");

        firestore.collection(NEWS_COLLECTION)
                .orderBy("postedDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<News> newsList = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            News news = document.toObject(News.class);
                            if (news != null) {
                                newsList.add(news);
                            }
                        }
                        Log.d(TAG, "Retrieved " + newsList.size() + " total news items");
                        callback.onSuccess(newsList);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to fetch all news", e);
                        callback.onFailure(e);
                    }
                });
    }

    // Like a news post
    public void likeNews(String newsId, String userId, LikeCallback callback) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("likedBy", FieldValue.arrayUnion(userId));
        updateData.put("likeCount", FieldValue.increment(1));

        firestore.collection(NEWS_COLLECTION).document(newsId)
                .update(updateData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "News liked successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to like news", e);
                        callback.onFailure(e);
                    }
                });
    }

    // Unlike a news post
    public void unlikeNews(String newsId, String userId, LikeCallback callback) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("likedBy", FieldValue.arrayRemove(userId));
        updateData.put("likeCount", FieldValue.increment(-1));

        firestore.collection(NEWS_COLLECTION).document(newsId)
                .update(updateData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "News unliked successfully");
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to unlike news", e);
                        callback.onFailure(e);
                    }
                });
    }

    // Follow an organizer
    public void followOrganizer(String organizerId, LikeCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        firestore.collection(FOLLOWS_COLLECTION).document(currentUser.getUid())
                .update("followedOrganizers", FieldValue.arrayUnion(organizerId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // Unfollow an organizer
    public void unfollowOrganizer(String organizerId, LikeCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        firestore.collection(FOLLOWS_COLLECTION).document(currentUser.getUid())
                .update("followedOrganizers", FieldValue.arrayRemove(organizerId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // Check if following an organizer
    public void isFollowingOrganizer(String organizerId, NewsFetchCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        firestore.collection(FOLLOWS_COLLECTION).document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followedOrganizers = (List<String>) documentSnapshot.get("followedOrganizers");
                        boolean isFollowing = followedOrganizers != null && followedOrganizers.contains(organizerId);
                        // You might want to create a different callback for this
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // Callback interfaces
    public interface NewsCreationCallback {
        void onSuccess(String newsId);
        void onFailure(Exception e);
    }

    public interface NewsFetchCallback {
        void onSuccess(List<News> newsList);
        void onFailure(Exception e);
    }

    public interface LikeCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}