package com.example.santaceciliatesis.controller;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;
import com.example.santaceciliatesis.model.Usuario;
import com.example.santaceciliatesis.service.SessionManager;

public final class CuentaController {
    public interface View {
        void mostrar(Usuario user, boolean validado, boolean cargando);

        void mensaje(String text);

        void cerrada(boolean remoto);
    }

    private final AuthRepository repository;
    private final SessionManager sessions;
    private View view;
    private RequestHandle pending;
    private boolean loading, closing;

    public CuentaController(AuthRepository repository, SessionManager sessions) {
        this.repository = repository;
        this.sessions = sessions;
    }

    public void attach(View value) {
        view = value;
        actualizar();
    }

    public void detach() {
        if (pending != null) pending.cancel();
        view = null;
        loading = false;
    }

    public void actualizar() {
        if (view == null || loading) return;
        SessionManager.Snapshot state = sessions.snapshot();
        if (state.session == null) return;
        loading = true;
        view.mostrar(state.session.getUsuario(), false, true);
        pending =
                repository.perfil(
                        new Callback<Usuario>() {
                            public void onSuccess(Usuario user) {
                                loading = false;
                                if (view != null) view.mostrar(user, true, false);
                            }

                            public void onFailure(ApiFailure error) {
                                loading = false;
                                if (view == null) return;
                                SessionManager.Snapshot saved = sessions.snapshot();
                                if (saved.session != null) {
                                    view.mostrar(saved.session.getUsuario(), false, false);
                                    view.mensaje(error.getMessage());
                                }
                            }
                        });
    }

    public void logout() {
        if (view == null || closing) return;
        closing = true;
        loading = true;
        if (pending != null) pending.cancel();
        SessionManager.Snapshot state = sessions.snapshot();
        if (state.session != null) view.mostrar(state.session.getUsuario(), false, true);
        repository.logout(
                new Callback<Boolean>() {
                    public void onSuccess(Boolean remote) {
                        loading = false;
                        closing = false;
                        if (view != null) view.cerrada(remote);
                    }

                    public void onFailure(ApiFailure error) {
                        loading = false;
                        closing = false;
                        if (view != null) view.mensaje(error.getMessage());
                    }
                });
    }
}
