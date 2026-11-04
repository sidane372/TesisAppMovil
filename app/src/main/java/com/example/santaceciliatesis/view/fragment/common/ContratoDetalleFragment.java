package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;

import org.json.*;

import java.time.Instant;

public final class ContratoDetalleFragment extends FlujoBaseFragment implements PrivateScreen {
    private JSONObject data;

    public static ContratoDetalleFragment nuevo(long id) {
        var f = new ContratoDetalleFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void refresh() {
        title("Contrato");
        request("GET", "contratos/" + requireArguments().getLong("id"), null, false, this::render);
    }

    private void render(JSONObject d) {
        data = d;
        clear();
        JSONObject snap = d.optJSONObject("snapshot"),
                s = snap.optJSONObject("servicio"),
                r = snap.optJSONObject("solicitud"),
                q = snap.optJSONObject("cotizacion");
        var box = card();
        label(box, s.optString("nombre"), true);
        label(box, d.optString("codigo") + "\n" + d.optString("estado").replace('_', ' '), false);
        label(box, money(d.optLong("total_centimos")), true);
        label(
                box,
                date(d.optString("inicio_evento_utc"))
                        + " → "
                        + date(d.optString("fin_evento_utc"))
                        + "\n"
                        + r.optString("tipo_evento")
                        + "\n"
                        + r.optString("lugar")
                        + " · "
                        + r.optString("ciudad"),
                false);
        for (String key : new String[] {"cliente", "proveedor"}) {
            JSONObject user = snap.optJSONObject(key);
            label(
                    box,
                    key.toUpperCase(new java.util.Locale("es", "PE"))
                            + ": "
                            + user.optString("nombres")
                            + " "
                            + user.optString("apellidos"),
                    false);
        }
        label(
                box,
                "PENDIENTE_PAGO".equals(d.optString("estado"))
                        ? "El contrato está pendiente de pago. El horario todavía no está"
                                + " reservado."
                        : "CONFIRMADO".equals(d.optString("estado"))
                                ? "Reserva confirmada con un abono verificado."
                                : "FINALIZADO".equals(d.optString("estado"))
                                        ? "Servicio finalizado."
                                        : "Contrato cancelado; se conserva su historial de pagos y"
                                              + " devoluciones.",
                false);
        button(box, "Pagos y comprobantes", () -> host().flujo("pagos", d.optLong("id")));
        var terms = card();
        label(terms, "Versión aceptada " + q.optInt("numero_version"), true);
        JSONArray lines = snap.optJSONArray("conceptos");
        if (lines != null)
            for (int i = 0; i < lines.length(); i++) {
                var l = lines.optJSONObject(i);
                label(
                        terms,
                        l.optString("concepto")
                                + " · "
                                + l.optString("cantidad")
                                + " × "
                                + money(l.optLong("precio_unitario_centimos"))
                                + " = "
                                + money(l.optLong("total_centimos")),
                        false);
            }
        label(
                terms,
                "Descuento: "
                        + money(q.optLong("descuento_centimos"))
                        + "\n"
                        + q.optString("condiciones"),
                false);
        label(terms, q.optString("politica_cancelacion"), false);
        label(
                terms,
                "Aceptado el "
                        + date(d.optString("aceptado_en"))
                        + "\nPreparación incluida en agenda: "
                        + date(d.optString("inicio_ocupacion_utc"))
                        + " → "
                        + date(d.optString("fin_ocupacion_utc"))
                        + "\nEstos márgenes no amplían las horas contratadas.",
                false);
        if ("CONFIRMADO".equals(d.optString("estado"))) {
            button(
                    box,
                    "Cancelar contrato",
                    () -> reason("Cancelar contrato", reason -> change("CANCELAR", reason)));
            if (actor() == d.optLong("id_proveedor")
                    || role() == com.example.santaceciliatesis.model.Rol.ADMINISTRADOR) {
                if (!Instant.parse(d.optString("ahora"))
                        .isBefore(Instant.parse(d.optString("fin_evento_utc"))))
                    button(
                            box,
                            "Registrar finalización",
                            () ->
                                    new com.google.android.material.dialog
                                                    .MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Registrar finalización")
                                            .setMessage(
                                                    "Se registrará el cierre del servicio con la"
                                                            + " hora del servidor.")
                                            .setNegativeButton("Volver", null)
                                            .setPositiveButton(
                                                    "Registrar",
                                                    (dialog, w) -> change("FINALIZAR", null))
                                            .show());
                else
                    label(
                            box,
                            "La finalización se habilita después de "
                                    + date(d.optString("fin_evento_utc")),
                            false);
            }
        }
        if (!d.isNull("motivo_cancelacion"))
            label(box, "Cancelado: " + d.optString("motivo_cancelacion"), false);
        label(content, "Historial conservado", true);
        JSONArray events = d.optJSONArray("historial");
        if (events != null)
            for (int i = 0; i < events.length(); i++) {
                var e = events.optJSONObject(i);
                label(
                        card(),
                        e.optString("accion").replace('_', ' ')
                                + "\n"
                                + date(e.optString("ocurrido_en"))
                                + "\n"
                                + SolicitudDetalleFragment.mensaje(e),
                        false);
            }
    }

    private void change(String action, String reason) {
        request(
                "POST",
                "contratos/" + data.optLong("id") + "/estado",
                obj("version", data.optLong("version"), "accion", action, "motivo", reason),
                false,
                this::render);
    }
}
