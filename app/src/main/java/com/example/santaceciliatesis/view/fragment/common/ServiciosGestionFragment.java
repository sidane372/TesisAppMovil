package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.ConsultaController;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.view.adapter.GestionServicioAdapter;

import java.util.*;

public final class ServiciosGestionFragment extends BaseFragment
        implements PrivateScreen, ConsultaController.View<Pagina<ServicioDetalle>>, Refreshable {
    private ConsultaController<Pagina<ServicioDetalle>> controller;
    private GestionServicioAdapter adapter;
    private List<ServicioDetalle> rows = Collections.emptyList();
    private boolean admin, next;
    private int page;

    public ServiciosGestionFragment() {
        super(R.layout.fragment_lista_gestion);
    }

    public static ServiciosGestionFragment nuevo(boolean admin) {
        ServiciosGestionFragment f = new ServiciosGestionFragment();
        Bundle b = new Bundle();
        b.putBoolean("admin", admin);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        admin = requireArguments().getBoolean("admin");
        if (saved != null) page = saved.getInt("page", 0);
        controller =
                new ConsultaController<>(
                        cb -> deps().getComunidadRepository().servicios(admin, page, cb));
        adapter = new GestionServicioAdapter(s -> host().gestionarServicio(s.resumen.getId()));
        list(R.id.lista_datos, adapter);
        var session = deps().getSessionManager().snapshot().session;
        boolean solo = session != null && session.getUsuario().getRol() == Rol.SOLISTA;
        int title = admin ? R.string.servicios : solo ? R.string.mi_servicio : R.string.mis_bandas;
        text(R.id.lista_titulo, getString(title));
        text(
                R.id.lista_descripcion,
                getString(
                        admin
                                ? R.string.servicios_descripcion
                                : R.string.mis_servicios_descripcion));
        text(R.id.lista_crear, getString(R.string.crear_servicio));
        visible(R.id.lista_crear, admin);
        text(
                R.id.lista_vacio,
                getString(admin ? R.string.sin_registros : R.string.sin_servicios_propios));
        text(
                R.id.lista_ayuda,
                getString(
                        admin
                                ? R.string.buscar_usuarios_ayuda
                                : R.string.datos_privados_sin_cache));
        spinner(
                R.id.lista_estado,
                List.of(
                        "Todos los estados",
                        "Borrador",
                        "Por verificar",
                        "Publicado",
                        "Observado",
                        "Desactivado"));
        if (saved != null) {
            ((EditText) v(R.id.edit_buscar_lista)).setText(saved.getString("q", ""));
            ((Spinner) v(R.id.lista_estado)).setSelection(saved.getInt("filter", 0));
        }
        ((EditText) v(R.id.edit_buscar_lista))
                .addTextChangedListener(
                        new TextWatcher() {
                            public void beforeTextChanged(
                                    CharSequence s, int a, int c, int after) {}

                            public void onTextChanged(CharSequence s, int a, int b, int c) {
                                filter();
                            }

                            public void afterTextChanged(Editable e) {}
                        });
        ((Spinner) v(R.id.lista_estado))
                .setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {
                            public void onNothingSelected(AdapterView<?> a) {}

                            public void onItemSelected(AdapterView<?> p, View v, int i, long id) {
                                filter();
                            }
                        });
        v(R.id.lista_crear).setOnClickListener(v -> host().formulario("SERVICIO_NUEVO", 0, 0));
        v(R.id.lista_retry).setOnClickListener(v -> refresh());
        v(R.id.lista_anterior)
                .setOnClickListener(
                        v -> {
                            if (page > 0) {
                                page--;
                                refresh();
                            }
                        });
        v(R.id.lista_siguiente)
                .setOnClickListener(
                        v -> {
                            if (next) {
                                page++;
                                refresh();
                            }
                        });
    }

    private void filter() {
        String q =
                ((EditText) v(R.id.edit_buscar_lista))
                        .getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);
        int filter = ((Spinner) v(R.id.lista_estado)).getSelectedItemPosition();
        String state =
                new String[] {"", "BORRADOR", "EN_REVISION", "PUBLICADO", "OBSERVADO", "RETIRADO"}
                        [Math.max(0, filter)];
        List<ServicioDetalle> selected = new ArrayList<>();
        for (ServicioDetalle s : rows)
            if ((state.isEmpty() || state.equals(s.estado))
                    && (s.resumen.getNombre() + " " + s.resumen.getCiudad())
                            .toLowerCase(Locale.ROOT)
                            .contains(q)) selected.add(s);
        adapter.submit(selected);
        visible(R.id.lista_vacio, selected.isEmpty());
    }

    private void controls(boolean active) {
        enabled(R.id.lista_retry, active);
        visible(R.id.lista_anterior, admin);
        visible(R.id.lista_siguiente, admin);
        enabled(R.id.lista_anterior, active && page > 0);
        enabled(R.id.lista_siguiente, active && next);
    }

    public void cargando() {
        rows = Collections.emptyList();
        adapter.submit(rows);
        visible(R.id.lista_vacio, false);
        visible(R.id.lista_progress, true);
        text(R.id.lista_mensaje, getString(R.string.cargando_datos));
        controls(false);
    }

    public void mostrar(Pagina<ServicioDetalle> result) {
        rows = result.items;
        page = result.pagina;
        next = result.haySiguiente();
        visible(R.id.lista_progress, false);
        text(R.id.lista_mensaje, getString(R.string.detalle_actualizado));
        text(R.id.lista_pagina, getString(R.string.pagina_info, page + 1, result.total));
        controls(true);
        filter();
    }

    public void error(ApiFailure e) {
        rows = Collections.emptyList();
        adapter.submit(rows);
        visible(R.id.lista_progress, false);
        visible(R.id.lista_vacio, false);
        text(R.id.lista_mensaje, e.getMessage());
        next = false;
        controls(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(admin ? R.string.servicios : R.string.mis_servicios));
        controller.attach(this);
        refresh();
    }

    @Override
    public void onStop() {
        controller.detach();
        rows = Collections.emptyList();
        adapter.submit(rows);
        super.onStop();
    }

    @Override
    public void refresh() {
        controller.cargar();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        out.putInt("page", page);
        if (getView() != null) {
            out.putString("q", ((EditText) v(R.id.edit_buscar_lista)).getText().toString());
            out.putInt("filter", ((Spinner) v(R.id.lista_estado)).getSelectedItemPosition());
        }
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroyView() {
        ((androidx.recyclerview.widget.RecyclerView) v(R.id.lista_datos)).setAdapter(null);
        adapter = null;
        super.onDestroyView();
    }
}
