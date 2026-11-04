package com.example.santaceciliatesis.controller;

import android.util.Patterns;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;
import com.example.santaceciliatesis.model.*;

import org.json.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Formularios de edición sobre contratos efectivos de la API, con versión original y sin reintentos
 * POST.
 */
public final class FormularioController {
    public interface View {
        void preparando();

        void preparar(Map<String, String> fields, List<Usuario> responsables);

        void busy(boolean value);

        void error(ApiFailure value);

        void guardado();
    }

    private final String modo;
    private final long id, cancionId;
    private final ComunidadRepository repo;
    private final AuthRepository auth;
    private View view;
    private RequestHandle task;
    private Usuario user;
    private ServicioDetalle servicio;
    private boolean saving;
    private int generation;

    public FormularioController(
            String modo, long id, long cancion, ComunidadRepository repo, AuthRepository auth) {
        this.modo = modo;
        this.id = id;
        this.cancionId = cancion;
        this.repo = repo;
        this.auth = auth;
    }

    public void attach(View value) {
        view = value;
    }

    public void detach() {
        generation++;
        if (task != null) task.cancel();
        view = null;
        saving = false;
    }

    private <T> Callback<T> cb(java.util.function.Consumer<T> success) {
        int expected = generation;
        return new Callback<T>() {
            public void onSuccess(T value) {
                if (view != null && expected == generation) success.accept(value);
            }

            public void onFailure(ApiFailure error) {
                if (view != null && expected == generation) {
                    saving = false;
                    view.busy(false);
                    view.error(error);
                }
            }
        };
    }

    public void cargar() {
        if (view == null || saving) return;
        if (task != null) task.cancel();
        generation++;
        view.preparando();
        if ("USUARIO_NUEVO".equals(modo)) {
            view.preparar(Collections.emptyMap(), Collections.emptyList());
            return;
        }
        if ("PERFIL".equals(modo)) {
            task =
                    auth.perfil(
                            cb(
                                    u -> {
                                        user = u;
                                        view.preparar(usuario(u), Collections.emptyList());
                                    }));
            return;
        }
        if ("USUARIO_EDITAR".equals(modo)) {
            task =
                    repo.usuario(
                            id,
                            cb(
                                    u -> {
                                        user = u;
                                        view.preparar(usuario(u), Collections.emptyList());
                                    }));
            return;
        }
        if ("SERVICIO_NUEVO".equals(modo)) {
            task = repo.responsables(cb(rows -> view.preparar(Collections.emptyMap(), rows)));
            return;
        }
        task =
                repo.servicio(
                        id,
                        cb(
                                s -> {
                                    servicio = s;
                                    Map<String, String> fields = new LinkedHashMap<>();
                                    fields.put("_version", Long.toString(s.version));
                                    fields.put("nombre", s.resumen.getNombre());
                                    fields.put("ciudad", s.resumen.getCiudad());
                                    fields.put("generos", String.join(", ", s.generos));
                                    fields.put("instrumentos", String.join(", ", s.instrumentos));
                                    fields.put("tiposEvento", String.join(", ", s.tiposEvento));
                                    fields.put("experiencia", s.experiencia);
                                    fields.put("incluye", s.incluye);
                                    fields.put(
                                            "tarifa",
                                            s.resumen.getTarifaCentimos() == null
                                                    ? ""
                                                    : BigDecimal.valueOf(
                                                                    s.resumen.getTarifaCentimos(),
                                                                    2)
                                                            .toPlainString());
                                    if ("CANCION".equals(modo) && cancionId > 0) {
                                        Cancion found = null;
                                        for (Cancion c : s.canciones)
                                            if (c.id == cancionId) found = c;
                                        if (found == null) {
                                            view.error(
                                                    new ApiFailure(
                                                            ApiFailure.Kind.HTTP,
                                                            "La canción ya no está disponible."
                                                                    + " Vuelve al servicio."));
                                            return;
                                        }
                                        fields.clear();
                                        fields.put("_version", Long.toString(s.version));
                                        fields.put("titulo", found.titulo);
                                        fields.put("genero", found.genero);
                                        fields.put("descripcion", found.descripcion);
                                        fields.put(
                                                "duracion",
                                                found.duracionSegundos == null
                                                        ? ""
                                                        : found.duracionSegundos.toString());
                                        fields.put("orden", Integer.toString(found.orden));
                                    }
                                    view.preparar(fields, Collections.emptyList());
                                }));
    }

    private Map<String, String> usuario(Usuario u) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("_version", Long.toString(u.getVersion()));
        f.put("nombres", u.getNombres());
        f.put("apellidos", u.getApellidos());
        f.put("telefono", u.getTelefono());
        f.put("ciudad", u.getCiudad());
        f.put("biografia", u.getBiografia());
        return f;
    }

    private static String text(Map<String, String> values, String key) {
        return values.getOrDefault(key, "").trim();
    }

    private static void required(Map<String, String> errors, String key, String value, int max) {
        if (value.isEmpty() || value.length() > max)
            errors.put(key, "Completa este campo (máximo " + max + " caracteres).");
    }

    private static JSONArray listas(String value, int max, int length) {
        LinkedHashSet<String> list = new LinkedHashSet<>();
        if (!value.trim().isEmpty())
            for (String item : value.split(",")) {
                String v = item.trim();
                if (v.isEmpty() || v.length() > length)
                    throw new IllegalArgumentException("Revisa las listas separadas por comas.");
                list.add(v);
            }
        if (list.size() > max)
            throw new IllegalArgumentException("La lista supera " + max + " elementos.");
        return new JSONArray(list);
    }

    private static Object nullable(String v) {
        return v.isEmpty() ? JSONObject.NULL : v;
    }

    private static Object tarifa(String value) {
        if (value.isEmpty()) return JSONObject.NULL;
        try {
            long c = new BigDecimal(value.replace(',', '.')).movePointRight(2).longValueExact();
            if (c < 0 || c > 999999999999L) throw new ArithmeticException();
            return c;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Ingresa una tarifa válida en soles con hasta dos decimales.");
        }
    }

    public void guardar(Map<String, String> values, Usuario responsable, String rol) {
        if (view == null || saving) return;
        Map<String, String> errors = new LinkedHashMap<>();
        JSONObject body = new JSONObject();
        String endpoint, method;
        boolean perfil = "PERFIL".equals(modo);
        try {
            if (perfil || "USUARIO_EDITAR".equals(modo) || "USUARIO_NUEVO".equals(modo)) {
                String nombres = text(values, "nombres"), apellidos = text(values, "apellidos");
                required(errors, "nombres", nombres, 100);
                required(errors, "apellidos", apellidos, 120);
                body.put("nombres", nombres).put("apellidos", apellidos);
                if ("USUARIO_NUEVO".equals(modo)) {
                    String email = text(values, "correo"),
                            secret = values.getOrDefault("password", "");
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length() > 254)
                        errors.put("correo", "Ingresa un correo válido.");
                    if (secret.codePointCount(0, secret.length()) < 12
                            || secret.getBytes(StandardCharsets.UTF_8).length > 72
                            || secret.codePoints().anyMatch(Character::isISOControl))
                        errors.put(
                                "password",
                                "Usa al menos 12 caracteres y hasta 72 bytes UTF-8, sin"
                                        + " controles.");
                    if (!"DIRECTOR".equals(rol) && !"SOLISTA".equals(rol))
                        throw new IllegalArgumentException("Selecciona un rol musical.");
                    body.put("correo", email).put("password", secret).put("rol", rol);
                    endpoint = ApiEndpoints.USUARIOS;
                    method = "POST";
                } else {
                    if (user == null) return;
                    body.put("version", user.getVersion())
                            .put("telefono", nullable(text(values, "telefono")))
                            .put("ciudad", nullable(text(values, "ciudad")))
                            .put("biografia", nullable(text(values, "biografia")));
                    endpoint = perfil ? ApiEndpoints.ME : ApiEndpoints.USUARIOS + "/" + id;
                    method = perfil ? "PATCH" : "PUT";
                }
            } else if ("SERVICIO_NUEVO".equals(modo) || "SERVICIO_EDITAR".equals(modo)) {
                String nombre = text(values, "nombre"), ciudad = text(values, "ciudad");
                required(errors, "nombre", nombre, 160);
                required(errors, "ciudad", ciudad, 100);
                if ("SERVICIO_NUEVO".equals(modo) && responsable == null)
                    throw new IllegalArgumentException("Selecciona un director o solista activo.");
                if ("SERVICIO_EDITAR".equals(modo) && servicio == null) return;
                long owner = servicio == null ? responsable.getId() : servicio.responsableId;
                String tipo =
                        servicio == null
                                ? (responsable.getRol() == Rol.DIRECTOR ? "BANDA" : "SOLISTA")
                                : servicio.resumen.getTipo().name();
                body.put("responsableId", owner)
                        .put("tipo", tipo)
                        .put("nombre", nombre)
                        .put("ciudad", ciudad)
                        .put("generos", listas(text(values, "generos"), 30, 80))
                        .put("instrumentos", listas(text(values, "instrumentos"), 40, 80))
                        .put("tiposEvento", listas(text(values, "tiposEvento"), 30, 100))
                        .put("experiencia", nullable(text(values, "experiencia")))
                        .put("incluye", nullable(text(values, "incluye")))
                        .put("tarifaCentimos", tarifa(text(values, "tarifa")))
                        .put(
                                "descripcionHistorica",
                                servicio == null
                                        ? JSONObject.NULL
                                        : nullable(servicio.descripcion));
                if (servicio != null) body.put("version", servicio.version);
                endpoint =
                        servicio == null
                                ? ApiEndpoints.ADMIN_SERVICIOS
                                : ApiEndpoints.ADMIN_SERVICIOS + "/" + id;
                method = servicio == null ? "POST" : "PUT";
            } else if ("TARIFA".equals(modo)) {
                if (servicio == null) return;
                body.put("version", servicio.version)
                        .put("tarifaCentimos", tarifa(text(values, "tarifa")));
                endpoint = ApiEndpoints.detalleServicio(id) + "/tarifa";
                method = "PATCH";
            } else {
                if (servicio == null) return;
                String titulo = text(values, "titulo"), genero = text(values, "genero");
                required(errors, "titulo", titulo, 140);
                required(errors, "genero", genero, 80);
                body.put("titulo", titulo)
                        .put("genero", genero)
                        .put("descripcion", nullable(text(values, "descripcion")));
                String duration = text(values, "duracion"), order = text(values, "orden");
                Integer seconds = null, sort = null;
                try {
                    if (!duration.isEmpty()) {
                        seconds = Integer.valueOf(duration);
                        if (seconds < 1 || seconds > 36000) throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    errors.put("duracion", "Usa segundos enteros entre 1 y 36000.");
                }
                try {
                    if (!order.isEmpty()) {
                        sort = Integer.valueOf(order);
                        if (sort < 0) throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    errors.put("orden", "Usa un número entero desde cero.");
                }
                body.put("duracionSegundos", seconds == null ? JSONObject.NULL : seconds)
                        .put("orden", sort == null ? JSONObject.NULL : sort);
                endpoint =
                        ApiEndpoints.detalleServicio(id)
                                + "/repertorio"
                                + (cancionId > 0 ? "/" + cancionId : "");
                method = cancionId > 0 ? "PUT" : "POST";
            }
            if (!errors.isEmpty()) {
                view.error(
                        new ApiFailure(
                                ApiFailure.Kind.HTTP,
                                400,
                                "Revisa los campos indicados.",
                                errors,
                                false));
                return;
            }
            saving = true;
            view.busy(true);
            task =
                    repo.guardar(
                            method,
                            endpoint,
                            body,
                            perfil,
                            cb(
                                    ok -> {
                                        saving = false;
                                        view.busy(false);
                                        view.guardado();
                                    }));
        } catch (IllegalArgumentException | JSONException e) {
            view.error(
                    new ApiFailure(
                            ApiFailure.Kind.HTTP,
                            e.getMessage() == null
                                    ? "Revisa los datos del formulario."
                                    : e.getMessage()));
        }
    }
}
