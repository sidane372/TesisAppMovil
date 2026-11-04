package com.example.santaceciliatesis.view.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.Cancion;

public final class CancionViewHolder extends RecyclerView.ViewHolder {
    public CancionViewHolder(View view) {
        super(view);
    }

    private void text(int id, String value) {
        ((TextView) itemView.findViewById(id)).setText(value);
    }

    public void bind(Cancion c, boolean editable, Runnable edit, Runnable remove) {
        text(R.id.cancion_titulo, c.titulo);
        text(R.id.cancion_genero, c.genero);
        text(R.id.cancion_duracion, c.duracion());
        text(R.id.cancion_descripcion, c.descripcion);
        itemView.findViewById(R.id.cancion_descripcion)
                .setVisibility(c.descripcion.isEmpty() ? View.GONE : View.VISIBLE);
        itemView.findViewById(R.id.cancion_acciones)
                .setVisibility(editable ? View.VISIBLE : View.GONE);
        itemView.findViewById(R.id.cancion_editar).setOnClickListener(v -> edit.run());
        itemView.findViewById(R.id.cancion_retirar).setOnClickListener(v -> remove.run());
    }
}
