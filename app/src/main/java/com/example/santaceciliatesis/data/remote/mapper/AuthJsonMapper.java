package com.example.santaceciliatesis.data.remote.mapper;

import com.example.santaceciliatesis.model.*;

import org.json.*;

public final class AuthJsonMapper {
    private AuthJsonMapper() {}

    public static Usuario usuario(JSONObject json) throws JSONException {
        try {
            return new Usuario(
                    json.getLong("id"),
                    json.getString("nombres"),
                    json.getString("apellidos"),
                    json.getString("correo"),
                    Rol.valueOf(json.getString("rol")),
                    json.getString("estado"),
                    json.getLong("version"),
                    json.optString("telefono", "").equals("null")
                            ? ""
                            : json.optString("telefono", ""),
                    json.optString("ciudad", "").equals("null") ? "" : json.optString("ciudad", ""),
                    json.optString("biografia", "").equals("null")
                            ? ""
                            : json.optString("biografia", ""));
        } catch (IllegalArgumentException e) {
            throw new JSONException("Perfil de usuario no válido");
        }
    }

    public static Sesion login(JSONObject response) throws JSONException {
        JSONObject data = response.getJSONObject("data");
        String access = data.getString("accessToken"), refresh = data.getString("refreshToken");
        long ttl = data.getLong("expiresIn");
        if (!"Bearer".equals(data.getString("tokenType"))
                || !access.matches("a1\\.[A-Za-z0-9_-]{43}")
                || !refresh.matches("r1\\.[A-Za-z0-9_-]{43}")
                || ttl < 1
                || ttl > 3600) throw new JSONException("Sesión no válida");
        Usuario user = usuario(data.getJSONObject("usuario"));
        if (!"ACTIVO".equals(user.getEstado())) throw new JSONException("Cuenta no activa");
        return new Sesion(access, refresh, System.currentTimeMillis() + ttl * 1000, user);
    }

    public static JSONObject guardar(Sesion session) throws JSONException {
        Usuario u = session.getUsuario();
        JSONObject user =
                new JSONObject()
                        .put("id", u.getId())
                        .put("nombres", u.getNombres())
                        .put("apellidos", u.getApellidos())
                        .put("correo", u.getCorreo())
                        .put("rol", u.getRol().name())
                        .put("estado", u.getEstado())
                        .put("version", u.getVersion());
        return new JSONObject()
                .put("accessToken", session.getAccessToken())
                .put("refreshToken", session.getRefreshToken())
                .put("venceEn", session.getVenceEn())
                .put("usuario", user);
    }

    public static Sesion restaurar(JSONObject json) throws JSONException {
        String access = json.getString("accessToken"), refresh = json.getString("refreshToken");
        if (!access.matches("a1\\.[A-Za-z0-9_-]{43}") || !refresh.matches("r1\\.[A-Za-z0-9_-]{43}"))
            throw new JSONException("Credenciales no válidas");
        return new Sesion(
                access, refresh, json.getLong("venceEn"), usuario(json.getJSONObject("usuario")));
    }
}
