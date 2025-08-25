package com.meow.utaract.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.meow.utaract.databinding.FragmentHomeBinding;
import com.meow.utaract.R;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;


public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EventsAdapter eventsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        List<Event> eventList = new ArrayList<>();
        eventList.add(new Event(
                "Let's Talk about Depression",
                "28 September 2025 (Sunday)",
                "UTAR Kampar, Dewan Tun Ling Liong Sik, Block M",
                "Open to all UTAR students & staff",
                "Growth",
                "28 SEPTEMBER",
                R.drawable.banner1
        ));

        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsAdapter = new EventsAdapter(eventList);
        binding.recyclerViewEvents.setAdapter(eventsAdapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}