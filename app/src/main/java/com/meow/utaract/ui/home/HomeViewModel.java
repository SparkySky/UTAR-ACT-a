package com.meow.utaract.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends ViewModel {

    private LiveData<Object> text;

    public LiveData<Object> getText() {
        return text;
    }

    public void setText(LiveData<Object> text) {
        this.text = text;
    }

    public enum Category {
        ALL,
        SPORTS,
        MUSIC,
        ART
    }

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Category> selectedCategory = new MutableLiveData<>(Category.ALL);
    private final MutableLiveData<List<Event>> allEvents = new MutableLiveData<>(seedEvents());

    private final MediatorLiveData<List<Event>> filteredEvents = new MediatorLiveData<>();

    public HomeViewModel() {
        filteredEvents.addSource(allEvents, list -> filter());
        filteredEvents.addSource(searchQuery, s -> filter());
        filteredEvents.addSource(selectedCategory, c -> filter());
        filter();
    }

    public LiveData<List<Event>> getFilteredEvents() {
        return filteredEvents;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query == null ? "" : query);
    }

    public void setSelectedCategory(Category category) {
        selectedCategory.setValue(category);
    }

    private void filter() {
        List<Event> base = allEvents.getValue();
        if (base == null) {
            filteredEvents.setValue(new ArrayList<>());
            return;
        }
        String q = searchQuery.getValue();
        if (q == null) q = "";
        q = q.trim().toLowerCase(Locale.ROOT);
        Category cat = selectedCategory.getValue();

        List<Event> out = new ArrayList<>();
        for (Event e : base) {
            boolean matchesQuery = q.isEmpty() ||
                    e.title.toLowerCase(Locale.ROOT).contains(q) ||
                    e.venue.toLowerCase(Locale.ROOT).contains(q) ||
                    e.openTo.toLowerCase(Locale.ROOT).contains(q);
            boolean matchesCategory = (cat == null || cat == Category.ALL) || e.tags.contains(cat);
            if (matchesQuery && matchesCategory) {
                out.add(e);
            }
        }
        filteredEvents.setValue(out);
    }

    private static List<Event> seedEvents() {
        List<Event> demo = new ArrayList<>();
        demo.add(new Event(
                "Let’s Talk about Depression",
                "28 September 2025 (Sunday)",
                "UTAR Kampar, Dewan Ling Liong Sik, Block M",
                "Open to all UTAR students & staff",
                Arrays.asList(Category.ART)));
        demo.add(new Event(
                "UTAR Futsal Cup",
                "02 October 2025 (Thursday)",
                "Sports Complex Court A",
                "Open to all students",
                Arrays.asList(Category.SPORTS)));
        demo.add(new Event(
                "Acoustic Night",
                "12 November 2025 (Wednesday)",
                "Grand Hall",
                "Open with registration",
                Arrays.asList(Category.MUSIC, Category.ART)));
        return demo;
    }

    public static class Event {
        public final String title;
        public final String date;
        public final String venue;
        public final String openTo;
        public final List<Category> tags;

        public Event(String title, String date, String venue, String openTo, List<Category> tags) {
            this.title = title;
            this.date = date;
            this.venue = venue;
            this.openTo = openTo;
            this.tags = tags;
        }
    }
}