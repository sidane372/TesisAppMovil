package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.widget.LinearLayout;

import org.json.*;

import java.util.UUID;

public final class SolicitudDetalleFragment extends FlujoBaseFragment implements PrivateScreen {
    private JSONObject data;
    private String clave = UUID.randomUUID().toString();

    public static SolicitudDetalleFragment nuevo(long id) {
        var f = new SolicitudDetalleFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle saved) {
        super.onViewCreated(view, saved);
        if (saved != null) clave = saved.getString("clave", clave);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString("clave", clave);
        super.onSaveInstanceState(out);
    }

    @Override
    public void refresh() {
        title("Detalle de solicitud");
        request(
                "GET",
                "solicitudes/" + requireArguments().getLong("id"),
                null,
                false,
                this::render);
    }

    private void state(String accion, String motivo) {
        request(
                "POST",
                "solicitudes/" + data.optLong("id") + "/estado",
                obj("version", data.optLong("version"), "accion", accion, "motivo", motivo),
                false,
                this::render);
    }

    private void response(JSONObject quote, String accion, String motivo) {
        request(
                "POST",
                "solicitudes/"
                        + data.optLong("id")
                        + "/cotizaciones/"
                        + quote.optLong("id")
                        + "/respuesta",
                obj("version", data.optLong("version"), "accion", accion, "motivo", motivo),
                false,
                this::render);
    }

    private void render(JSONObject value) {
        data = value;
        clear();
        var c = card();
        label(c, data.optString("servicio"), true);
        label(c, data.optString("estado").replace('_', ' '), false);
        label(
                c,
                data.optString("cliente")
                        + "\n"
                        + data.optString("tipo_evento")
                        + " · "
                        + data.optString("ciudad")
                        + "\n"
                        + date(data.optString("inicio_evento_utc"))
                        + " → "
                        + date(data.optString("fin_evento_utc"))
                        + "\n"
                        + data.optString("lugar"),
                false);
        for (String key : new String[] {"asistentes", "preferencias", "requerimientos", "notas"})
            if (!data.isNull(key) && !data.optString(key).isEmpty())
                label(
                        c,
                        key.toUpperCase(new java.util.Locale("es")) + "\n" + data.optString(key),
                        false);
        boolean contracted = !data.isNull("contratoId");
        boolean open =
                java.util.Set.of("ENVIADA", "EN_REVISION", "COTIZACION_RECIBIDA")
                                .contains(data.optString("estado"))
                        && !contracted;
        if (contracted)
            button(c, "Abrir contrato", () -> host().flujo("contrato", data.optLong("contratoId")));
        if (open && actor() == data.optLong("id_cliente"))
            button(
                    c,
                    "Cancelar solicitud",
                    () -> reason("Cancelar solicitud", reason -> state("CANCELAR", reason)));
        if (open && actor() == data.optLong("id_proveedor")) {
            button(
                    c,
                    "Elaborar nueva cotización",
                    () -> host().flujo("cotizar", data.optLong("id")));
            button(c, "Marcar en revisión", () -> state("REVISION", null));
            button(
                    c,
                    "Rechazar solicitud",
                    () -> reason("Rechazar solicitud", reason -> state("RECHAZAR", reason)));
        }
        JSONArray versions = data.optJSONArray("cotizaciones");
        label(content, "Propuestas y versiones", true);
        if (versions == null || versions.length() == 0)
            label(card(), "El proveedor aún no ha enviado una cotización.", false);
        if (versions != null)
            for (int i = 0; i < versions.length(); i++) {
                JSONObject q = versions.optJSONObject(i);
                LinearLayout box = card();
                label(box, "PROPUESTA · VERSIÓN " + q.optInt("numero_version"), true);
                label(box, q.optString("estado"), false);
                label(box, money(q.optLong("total_centimos")), true);
                label(
                        box,
                        date(q.optString("inicio_propuesto_utc"))
                                + " → "
                                + date(q.optString("fin_propuesto_utc")),
                        false);
                JSONArray lines = q.optJSONArray("conceptos");
                if (lines != null)
                    for (int n = 0; n < lines.length(); n++) {
                        var l = lines.optJSONObject(n);
                        label(
                                box,
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
                        box,
                        "Subtotal: "
                                + money(q.optLong("subtotal_centimos"))
                                + "\nDescuento: "
                                + money(q.optLong("descuento_centimos")),
                        false);
                label(box, "Condiciones\n" + q.optString("condiciones"), false);
                label(
                        box,
                        "Política de cancelación · "
                                + q.optString("politica_version")
                                + "\n"
                                + q.optString("politica_cancelacion"),
                        false);
                label(box, "Válida hasta " + date(q.optString("vigente_hasta_utc")), false);
                if (i == 0
                        && open
                        && actor() == data.optLong("id_cliente")
                        && "ENVIADA".equals(q.optString("estado"))) {
                    var consent =
                            check(
                                    box,
                                    "He leído y acepto el precio, el horario y las condiciones de"
                                            + " esta versión.",
                                    false);
                    var acceptAction =
                            button(
                                    box,
                                    "Aceptar y crear contrato",
                                    () -> {
                                        if (!consent.isChecked()) {
                                            invalid(
                                                    "Acepta expresamente esta versión para"
                                                        + " continuar");
                                            return;
                                        }
                                        new com.google.android.material.dialog
                                                        .MaterialAlertDialogBuilder(
                                                        requireContext())
                                                .setTitle(
                                                        "Aceptar versión "
                                                                + q.optInt("numero_version"))
                                                .setMessage(
                                                        "Total: "
                                                                + money(q.optLong("total_centimos"))
                                                                + ". El contrato quedará pendiente"
                                                                + " de pago. El horario aún no"
                                                                + " estará reservado.")
                                                .setNegativeButton("Volver", null)
                                                .setPositiveButton(
                                                        "Aceptar",
                                                        (d, w) ->
                                                                request(
                                                                        "POST",
                                                                        "solicitudes/"
                                                                                + data.optLong("id")
                                                                                + "/cotizaciones/"
                                                                                + q.optLong("id")
                                                                                + "/aceptacion",
                                                                        obj(
                                                                                "version",
                                                                                data.optLong(
                                                                                        "version"),
                                                                                "aceptaCondiciones",
                                                                                true,
                                                                                "clave",
                                                                                clave),
                                                                        false,
                                                                        r ->
                                                                                host().flujo(
                                                                                                "contrato",
                                                                                                r
                                                                                                        .optLong(
                                                                                                                "id"))))
                                                .show();
                                    });
                    requireConsent(acceptAction, consent);
                    button(
                            box,
                            "Solicitar cambios",
                            () ->
                                    reason(
                                            "Solicitar cambios",
                                            reason -> response(q, "CAMBIOS", reason)));
                    button(
                            box,
                            "Rechazar propuesta",
                            () ->
                                    reason(
                                            "Rechazar propuesta",
                                            reason -> response(q, "RECHAZAR", reason)));
                }
            }
        label(content, "Historial", true);
        JSONArray events = data.optJSONArray("historial");
        if (events != null)
            for (int n = 0; n < events.length(); n++) {
                var e = events.optJSONObject(n);
                label(
                        card(),
                        e.optString("accion").replace('_', ' ')
                                + "\n"
                                + date(e.optString("ocurrido_en"))
                                + "\n"
                                + mensaje(e),
                        false);
            }
    }

    static String mensaje(JSONObject event) {
        try {
            return new JSONObject(event.optString("detalle")).optString("mensaje");
        } catch (Exception e) {
            return "";
        }
    }
}
