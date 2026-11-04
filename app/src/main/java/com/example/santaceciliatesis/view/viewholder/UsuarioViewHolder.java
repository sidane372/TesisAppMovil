package com.example.santaceciliatesis.view.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.Usuario;
import com.example.santaceciliatesis.util.Presentacion;

public final class UsuarioViewHolder extends RecyclerView.ViewHolder {
    public UsuarioViewHolder(View view) {
        super(view);
    }

    private void text(int id, String value) {
        ((TextView) itemView.findViewById(id)).setText(value);
    }

    public void bind(Usuario u, Runnable open) {
        text(
                R.id.usuario_inicial,
                u.getNombres()
                        .substring(0, u.getNombres().offsetByCodePoints(0, 1))
                        .toUpperCase(java.util.Locale.ROOT));
        text(R.id.usuario_nombre, u.getNombres() + " " + u.getApellidos());
        text(R.id.usuario_rol, Presentacion.rol(u.getRol()));
        text(R.id.usuario_correo, u.getCorreo());
        text(
                R.id.usuario_ciudad,
                Presentacion.disponible(
                        u.getCiudad(),
                        itemView.getContext().getString(R.string.cuenta_sin_ciudad)));
        Presentacion.badge(itemView.findViewById(R.id.usuario_estado), u.getEstado());
        itemView.findViewById(R.id.usuario_ver).setOnClickListener(v -> open.run());
    }
}
