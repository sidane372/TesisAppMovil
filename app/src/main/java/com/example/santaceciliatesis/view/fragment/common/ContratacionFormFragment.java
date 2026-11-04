package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.text.*;
import android.widget.*;

import androidx.annotation.*;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import org.json.*;

import java.time.*;
import java.util.*;

/** Asistente y compositor: conservan el borrador y la clave de envío durante la recreación. */
public final class ContratacionFormFragment extends FlujoBaseFragment implements PrivateScreen {
    private String modo, clave = UUID.randomUUID().toString();
    private int paso;
    private long baseVersion = -1;
    private JSONObject draft = new JSONObject(), data;
    private final Map<String, TextInputEditText> fields = new LinkedHashMap<>();
    private final List<TextInputEditText[]> lines = new ArrayList<>();
    private MaterialCheckBox consent;
    private TextView totalView;

    public static ContratacionFormFragment nuevo(String modo, long id) {
        var f = new ContratacionFormFragment();
        Bundle b = new Bundle();
        b.putString("modo", modo);
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        modo = requireArguments().getString("modo");
        if (saved != null) {
            paso = saved.getInt("paso");
            baseVersion = saved.getLong("baseVersion", -1);
            clave = saved.getString("clave", clave);
            try {
                draft = new JSONObject(saved.getString("draft", "{}"));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void refresh() {
        title(modo.equals("solicitar") ? "Solicitar cotización" : "Elaborar cotización");
        if (data == null)
            request(
                    "GET",
                    (modo.equals("solicitar") ? "servicios/" : "solicitudes/")
                            + requireArguments().getLong("id"),
                    null,
                    modo.equals("solicitar"),
                    d -> {
                        data = d;
                        if (baseVersion < 0) baseVersion = d.optLong("version");
                        render();
                    });
        else render();
    }

    private TextInputEditText input(
            LinearLayout target, String key, String label, String fallback, int max, int type) {
        TextInputEditText edit = field(target, label, draft.optString(key, fallback), max, type);
        fields.put(key, edit);
        return edit;
    }

    private TextInputEditText dateInput(
            LinearLayout target, String key, String label, String fallback) {
        TextInputEditText e = dateField(target, label, draft.optString(key, fallback));
        fields.put(key, e);
        return e;
    }

    private void capture() {
        for (var e : fields.entrySet())
            try {
                draft.put(e.getKey(), value(e.getValue()));
            } catch (JSONException ignored) {
            }
        if (modo != null && modo.equals("cotizar") && consent != null) {
            JSONArray rows = new JSONArray();
            for (var line : lines)
                rows.put(
                        obj(
                                "concepto",
                                value(line[0]),
                                "cantidad",
                                value(line[1]),
                                "precio",
                                value(line[2])));
            try {
                draft.put("lineas", rows);
                draft.put("politica", consent.isChecked());
            } catch (JSONException ignored) {
            }
        }
    }

    private void render() {
        clear();
        fields.clear();
        lines.clear();
        consent = null;
        totalView = null;
        label(
                content,
                modo.equals("solicitar") ? data.optString("nombre") : data.optString("servicio"),
                true);
        if (modo.equals("solicitar")) wizard();
        else quote();
    }

    private void wizard() {
        label(content, "Paso " + (paso + 1) + " de 3 · Hora de Perú", false);
        var c = card();
        LocalDate chosen =
                LocalDate.parse(
                        requireArguments()
                                .getString("fecha", LocalDate.now(PERU).plusDays(1).toString()));
        String start = chosen.atTime(10, 0).format(LOCAL), end = chosen.atTime(13, 0).format(LOCAL);
        if (paso == 0) {
            label(c, "Fecha y horario", true);
            dateInput(c, "inicio", "Inicio del evento", start);
            dateInput(c, "fin", "Fin del evento", end);
            label(
                    c,
                    "Selecciona fecha y hora. El evento puede cruzar medianoche y durar hasta 24"
                        + " horas.",
                    false);
            button(
                    c,
                    "Continuar",
                    () -> {
                        capture();
                        try {
                            Instant a = Instant.parse(utc(draft.optString("inicio"))),
                                    b = Instant.parse(utc(draft.optString("fin")));
                            if (!a.isAfter(Instant.now())
                                    || !b.isAfter(a)
                                    || Duration.between(a, b).toHours() > 24
                                    || Duration.between(a, b).compareTo(Duration.ofHours(24)) > 0)
                                throw new IllegalArgumentException();
                            paso = 1;
                            render();
                            resetScroll();
                        } catch (Exception e) {
                            invalid("Elige un intervalo futuro de hasta 24 horas");
                        }
                    });
        } else if (paso == 1) {
            label(c, "Cuéntanos sobre tu evento", true);
            input(c, "tipoEvento", "Tipo de evento *", "", 100, 1);
            input(c, "lugar", "Dirección del evento *", "", 250, 1);
            input(c, "ciudad", "Distrito / provincia *", "Ambo, Huánuco", 100, 1);
            input(c, "asistentes", "Asistentes aproximados (opcional)", "", 6, 2);
            input(c, "preferencias", "Canciones preferidas", "", 1000, 131073);
            input(c, "requerimientos", "Sonido y otros requerimientos", "", 1000, 131073);
            input(c, "notas", "Observaciones", "", 1500, 131073);
            button(
                    c,
                    "Continuar",
                    () -> {
                        capture();
                        if (draft.optString("tipoEvento").length() < 2
                                || draft.optString("lugar").length() < 5
                                || draft.optString("ciudad").length() < 2) {
                            invalid("Completa el evento, la dirección y la ciudad");
                            return;
                        }
                        if (!draft.optString("asistentes").isEmpty())
                            try {
                                int n = Integer.parseInt(draft.optString("asistentes"));
                                if (n < 1 || n > 100000) throw new Exception();
                            } catch (Exception e) {
                                invalid("Los asistentes deben estar entre 1 y 100 000");
                                return;
                            }
                        paso = 2;
                        render();
                        resetScroll();
                    });
        } else {
            label(c, "Revisa antes de enviar", true);
            label(
                    c,
                    "Inicio: "
                            + draft.optString("inicio")
                            + "\nFin: "
                            + draft.optString("fin")
                            + "\nDuración: "
                            + String.format(
                                    new Locale("es", "PE"),
                                    "%.2f h",
                                    Duration.between(
                                                            Instant.parse(
                                                                    utc(draft.optString("inicio"))),
                                                            Instant.parse(
                                                                    utc(draft.optString("fin"))))
                                                    .toMinutes()
                                            / 60.0),
                    false);
            for (String key :
                    new String[] {
                        "tipoEvento",
                        "lugar",
                        "ciudad",
                        "asistentes",
                        "preferencias",
                        "requerimientos",
                        "notas"
                    }) if (!draft.optString(key).isEmpty()) label(c, draft.optString(key), false);
            label(
                    c,
                    "El proveedor te enviará el precio y las condiciones. Esta solicitud no"
                        + " confirma una reserva.",
                    true);
            button(
                    c,
                    "Enviar solicitud",
                    () -> {
                        JSONObject body =
                                obj(
                                        "servicioId",
                                        requireArguments().getLong("id"),
                                        "inicio",
                                        utc(draft.optString("inicio")),
                                        "fin",
                                        utc(draft.optString("fin")),
                                        "tipoEvento",
                                        draft.optString("tipoEvento"),
                                        "lugar",
                                        draft.optString("lugar"),
                                        "ciudad",
                                        draft.optString("ciudad"),
                                        "preferencias",
                                        draft.optString("preferencias"),
                                        "requerimientos",
                                        draft.optString("requerimientos"),
                                        "notas",
                                        draft.optString("notas"),
                                        "clave",
                                        clave);
                        try {
                            if (!draft.optString("asistentes").isEmpty())
                                body.put(
                                        "asistentes",
                                        Integer.parseInt(draft.optString("asistentes")));
                        } catch (JSONException e) {
                            return;
                        }
                        request(
                                "POST",
                                "solicitudes",
                                body,
                                false,
                                r -> {
                                    // abrir() retira este asistente del back stack inmediatamente.
                                    var navigation = host();
                                    navigation.abrir("solicitudes");
                                    navigation.flujo("solicitud", r.optLong("id"));
                                });
                    });
        }
        if (paso > 0)
            button(
                    c,
                    "Atrás",
                    () -> {
                        capture();
                        paso--;
                        render();
                        resetScroll();
                    });
    }

    private final TextWatcher totals =
            new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

                public void onTextChanged(CharSequence s, int a, int b, int c) {
                    recalculate();
                }

                public void afterTextChanged(Editable e) {}
            };

    private void quote() {
        var c = card();
        label(c, "Horario propuesto", true);
        dateInput(c, "inicio", "Inicio", local(data.optString("inicio_evento_utc")));
        dateInput(c, "fin", "Fin", local(data.optString("fin_evento_utc")));
        label(content, "Conceptos (máximo 15)", true);
        JSONArray rows = draft.optJSONArray("lineas");
        if (rows == null) {
            rows = new JSONArray();
            rows.put(obj("concepto", "Servicio musical", "cantidad", "1", "precio", "0.00"));
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            LinearLayout lineBox = card();
            TextInputEditText
                    concepto = field(lineBox, "Concepto", row.optString("concepto"), 150, 1),
                    cantidad = field(lineBox, "Cantidad entera", row.optString("cantidad"), 4, 2),
                    precio =
                            field(
                                    lineBox,
                                    "Precio unitario (S/)",
                                    row.optString("precio"),
                                    12,
                                    8194);
            TextInputEditText[] line = {concepto, cantidad, precio};
            lines.add(line);
            cantidad.addTextChangedListener(totals);
            precio.addTextChangedListener(totals);
            final int index = i;
            button(
                    lineBox,
                    "Retirar concepto",
                    () -> {
                        if (lines.size() <= 1) {
                            invalid("Conserva al menos un concepto");
                            return;
                        }
                        capture();
                        JSONArray old = draft.optJSONArray("lineas");
                        old.remove(index);
                        render();
                    });
        }
        button(
                content,
                "Agregar concepto",
                () -> {
                    if (lines.size() >= 15) {
                        invalid("Puedes incluir hasta 15 conceptos");
                        return;
                    }
                    capture();
                    draft.optJSONArray("lineas")
                            .put(obj("concepto", "", "cantidad", "1", "precio", "0.00"));
                    render();
                });
        var terms = card();
        TextInputEditText descuento = input(terms, "descuento", "Descuento (S/)", "0.00", 12, 8194);
        descuento.addTextChangedListener(totals);
        totalView = label(terms, "", true);
        Instant event = Instant.parse(data.optString("inicio_evento_utc")),
                expiry = Instant.now().plus(Duration.ofDays(3));
        if (expiry.isAfter(event.minusSeconds(3600))) expiry = event.minusSeconds(3600);
        dateInput(terms, "vigencia", "Válida hasta", local(expiry.toString()));
        input(terms, "condiciones", "Condiciones del servicio *", "", 2000, 131073);
        consent =
                check(
                        terms,
                        "Incluir política de cancelación: En caso de cancelación por parte del"
                            + " cliente, no se devolverán los importes abonados. El rechazo de una"
                            + " cotización antes del pago no genera ningún cobro.",
                        draft.optBoolean("politica"));
        recalculate();
        button(
                terms,
                "Enviar cotización",
                () -> {
                    capture();
                    try {
                        if (!consent.isChecked())
                            throw new IllegalArgumentException(
                                    "Incluye expresamente la política de cancelación");
                        JSONArray conceptos = new JSONArray();
                        for (var line : lines) {
                            int cantidad = Integer.parseInt(value(line[1]));
                            long precio = cents(value(line[2]));
                            if (value(line[0]).length() < 2
                                    || cantidad < 1
                                    || cantidad > 1000
                                    || precio < 0)
                                throw new IllegalArgumentException(
                                        "Revisa los conceptos y sus importes");
                            conceptos.put(
                                    obj(
                                            "concepto",
                                            value(line[0]),
                                            "cantidad",
                                            cantidad,
                                            "precioCentimos",
                                            precio));
                        }
                        JSONObject body =
                                obj(
                                        "version",
                                        baseVersion,
                                        "inicio",
                                        utc(draft.optString("inicio")),
                                        "fin",
                                        utc(draft.optString("fin")),
                                        "conceptos",
                                        conceptos,
                                        "descuentoCentimos",
                                        cents(draft.optString("descuento")),
                                        "condiciones",
                                        draft.optString("condiciones"),
                                        "incluyePolitica",
                                        true,
                                        "vigenteHasta",
                                        utc(draft.optString("vigencia")),
                                        "clave",
                                        clave);
                        request(
                                "POST",
                                "solicitudes/" + requireArguments().getLong("id") + "/cotizaciones",
                                body,
                                false,
                                r -> {
                                    host().terminarFormulario();
                                });
                    } catch (Exception e) {
                        invalid(
                                e instanceof IllegalArgumentException && e.getMessage() != null
                                        ? e.getMessage()
                                        : "Revisa las fechas y los importes, con hasta dos"
                                              + " decimales");
                    }
                });
    }

    private void recalculate() {
        if (totalView == null) return;
        try {
            long total = 0;
            for (var l : lines)
                total =
                        Math.addExact(
                                total,
                                Math.multiplyExact(
                                        Integer.parseInt(value(l[1])), cents(value(l[2]))));
            TextInputEditText discount = fields.get("descuento");
            if (discount != null) total -= cents(value(discount));
            totalView.setText("Total: " + money(total));
        } catch (Exception e) {
            totalView.setText("Completa cantidades e importes para calcular el total");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        capture();
        out.putLong("baseVersion", baseVersion);
        out.putString("draft", draft.toString());
        out.putString("clave", clave);
        out.putInt("paso", paso);
        super.onSaveInstanceState(out);
    }

    @Override
    public void onStop() {
        capture();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        fields.clear();
        lines.clear();
        consent = null;
        totalView = null;
        super.onDestroyView();
    }
}
