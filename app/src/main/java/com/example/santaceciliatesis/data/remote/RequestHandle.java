package com.example.santaceciliatesis.data.remote;

import android.os.Handler;

import javax.net.ssl.HttpsURLConnection;

/** Una petición de vista se puede cancelar sin cancelar una renovación compartida. */
public final class RequestHandle {
    private volatile boolean cancelled;
    private HttpsURLConnection connection;

    public boolean isCancelled() {
        return cancelled;
    }

    public synchronized void attach(HttpsURLConnection value) {
        if (cancelled) value.disconnect();
        else connection = value;
    }

    public synchronized void detach(HttpsURLConnection value) {
        if (connection == value) connection = null;
    }

    public synchronized void cancel() {
        cancelled = true;
        if (connection != null) connection.disconnect();
        connection = null;
    }

    public void post(Handler main, Runnable action) {
        if (!cancelled)
            main.post(
                    () -> {
                        if (!cancelled) action.run();
                    });
    }
}
