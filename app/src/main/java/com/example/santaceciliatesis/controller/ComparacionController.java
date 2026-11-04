package com.example.santaceciliatesis.controller;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;
import com.example.santaceciliatesis.model.ServicioDetalle;
import com.example.santaceciliatesis.service.ComparacionStore;

import java.util.*;

public final class ComparacionController {
    public interface View {
        void cargando();

        void mostrar(List<Long> ids, Map<Long, ServicioDetalle> data, Map<Long, String> errors);
    }

    private final ServicioRepository repo;
    private final ComparacionStore store;
    private final List<RequestHandle> pending = new ArrayList<>();
    private View view;
    private int generation;

    public ComparacionController(ServicioRepository repo, ComparacionStore store) {
        this.repo = repo;
        this.store = store;
    }

    public void attach(View view) {
        this.view = view;
    }

    public void cargar() {
        if (view == null) return;
        cancel();
        int expected = ++generation;
        List<Long> ids = store.ids();
        Map<Long, ServicioDetalle> data = new LinkedHashMap<>();
        Map<Long, String> errors = new LinkedHashMap<>();
        view.cargando();
        if (ids.isEmpty()) {
            view.mostrar(ids, data, errors);
            return;
        }
        int[] remaining = {ids.size()};
        for (long id : ids)
            pending.add(
                    repo.detalle(
                            id,
                            new Callback<ServicioDetalle>() {
                                private void done() {
                                    if (view != null
                                            && generation == expected
                                            && --remaining[0] == 0) view.mostrar(ids, data, errors);
                                }

                                public void onSuccess(ServicioDetalle s) {
                                    if (generation != expected) return;
                                    data.put(id, s);
                                    done();
                                }

                                public void onFailure(ApiFailure e) {
                                    if (generation != expected) return;
                                    errors.put(
                                            id,
                                            e.getStatus() == 404
                                                    ? "Este servicio ya no está publicado."
                                                    : e.getMessage());
                                    done();
                                }
                            }));
    }

    private void cancel() {
        for (RequestHandle task : pending) task.cancel();
        pending.clear();
    }

    public void quitar(long id) {
        store.remove(id);
        cargar();
    }

    public void vaciar() {
        store.clear();
        cargar();
    }

    public void detach() {
        generation++;
        cancel();
        view = null;
    }
}
