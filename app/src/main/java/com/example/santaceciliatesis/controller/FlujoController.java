package com.example.santaceciliatesis.controller;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;

import org.json.JSONObject;

/** Una operación activa por pantalla; evita respuestas tardías y dobles pulsaciones. */
public final class FlujoController {
    public interface View {
        void ocupado(boolean value);

        void error(ApiFailure error);
    }

    public interface Result {
        void mostrar(JSONObject value);
    }

    private final ContratacionRepository repository;
    private View view;
    private RequestHandle task;
    private int generation;
    private boolean busy;

    public FlujoController(ContratacionRepository repository) {
        this.repository = repository;
    }

    public void attach(View view) {
        this.view = view;
    }

    public void ejecutar(
            String method, String path, JSONObject body, boolean publica, Result result) {
        if (view == null || busy) return;
        busy = true;
        view.ocupado(true);
        int current = ++generation;
        task =
                repository.request(
                        method,
                        path,
                        body,
                        publica,
                        new Callback<JSONObject>() {
                            public void onSuccess(JSONObject data) {
                                if (view != null && current == generation) {
                                    busy = false;
                                    view.ocupado(false);
                                    result.mostrar(data);
                                }
                            }

                            public void onFailure(ApiFailure error) {
                                if (view != null && current == generation) {
                                    busy = false;
                                    view.ocupado(false);
                                    view.error(error);
                                }
                            }
                        });
    }

    public void detach() {
        generation++;
        if (task != null) task.cancel();
        view = null;
        busy = false;
    }
}
