package com.example.santaceciliatesis.service;

import android.os.Handler;

import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.*;

import java.util.Set;
import java.util.concurrent.*;

/** Las revisiones de cuenta impiden que una respuesta tardía restaure una sesión cerrada. */
public final class SessionManager {
    public interface Listener {
        void onChanged(Snapshot snapshot);
    }

    public static final class Snapshot {
        public final long epoch;
        public final Sesion session;
        public final boolean loaded;
        public final String notice;

        Snapshot(long epoch, Sesion session, boolean loaded, String notice) {
            this.epoch = epoch;
            this.session = session;
            this.loaded = loaded;
            this.notice = notice;
        }
    }

    private final Object stateLock = new Object(), changeLock = new Object();
    private final SessionStore store;
    private final Handler main;
    private final CountDownLatch ready = new CountDownLatch(1);
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private long epoch;
    private Sesion session;
    private boolean loaded;
    private String notice;

    public SessionManager(SessionStore store, Handler main) {
        this.store = store;
        this.main = main;
    }

    public Snapshot snapshot() {
        synchronized (stateLock) {
            return new Snapshot(epoch, session, loaded, notice);
        }
    }

    public void restore() {
        Sesion restored = null;
        try {
            restored = store.read();
        } catch (Exception e) {
            try {
                store.clear();
            } catch (Exception ignored) {
            }
        }
        synchronized (stateLock) {
            session = restored;
            loaded = true;
            epoch++;
        }
        ready.countDown();
        publish();
    }

    public void awaitReady() throws ApiFailure {
        try {
            if (!ready.await(10, TimeUnit.SECONDS))
                throw new ApiFailure(
                        ApiFailure.Kind.STORAGE, "No pudimos recuperar tu acceso guardado.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiFailure(ApiFailure.Kind.CANCELLED, "");
        }
    }

    public void login(long expected, Sesion value) throws ApiFailure {
        change(expected, value, true);
    }

    public void refresh(long expected, Sesion value) throws ApiFailure {
        change(expected, value, false);
    }

    public void usuario(long expected, Usuario user) throws ApiFailure {
        synchronized (changeLock) {
            Snapshot before = snapshot();
            if (before.epoch != expected
                    || before.session == null
                    || before.session.getUsuario().getId() != user.getId()) throw changed();
            change(expected, before.session.conUsuario(user), false);
        }
    }

    private void change(long expected, Sesion value, boolean newAccount) throws ApiFailure {
        synchronized (changeLock) {
            if (snapshot().epoch != expected) throw changed();
            try {
                store.save(value);
            } catch (Exception e) {
                clear(expected);
                throw new ApiFailure(
                        ApiFailure.Kind.STORAGE,
                        "No pudimos guardar el acceso de forma segura. Inicia sesión nuevamente.");
            }
            synchronized (stateLock) {
                session = value;
                notice = null;
                if (newAccount) epoch++;
            }
            publish();
        }
    }

    public void clear(long expected) throws ApiFailure {
        clear(expected, "Tu sesión venció. Inicia sesión nuevamente.");
    }

    public void clear(long expected, String message) throws ApiFailure {
        synchronized (changeLock) {
            if (snapshot().epoch != expected) return;
            boolean failure = false;
            try {
                store.clear();
            } catch (Exception e) {
                failure = true;
            }
            synchronized (stateLock) {
                session = null;
                notice = failure ? "No pudimos completar el cierre local de sesión." : message;
                epoch++;
            }
            publish();
            if (failure)
                throw new ApiFailure(
                        ApiFailure.Kind.STORAGE, "No pudimos completar el cierre local de sesión.");
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        Snapshot value = snapshot();
        main.post(
                () -> {
                    if (listeners.contains(listener) && snapshot().epoch == value.epoch)
                        listener.onChanged(value);
                });
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void publish() {
        Snapshot value = snapshot();
        main.post(
                () -> {
                    if (snapshot().epoch == value.epoch)
                        for (Listener listener : listeners) listener.onChanged(value);
                });
    }

    public static ApiFailure changed() {
        return new ApiFailure(
                ApiFailure.Kind.SESSION_CHANGED, "La sesión cambió. Vuelve a abrir esta pantalla.");
    }
}
