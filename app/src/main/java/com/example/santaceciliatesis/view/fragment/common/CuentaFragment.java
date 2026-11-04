package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.CuentaController;
import com.example.santaceciliatesis.model.Usuario;
import com.example.santaceciliatesis.util.Presentacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class CuentaFragment extends BaseFragment
        implements PrivateScreen, CuentaController.View, Refreshable {
    private CuentaController controller;
    private boolean closing;

    public CuentaFragment() {
        super(R.layout.fragment_cuenta);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        controller = new CuentaController(deps().getAuthRepository(), deps().getSessionManager());
        visible(R.id.cuenta_cambiar_estado, false);
        v(R.id.cuenta_actualizar).setOnClickListener(v -> refresh());
        v(R.id.cuenta_editar).setOnClickListener(v -> host().formulario("PERFIL", 0, 0));
        v(R.id.cuenta_salir)
                .setOnClickListener(
                        v -> {
                            if (!closing)
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.confirmar_salida)
                                        .setMessage(R.string.salida_ayuda)
                                        .setNegativeButton(R.string.cancelar, null)
                                        .setPositiveButton(
                                                R.string.cerrar_sesion,
                                                (d, w) -> {
                                                    closing = true;
                                                    controller.logout();
                                                })
                                        .show();
                        });
    }

    public void mostrar(Usuario user, boolean validado, boolean cargando) {
        text(R.id.cuenta_nombre, user.getNombres() + " " + user.getApellidos());
        text(R.id.cuenta_correo, user.getCorreo());
        text(R.id.cuenta_rol, Presentacion.rol(user.getRol()));
        Presentacion.badge(v(R.id.cuenta_badge), user.getEstado());
        text(
                R.id.cuenta_telefono,
                Presentacion.disponible(
                        user.getTelefono(), getString(R.string.cuenta_sin_telefono)));
        text(
                R.id.cuenta_ciudad,
                Presentacion.disponible(user.getCiudad(), getString(R.string.cuenta_sin_ciudad)));
        text(
                R.id.cuenta_biografia,
                Presentacion.disponible(
                        user.getBiografia(), getString(R.string.cuenta_sin_biografia)));
        text(
                R.id.cuenta_estado,
                getString(
                        closing
                                ? R.string.cuenta_cerrando
                                : cargando
                                        ? R.string.cuenta_verificando
                                        : validado
                                                ? R.string.cuenta_validada
                                                : R.string.cuenta_guardada));
        visible(R.id.cuenta_progress, cargando);
        enabled(R.id.cuenta_actualizar, !cargando && !closing);
        enabled(R.id.cuenta_editar, validado && !closing);
        enabled(R.id.cuenta_salir, !closing);
    }

    public void mensaje(String text) {
        closing = false;
        text(R.id.cuenta_estado, getString(R.string.cuenta_error_guardada, text));
        visible(R.id.cuenta_progress, false);
        enabled(R.id.cuenta_actualizar, true);
        enabled(R.id.cuenta_salir, true);
        enabled(R.id.cuenta_editar, false);
    }

    public void cerrada(boolean remoto) {
        closing = false;
        host().mostrarInicio();
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.mi_cuenta));
        controller.attach(this);
    }

    @Override
    public void onStop() {
        controller.detach();
        super.onStop();
    }

    @Override
    public void refresh() {
        controller.actualizar();
    }

    @Override
    public void onDestroyView() {
        text(R.id.cuenta_nombre, "");
        text(R.id.cuenta_correo, "");
        text(R.id.cuenta_telefono, "");
        text(R.id.cuenta_biografia, "");
        super.onDestroyView();
    }
}
