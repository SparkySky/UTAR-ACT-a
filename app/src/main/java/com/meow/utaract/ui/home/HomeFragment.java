package com.meow.utaract.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.meow.utaract.EventCreationActivity;
import com.meow.utaract.GuestFormActivity;
import com.meow.utaract.LoginActivity;
import com.meow.utaract.MainActivity;
import com.meow.utaract.MainViewModel;
import com.meow.utaract.R;
import com.meow.utaract.databinding.FragmentHomeBinding;
import com.meow.utaract.utils.GuestProfileStorage;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EventsAdapter eventsAdapter;
    private HomeViewModel homeViewModel;
    private MainViewModel mainViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        setupUIListeners();
        observeViewModels();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        homeViewModel.fetchEvents();
    }

    private void setupRecyclerView() {
        eventsAdapter = new EventsAdapter(new ArrayList<>());
        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewEvents.setAdapter(eventsAdapter);
    }

    private void setupUIListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> homeViewModel.fetchEvents());

        binding.menuIcon.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        binding.userAvatar.setOnClickListener(this::showPopupMenu);

        // Add a TextWatcher to the search input field
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // As the user types, call the filter method in the ViewModel
                homeViewModel.filterEvents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModels() {
        homeViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            eventsAdapter.updateEvents(events);
        });

        homeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefreshLayout.setRefreshing(isLoading);
        });

        mainViewModel.isOrganiser().observe(getViewLifecycleOwner(), isOrganiser -> {
            if (isOrganiser != null && isOrganiser) {
                binding.addEventFab.setVisibility(View.VISIBLE);
                binding.addEventFab.setOnClickListener(v ->
                        startActivity(new Intent(getActivity(), EventCreationActivity.class)));
            } else {
                binding.addEventFab.setVisibility(View.GONE);
            }
        });
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.main, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_profile_setting) {
                Intent intent = new Intent(getActivity(), GuestFormActivity.class);
                intent.putExtra("IS_EDIT", true);
                mainViewModel.isOrganiser().observe(getViewLifecycleOwner(), isOrganiser ->
                        intent.putExtra("IS_ORGANISER", isOrganiser));
                startActivity(intent);
                return true;
            }
            if (id == R.id.action_logout) {
                new GuestProfileStorage(requireContext()).clearProfile();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}