package com.example.santaceciliatesis.view.adapter;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.ServicioDetalle;
import com.example.santaceciliatesis.view.viewholder.GestionServicioViewHolder;

import java.util.*;

public final class GestionServicioAdapter extends RecyclerView.Adapter<GestionServicioViewHolder> {
    private List<ServicioDetalle> rows = Collections.emptyList();
    private final java.util.function.Consumer<ServicioDetalle> click;

    public GestionServicioAdapter(java.util.function.Consumer<ServicioDetalle> click) {
        this.click = click;
    }

    public void submit(List<ServicioDetalle> value) {
        rows = List.copyOf(value);
        notifyDataSetChanged();
    }

    @NonNull
    public GestionServicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new GestionServicioViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_gestion_servicio, parent, false));
    }

    public void onBindViewHolder(@NonNull GestionServicioViewHolder holder, int position) {
        ServicioDetalle value = rows.get(position);
        holder.bind(value, () -> click.accept(value));
    }

    public int getItemCount() {
        return rows.size();
    }
}
