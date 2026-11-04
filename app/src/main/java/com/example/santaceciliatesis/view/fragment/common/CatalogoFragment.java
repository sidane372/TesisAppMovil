package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.example.santaceciliatesis.*;
import com.example.santaceciliatesis.controller.CatalogoController;
import com.example.santaceciliatesis.model.ServicioMusical;
import com.example.santaceciliatesis.util.Resource;
import com.example.santaceciliatesis.view.adapter.ServicioAdapter;
import com.google.android.material.textfield.TextInputLayout;

import java.util.*;

public final class CatalogoFragment extends BaseFragment
        implements CatalogoController.View, Refreshable {
    private CatalogoController controller;
    private ServicioAdapter adapter;
    private List<String> genres = Collections.emptyList();
    private String savedGenre = "";
    private boolean restoring;

    public CatalogoFragment() {
        super(R.layout.fragment_catalogo);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        controller = new CatalogoController(deps().getServicioRepository());
        adapter =
                new ServicioAdapter(
                        s -> host().mostrarServicio(s.getId()),
                        deps().getComparacion(),
                        id -> {
                            if (!deps().getComparacion().toggle(id))
                                host().mostrarMensaje(getString(R.string.comparacion_limite));
                            comparison();
                        });
        list(R.id.catalogo_lista, adapter);
        spinner(
                R.id.catalogo_tipo,
                List.of(
                        getString(R.string.todo_tipo),
                        getString(R.string.banda),
                        getString(R.string.solista)));
        spinner(R.id.catalogo_genero, List.of(getString(R.string.todos_generos)));
        if (saved != null) {
            ((EditText) v(R.id.edit_busqueda)).setText(saved.getString("q", ""));
            ((EditText) v(R.id.edit_maximo)).setText(saved.getString("max", ""));
            ((Spinner) v(R.id.catalogo_tipo)).setSelection(saved.getInt("tipo", 0));
            savedGenre = saved.getString("genero", "");
            visible(R.id.catalogo_filtros, saved.getBoolean("filters", false));
        }
        TextWatcher watcher =
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        apply();
                    }

                    public void afterTextChanged(Editable e) {}
                };
        ((EditText) v(R.id.edit_busqueda)).addTextChangedListener(watcher);
        ((EditText) v(R.id.edit_maximo)).addTextChangedListener(watcher);
        AdapterView.OnItemSelectedListener listener =
                new AdapterView.OnItemSelectedListener() {
                    public void onNothingSelected(AdapterView<?> parent) {}

                    public void onItemSelected(
                            AdapterView<?> parent, View selected, int position, long id) {
                        apply();
                    }
                };
        ((Spinner) v(R.id.catalogo_tipo)).setOnItemSelectedListener(listener);
        ((Spinner) v(R.id.catalogo_genero)).setOnItemSelectedListener(listener);
        v(R.id.catalogo_filtros_toggle)
                .setOnClickListener(
                        v ->
                                visible(
                                        R.id.catalogo_filtros,
                                        v(R.id.catalogo_filtros).getVisibility() != View.VISIBLE));
        v(R.id.catalogo_limpiar)
                .setOnClickListener(
                        v -> {
                            restoring = true;
                            ((EditText) v(R.id.edit_busqueda)).setText("");
                            ((EditText) v(R.id.edit_maximo)).setText("");
                            ((Spinner) v(R.id.catalogo_tipo)).setSelection(0);
                            ((Spinner) v(R.id.catalogo_genero)).setSelection(0);
                            savedGenre = "";
                            restoring = false;
                            apply();
                        });
        v(R.id.catalogo_retry)
                .setOnClickListener(
                        v -> {
                            if (host().solicitarRedLocal()) refresh();
                        });
        v(R.id.catalogo_comparar).setOnClickListener(v -> host().abrir("comparar"));
        comparison();
    }

    private String value(int id) {
        return ((EditText) v(id)).getText().toString();
    }

    private void comparison() {
        text(
                R.id.catalogo_comparar,
                getString(R.string.comparar_seleccion, deps().getComparacion().size()));
    }

    private void apply() {
        if (controller == null || restoring || getView() == null) return;
        int type = ((Spinner) v(R.id.catalogo_tipo)).getSelectedItemPosition();
        int g = ((Spinner) v(R.id.catalogo_genero)).getSelectedItemPosition();
        controller.filtrar(
                value(R.id.edit_busqueda),
                type == 1 ? "BANDA" : type == 2 ? "SOLISTA" : "",
                g > 0 && g <= genres.size() ? genres.get(g - 1) : "",
                value(R.id.edit_maximo));
    }

    @Override
    public void generos(List<String> values) {
        int selected = ((Spinner) v(R.id.catalogo_genero)).getSelectedItemPosition();
        String current =
                selected > 0 && selected <= genres.size() ? genres.get(selected - 1) : savedGenre;
        if (genres.equals(values)) return;
        genres = List.copyOf(values);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.todos_generos));
        labels.addAll(values);
        restoring = true;
        spinner(R.id.catalogo_genero, labels);
        int index = current.isEmpty() ? 0 : values.indexOf(current) + 1;
        ((Spinner) v(R.id.catalogo_genero)).setSelection(Math.max(0, index));
        restoring = false;
        apply();
    }

    @Override
    public void filtroError(String error) {
        ((TextInputLayout) v(R.id.field_maximo)).setError(error);
    }

    @Override
    public void render(Resource<List<ServicioMusical>> result) {
        boolean loading = result.getState() == Resource.State.LOADING;
        visible(R.id.catalogo_conteo, !loading);
        visible(R.id.catalogo_progress, loading);
        enabled(R.id.catalogo_retry, !loading);
        adapter.submitList(result.getData());
        visible(R.id.catalogo_empty, !loading && result.getData().isEmpty());
        text(
                R.id.catalogo_conteo,
                getResources()
                        .getQuantityString(
                                R.plurals.catalogo_resultados,
                                result.getData().size(),
                                result.getData().size()));
        int message;
        switch (result.getState()) {
            case LOADING:
                message = R.string.catalogo_cargando;
                break;
            case REMOTE:
                message = R.string.catalogo_actualizado;
                break;
            case CACHE:
                message = R.string.catalogo_cache;
                break;
            case NOT_CONFIGURED:
                message = R.string.catalogo_no_disponible;
                break;
            case LOCAL_PERMISSION:
                message = R.string.catalogo_permiso_local;
                break;
            case INVALID_DATA:
                message = R.string.catalogo_datos_invalidos;
                break;
            case LOCAL_ERROR:
                message = R.string.catalogo_sin_guardar;
                break;
            default:
                message = R.string.catalogo_cache;
        }
        text(R.id.catalogo_estado, getString(message));
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.explorar_musicos));
        controller.attach(this);
        apply();
        controller.cargar();
        comparison();
    }

    @Override
    public void refresh() {
        if (controller != null) controller.cargar();
    }

    public void actualizarDatos() {
        refresh();
    }

    @Override
    public void onStop() {
        controller.detach();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        if (getView() != null) {
            out.putString("q", value(R.id.edit_busqueda));
            out.putString("max", value(R.id.edit_maximo));
            out.putInt("tipo", ((Spinner) v(R.id.catalogo_tipo)).getSelectedItemPosition());
            int index = ((Spinner) v(R.id.catalogo_genero)).getSelectedItemPosition();
            out.putString(
                    "genero", index > 0 && index <= genres.size() ? genres.get(index - 1) : "");
            out.putBoolean("filters", v(R.id.catalogo_filtros).getVisibility() == View.VISIBLE);
        }
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroyView() {
        ((androidx.recyclerview.widget.RecyclerView) v(R.id.catalogo_lista)).setAdapter(null);
        adapter = null;
        controller = null;
        super.onDestroyView();
    }
}
