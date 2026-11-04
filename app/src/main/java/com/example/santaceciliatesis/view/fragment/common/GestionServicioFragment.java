package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.ConsultaController;
import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.Callback;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Presentacion;
import com.example.santaceciliatesis.view.adapter.CancionAdapter;
import com.example.santaceciliatesis.view.dialog.EstadoDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.*;

public final class GestionServicioFragment extends BaseFragment
        implements PrivateScreen, ConsultaController.View<ServicioDetalle>, Refreshable {
    private ConsultaController<ServicioDetalle> controller;
    private ServicioDetalle service;
    private CancionAdapter adapter;
    private RequestHandle mutation;
    private boolean saving, admin;
    private long id;

    public GestionServicioFragment() {
        super(R.layout.fragment_gestion_servicio);
    }

    public static GestionServicioFragment nuevo(long id) {
        GestionServicioFragment f = new GestionServicioFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        id = requireArguments().getLong("id");
        controller =
                new ConsultaController<>(cb -> deps().getComunidadRepository().servicio(id, cb));
        var session = deps().getSessionManager().snapshot().session;
        admin = session != null && session.getUsuario().getRol() == Rol.ADMINISTRADOR;
        visible(R.id.gestion_editar, admin);
        visible(R.id.gestion_publicacion, admin);
        v(R.id.gestion_editar).setOnClickListener(v -> host().formulario("SERVICIO_EDITAR", id, 0));
        v(R.id.gestion_tarifa_editar).setOnClickListener(v -> host().formulario("TARIFA", id, 0));
        v(R.id.gestion_agregar_cancion)
                .setOnClickListener(v -> host().formulario("CANCION", id, 0));
        v(R.id.gestion_publicacion).setOnClickListener(v -> publication());
        v(R.id.gestion_retry).setOnClickListener(v -> refresh());
        v(R.id.gestion_agenda).setOnClickListener(v -> host().flujo("agenda", id));
    }

    private void publication() {
        if (service == null || saving || !admin) return;
        List<String> states =
                new ArrayList<>(
                        List.of("BORRADOR", "EN_REVISION", "PUBLICADO", "OBSERVADO", "RETIRADO"));
        states.remove(service.estado);
        EstadoDialog.show(
                this,
                getString(R.string.revisar_publicacion),
                states,
                (state, reason) -> {
                    if (service == null || saving) return;
                    busy(true);
                    mutation =
                            deps().getComunidadRepository()
                                    .publicar(
                                            service,
                                            state,
                                            reason,
                                            new Callback<ServicioDetalle>() {
                                                public void onSuccess(ServicioDetalle s) {
                                                    busy(false);
                                                    host().mostrarMensaje(
                                                                    getString(R.string.guardado));
                                                    mostrar(s);
                                                }

                                                public void onFailure(ApiFailure e) {
                                                    busy(false);
                                                    host().mostrarMensaje(e.getMessage());
                                                    refresh();
                                                }
                                            });
                });
    }

    private void remove(Cancion song) {
        if (saving) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.retirar)
                .setMessage("¿Retirar «" + song.titulo + "» del repertorio de este servicio?")
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(
                        R.string.retirar,
                        (dialog, which) -> {
                            busy(true);
                            mutation =
                                    deps().getComunidadRepository()
                                            .retirarCancion(
                                                    id,
                                                    song.id,
                                                    new Callback<Boolean>() {
                                                        public void onSuccess(Boolean ok) {
                                                            busy(false);
                                                            host().mostrarMensaje(
                                                                            getString(
                                                                                    R.string
                                                                                            .guardado));
                                                            refresh();
                                                        }

                                                        public void onFailure(ApiFailure e) {
                                                            busy(false);
                                                            host().mostrarMensaje(e.getMessage());
                                                            refresh();
                                                        }
                                                    });
                        })
                .show();
    }

    private void busy(boolean value) {
        saving = value;
        visible(R.id.gestion_progress, value);
        enabled(R.id.gestion_editar, !value);
        enabled(R.id.gestion_publicacion, !value);
        enabled(R.id.gestion_tarifa_editar, !value);
        enabled(R.id.gestion_agregar_cancion, !value);
        enabled(R.id.gestion_retry, !value);
    }

    public void cargando() {
        service = null;
        visible(R.id.gestion_panel, false);
        visible(R.id.gestion_progress, true);
        text(R.id.gestion_mensaje, getString(R.string.cargando_datos));
    }

    public void mostrar(ServicioDetalle s) {
        var session = deps().getSessionManager().snapshot().session;
        if (session == null || (!admin && session.getUsuario().getId() != s.responsableId)) {
            error(
                    new ApiFailure(
                            ApiFailure.Kind.AUTH_REQUIRED,
                            "Este servicio no está asignado a tu cuenta."));
            return;
        }
        service = s;
        busy(false);
        visible(R.id.gestion_panel, true);
        text(R.id.gestion_mensaje, getString(R.string.detalle_actualizado));
        text(R.id.gestion_detalle_nombre, s.resumen.getNombre());
        Presentacion.badge(v(R.id.gestion_detalle_estado), s.estado);
        text(
                R.id.gestion_detalle_info,
                s.resumen.getCiudad()
                        + "\n\n"
                        + String.join(" · ", s.generos)
                        + "\n\n"
                        + getString(R.string.instrumentos)
                        + ": "
                        + String.join(" · ", s.instrumentos)
                        + "\n\n"
                        + getString(R.string.tipos_evento)
                        + ": "
                        + String.join(" · ", s.tiposEvento)
                        + "\n\n"
                        + getString(R.string.experiencia)
                        + ": "
                        + Presentacion.disponible(s.experiencia, getString(R.string.por_informar))
                        + "\n\n"
                        + getString(R.string.incluye)
                        + ": "
                        + Presentacion.disponible(s.incluye, getString(R.string.segun_cotizacion)));
        text(
                R.id.gestion_detalle_tarifa,
                Presentacion.tarifa(requireContext(), s.resumen.getTarifaCentimos()));
        adapter =
                new CancionAdapter(
                        true,
                        c -> {
                            if (!saving) host().formulario("CANCION", id, c.id);
                        },
                        this::remove);
        list(R.id.gestion_canciones, adapter);
        adapter.submit(s.canciones);
        visible(R.id.gestion_repertorio_vacio, s.canciones.isEmpty());
    }

    public void error(ApiFailure e) {
        service = null;
        visible(R.id.gestion_panel, false);
        visible(R.id.gestion_progress, false);
        text(R.id.gestion_mensaje, e.getMessage());
        enabled(R.id.gestion_retry, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.administrar));
        controller.attach(this);
        refresh();
    }

    @Override
    public void refresh() {
        if (!saving) controller.cargar();
    }

    @Override
    public void onStop() {
        controller.detach();
        if (mutation != null) mutation.cancel();
        service = null;
        saving = false;
        if (adapter != null) adapter.submit(Collections.emptyList());
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        ((androidx.recyclerview.widget.RecyclerView) v(R.id.gestion_canciones)).setAdapter(null);
        adapter = null;
        super.onDestroyView();
    }
}
