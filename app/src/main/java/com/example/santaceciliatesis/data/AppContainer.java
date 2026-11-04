package com.example.santaceciliatesis.data;

import android.content.Context;
import android.os.*;

import com.example.santaceciliatesis.BuildConfig;
import com.example.santaceciliatesis.data.local.AppDatabase;
import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;
import com.example.santaceciliatesis.service.*;

import java.util.concurrent.*;

/** Composición de dependencias con alcance de aplicación, sin retener Activities. */
public final class AppContainer {
    private final ComparacionStore comparacion = new ComparacionStore();
    private final ComunidadRepository comunidad;
    private final ContratacionRepository contratacion;
    private final ServicioRepository servicios;
    private final AuthRepository auth;
    private final SessionManager sessions;

    public AppContainer(Context context) {
        Handler main = new Handler(Looper.getMainLooper());
        ExecutorService io = Executors.newFixedThreadPool(3);
        ApiClient api = new ApiClient(context, new ApiConfig(BuildConfig.API_BASE_URL));
        sessions = new SessionManager(new SessionStore(context, api.getOrigin()), main);
        io.execute(sessions::restore);
        AuthenticatedApiClient secured = new AuthenticatedApiClient(api, sessions);
        auth = new AuthRepository(api, secured, sessions, io, main);
        comunidad = new ComunidadRepository(secured, sessions, io, main);
        contratacion = new ContratacionRepository(api, secured, sessions, io, main);
        servicios =
                new ServicioRepository(
                        AppDatabase.getInstance(context).servicioDao(), api, io, main);
    }

    public ContratacionRepository getContratacionRepository() {
        return contratacion;
    }

    public ComunidadRepository getComunidadRepository() {
        return comunidad;
    }

    public ComparacionStore getComparacion() {
        return comparacion;
    }

    public ServicioRepository getServicioRepository() {
        return servicios;
    }

    public AuthRepository getAuthRepository() {
        return auth;
    }

    public SessionManager getSessionManager() {
        return sessions;
    }
}
