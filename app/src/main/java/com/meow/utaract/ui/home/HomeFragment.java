package com.meow.utaract.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.meow.utaract.EventCreationActivity;
import com.meow.utaract.EventDetailActivity;
import com.meow.utaract.GuestFormActivity;
import com.meow.utaract.LoginActivity;
import com.meow.utaract.MainActivity;
import com.meow.utaract.MainViewModel;
import com.meow.utaract.R;
import com.meow.utaract.databinding.FragmentHomeBinding;
import com.meow.utaract.ui.event.EventsAdapter;
import com.meow.utaract.utils.GuestProfile;
import com.meow.utaract.utils.GuestProfileStorage;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements FilterBottomSheetDialogFragment.FilterListener {

    private FragmentHomeBinding binding;
    private EventsAdapter eventsAdapter;
    private HomeViewModel homeViewModel;
    private MainViewModel mainViewModel;
    private MotionLayout motionLayoutHeader;
    private EditText searchInput;
    private boolean isInitialLoad = true;

    // QR Scanner Launcher
    private final ActivityResultLauncher<ScanOptions> qrScannerLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleScannedUrl(result.getContents());
                } else {
                    Toast.makeText(getContext(), "Exit QR Scanner", Toast.LENGTH_SHORT).show();
                }
            });

    // --- QR Image Picker Launcher (for gallery) ---
    private final ActivityResultLauncher<String> galleryImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        scanBitmapForQrCode(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        if (getContext() != null) Toast.makeText(getContext(), "File not found.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

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

    private void showScanOptionsDialog() {
        if (getContext() == null) return;
        final CharSequence[] options = {"Scan with Camera", "Scan from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Scan Event QR Code");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Scan with Camera")) {
                launchCameraScanner();
            } else if (options[item].equals("Scan from Gallery")) {
                galleryImageLauncher.launch("image/*");
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void launchCameraScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan Event QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        qrScannerLauncher.launch(options);
    }

    private void scanBitmapForQrCode(Bitmap bitmap) {
        if (bitmap == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Could not decode image.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ZXing library logic to find a QR code in a bitmap
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = new QRCodeReader().decode(binaryBitmap);
            handleScannedUrl(result.getText());
        } catch (NotFoundException | ChecksumException | FormatException e) {
            e.printStackTrace();
            if (getContext() != null) Toast.makeText(getContext(), "No QR Code found in the selected image.", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateHeaderOnScroll();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isInitialLoad) {
            loadDataWithPreferences();
            isInitialLoad = false;
        } else {
            homeViewModel.fetchEvents(null); // Refresh data without resetting filters
        }
    }

    private void loadDataWithPreferences() {
        if (getContext() == null) return;

        GuestProfile profile = new GuestProfileStorage(getContext()).loadProfile();
        List<String> preferences = (profile != null && profile.getPreferences() != null)
                ? profile.getPreferences()
                : new ArrayList<>();

        homeViewModel.fetchEvents(preferences);
    }

    private void handleScannedUrl(String url) {
        if (url == null) {
            Toast.makeText(getContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url);
        // Check if the URL matches the expected format
        if ("https".equals(uri.getScheme()) && "utaract.page.link".equals(uri.getHost()) && "/event".equals(uri.getPath())) {
            String eventId = uri.getQueryParameter("id");
            if (eventId != null && !eventId.isEmpty()) {
                // If a valid event ID is found, open the event detail page
                Intent intent = new Intent(getActivity(), EventDetailActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Invalid Event Link: No ID found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "QR Code is not a valid Event Link", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        // THE FIX: Pass the fragment's root view (binding.getRoot()) to the adapter's constructor.
        eventsAdapter = new EventsAdapter(new ArrayList<>(), binding.getRoot());
        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewEvents.setAdapter(eventsAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupUIListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> homeViewModel.fetchEvents(null));

        binding.menuIcon.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        binding.searchContainer.setOnClickListener(v -> showScanOptionsDialog());

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