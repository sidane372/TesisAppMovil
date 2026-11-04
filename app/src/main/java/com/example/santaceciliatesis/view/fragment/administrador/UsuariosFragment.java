package com.example.santaceciliatesis.view.fragment.administrador;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.ConsultaController;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Presentacion;
import com.example.santaceciliatesis.view.adapter.UsuarioAdapter;
import com.example.santaceciliatesis.view.fragment.common.*;

import java.util.*;

public final class UsuariosFragment extends BaseFragment
        implements ConsultaController.View<Pagina<Usuario>>, Refreshable {
    private ConsultaController<Pagina<Usuario>> controller;
    private UsuarioAdapter adapter;
    private List<Usuario> rows = Collections.emptyList();
    private int page;
    private boolean next;

    public UsuariosFragment() {
        super(R.layout.fragment_lista_gestion);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        if (saved != null) page = saved.getInt("page", 0);
        controller =
                new ConsultaController<>(
                        cb -> deps().getComunidadRepository().usuarios(page, 20, cb));
        adapter = new UsuarioAdapter(u -> host().mostrarUsuario(u.getId()));
        list(R.id.lista_datos, adapter);
        spinner(
                R.id.lista_estado,
                List.of("Todas las cuentas", "Activas", "Suspendidas", "Desactivadas"));
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
        v(R.id.lista_crear).setOnClickListener(v -> host().formulario("USUARIO_NUEVO", 0, 0));
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
                filter == 1
                        ? "ACTIVO"
                        : filter == 2 ? "SUSPENDIDO" : filter == 3 ? "DESACTIVADO" : "";
        List<Usuario> selected = new ArrayList<>();
        for (Usuario u : rows)
            if ((state.isEmpty() || state.equals(u.getEstado()))
                    && (u.getNombres()
                                    + " "
                                    + u.getApellidos()
                                    + " "
                                    + u.getCorreo()
                                    + " "
                                    + Presentacion.rol(u.getRol()))
                            .toLowerCase(Locale.ROOT)
                            .contains(q)) selected.add(u);
        adapter.submit(selected);
        visible(R.id.lista_vacio, selected.isEmpty());
    }

    public void cargando() {
        rows = Collections.emptyList();
        adapter.submit(rows);
        visible(R.id.lista_vacio, false);
        visible(R.id.lista_progress, true);
        text(R.id.lista_mensaje, getString(R.string.cargando_datos));
        controls(false);
    }

    private void controls(boolean available) {
        enabled(R.id.lista_retry, available);
        enabled(R.id.lista_anterior, available && page > 0);
        enabled(R.id.lista_siguiente, available && next);
    }

    public void mostrar(Pagina<Usuario> result) {
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
        host().titulo(getString(R.string.usuarios));
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
