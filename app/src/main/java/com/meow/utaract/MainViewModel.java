package com.meow.utaract;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.meow.utaract.utils.GuestProfile; // Import GuestProfile

public class MainViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isOrganiser = new MutableLiveData<>();

    // --- Add this section if it's not already there ---
    private final MutableLiveData<GuestProfile> userProfile = new MutableLiveData<>();

    public void setProfile(GuestProfile profile) {
        this.userProfile.setValue(profile);
    }

    public LiveData<GuestProfile> getProfile() {
        return userProfile;
    }
    // ---------------------------------------------

    public void setOrganiser(boolean isOrganiser) {
        this.isOrganiser.setValue(isOrganiser);
    }

    public LiveData<Boolean> isOrganiser() {
        return isOrganiser;
    }
}