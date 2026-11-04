package com.example.santaceciliatesis.view.adapter;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.Usuario;
import com.example.santaceciliatesis.view.viewholder.UsuarioViewHolder;

import java.util.*;

public final class UsuarioAdapter extends RecyclerView.Adapter<UsuarioViewHolder> {
    private List<Usuario> rows = Collections.emptyList();
    private final java.util.function.Consumer<Usuario> click;

    public UsuarioAdapter(java.util.function.Consumer<Usuario> click) {
        this.click = click;
    }

    public void submit(List<Usuario> value) {
        rows = List.copyOf(value);
        notifyDataSetChanged();
    }

    @NonNull
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new UsuarioViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_usuario, parent, false));
    }

    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario value = rows.get(position);
        holder.bind(value, () -> click.accept(value));
    }

    public int getItemCount() {
        return rows.size();
    }
}
