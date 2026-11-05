package com.example.santaceciliatesis.view.fragment.administrador;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.ConsultaController;
import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.Callback;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Presentacion;
import com.example.santaceciliatesis.view.dialog.EstadoDialog;
import com.example.santaceciliatesis.view.fragment.common.*;

import java.util.*;

public final class UsuarioDetalleFragment extends BaseFragment
        implements PrivateScreen, ConsultaController.View<Usuario>, Refreshable {
    private ConsultaController<Usuario> controller;
    private Usuario user;
    private RequestHandle mutation;
    private boolean saving;

    public UsuarioDetalleFragment() {
        super(R.layout.fragment_cuenta);
    }

    public static UsuarioDetalleFragment nuevo(long id) {
        UsuarioDetalleFragment f = new UsuarioDetalleFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        long id = requireArguments().getLong("id");
        controller =
                new ConsultaController<>(cb -> deps().getComunidadRepository().usuario(id, cb));
        text(R.id.cuenta_titulo, getString(R.string.detalle_usuario));
        text(R.id.cuenta_actualizar,getString(R.string.actualizar_otra_cuenta));
        text(R.id.cuenta_editar, getString(R.string.editar_informacion));
        visible(R.id.cuenta_salir, false);
        v(R.id.cuenta_actualizar).setOnClickListener(v -> refresh());
        v(R.id.cuenta_editar)
                .setOnClickListener(
                        v -> {
                            if (user != null) host().formulario("USUARIO_EDITAR", user.getId(), 0);
                        });
        v(R.id.cuenta_cambiar_estado).setOnClickListener(v -> change(false));
        v(R.id.cuenta_eliminar).setOnClickListener(v -> change(true));
    }

    private void change(boolean eliminar) {
        if (user == null || saving) return;
        List<String> states =
                new ArrayList<>(
                        eliminar ? List.of("DESACTIVADO") : List.of("ACTIVO", "SUSPENDIDO"));
        states.remove(user.getEstado());
        EstadoDialog.show(
                this,
                getString(eliminar ? R.string.eliminar_usuario_aviso : R.string.estado_cuenta),
                states,
                (state, reason) -> {
                    if (user == null || saving) return;
                    saving = true;
                    enabled(R.id.cuenta_cambiar_estado, false);
                    enabled(R.id.cuenta_editar, false);
                    enabled(R.id.cuenta_eliminar, false);
                    visible(R.id.cuenta_progress, true);
                    mutation =
                            deps().getComunidadRepository()
                                    .cambiarEstado(
                                            user,
                                            state,
                                            reason,
                                            eliminar,
                                            new Callback<Usuario>() {
                                                public void onSuccess(Usuario u) {
                                                    saving = false;
                                                    host().mostrarMensaje(
                                                                    getString(R.string.guardado));
                                                    mostrar(u);
                                                }

                                                public void onFailure(ApiFailure e) {
                                                    saving = false;
                                                    host().mostrarMensaje(e.getMessage());
                                                    refresh();
                                                }
                                            });
                });
    }

    public void cargando() {
        user = null;
        visible(R.id.cuenta_panel, false);
        visible(R.id.cuenta_progress, true);
        enabled(R.id.cuenta_editar, false);
        enabled(R.id.cuenta_cambiar_estado, false);
        visible(R.id.cuenta_eliminar, false);
        text(R.id.cuenta_estado, getString(R.string.cargando_datos));
    }

    public void mostrar(Usuario u) {
        user = u;
        visible(R.id.cuenta_panel, true);
        visible(R.id.cuenta_progress, false);
        text(R.id.cuenta_estado, getString(R.string.detalle_actualizado));
        text(R.id.cuenta_nombre, u.getNombres() + " " + u.getApellidos());
        text(R.id.cuenta_correo, u.getCorreo());
        text(R.id.cuenta_rol, Presentacion.rol(u.getRol()));
        text(
                R.id.cuenta_telefono,
                Presentacion.disponible(u.getTelefono(), getString(R.string.cuenta_sin_telefono)));
        text(
                R.id.cuenta_ciudad,
                Presentacion.disponible(u.getCiudad(), getString(R.string.cuenta_sin_ciudad)));
        text(
                R.id.cuenta_biografia,
                Presentacion.disponible(
                        u.getBiografia(), getString(R.string.cuenta_sin_biografia)));
        Presentacion.badge(v(R.id.cuenta_badge), u.getEstado());
        boolean musical = u.getRol() != Rol.ADMINISTRADOR;
        visible(R.id.cuenta_editar, musical);
        visible(R.id.cuenta_cambiar_estado, musical);
        enabled(R.id.cuenta_editar, musical);
        enabled(R.id.cuenta_cambiar_estado, musical);
        visible(R.id.cuenta_eliminar, musical && !"DESACTIVADO".equals(u.getEstado()));
        enabled(R.id.cuenta_eliminar, musical);
    }

    public void error(ApiFailure error) {
        visible(R.id.cuenta_eliminar, false);
        user = null;
        visible(R.id.cuenta_progress, false);
        visible(R.id.cuenta_panel, false);
        visible(R.id.cuenta_editar, false);
        visible(R.id.cuenta_cambiar_estado, false);
        text(R.id.cuenta_estado, error.getMessage());
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.detalle_usuario));
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
        user = null;
        saving = false;
        super.onStop();
    }
}
