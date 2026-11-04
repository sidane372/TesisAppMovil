package com.example.santaceciliatesis.view.adapter;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.Cancion;
import com.example.santaceciliatesis.view.viewholder.CancionViewHolder;

import java.util.*;

public final class CancionAdapter extends RecyclerView.Adapter<CancionViewHolder> {
    private List<Cancion> rows = Collections.emptyList();
    private final boolean editable;
    private final java.util.function.Consumer<Cancion> edit, remove;

    public CancionAdapter(
            boolean editable,
            java.util.function.Consumer<Cancion> edit,
            java.util.function.Consumer<Cancion> remove) {
        this.editable = editable;
        this.edit = edit;
        this.remove = remove;
    }

    public void submit(List<Cancion> rows) {
        this.rows = List.copyOf(rows);
        notifyDataSetChanged();
    }

    @NonNull
    public CancionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new CancionViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_cancion, parent, false));
    }

    public void onBindViewHolder(@NonNull CancionViewHolder holder, int position) {
        Cancion c = rows.get(position);
        holder.bind(c, editable, () -> edit.accept(c), () -> remove.accept(c));
    }

    public int getItemCount() {
        return rows.size();
    }
}
