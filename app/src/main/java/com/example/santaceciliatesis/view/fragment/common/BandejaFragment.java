package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.google.android.material.textfield.TextInputEditText;

import org.json.*;

public final class BandejaFragment extends FlujoBaseFragment implements PrivateScreen {
    private boolean contratos;
    private int pagina, estado;
    private String query = "";
    private TextInputEditText search;
    private Spinner states;

    public static BandejaFragment nuevo(boolean contratos) {
        var f = new BandejaFragment();
        f.setArguments(objBundle(contratos));
        return f;
    }

    private static Bundle objBundle(boolean value) {
        Bundle b = new Bundle();
        b.putBoolean("contratos", value);
        return b;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        contratos = requireArguments().getBoolean("contratos");
        if (saved != null) {
            pagina = saved.getInt("pagina");
            estado = saved.getInt("estado");
            query = saved.getString("q", "");
        }
    }

    @Override
    public void refresh() {
        title(contratos ? "Mis contratos" : "Solicitudes de cotización");
        clear();
        var filters = card();
        search = field(filters, "Buscar evento o servicio", query, 100, 1);
        String[] options =
                contratos
                        ? new String[] {
                            "Todos", "PENDIENTE_PAGO", "CONFIRMADO", "FINALIZADO", "CANCELADO"
                        }
                        : new String[] {
                            "Todos",
                            "ENVIADA",
                            "EN_REVISION",
                            "COTIZACION_RECIBIDA",
                            "RECHAZADA",
                            "CANCELADA",
                            "CONTRATADA",
                            "RESERVADA"
                        };
        label(filters, "Estado", false);
        states = new Spinner(requireContext());
        states.setAdapter(
                new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, options));
        states.setSelection(Math.min(estado, options.length - 1));
        add(filters, states);
        button(
                filters,
                "Buscar",
                () -> {
                    query = value(search);
                    estado = states.getSelectedItemPosition();
                    pagina = 0;
                    refresh();
                });
        String path =
                (contratos ? "contratos" : "solicitudes")
                        + "?pagina="
                        + pagina
                        + "&tamano=20"
                        + (estado == 0 ? "" : "&estado=" + options[estado])
                        + (query.isEmpty() ? "" : "&q=" + android.net.Uri.encode(query));
        request(
                "GET",
                path,
                null,
                false,
                data -> {
                    JSONArray rows = data.optJSONArray("items");
                    label(
                            content,
                            data.optLong("total")
                                    + (contratos ? " contratos únicos" : " solicitudes"),
                            false);
                    if (rows == null || rows.length() == 0)
                        label(card(), "No hay resultados para esta consulta.", false);
                    if (rows != null)
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.optJSONObject(i);
                            var c = card();
                            label(c, row.optString("servicio"), true);
                            label(c, row.optString("estado").replace('_', ' '), false);
                            label(
                                    c,
                                    row.optString("tipo_evento")
                                            + " · "
                                            + row.optString("ciudad")
                                            + "\n"
                                            + date(row.optString("inicio_evento_utc"))
                                            + "\n"
                                            + date(row.optString("fin_evento_utc")),
                                    false);
                            if (!row.isNull("total_centimos"))
                                label(c, money(row.optLong("total_centimos")), true);
                            button(
                                    c,
                                    contratos ? "Ver contrato" : "Ver solicitud",
                                    () ->
                                            host().flujo(
                                                            contratos ? "contrato" : "solicitud",
                                                            row.optLong("id")));
                        }
                    if (pagina > 0)
                        button(
                                content,
                                "Página anterior",
                                () -> {
                                    pagina--;
                                    refresh();
                                    resetScroll();
                                });
                    if ((pagina + 1L) * 20 < data.optLong("total"))
                        button(
                                content,
                                "Página siguiente",
                                () -> {
                                    pagina++;
                                    refresh();
                                    resetScroll();
                                });
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        out.putInt("pagina", pagina);
        out.putInt("estado", states == null ? estado : states.getSelectedItemPosition());
        out.putString("q", search == null ? query : value(search));
        super.onSaveInstanceState(out);
    }
}
