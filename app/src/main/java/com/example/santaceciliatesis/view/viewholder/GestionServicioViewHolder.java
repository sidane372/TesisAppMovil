package com.example.santaceciliatesis.view.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Presentacion;

public final class GestionServicioViewHolder extends RecyclerView.ViewHolder {
    public GestionServicioViewHolder(View v) {
        super(v);
    }

    private void text(int id, String value) {
        ((TextView) itemView.findViewById(id)).setText(value);
    }

    public void bind(ServicioDetalle d, Runnable open) {
        text(
                R.id.gestion_tipo,
                itemView.getContext()
                        .getString(
                                d.resumen.getTipo() == TipoServicio.BANDA
                                        ? R.string.banda
                                        : R.string.solista));
        text(R.id.gestion_nombre, d.resumen.getNombre());
        text(R.id.gestion_ciudad, d.resumen.getCiudad());
        text(
                R.id.gestion_tarifa,
                Presentacion.tarifa(itemView.getContext(), d.resumen.getTarifaCentimos()));
        text(R.id.gestion_repertorio, d.canciones.size() + " canciones en el repertorio");
        Presentacion.badge(itemView.findViewById(R.id.gestion_estado), d.estado);
        itemView.findViewById(R.id.gestion_abrir).setOnClickListener(v -> open.run());
    }
}
