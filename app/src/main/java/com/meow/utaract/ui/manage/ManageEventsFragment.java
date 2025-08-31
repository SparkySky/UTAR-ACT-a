package com.meow.utaract.ui.manage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.meow.utaract.databinding.FragmentManageEventsBinding;
import com.meow.utaract.ui.event.MyEventsAdapter;
import com.meow.utaract.utils.EventCreationStorage;
import java.util.ArrayList;

public class ManageEventsFragment extends Fragment {

    private FragmentManageEventsBinding binding;
    private ManageEventsViewModel manageEventsViewModel;
    private MyEventsAdapter myEventsAdapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        manageEventsViewModel = new ViewModelProvider(this).get(ManageEventsViewModel.class);
        binding = FragmentManageEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = binding.myEventsRecyclerView;
        emptyView = binding.emptyView;
        progressBar = binding.progressBar;

        setupRecyclerView();
        observeViewModel();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data every time the user comes to this screen
        manageEventsViewModel.fetchMyEvents();
    }

    private void setupRecyclerView() {
        myEventsAdapter = new MyEventsAdapter(new ArrayList<>(), getContext(), new EventCreationStorage());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(myEventsAdapter);
    }

    private void observeViewModel() {
        manageEventsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });

        manageEventsViewModel.getMyEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null && !events.isEmpty()) {
                myEventsAdapter.updateEvents(events); // You'll need to add this method to your adapter
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}