package com.meow.utaract;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isOrganiser = new MutableLiveData<>();

    public void setOrganiser(boolean isOrganiser) {
        this.isOrganiser.setValue(isOrganiser);
    }

    public LiveData<Boolean> isOrganiser() {
        return isOrganiser;
    }
}