package com.example.santaceciliatesis.controller;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.*;
import com.example.santaceciliatesis.model.ServicioDetalle;

public final class DetalleServicioController {
    public interface View {
        void cargando();

        void mostrar(ServicioDetalle detalle);

        void error(String message);
    }

    private final ServicioRepository repository;
    private View view;
    private RequestHandle pending;

    public DetalleServicioController(ServicioRepository repository) {
        this.repository = repository;
    }

    public void attach(View view, long id) {
        this.view = view;
        view.cargando();
        pending =
                repository.detalle(
                        id,
                        new Callback<ServicioDetalle>() {
                            public void onSuccess(ServicioDetalle detail) {
                                if (DetalleServicioController.this.view != null)
                                    DetalleServicioController.this.view.mostrar(detail);
                            }

                            public void onFailure(ApiFailure error) {
                                if (DetalleServicioController.this.view != null)
                                    DetalleServicioController.this.view.error(error.getMessage());
                            }
                        });
    }

    public void detach() {
        if (pending != null) pending.cancel();
        view = null;
    }
}
