package com.example.santaceciliatesis.controller;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.Callback;

/**
 * Un controlador de consulta por vista. Invalida callbacks al cambiar consulta o abandonar
 * pantalla.
 */
public final class ConsultaController<T> {
    public interface Source<T> {
        RequestHandle load(Callback<T> callback);
    }

    public interface View<T> {
        void cargando();

        void mostrar(T value);

        void error(ApiFailure error);
    }

    private final Source<T> source;
    private View<T> view;
    private RequestHandle task;
    private int generation;

    public ConsultaController(Source<T> source) {
        this.source = source;
    }

    public void attach(View<T> value) {
        view = value;
    }

    public void cargar() {
        if (view == null) return;
        if (task != null) task.cancel();
        int current = ++generation;
        view.cargando();
        task =
                source.load(
                        new Callback<T>() {
                            public void onSuccess(T value) {
                                if (view != null && generation == current) view.mostrar(value);
                            }

                            public void onFailure(ApiFailure error) {
                                if (view != null && generation == current) view.error(error);
                            }
                        });
    }

    public void detach() {
        generation++;
        if (task != null) task.cancel();
        view = null;
    }
}
