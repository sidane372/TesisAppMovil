package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.ComparacionController;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Presentacion;

import java.util.*;

public final class CompararFragment extends BaseFragment
        implements ComparacionController.View, Refreshable {
    private ComparacionController controller;

    public CompararFragment() {
        super(R.layout.fragment_comparar);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        controller =
                new ComparacionController(deps().getServicioRepository(), deps().getComparacion());
        v(R.id.comparar_explorar).setOnClickListener(v -> host().abrir("catalogo"));
        v(R.id.comparar_limpiar).setOnClickListener(v -> controller.vaciar());
    }

    public void cargando() {
        visible(R.id.comparar_progress, true);
        visible(R.id.comparar_vacio, false);
        text(R.id.comparar_estado, getString(R.string.cargando_datos));
        ((LinearLayout) v(R.id.comparar_items)).removeAllViews();
    }

    public void mostrar(List<Long> ids, Map<Long, ServicioDetalle> data, Map<Long, String> errors) {
        visible(R.id.comparar_progress, false);
        visible(R.id.comparar_vacio, ids.isEmpty());
        visible(R.id.comparar_limpiar, !ids.isEmpty());
        text(
                R.id.comparar_estado,
                ids.isEmpty() ? "" : getString(R.string.comparar_seleccion, ids.size()));
        LinearLayout list = v(R.id.comparar_items);
        list.removeAllViews();
        for (long id : ids) {
            View card = getLayoutInflater().inflate(R.layout.item_comparacion, list, false);
            ServicioDetalle s = data.get(id);
            ((TextView) card.findViewById(R.id.comparacion_nombre))
                    .setText(
                            s == null
                                    ? getString(R.string.perfil_servicio)
                                    : s.resumen.getNombre());
            String content =
                    s == null
                            ? errors.get(id)
                            : getString(
                                            s.resumen.getTipo() == TipoServicio.BANDA
                                                    ? R.string.banda
                                                    : R.string.solista)
                                    + "\n\n"
                                    + s.resumen.getCiudad()
                                    + "\n\n"
                                    + String.join(" · ", s.generos)
                                    + "\n\n"
                                    + Presentacion.tarifa(
                                            requireContext(), s.resumen.getTarifaCentimos())
                                    + "\n\n"
                                    + getString(R.string.instrumentos)
                                    + ": "
                                    + String.join(" · ", s.instrumentos)
                                    + "\n\n"
                                    + getString(R.string.repertorio)
                                    + ": "
                                    + (s.repertorio.isEmpty()
                                            ? getString(R.string.repertorio_vacio)
                                            : String.join("; ", s.repertorio));
            ((TextView) card.findViewById(R.id.comparacion_datos)).setText(content);
            card.findViewById(R.id.comparacion_perfil).setEnabled(s != null);
            card.findViewById(R.id.comparacion_perfil)
                    .setOnClickListener(v -> host().mostrarServicio(id));
            card.findViewById(R.id.comparacion_quitar)
                    .setOnClickListener(v -> controller.quitar(id));
            list.addView(card);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.comparar));
        controller.attach(this);
        refresh();
    }

    @Override
    public void onStop() {
        controller.detach();
        super.onStop();
    }

    @Override
    public void refresh() {
        controller.cargar();
    }
}
