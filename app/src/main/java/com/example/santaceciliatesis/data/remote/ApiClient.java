package com.example.santaceciliatesis.data.remote;

import org.json.*;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

/**
 * Transporte HTTPS síncrono: los repositorios lo ejecutan en ExecutorService. Sin reintentos de
 * POST.
 */
public final class ApiClient {
    private final ApiConfig config;
    private final android.content.Context context;

    public ApiClient(android.content.Context context, ApiConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
    }

    public boolean isConfigured() {
        return config.isConfigured();
    }

    public String getOrigin() {
        return config.getBaseUrl();
    }

    public JSONObject request(
            String method, String endpoint, JSONObject body, String bearer, RequestHandle operation)
            throws ApiFailure {
        if (!config.isConfigured())
            throw new ApiFailure(
                    ApiFailure.Kind.NOT_CONFIGURED,
                    "El acceso a Santa Cecilia no está disponible temporalmente.");
        if (com.example.santaceciliatesis.BuildConfig.DEBUG
                && android.os.Build.VERSION.SDK_INT >= 37
                && config.isLocalEndpoint()
                && context.checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new ApiFailure(
                    ApiFailure.Kind.LOCAL_NETWORK_PERMISSION,
                    "Autoriza el acceso a la red local para conectar con Santa Cecilia.");
        if (operation != null && operation.isCancelled()) throw cancelled();
        HttpsURLConnection connection = null;
        boolean sent = false;
        try {
            connection = (HttpsURLConnection) new URL(config.endpoint(endpoint)).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            if (bearer != null) connection.setRequestProperty("Authorization", "Bearer " + bearer);
            if (operation != null) {
                operation.attach(connection);
                if (operation.isCancelled()) throw cancelled();
            }
            if (body != null) {
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream output = connection.getOutputStream()) {
                    sent = true;
                    output.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            if (operation != null && operation.isCancelled()) throw cancelled();
            InputStream stream =
                    status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = read(stream);
            JSONObject json;
            try {
                json = text.isEmpty() ? new JSONObject() : new JSONObject(text);
            } catch (JSONException e) {
                throw new ApiFailure(
                        ApiFailure.Kind.INVALID_DATA,
                        status,
                        "No pudimos interpretar la respuesta del servicio.",
                        Collections.emptyMap(),
                        sent);
            }
            if (status < 200 || status >= 300) {
                Map<String, String> fields = new LinkedHashMap<>();
                JSONObject fieldErrors = json.optJSONObject("fieldErrors");
                if (fieldErrors != null) {
                    Iterator<String> keys = fieldErrors.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = fieldErrors.optString(key, "");
                        if (value.length() <= 300) fields.put(key, value);
                    }
                }
                String message = json.optString("message", "No pudimos completar la operación.");
                if (message.length() > 500) message = "No pudimos completar la operación.";
                throw new ApiFailure(ApiFailure.Kind.HTTP, status, message, fields, sent);
            }
            return json;
        } catch (ApiFailure e) {
            throw e;
        } catch (SSLException e) {
            throw new ApiFailure(
                    ApiFailure.Kind.TLS,
                    0,
                    "No pudimos establecer una conexión segura con Santa Cecilia.",
                    Collections.emptyMap(),
                    sent);
        } catch (IOException e) {
            if (operation != null && operation.isCancelled()) throw cancelled();
            throw new ApiFailure(
                    ApiFailure.Kind.NETWORK,
                    0,
                    com.example.santaceciliatesis.BuildConfig.DEBUG && config.isLocalEndpoint()
                            ? "Servidor local no disponible. Ejecuta INICIAR_SANTA_CECILIA.cmd en"
                                  + " la computadora y vuelve a intentarlo."
                            : "No pudimos conectar con Santa Cecilia. Vuelve a intentarlo en un"
                                  + " momento.",
                    Collections.emptyMap(),
                    sent);
        } finally {
            if (connection != null) {
                if (operation != null) operation.detach(connection);
                connection.disconnect();
            }
        }
    }

    private static ApiFailure cancelled() {
        return new ApiFailure(ApiFailure.Kind.CANCELLED, "");
    }

    private static String read(InputStream stream) throws IOException, ApiFailure {
        if (stream == null) return "";
        try (InputStream input = stream;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > 2 * 1024 * 1024)
                    throw new ApiFailure(
                            ApiFailure.Kind.INVALID_DATA,
                            "La respuesta es demasiado grande. Vuelve a intentarlo más adelante.");
                bytes.write(buffer, 0, count);
            }
            return bytes.toString(StandardCharsets.UTF_8.name());
        }
    }
}
