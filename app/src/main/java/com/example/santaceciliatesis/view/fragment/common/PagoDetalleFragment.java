package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;

import com.example.santaceciliatesis.model.Rol;

import org.json.*;

import java.util.*;

public final class PagoDetalleFragment extends FlujoBaseFragment implements PrivateScreen {
    private JSONObject data;
    private String clave = UUID.randomUUID().toString();

    public static PagoDetalleFragment nuevo(long id) {
        var f = new PagoDetalleFragment();
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
        title("Detalle de pago");
        request("GET", "pagos/" + requireArguments().getLong("id"), null, false, this::render);
    }

    private void render(JSONObject d) {
        data = d;
        clear();
        JSONObject direct = d.optJSONObject("directo"),
                destino = direct.optJSONObject("destino_snapshot");
        var box = card();
        label(
                box,
                d.optString("metodo") + " · " + PagosFragment.estado(d.optString("estado")),
                true);
        label(box, "Importe previsto: " + money(d.optLong("importe_centimos")), true);
        label(
                box,
                "Cuenta receptora\n"
                        + destino.optString("titular")
                        + "\n"
                        + destino.optString("banco"),
                false);
        for (String key : new String[] {"telefono", "numero_cuenta", "cci"})
            if (!destino.isNull(key))
                label(
                        box,
                        (key.equals("telefono")
                                        ? "Celular: "
                                        : key.equals("cci") ? "CCI: " : "Cuenta: ")
                                + destino.optString(key),
                        false);
        label(
                box,
                "Realiza el pago en tu app bancaria o billetera y registra aquí el comprobante."
                    + " Este sistema no debita dinero de tus cuentas.",
                false);
        if ("PENDIENTE".equals(d.optString("estado")) && role() == Rol.CLIENTE) {
            button(box, "Adjuntar comprobante", () -> host().flujo("comprobante", d.optLong("id")));
            button(
                    box,
                    "Cancelar registro sin comprobante",
                    () ->
                            new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                                            requireContext())
                                    .setTitle("Cancelar registro")
                                    .setMessage(
                                            "Esto no revierte ningún Yape, Plin o transferencia que"
                                                + " ya hayas realizado.")
                                    .setNegativeButton("Volver", null)
                                    .setPositiveButton(
                                            "Cancelar registro",
                                            (dialog, w) ->
                                                    request(
                                                            "POST",
                                                            "pagos/"
                                                                    + d.optLong("id")
                                                                    + "/cancelacion",
                                                            obj("version", d.optLong("version")),
                                                            false,
                                                            this::render))
                                    .show());
        }
        if (d.optBoolean("comprobante")) {
            label(
                    box,
                    "Reportado por: "
                            + direct.optString("titular_origen")
                            + "\nOperación: "
                            + direct.optString("operacion_reportada")
                            + "\nFecha: "
                            + direct.optString("fecha_reportada")
                            + "\nMonto reportado: "
                            + money(direct.optLong("importe_reportado_centimos")),
                    false);
            button(
                    box,
                    "Ver comprobante del cliente",
                    () -> verImagen("pagos/" + d.optLong("id") + "/comprobante"));
        }
        if (!direct.isNull("motivo_revision"))
            label(box, "Revisión: " + direct.optString("motivo_revision"), false);
        JSONArray movements = d.optJSONArray("movimientos");
        long received = 0;
        if (movements != null)
            for (int i = 0; i < movements.length(); i++) {
                var m = movements.optJSONObject(i);
                received += m.optLong("importe_centimos");
                label(
                        box,
                        "Ingreso contrastado: "
                                + money(m.optLong("importe_centimos"))
                                + "\nOperación verificada: "
                                + m.optString("operacion")
                                + " · "
                                + m.optString("fecha"),
                        false);
            }
        var balance = d.optJSONObject("saldo");
        label(
                box,
                "Abonado al contrato: "
                        + money(balance.optLong("abonado_centimos"))
                        + "\nSaldo contractual: "
                        + money(balance.optLong("saldo_centimos")),
                false);
        button(box, "Ver contrato", () -> host().flujo("contrato", d.optLong("id_contrato")));
        if (role() == Rol.ADMINISTRADOR && "PROCESANDO".equals(d.optString("estado"))) {
            var form = card();
            label(form, "Contrastar ingreso en cuenta", true);
            label(
                    form,
                    "Abre los movimientos de la cuenta receptora. Verifica el ingreso y sus datos;"
                        + " la captura del cliente por sí sola no acredita el pago.",
                    false);
            var op =
                    field(
                            form,
                            "Operación verificada",
                            direct.optString("operacion_reportada"),
                            100,
                            1);
            var fecha = dayField(form, "Fecha del movimiento", direct.optString("fecha_reportada"));
            var monto =
                    field(
                            form,
                            "Importe recibido (S/)",
                            decimal(direct.optLong("importe_reportado_centimos")),
                            14,
                            8194);
            var note = field(form, "Observación de revisión (opcional)", "", 1000, 131073);
            var consent =
                    check(
                            form,
                            "He contrastado el ingreso, el importe, la fecha y la cuenta receptora"
                                + " en los movimientos de la banca o billetera.",
                            false);
            var approve =
                    button(
                            form,
                            "Registrar conciliación del ingreso",
                            () -> {
                                try {
                                    long amount = cents(value(monto));
                                    new com.google.android.material.dialog
                                                    .MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Conciliar " + money(amount))
                                            .setMessage(
                                                    "Se registrará que administración verificó este"
                                                        + " ingreso real. La reserva se confirmará"
                                                        + " solo si el contrato y su horario lo"
                                                        + " permiten.")
                                            .setNegativeButton("Volver", null)
                                            .setPositiveButton(
                                                    "Registrar",
                                                    (dialog, w) ->
                                                            request(
                                                                    "POST",
                                                                    "admin/pagos/"
                                                                            + d.optLong("id")
                                                                            + "/revision",
                                                                    obj(
                                                                            "version",
                                                                            d.optLong("version"),
                                                                            "accion",
                                                                            "VERIFICAR",
                                                                            "contrasteEnCuenta",
                                                                            consent.isChecked(),
                                                                            "operacion",
                                                                            value(op),
                                                                            "fecha",
                                                                            value(fecha),
                                                                            "importeCentimos",
                                                                            amount,
                                                                            "motivo",
                                                                            value(note)),
                                                                    false,
                                                                    this::render))
                                            .show();
                                } catch (Exception e) {
                                    invalid("Revisa el importe, con hasta dos decimales");
                                }
                            });
            requireConsent(approve, consent);
            button(
                    form,
                    "Rechazar comprobante con motivo",
                    () ->
                            reason(
                                    "Rechazar comprobante",
                                    reason ->
                                            request(
                                                    "POST",
                                                    "admin/pagos/" + d.optLong("id") + "/revision",
                                                    obj(
                                                            "version",
                                                            d.optLong("version"),
                                                            "accion",
                                                            "RECHAZAR",
                                                            "motivo",
                                                            reason),
                                                    false,
                                                    this::render)));
        }
        JSONArray incidents = d.optJSONArray("incidencias");
        if (incidents != null)
            for (int i = 0; i < incidents.length(); i++) {
                var e = incidents.optJSONObject(i);
                label(
                        card(),
                        "Incidencia · "
                                + e.optString("estado")
                                + "\n"
                                + e.optString("tipo").replace('_', ' ')
                                + "\n"
                                + e.optString("detalle"),
                        false);
            }
        JSONArray refunds = d.optJSONArray("reembolsos");
        long committed = 0;
        if (refunds != null)
            for (int i = 0; i < refunds.length(); i++) {
                JSONObject r = refunds.optJSONObject(i);
                if (!"RECHAZADO".equals(r.optString("estado")))
                    committed += r.optLong("importe_centimos");
                var c = card();
                label(c, "Devolución · " + r.optString("estado"), true);
                label(
                        c,
                        money(r.optLong("importe_centimos")) + "\n" + r.optString("motivo"),
                        false);
                if ("APROBADO".equals(r.optString("estado")))
                    button(
                            c,
                            "Ver comprobante de devolución",
                            () -> verImagen("reembolsos/" + r.optLong("id") + "/comprobante"));
                if (role() == Rol.ADMINISTRADOR && "PENDIENTE".equals(r.optString("estado")))
                    button(
                            c,
                            "Registrar devolución realizada",
                            () -> host().flujo("devolucion", r.optLong("id")));
                if (role() == Rol.ADMINISTRADOR && "PENDIENTE".equals(r.optString("estado")))
                    button(
                            c,
                            "Anular solicitud de devolución",
                            () ->
                                    reason(
                                            "Anular solicitud no transferida",
                                            reason ->
                                                    request(
                                                            "POST",
                                                            "admin/reembolsos/"
                                                                    + r.optLong("id")
                                                                    + "/rechazo",
                                                            obj(
                                                                    "version",
                                                                    r.optLong("version"),
                                                                    "accion",
                                                                    "RECHAZAR",
                                                                    "motivo",
                                                                    reason),
                                                            false,
                                                            this::render)));
            }
        long available = received - committed;
        if (role() == Rol.ADMINISTRADOR
                && available > 0
                && (d.isNull("aplicado_en") || "CANCELADO".equals(d.optString("contratoEstado")))) {
            var f = card();
            label(f, "Solicitar devolución documentada", true);
            label(
                    f,
                    "La devolución se realiza fuera de la app. Este registro no devuelve dinero por"
                        + " sí solo.",
                    false);
            var amount = field(f, "Importe a devolver (S/)", decimal(available), 14, 8194);
            var reason = field(f, "Motivo de devolución", "", 1000, 131073);
            button(
                    f,
                    "Registrar solicitud de devolución",
                    () -> {
                        try {
                            request(
                                    "POST",
                                    "admin/pagos/" + d.optLong("id") + "/reembolsos",
                                    obj(
                                            "importeCentimos",
                                            cents(value(amount)),
                                            "motivo",
                                            value(reason),
                                            "clave",
                                            clave),
                                    false,
                                    r -> {
                                        clave = UUID.randomUUID().toString();
                                        render(r);
                                    });
                        } catch (Exception e) {
                            invalid("Revisa el importe a devolver");
                        }
                    });
        }
    }

    private void verImagen(String path) {
        request(
                "GET",
                path,
                null,
                false,
                r -> ComprobanteDialog.mostrar(this, r.optString("imagenBase64")));
    }
}
