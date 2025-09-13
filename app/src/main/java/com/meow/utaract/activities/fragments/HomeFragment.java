// sparkysky/utar-act-a/UTAR-ACT-a-CP10/app/src/main/java/com/meow/utaract/ui/home/HomeFragment.java
package com.meow.utaract.activities.fragments;

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
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.ImageButton;

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
import com.meow.utaract.activities.ChatActivity;
import com.meow.utaract.activities.EventCreationActivity;
import com.meow.utaract.activities.EventDetailActivity;
import com.meow.utaract.activities.GuestFormActivity;
import com.meow.utaract.activities.LoginActivity;
import com.meow.utaract.activities.MainActivity;
import com.meow.utaract.viewmodels.HomeViewModel;
import com.meow.utaract.viewmodels.MainViewModel;
import com.meow.utaract.activities.NotificationActivity;
import com.meow.utaract.activities.PortraitCaptureActivity;
import com.meow.utaract.R;
import com.meow.utaract.databinding.FragmentHomeBinding;
import com.meow.utaract.adapters.EventsAdapter;
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
    private CircleImageView userAvatar;
    private ImageButton moreOptionsButton;
    private boolean isOrganiser;
    private GuestProfileStorage guestProfileStorage; // You need to initialize this

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
        View root = binding.getRoot();

        // Initialize GuestProfileStorage
        guestProfileStorage = new GuestProfileStorage(requireContext());

        userAvatar = binding.userAvatar;
        moreOptionsButton = binding.moreOptionsButton;

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        motionLayoutHeader = binding.motionLayoutHeader;
        motionLayoutHeader.setProgress(1.0f);

        searchInput = binding.searchInput;

        setupRecyclerView();
        setupUIListeners();
        observeViewModels();
        loadDataWithPreferences();

        return root;
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
        options.setCaptureActivity(PortraitCaptureActivity.class); // Launch in portrait
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan Event QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
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

        userAvatar.setOnClickListener(this::showPopupMenu);
        moreOptionsButton.setOnClickListener(this::showPopupMenu);
        updateHeaderOnScroll();

        // GET FLAG FROM MAIN ACTIVITY
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            isOrganiser = activity.isOrganiser();
        }

        // Let the ViewModel handle fetching and providing the profile
        homeViewModel.fetchUserProfile();
        homeViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null && profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(profile.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person)
                        .into(userAvatar);
            }
        });

        if (isOrganiser) {
            userAvatar.setVisibility(View.VISIBLE);
            moreOptionsButton.setVisibility(View.GONE);

        } else {
            userAvatar.setVisibility(View.GONE);
            moreOptionsButton.setVisibility(View.VISIBLE);
        }
    }

    private void loadDataWithPreferences() {
        GuestProfile profile = guestProfileStorage.loadProfile();
        List<String> preferences = (profile != null && profile.getPreferences() != null)
                ? profile.getPreferences()
                : new ArrayList<>();

        homeViewModel.setCategoryFilters(preferences);
        homeViewModel.fetchEvents();
    }

    private void handleScannedUrl(String url) {
        if (url == null) {
            Toast.makeText(getContext(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url);
        if ("https".equals(uri.getScheme()) && "utaract.page.link".equals(uri.getHost()) && "/event".equals(uri.getPath())) {
            String eventId = uri.getQueryParameter("id");
            if (eventId != null && !eventId.isEmpty()) {
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
        eventsAdapter = new EventsAdapter(new ArrayList<>(), binding.getRoot());
        binding.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewEvents.setAdapter(eventsAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupUIListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> homeViewModel.fetchEvents());

        binding.menuIcon.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                DrawerLayout drawerLayout = ((MainActivity) getActivity()).getDrawerLayout();
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        binding.qrScannerIcon.setOnClickListener(v -> showScanOptionsDialog());
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

        binding.notificationIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationActivity.class);
            intent.putExtra("IS_ORGANISER", isOrganiser);
            startActivity(intent);
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

    public void observeViewModels() {
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

        // THE FIX: Move the FAB visibility logic to the observer for the loading state,
        // and check both the loading state and the organiser status.
        homeViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && !isLoading) { // Only set visibility when loading is complete
                if (isOrganiser) {
                    binding.addEventFab.setVisibility(View.VISIBLE);
                    binding.addEventFab.setOnClickListener(v ->
                            startActivity(new Intent(getActivity(), EventCreationActivity.class)));
                    binding.askBotGeneral.setVisibility(View.VISIBLE);
                    binding.askBotGeneral.setOnClickListener(v -> {
                        startActivity(new Intent(getActivity(), ChatActivity.class)
                                .putExtra("MODE", "GENERAL"));
                    });
                } else {
                    binding.addEventFab.setVisibility(View.GONE);
                    binding.askBotGeneral.setVisibility(View.VISIBLE);
                    binding.askBotGeneral.setOnClickListener(v -> {
                        startActivity(new Intent(getActivity(), ChatActivity.class)
                                .putExtra("MODE", "GENERAL"));
                    });
                }
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


                // Use the fragment's local 'isOrganiser' field instead of the LiveData value
                intent.putExtra("IS_ORGANISER", isOrganiser);


                startActivity(intent);
                return true;
            }
            if (id == R.id.action_logout) {
                guestProfileStorage.clearProfile();
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
    public void onResume() {
        super.onResume();
        // Explicitly set the organiser status on resume to ensure the fragment's observer gets the value.
        mainViewModel.setOrganiser(isOrganiser);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}