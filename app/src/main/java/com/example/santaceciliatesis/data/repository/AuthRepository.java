package com.example.santaceciliatesis.data.repository;

import android.os.Handler;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.remote.mapper.AuthJsonMapper;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.service.SessionManager;

import org.json.*;

import java.util.concurrent.ExecutorService;

public final class AuthRepository {
    private final ApiClient api;
    private final AuthenticatedApiClient authenticated;
    private final SessionManager sessions;
    private final ExecutorService io;
    private final Handler main;

    public AuthRepository(
            ApiClient api,
            AuthenticatedApiClient authenticated,
            SessionManager sessions,
            ExecutorService io,
            Handler main) {
        this.api = api;
        this.authenticated = authenticated;
        this.sessions = sessions;
        this.io = io;
        this.main = main;
    }

    private interface Work<T> {
        T run(RequestHandle operation) throws Exception;
    }

    private <T> RequestHandle run(Callback<T> callback, Work<T> work) {
        RequestHandle operation = new RequestHandle();
        io.execute(
                () -> {
                    if (operation.isCancelled()) return;
                    try {
                        T value = work.run(operation);
                        operation.post(main, () -> callback.onSuccess(value));
                    } catch (ApiFailure error) {
                        operation.post(main, () -> callback.onFailure(error));
                    } catch (Exception error) {
                        ApiFailure failure =
                                new ApiFailure(
                                        ApiFailure.Kind.INVALID_DATA,
                                        "No pudimos procesar la información recibida. Vuelve a"
                                                + " intentarlo.");
                        operation.post(main, () -> callback.onFailure(failure));
                    }
                });
        return operation;
    }

    public RequestHandle login(String correo, String password, Callback<Usuario> callback) {
        return run(
                callback,
                operation -> {
                    sessions.awaitReady();
                    long epoch = sessions.snapshot().epoch;
                    JSONObject body =
                            new JSONObject().put("correo", correo).put("password", password);
                    Sesion session =
                            AuthJsonMapper.login(
                                    api.request("POST", ApiEndpoints.LOGIN, body, null, operation));
                    if (operation.isCancelled())
                        throw new ApiFailure(ApiFailure.Kind.CANCELLED, "");
                    sessions.login(epoch, session);
                    return session.getUsuario();
                });
    }

    public RequestHandle registrar(
            String nombres,
            String apellidos,
            String correo,
            String password,
            boolean acepta,
            Callback<Usuario> callback) {
        return run(
                callback,
                operation -> {
                    JSONObject body =
                            new JSONObject()
                                    .put("nombres", nombres)
                                    .put("apellidos", apellidos)
                                    .put("correo", correo)
                                    .put("password", password)
                                    .put("aceptaPrivacidad", acepta)
                                    .put("privacidadVersion", "v1");
                    return AuthJsonMapper.usuario(
                            api.request("POST", ApiEndpoints.REGISTRO, body, null, operation)
                                    .getJSONObject("data"));
                });
    }

    public RequestHandle recuperar(String correo, Callback<String> callback) {
        return run(
                callback,
                operation ->
                        api.request(
                                        "POST",
                                        ApiEndpoints.RECUPERAR,
                                        new JSONObject().put("correo", correo),
                                        null,
                                        operation)
                                .getJSONObject("data")
                                .getString("mensaje"));
    }

    public RequestHandle perfil(Callback<Usuario> callback) {
        return run(
                callback,
                operation -> {
                    sessions.awaitReady();
                    long epoch = sessions.snapshot().epoch;
                    Usuario user =
                            AuthJsonMapper.usuario(
                                    authenticated
                                            .request("GET", ApiEndpoints.ME, null, operation)
                                            .getJSONObject("data"));
                    sessions.usuario(epoch, user);
                    return user;
                });
    }

    /**
     * El cierre local termina aunque se abandone la vista; no se conserva el refresh para una cola
     * offline.
     */
    public void logout(Callback<Boolean> callback) {
        io.execute(
                () -> {
                    boolean remote = false;
                    ApiFailure failure = null;
                    try {
                        sessions.awaitReady();
                        SessionManager.Snapshot current = sessions.snapshot();
                        if (current.session != null) {
                            try {
                                authenticated.request("POST", ApiEndpoints.LOGOUT, null, null);
                                remote = true;
                            } catch (ApiFailure ignored) {
                            }
                            sessions.clear(
                                    current.epoch,
                                    remote
                                            ? "Sesión cerrada."
                                            : "Sesión cerrada en este dispositivo. No pudimos"
                                                    + " avisar al servidor.");
                        }
                    } catch (ApiFailure error) {
                        failure = error;
                    }
                    boolean completed = remote;
                    ApiFailure finalError = failure;
                    main.post(
                            () -> {
                                if (finalError == null) callback.onSuccess(completed);
                                else callback.onFailure(finalError);
                            });
                });
    }
}
