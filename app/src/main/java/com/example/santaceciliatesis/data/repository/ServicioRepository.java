package com.example.santaceciliatesis.data.repository;

import android.os.Handler;

import com.example.santaceciliatesis.data.local.dao.ServicioDao;
import com.example.santaceciliatesis.data.local.mapper.ServicioCacheMapper;
import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.remote.mapper.*;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Resource;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public final class ServicioRepository {
    public interface CatalogoCallback {
        void onResult(Resource<List<ServicioMusical>> result);
    }

    private final ServicioDao dao;
    private final ApiClient api;
    private final ExecutorService io;
    private final Handler main;
    private final AtomicLong revision = new AtomicLong();
    private final Object cacheLock = new Object();

    public ServicioRepository(ServicioDao dao, ApiClient api, ExecutorService io, Handler main) {
        this.dao = dao;
        this.api = api;
        this.io = io;
        this.main = main;
    }

    public RequestHandle cargar(CatalogoCallback callback) {
        RequestHandle operation = new RequestHandle();
        long current = revision.incrementAndGet();
        io.execute(
                () -> {
                    List<ServicioMusical> cache = Collections.emptyList();
                    try {
                        synchronized (cacheLock) {
                            cache = ServicioCacheMapper.toDomain(dao.leerCatalogo(api.getOrigin()));
                        }
                    } catch (RuntimeException ignored) {
                    }
                    if (operation.isCancelled()) return;
                    List<ServicioMusical> previous = cache;
                    if (!api.isConfigured()) {
                        deliver(operation, callback, Resource.State.NOT_CONFIGURED, previous);
                        return;
                    }
                    if (!cache.isEmpty()) deliver(operation, callback, Resource.State.CACHE, cache);
                    try {
                        List<ServicioMusical> fresh =
                                ServicioJsonMapper.parse(
                                        api.request(
                                                "GET",
                                                ApiEndpoints.SERVICIOS,
                                                null,
                                                null,
                                                operation));
                        if (operation.isCancelled() || revision.get() != current) return;
                        try {
                            synchronized (cacheLock) {
                                if (revision.get() != current || operation.isCancelled()) return;
                                dao.guardarCatalogo(
                                        api.getOrigin(),
                                        ServicioCacheMapper.toCache(
                                                fresh, System.currentTimeMillis()),
                                        System.currentTimeMillis());
                            }
                        } catch (RuntimeException error) {
                            deliver(operation, callback, Resource.State.LOCAL_ERROR, fresh);
                            return;
                        }
                        deliver(operation, callback, Resource.State.REMOTE, fresh);
                    } catch (ApiFailure error) {
                        if (!operation.isCancelled())
                            deliver(
                                    operation,
                                    callback,
                                    error.getKind() == ApiFailure.Kind.LOCAL_NETWORK_PERMISSION
                                            ? Resource.State.LOCAL_PERMISSION
                                            : Resource.State.NETWORK_ERROR,
                                    previous);
                    } catch (Exception error) {
                        deliver(operation, callback, Resource.State.INVALID_DATA, previous);
                    }
                });
        return operation;
    }

    private void deliver(
            RequestHandle operation,
            CatalogoCallback callback,
            Resource.State state,
            List<ServicioMusical> data) {
        operation.post(main, () -> callback.onResult(new Resource<>(state, data)));
    }

    public RequestHandle detalle(long id, Callback<ServicioDetalle> callback) {
        RequestHandle operation = new RequestHandle();
        io.execute(
                () -> {
                    try {
                        ServicioDetalle value =
                                ServicioDetalleMapper.parse(
                                        api.request(
                                                "GET",
                                                ApiEndpoints.detalleServicio(id),
                                                null,
                                                null,
                                                operation));
                        operation.post(main, () -> callback.onSuccess(value));
                    } catch (ApiFailure error) {
                        operation.post(main, () -> callback.onFailure(error));
                    } catch (Exception error) {
                        ApiFailure failure =
                                new ApiFailure(
                                        ApiFailure.Kind.INVALID_DATA,
                                        "No pudimos mostrar el perfil del servicio.");
                        operation.post(main, () -> callback.onFailure(failure));
                    }
                });
        return operation;
    }

    public void cancelar(RequestHandle operation) {
        operation.cancel();
    }
}
