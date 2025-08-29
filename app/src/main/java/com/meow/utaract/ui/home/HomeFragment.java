package com.meow.utaract.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
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
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements FilterBottomSheetDialogFragment.FilterListener {

    private FragmentHomeBinding binding;
    private EventsAdapter eventsAdapter;
    private HomeViewModel homeViewModel;
    private MainViewModel mainViewModel;
    private MotionLayout motionLayoutHeader;
    private EditText searchInput;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        motionLayoutHeader = binding.motionLayoutHeader;
        motionLayoutHeader.setProgress(1.0f);
        searchInput = binding.searchInput;

        setupRecyclerView();
        setupUIListeners();
        observeViewModels();
        loadDataWithPreferences();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateHeaderOnScroll();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataWithPreferences();
    }

    private void loadDataWithPreferences() {
        if (getContext() == null) return;

        GuestProfile profile = new GuestProfileStorage(getContext()).loadProfile();
        List<String> preferences = (profile != null && profile.getPreferences() != null)
                ? profile.getPreferences()
                : new ArrayList<>();

        homeViewModel.fetchEvents(preferences);
    }

    private void setupRecyclerView() {
        // THE FIX: Pass the fragment's root view (binding.getRoot()) to the adapter's constructor.
        eventsAdapter = new EventsAdapter(new ArrayList<>(), binding.getRoot());
        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewEvents.setAdapter(eventsAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupUIListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadDataWithPreferences);

        binding.menuIcon.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        binding.userAvatar.setOnClickListener(this::showPopupMenu);
        binding.filterButton.setOnClickListener(v -> showFilterDialog());

        binding.nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            updateHeaderOnScroll();
        });

        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                motionLayoutHeader.transitionToEnd();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                homeViewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.nestedScrollView.setOnTouchListener((v, event) -> {
            if (searchInput.hasFocus()) {
                searchInput.clearFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                }
            }
            return false;
        });
    }

    private void updateHeaderOnScroll() {
        float headerHeight = binding.mainSection.getHeight();
        if (headerHeight == 0) return;
        float scrollProgress = Math.min(binding.nestedScrollView.getScrollY() / headerHeight, 1.0f);
        motionLayoutHeader.setProgress(1.0f - scrollProgress);
    }

    private void showFilterDialog() {
        String[] categories = getResources().getStringArray(R.array.event_categories);
        List<String> currentFilters = homeViewModel.getActiveFilters().getValue();
        ArrayList<String> selected = (currentFilters != null) ? new ArrayList<>(currentFilters) : new ArrayList<>();

        FilterBottomSheetDialogFragment bottomSheet = FilterBottomSheetDialogFragment.newInstance(categories, selected);
        bottomSheet.setFilterListener(this);
        bottomSheet.show(getParentFragmentManager(), "FilterBottomSheet");
    }

    @Override
    public void onFilterApplied(List<String> selectedCategories) {
        homeViewModel.setCategoryFilters(selectedCategories);
    }

    private void observeViewModels() {
        homeViewModel.getEventItems().observe(getViewLifecycleOwner(), eventItems -> {
            if (eventItems != null) {
                eventsAdapter.updateEvents(eventItems);
            }
        });

        homeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
            }
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
                if (mainViewModel.isOrganiser().getValue() != null) {
                    intent.putExtra("IS_ORGANISER", mainViewModel.isOrganiser().getValue());
                }
                startActivity(intent);
                return true;
            }
            if (id == R.id.action_logout) {
                if (getContext() != null) {
                    new GuestProfileStorage(getContext()).clearProfile();
                }
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
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