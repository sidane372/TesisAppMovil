package com.example.santaceciliatesis.view.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.Presentacion;
import com.google.android.material.checkbox.MaterialCheckBox;

public final class ServicioViewHolder extends RecyclerView.ViewHolder {
    public ServicioViewHolder(View view) {
        super(view);
    }

    private void text(int id, String value) {
        ((TextView) itemView.findViewById(id)).setText(value);
    }

    public void bind(
            ServicioMusical s,
            Runnable open,
            boolean selectable,
            boolean selected,
            Runnable compare) {
        text(R.id.servicio_nombre, s.getNombre());
        text(
                R.id.servicio_tipo,
                itemView.getContext()
                        .getString(
                                s.getTipo() == TipoServicio.BANDA
                                        ? R.string.banda
                                        : R.string.solista));
        text(R.id.servicio_ciudad, s.getCiudad());
        text(
                R.id.servicio_tarifa,
                Presentacion.tarifa(itemView.getContext(), s.getTarifaCentimos()));
        text(
                R.id.servicio_generos,
                s.getGeneros().isEmpty()
                        ? itemView.getContext().getString(R.string.nota_musical)
                        : String.join(" · ", s.getGeneros()));
        itemView.findViewById(R.id.servicio_perfil).setOnClickListener(v -> open.run());
        itemView.findViewById(R.id.servicio_abrir).setOnClickListener(v -> open.run());
        MaterialCheckBox box = itemView.findViewById(R.id.servicio_comparar);
        box.setVisibility(selectable ? View.VISIBLE : View.GONE);
        box.setOnCheckedChangeListener(null);
        box.setChecked(selected);
        box.setOnCheckedChangeListener((b, value) -> compare.run());
    }
}
