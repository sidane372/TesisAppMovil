package com.example.santaceciliatesis;

import android.app.Application;

import com.example.santaceciliatesis.data.AppContainer;

public final class SantaCeciliaApplication extends Application {
    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}
