package com.example.santaceciliatesis.data.repository;

import android.os.Handler;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.remote.mapper.*;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.service.SessionManager;

import org.json.*;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Operaciones privadas sin caché persistente. El servidor valida rol y propiedad en cada llamada.
 */
public final class ComunidadRepository {
    private final AuthenticatedApiClient api;
    private final SessionManager sessions;
    private final ExecutorService io;
    private final Handler main;

    public ComunidadRepository(
            AuthenticatedApiClient api, SessionManager sessions, ExecutorService io, Handler main) {
        this.api = api;
        this.sessions = sessions;
        this.io = io;
        this.main = main;
    }

    private interface Work<T> {
        T run(RequestHandle task) throws Exception;
    }

    private <T> RequestHandle run(Callback<T> callback, Work<T> work) {
        RequestHandle task = new RequestHandle();
        io.execute(
                () -> {
                    try {
                        sessions.awaitReady();
                        if (task.isCancelled()) return;
                        long epoch = sessions.snapshot().epoch;
                        T value = work.run(task);
                        task.post(
                                main,
                                () -> {
                                    if (sessions.snapshot().epoch == epoch)
                                        callback.onSuccess(value);
                                    else callback.onFailure(SessionManager.changed());
                                });
                    } catch (ApiFailure error) {
                        task.post(main, () -> callback.onFailure(error));
                    } catch (Exception error) {
                        task.post(
                                main,
                                () ->
                                        callback.onFailure(
                                                new ApiFailure(
                                                        ApiFailure.Kind.INVALID_DATA,
                                                        "No pudimos interpretar la información."
                                                                + " Vuelve a consultarla.")));
                    }
                });
        return task;
    }

    public RequestHandle cuentasVigentes(Callback<Long> callback) {
        return run(
                callback,
                task -> {
                    long count = 0;
                    int page = 0;
                    while (true) {
                        JSONObject data =
                                api.request(
                                                "GET",
                                                ApiEndpoints.pagina(
                                                        ApiEndpoints.USUARIOS, page, 100),
                                                null,
                                                task)
                                        .getJSONObject("data");
                        JSONArray rows = data.getJSONArray("items");
                        for (int row = 0; row < rows.length(); row++)
                            if (!"DESACTIVADO".equals(rows.getJSONObject(row).getString("estado")))
                                count++;
                        if (rows.length() == 0 || ((long) ++page) * 100 >= data.getLong("total"))
                            break;
                    }
                    return count;
                });
    }

    public RequestHandle usuarios(int pagina, int tamano, Callback<Pagina<Usuario>> callback) {
        return run(
                callback,
                t -> {
                    JSONObject data =
                            api.request(
                                            "GET",
                                            ApiEndpoints.pagina(
                                                    ApiEndpoints.USUARIOS, pagina, tamano),
                                            null,
                                            t)
                                    .getJSONObject("data");
                    List<Usuario> items = new ArrayList<>();
                    JSONArray rows = data.getJSONArray("items");
                    for (int i = 0; i < rows.length(); i++)
                        items.add(AuthJsonMapper.usuario(rows.getJSONObject(i)));
                    return new Pagina<>(
                            items,
                            data.getInt("pagina"),
                            data.getInt("tamano"),
                            data.getLong("total"));
                });
    }

    public RequestHandle usuario(long id, Callback<Usuario> callback) {
        return run(
                callback,
                t ->
                        AuthJsonMapper.usuario(
                                api.request("GET", ApiEndpoints.USUARIOS + "/" + id, null, t)
                                        .getJSONObject("data")));
    }

    public RequestHandle responsables(Callback<List<Usuario>> callback) {
        return run(
                callback,
                t -> {
                    List<Usuario> result = new ArrayList<>();
                    int page = 0;
                    while (true) {
                        JSONObject data =
                                api.request(
                                                "GET",
                                                ApiEndpoints.pagina(
                                                        ApiEndpoints.USUARIOS, page, 100),
                                                null,
                                                t)
                                        .getJSONObject("data");
                        JSONArray rows = data.getJSONArray("items");
                        for (int i = 0; i < rows.length(); i++) {
                            Usuario u = AuthJsonMapper.usuario(rows.getJSONObject(i));
                            if ("ACTIVO".equals(u.getEstado())
                                    && (u.getRol() == Rol.DIRECTOR || u.getRol() == Rol.SOLISTA))
                                result.add(u);
                        }
                        if (rows.length() == 0 || ((long) ++page) * 100 >= data.getLong("total"))
                            break;
                    }
                    return result;
                });
    }

    public RequestHandle servicios(
            boolean admin, int pagina, Callback<Pagina<ServicioDetalle>> callback) {
        return run(
                callback,
                t -> {
                    JSONObject response =
                            api.request(
                                    "GET",
                                    admin
                                            ? ApiEndpoints.pagina(
                                                    ApiEndpoints.ADMIN_SERVICIOS, pagina, 20)
                                            : ApiEndpoints.PROPIOS,
                                    null,
                                    t);
                    JSONObject data = admin ? response.getJSONObject("data") : null;
                    JSONArray rows =
                            admin ? data.getJSONArray("items") : response.getJSONArray("data");
                    List<ServicioDetalle> result = new ArrayList<>();
                    for (int i = 0; i < rows.length(); i++)
                        result.add(ServicioDetalleMapper.objeto(rows.getJSONObject(i)));
                    return new Pagina<>(
                            result,
                            admin ? data.getInt("pagina") : 0,
                            admin ? data.getInt("tamano") : Math.max(1, result.size()),
                            admin ? data.getLong("total") : result.size());
                });
    }

    public RequestHandle servicio(long id, Callback<ServicioDetalle> callback) {
        return run(
                callback,
                t ->
                        ServicioDetalleMapper.parse(
                                api.request("GET", ApiEndpoints.detalleServicio(id), null, t)));
    }

    public RequestHandle cambiarEstado(
            Usuario u, String estado, String motivo, Callback<Usuario> callback) {
        return cambiarEstado(u, estado, motivo, false, callback);
    }

    public RequestHandle cambiarEstado(
            Usuario u, String estado, String motivo, boolean eliminar, Callback<Usuario> callback) {
        return run(
                callback,
                t ->
                        AuthJsonMapper.usuario(
                                api.request(
                                                eliminar ? "DELETE" : "PATCH",
                                                ApiEndpoints.USUARIOS + "/" + u.getId() + (eliminar ? "" : "/estado"),
                                                new JSONObject()
                                                        .put("version", u.getVersion())
                                                        .put("estado", eliminar ? null : estado)
                                                        .put("motivo", motivo),
                                                t)
                                        .getJSONObject("data")));
    }

    public RequestHandle publicar(
            ServicioDetalle s, String estado, String motivo, Callback<ServicioDetalle> callback) {
        return run(
                callback,
                t ->
                        ServicioDetalleMapper.parse(
                                api.request(
                                        "POST",
                                        ApiEndpoints.ADMIN_SERVICIOS
                                                + "/"
                                                + s.resumen.getId()
                                                + "/publicacion",
                                        new JSONObject()
                                                .put("version", s.version)
                                                .put("estado", estado)
                                                .put("motivo", motivo),
                                        t)));
    }

    public RequestHandle retirarCancion(long servicio, long cancion, Callback<Boolean> callback) {
        return run(
                callback,
                t -> {
                    api.request(
                            "DELETE",
                            ApiEndpoints.detalleServicio(servicio) + "/repertorio/" + cancion,
                            null,
                            t);
                    return true;
                });
    }

    public RequestHandle guardar(
            String method,
            String endpoint,
            JSONObject body,
            boolean perfil,
            Callback<Boolean> callback) {
        return run(
                callback,
                t -> {
                    long epoch = sessions.snapshot().epoch;
                    JSONObject response = api.request(method, endpoint, body, t);
                    if (perfil)
                        sessions.usuario(
                                epoch, AuthJsonMapper.usuario(response.getJSONObject("data")));
                    return true;
                });
    }
}
