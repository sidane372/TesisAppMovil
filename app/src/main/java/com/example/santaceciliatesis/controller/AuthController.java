package com.example.santaceciliatesis.controller;

import android.util.Patterns;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;
import com.example.santaceciliatesis.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AuthController {
    public interface View {
        void onBusy(boolean busy);

        void onError(ApiFailure error);

        void onLoggedIn();

        void onRegistered();

        void onRecoveryRequested(String message);
    }

    private final AuthRepository repository;
    private View view;
    private RequestHandle pending;
    private boolean busy;

    public AuthController(AuthRepository repository) {
        this.repository = repository;
    }

    public void attach(View value) {
        view = value;
    }

    public void detach() {
        if (pending != null) pending.cancel();
        view = null;
        busy = false;
    }

    private boolean validate(
            String correo,
            String password,
            boolean registration,
            String nombres,
            String apellidos,
            boolean privacy) {
        if (view == null || busy) return false;
        Map<String, String> errors = new LinkedHashMap<>();
        if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()
                || correo.trim().length() > 254) errors.put("correo", "Ingresa un correo válido.");
        if (password != null) {
            int bytes = password.getBytes(StandardCharsets.UTF_8).length;
            if (password.isEmpty() || bytes > 72)
                errors.put("password", "Ingresa tu contraseña (máximo 72 bytes UTF-8).");
            if (registration
                    && (password.codePointCount(0, password.length()) < 12
                            || password.codePoints().anyMatch(Character::isISOControl)))
                errors.put("password", "Usa al menos 12 caracteres, sin caracteres de control.");
        }
        if (registration) {
            if (nombres.trim().isEmpty() || nombres.trim().length() > 100)
                errors.put("nombres", "Ingresa tus nombres.");
            if (apellidos.trim().isEmpty() || apellidos.trim().length() > 120)
                errors.put("apellidos", "Ingresa tus apellidos.");
            if (!privacy)
                errors.put(
                        "aceptaPrivacidad", "Acepta el aviso de privacidad para crear tu cuenta.");
        }
        if (!errors.isEmpty()) {
            view.onError(
                    new ApiFailure(
                            ApiFailure.Kind.HTTP,
                            400,
                            "Revisa los campos indicados.",
                            errors,
                            false));
            return false;
        }
        return true;
    }

    private void start() {
        busy = true;
        view.onBusy(true);
    }

    private void finish() {
        busy = false;
        if (view != null) view.onBusy(false);
    }

    public void login(String correo, String password) {
        if (!validate(correo, password, false, "", "", false)) return;
        start();
        pending =
                repository.login(
                        correo.trim(),
                        password,
                        new Callback<Usuario>() {
                            public void onSuccess(Usuario user) {
                                finish();
                                if (view != null) view.onLoggedIn();
                            }

                            public void onFailure(ApiFailure error) {
                                finish();
                                if (view != null) view.onError(error);
                            }
                        });
    }

    public void registrar(
            String nombres, String apellidos, String correo, String password, boolean acepta) {
        if (!validate(correo, password, true, nombres, apellidos, acepta)) return;
        start();
        pending =
                repository.registrar(
                        nombres.trim(),
                        apellidos.trim(),
                        correo.trim(),
                        password,
                        acepta,
                        new Callback<Usuario>() {
                            public void onSuccess(Usuario user) {
                                finish();
                                if (view != null) view.onRegistered();
                            }

                            public void onFailure(ApiFailure error) {
                                finish();
                                if (view != null) view.onError(error);
                            }
                        });
    }

    public void recuperar(String correo) {
        if (!validate(correo, null, false, "", "", false)) return;
        start();
        pending =
                repository.recuperar(
                        correo.trim(),
                        new Callback<String>() {
                            public void onSuccess(String message) {
                                finish();
                                if (view != null) view.onRecoveryRequested(message);
                            }

                            public void onFailure(ApiFailure error) {
                                finish();
                                if (view != null) view.onError(error);
                            }
                        });
    }
}
