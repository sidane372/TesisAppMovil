package com.example.santaceciliatesis.data.repository;

import android.os.Handler;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.service.SessionManager;

import org.json.*;

import java.util.concurrent.ExecutorService;

/** Datos contractuales exclusivamente en memoria; no hay cola de escrituras sin conexión. */
public final class ContratacionRepository {
    private final ApiClient publicApi;
    private final AuthenticatedApiClient api;
    private final SessionManager sessions;
    private final ExecutorService io;
    private final Handler main;

    public ContratacionRepository(
            ApiClient publicApi,
            AuthenticatedApiClient api,
            SessionManager sessions,
            ExecutorService io,
            Handler main) {
        this.publicApi = publicApi;
        this.api = api;
        this.sessions = sessions;
        this.io = io;
        this.main = main;
    }

    public RequestHandle request(
            String method,
            String path,
            JSONObject body,
            boolean publica,
            Callback<JSONObject> callback) {
        RequestHandle task = new RequestHandle();
        io.execute(
                () -> {
                    try {
                        sessions.awaitReady();
                        long epoch = sessions.snapshot().epoch;
                        JSONObject response =
                                publica
                                        ? publicApi.request(method, path, body, null, task)
                                        : api.request(method, path, body, task);
                        JSONObject value = response.optJSONObject("data");
                        if (value == null) {
                            value = new JSONObject();
                            if (response.optJSONArray("data") != null)
                                value.put("servicios", response.getJSONArray("data"));
                        }
                        final JSONObject result = value;
                        task.post(
                                main,
                                () -> {
                                    if (sessions.snapshot().epoch == epoch)
                                        callback.onSuccess(result);
                                    else callback.onFailure(SessionManager.changed());
                                });
                    } catch (ApiFailure e) {
                        task.post(main, () -> callback.onFailure(e));
                    } catch (Exception e) {
                        task.post(
                                main,
                                () ->
                                        callback.onFailure(
                                                new ApiFailure(
                                                        ApiFailure.Kind.INVALID_DATA,
                                                        "No pudimos interpretar la respuesta."
                                                                + " Actualiza la información.")));
                    }
                });
        return task;
    }

    public RequestHandle prepararImagen(
            android.content.Context context, android.net.Uri uri, Callback<String> callback) {
        RequestHandle task = new RequestHandle();
        io.execute(
                () -> {
                    android.graphics.Bitmap bitmap = null;
                    try {
                        sessions.awaitReady();
                        long epoch = sessions.snapshot().epoch;
                        if (uri == null || !"content".equals(uri.getScheme()))
                            throw new Exception();
                        java.io.ByteArrayOutputStream input = new java.io.ByteArrayOutputStream();
                        try (java.io.InputStream stream =
                                context.getContentResolver().openInputStream(uri)) {
                            if (stream == null) throw new Exception();
                            byte[] buffer = new byte[8192];
                            int n;
                            while ((n = stream.read(buffer)) != -1) {
                                if (task.isCancelled()) return;
                                if (input.size() + n > 10 * 1024 * 1024) throw new Exception();
                                input.write(buffer, 0, n);
                            }
                        }
                        byte[] raw = input.toByteArray();
                        android.graphics.BitmapFactory.Options options =
                                new android.graphics.BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
                        if (!java.util.Arrays.asList("image/jpeg", "image/png")
                                        .contains(options.outMimeType)
                                || options.outWidth <= 0
                                || options.outHeight <= 0
                                || options.outWidth > 20000
                                || options.outHeight > 20000) throw new Exception();
                        int sample = 1;
                        while (Math.max(options.outWidth, options.outHeight) / sample > 1600)
                            sample *= 2;
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = sample;
                        bitmap =
                                android.graphics.BitmapFactory.decodeByteArray(
                                        raw, 0, raw.length, options);
                        if (bitmap == null) throw new Exception();
                        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, output);
                        if (output.size() > 1048576) {
                            output.reset();
                            bitmap.compress(
                                    android.graphics.Bitmap.CompressFormat.JPEG, 60, output);
                        }
                        if (output.size() > 1048576) throw new Exception();
                        String value =
                                android.util.Base64.encodeToString(
                                        output.toByteArray(), android.util.Base64.NO_WRAP);
                        task.post(
                                main,
                                () -> {
                                    if (sessions.snapshot().epoch == epoch)
                                        callback.onSuccess(value);
                                    else callback.onFailure(SessionManager.changed());
                                });
                    } catch (Exception e) {
                        task.post(
                                main,
                                () ->
                                        callback.onFailure(
                                                new ApiFailure(
                                                        ApiFailure.Kind.INVALID_DATA,
                                                        "Selecciona un comprobante JPEG o PNG de"
                                                            + " hasta 10 MB. Si el acceso al"
                                                            + " archivo venció, selecciónalo otra"
                                                            + " vez.")));
                    } finally {
                        if (bitmap != null) bitmap.recycle();
                    }
                });
        return task;
    }
}
