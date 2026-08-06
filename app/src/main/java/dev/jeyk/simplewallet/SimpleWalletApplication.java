package dev.jeyk.simplewallet;

import android.app.Application;

public final class SimpleWalletApplication extends Application {
    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        if (container == null) {
            throw new IllegalStateException("Application container is not initialized");
        }
        return container;
    }

    @Override
    public void onTerminate() {
        if (container != null) {
            container.close();
        }
        super.onTerminate();
    }
}
