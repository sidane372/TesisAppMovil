package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.widget.*;

import com.google.android.material.textfield.TextInputEditText;

import org.json.*;

import java.time.*;
import java.util.*;

public final class HorariosFragment extends FlujoBaseFragment implements PrivateScreen {
    private long version;
    private JSONArray draft;
    private final List<Spinner> dias = new ArrayList<>();
    private final List<TextInputEditText[]> fields = new ArrayList<>();

    public static HorariosFragment nuevo(long id) {
        var f = new HorariosFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle saved) {
        super.onViewCreated(view, saved);
        if (saved != null)
            try {
                draft = new JSONArray(saved.getString("draft", "[]"));
                version = saved.getLong("version");
            } catch (Exception ignored) {
            }
    }

    @Override
    public void refresh() {
        title("Franjas de atención");
        if (draft != null) {
            render();
            return;
        }
        YearMonth month = YearMonth.now(PERU);
        request(
                "GET",
                "servicios/"
                        + requireArguments().getLong("id")
                        + "/agenda?desde="
                        + month.atDay(1)
                        + "&hasta="
                        + month.atEndOfMonth(),
                null,
                false,
                d -> {
                    version = d.optLong("version");
                    draft = new JSONArray();
                    JSONArray franjas = d.optJSONArray("horarios");
                    if (franjas != null)
                        for (int i = 0; i < franjas.length(); i++) {
                            var f = franjas.optJSONObject(i);
                            draft.put(
                                    obj(
                                            "dia",
                                            f.optInt("dia_semana"),
                                            "desde",
                                            f.optString("hora_inicio"),
                                            "hasta",
                                            f.optString("hora_fin"),
                                            "previoMin",
                                            f.optInt("margen_previo_min"),
                                            "posteriorMin",
                                            f.optInt("margen_posterior_min")));
                        }
                    render();
                });
    }

    private void capture() {
        if (fields.isEmpty()) return;
        JSONArray data = new JSONArray();
        for (int i = 0; i < fields.size(); i++) {
            var f = fields.get(i);
            data.put(
                    obj(
                            "dia",
                            dias.get(i).getSelectedItemPosition() + 1,
                            "desde",
                            value(f[0]),
                            "hasta",
                            value(f[1]),
                            "previoMin",
                            value(f[2]),
                            "posteriorMin",
                            value(f[3])));
        }
        draft = data;
    }

    private void render() {
        clear();
        dias.clear();
        fields.clear();
        label(
                content,
                "Hora de Perú. Usa HH:mm. El fin puede ser 24:00. Para cruzar medianoche, agrega"
                    + " otra franja del día siguiente. Los contratos anteriores conservan sus"
                    + " márgenes aceptados.",
                false);
        for (int i = 0; i < draft.length(); i++) {
            JSONObject row = draft.optJSONObject(i);
            var c = card();
            Spinner day = new Spinner(requireContext());
            day.setAdapter(
                    new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            new String[] {
                                "Lunes",
                                "Martes",
                                "Miércoles",
                                "Jueves",
                                "Viernes",
                                "Sábado",
                                "Domingo"
                            }));
            day.setSelection(row.optInt("dia", 1) - 1);
            add(c, day);
            dias.add(day);
            TextInputEditText from = field(c, "Desde HH:mm", row.optString("desde", "08:00"), 8, 1),
                    to = field(c, "Hasta HH:mm", row.optString("hasta", "22:00"), 8, 1),
                    before =
                            field(
                                    c,
                                    "Preparación antes (min)",
                                    row.optString("previoMin", "0"),
                                    4,
                                    2),
                    after =
                            field(
                                    c,
                                    "Preparación después (min)",
                                    row.optString("posteriorMin", "0"),
                                    4,
                                    2);
            fields.add(new TextInputEditText[] {from, to, before, after});
            final int index = i;
            button(
                    c,
                    "Quitar franja",
                    () -> {
                        capture();
                        draft.remove(index);
                        render();
                    });
        }
        button(
                content,
                "Agregar franja",
                () -> {
                    capture();
                    if (draft.length() >= 28) {
                        invalid("Puedes configurar hasta 28 franjas");
                        return;
                    }
                    draft.put(
                            obj(
                                    "dia",
                                    1,
                                    "desde",
                                    "08:00",
                                    "hasta",
                                    "22:00",
                                    "previoMin",
                                    0,
                                    "posteriorMin",
                                    0));
                    render();
                });
        button(
                content,
                "Guardar horarios",
                () -> {
                    capture();
                    try {
                        JSONArray franjas = new JSONArray();
                        for (int i = 0; i < draft.length(); i++) {
                            var f = draft.optJSONObject(i);
                            String from = LocalTime.parse(f.optString("desde")).toString(),
                                    end = f.optString("hasta");
                            if (!end.equals("24:00") && !end.equals("24:00:00"))
                                end = LocalTime.parse(end).toString();
                            franjas.put(
                                    obj(
                                            "dia",
                                            f.optInt("dia"),
                                            "desde",
                                            from,
                                            "hasta",
                                            end,
                                            "previoMin",
                                            Integer.parseInt(f.optString("previoMin")),
                                            "posteriorMin",
                                            Integer.parseInt(f.optString("posteriorMin"))));
                        }
                        request(
                                "PUT",
                                "servicios/" + requireArguments().getLong("id") + "/horarios",
                                obj("version", version, "franjas", franjas),
                                false,
                                r -> host().terminarFormulario());
                    } catch (Exception e) {
                        invalid("Revisa las horas (HH:mm) y los márgenes en minutos");
                    }
                });
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        capture();
        if (draft != null) out.putString("draft", draft.toString());
        out.putLong("version", version);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onStop() {
        capture();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        dias.clear();
        fields.clear();
        super.onDestroyView();
    }
}
