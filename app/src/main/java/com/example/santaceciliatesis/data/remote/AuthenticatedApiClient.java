package com.example.santaceciliatesis.data.remote;

import com.example.santaceciliatesis.data.remote.mapper.AuthJsonMapper;
import com.example.santaceciliatesis.model.Sesion;
import com.example.santaceciliatesis.service.SessionManager;

import org.json.*;

/**
 * Renovación única compartida. Nunca reenvía automáticamente un refresh cuyo resultado es incierto.
 */
public final class AuthenticatedApiClient {
    private final ApiClient api;
    private final SessionManager sessions;
    private final Object refreshLock = new Object();

    public AuthenticatedApiClient(ApiClient api, SessionManager sessions) {
        this.api = api;
        this.sessions = sessions;
    }

    public JSONObject request(String method, String endpoint, JSONObject body, RequestHandle task)
            throws ApiFailure {
        sessions.awaitReady();
        SessionManager.Snapshot initial = sessions.snapshot();
        if (initial.session == null) throw ApiFailure.session();
        Sesion active = initial.session;
        if (active.getVenceEn() <= System.currentTimeMillis() + 30000)
            active = renew(initial.epoch, active.getAccessToken(), false);
        JSONObject response;
        try {
            response = api.request(method, endpoint, body, active.getAccessToken(), task);
        } catch (ApiFailure error) {
            if (error.getStatus() != 401) throw error;
            active = renew(initial.epoch, active.getAccessToken(), true);
            try {
                response = api.request(method, endpoint, body, active.getAccessToken(), task);
            } catch (ApiFailure retry) {
                if (retry.getStatus() == 401) {
                    sessions.clear(initial.epoch);
                    throw ApiFailure.session();
                }
                throw retry;
            }
        }
        if (sessions.snapshot().epoch != initial.epoch) throw SessionManager.changed();
        return response;
    }

    private Sesion renew(long expected, String rejectedToken, boolean forced) throws ApiFailure {
        synchronized (refreshLock) {
            SessionManager.Snapshot current = sessions.snapshot();
            if (current.epoch != expected || current.session == null)
                throw SessionManager.changed();
            if (!current.session.getAccessToken().equals(rejectedToken)) return current.session;
            if (!forced && current.session.getVenceEn() > System.currentTimeMillis() + 30000)
                return current.session;
            try {
                JSONObject body =
                        new JSONObject().put("refreshToken", current.session.getRefreshToken());
                // Independiente del ciclo de vida de la vista: completar y persistir esta rotación.
                Sesion next =
                        AuthJsonMapper.login(
                                api.request("POST", ApiEndpoints.REFRESH, body, null, null));
                if (next.getUsuario().getId() != current.session.getUsuario().getId())
                    throw new JSONException("Titular no coincide");
                sessions.refresh(expected, next);
                return next;
            } catch (JSONException error) {
                sessions.clear(expected);
                throw ApiFailure.session();
            } catch (ApiFailure error) {
                if (error.getStatus() == 400
                        || error.getStatus() == 401
                        || error.mayHaveBeenSent()
                        || error.getKind() == ApiFailure.Kind.INVALID_DATA) {
                    sessions.clear(expected);
                    throw ApiFailure.session();
                }
                throw error;
            }
        }
    }
}
