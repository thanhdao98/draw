package com.tool.draw.network;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConnectionViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);

    public LiveData<Boolean> getConnectionStatus() {
        return isConnected;
    }

    public void setConnectionStatus(boolean status) {
        isConnected.postValue(status);
    }
}

