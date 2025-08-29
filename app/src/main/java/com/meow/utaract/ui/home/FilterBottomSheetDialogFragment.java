package com.meow.utaract.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.meow.utaract.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApplied(List<String> selectedCategories);
    }

    private static final String ARG_CATEGORIES = "categories";
    private static final String ARG_SELECTED_CATEGORIES = "selected_categories";

    private ChipGroup filterChipGroup;
    private FilterListener listener;
    private Set<String> selectedCategories;

    public static FilterBottomSheetDialogFragment newInstance(String[] categories, ArrayList<String> selected) {
        FilterBottomSheetDialogFragment fragment = new FilterBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putStringArray(ARG_CATEGORIES, categories);
        args.putStringArrayList(ARG_SELECTED_CATEGORIES, selected);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_for_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        if (getArguments() != null) {
            String[] categories = getArguments().getStringArray(ARG_CATEGORIES);
            ArrayList<String> preSelected = getArguments().getStringArrayList(ARG_SELECTED_CATEGORIES);
            selectedCategories = new HashSet<>(preSelected);

            if (categories != null) {
                for (String category : categories) {
                    Chip chip = new Chip(getContext());
                    chip.setText(category);
                    chip.setCheckable(true);
                    chip.setChecked(selectedCategories.contains(category));

                    chip.setChipBackgroundColorResource(R.color.preference_chip_background);
                    chip.setTextColor(getResources().getColorStateList(R.color.preference_chip_text_color, requireActivity().getTheme()));
                    chip.setChipStrokeWidth(0); // Hide the default stroke

                    filterChipGroup.addView(chip);
                }
            }
        }

        view.findViewById(R.id.applyButton).setOnClickListener(v -> {
            if (listener != null) {
                List<String> currentSelection = new ArrayList<>();
                for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) filterChipGroup.getChildAt(i);
                    if (chip.isChecked()) {
                        currentSelection.add(chip.getText().toString());
                    }
                }
                listener.onFilterApplied(currentSelection);
            }
            dismiss();
        });

        view.findViewById(R.id.resetButton).setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(new ArrayList<>()); // Send back an empty list
            }
            dismiss();
        });
    }
}