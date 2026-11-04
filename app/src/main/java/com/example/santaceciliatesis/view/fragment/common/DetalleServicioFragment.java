package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.ConsultaController;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.ServicioDetalle;
import com.example.santaceciliatesis.util.Presentacion;
import com.example.santaceciliatesis.view.adapter.CancionAdapter;
import com.google.android.material.tabs.TabLayout;

public final class DetalleServicioFragment extends BaseFragment
        implements ConsultaController.View<ServicioDetalle>, Refreshable {
    private ConsultaController<ServicioDetalle> controller;
    private CancionAdapter songs;
    private int selected;

    public DetalleServicioFragment() {
        super(R.layout.fragment_detalle_servicio);
    }

    public static DetalleServicioFragment nuevo(long id) {
        DetalleServicioFragment f = new DetalleServicioFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        long id = requireArguments().getLong("id");
        controller = new ConsultaController<>(cb -> deps().getServicioRepository().detalle(id, cb));
        songs = new CancionAdapter(false, c -> {}, c -> {});
        list(R.id.detalle_canciones, songs);
        selected = saved == null ? 0 : saved.getInt("tab", 0);
        TabLayout tabs = v(R.id.detalle_tabs);
        tabs.addTab(tabs.newTab().setText(R.string.informacion));
        tabs.addTab(tabs.newTab().setText(R.string.repertorio));
        tabs.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    public void onTabSelected(TabLayout.Tab tab) {
                        selected = tab.getPosition();
                        panels();
                    }

                    public void onTabUnselected(TabLayout.Tab t) {}

                    public void onTabReselected(TabLayout.Tab t) {}
                });
        tabs.selectTab(tabs.getTabAt(selected));
        panels();
        v(R.id.detalle_actualizar).setOnClickListener(v -> refresh());
        v(R.id.detalle_disponibilidad).setOnClickListener(v -> host().flujo("disponibilidad", id));
    }

    private void panels() {
        visible(R.id.detalle_informacion, selected == 0);
        visible(R.id.detalle_repertorio_panel, selected == 1);
    }

    public void cargando() {
        visible(R.id.detalle_progress, true);
        visible(R.id.detalle_contenido, false);
        text(R.id.detalle_estado, getString(R.string.detalle_cargando));
        enabled(R.id.detalle_actualizar, false);
    }

    public void mostrar(ServicioDetalle d) {
        visible(R.id.detalle_progress, false);
        visible(R.id.detalle_contenido, true);
        enabled(R.id.detalle_actualizar, true);
        text(R.id.detalle_estado, getString(R.string.detalle_actualizado));
        text(
                R.id.detalle_tipo,
                getString(
                        d.resumen.getTipo()
                                        == com.example.santaceciliatesis.model.TipoServicio.BANDA
                                ? R.string.banda
                                : R.string.solista));
        text(R.id.detalle_nombre, d.resumen.getNombre());
        text(R.id.detalle_generos, String.join(" · ", d.generos));
        text(R.id.detalle_ciudad, d.resumen.getCiudad());
        text(R.id.detalle_instrumentos, String.join(" · ", d.instrumentos));
        text(R.id.detalle_descripcion, d.descripcion);
        visible(R.id.detalle_descripcion, !d.descripcion.isEmpty());
        text(
                R.id.detalle_experiencia,
                Presentacion.disponible(d.experiencia, getString(R.string.por_informar)));
        text(
                R.id.detalle_eventos,
                d.tiposEvento.isEmpty()
                        ? getString(R.string.por_informar)
                        : String.join(" · ", d.tiposEvento));
        text(
                R.id.detalle_incluye,
                Presentacion.disponible(d.incluye, getString(R.string.segun_cotizacion)));
        text(
                R.id.detalle_tarifa,
                Presentacion.tarifa(requireContext(), d.resumen.getTarifaCentimos()));
        songs.submit(d.canciones);
        visible(R.id.detalle_repertorio_vacio, d.canciones.isEmpty());
    }

    public void error(ApiFailure error) {
        visible(R.id.detalle_progress, false);
        visible(R.id.detalle_contenido, false);
        text(R.id.detalle_estado, error.getMessage());
        enabled(R.id.detalle_actualizar, true);
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.perfil_servicio));
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        out.putInt("tab", selected);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroyView() {
        ((androidx.recyclerview.widget.RecyclerView) v(R.id.detalle_canciones)).setAdapter(null);
        songs = null;
        super.onDestroyView();
    }
}
