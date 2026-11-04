package com.example.santaceciliatesis.view.adapter;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.model.ServicioMusical;
import com.example.santaceciliatesis.service.ComparacionStore;
import com.example.santaceciliatesis.view.viewholder.ServicioViewHolder;

import java.util.Objects;

public final class ServicioAdapter extends ListAdapter<ServicioMusical, ServicioViewHolder> {
    public interface OnServicioClickListener {
        void onServicioClick(ServicioMusical value);
    }

    private final OnServicioClickListener listener;
    private final ComparacionStore selection;
    private final java.util.function.LongConsumer compare;

    public ServicioAdapter(OnServicioClickListener listener) {
        this(listener, null, id -> {});
    }

    public ServicioAdapter(
            OnServicioClickListener listener,
            ComparacionStore selection,
            java.util.function.LongConsumer compare) {
        super(
                new DiffUtil.ItemCallback<ServicioMusical>() {
                    public boolean areItemsTheSame(
                            @NonNull ServicioMusical a, @NonNull ServicioMusical b) {
                        return a.getId() == b.getId();
                    }

                    public boolean areContentsTheSame(
                            @NonNull ServicioMusical a, @NonNull ServicioMusical b) {
                        return a.getNombre().equals(b.getNombre())
                                && a.getCiudad().equals(b.getCiudad())
                                && a.getTipo().equals(b.getTipo())
                                && Objects.equals(a.getTarifaCentimos(), b.getTarifaCentimos())
                                && a.getGeneros().equals(b.getGeneros())
                                && a.getInstrumentos().equals(b.getInstrumentos());
                    }
                });
        this.listener = listener;
        this.selection = selection;
        this.compare = compare;
    }

    @NonNull
    public ServicioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new ServicioViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_servicio, parent, false));
    }

    public void onBindViewHolder(@NonNull ServicioViewHolder holder, int position) {
        ServicioMusical s = getItem(position);
        holder.bind(
                s,
                () -> listener.onServicioClick(s),
                selection != null,
                selection != null && selection.contains(s.getId()),
                () -> {
                    compare.accept(s.getId());
                    int current = holder.getBindingAdapterPosition();
                    if (current != RecyclerView.NO_POSITION) notifyItemChanged(current);
                });
    }
}
