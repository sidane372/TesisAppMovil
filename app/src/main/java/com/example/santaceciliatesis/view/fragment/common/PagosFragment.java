package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.widget.*;

import com.example.santaceciliatesis.model.Rol;

import org.json.*;

import java.util.*;

/** Bandeja por actor y panel de saldos del contrato. Ningún botón realiza una transferencia. */
public final class PagosFragment extends FlujoBaseFragment implements PrivateScreen {
    private int pagina, estado;
    private String clave = UUID.randomUUID().toString();

    public static PagosFragment nuevo(long contrato) {
        var f = new PagosFragment();
        Bundle b = new Bundle();
        b.putLong("id", contrato);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle saved) {
        super.onViewCreated(view, saved);
        if (saved != null) {
            pagina = saved.getInt("pagina");
            estado = saved.getInt("estado");
            clave = saved.getString("clave", clave);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putInt("pagina", pagina);
        out.putInt("estado", estado);
        out.putString("clave", clave);
        super.onSaveInstanceState(out);
    }

    @Override
    public void refresh() {
        title("Pagos y saldos");
        long id = requireArguments().getLong("id");
        if (id > 0) request("GET", "contratos/" + id + "/pagos", null, false, this::contrato);
        else listar();
    }

    static String estado(String s) {
        switch (s) {
            case "PENDIENTE":
                return "Instrucciones listas";
            case "PROCESANDO":
                return "Comprobante pendiente de revisión";
            case "APROBADO":
                return "Abono verificado";
            case "RECHAZADO":
                return "Comprobante rechazado";
            case "EN_REVISION":
                return "Ingreso con incidencia";
            case "REEMBOLSO_PENDIENTE":
                return "Devolución pendiente";
            case "REEMBOLSADO":
                return "Devuelto";
            case "CANCELADO":
                return "Registro cancelado";
            default:
                return s.replace('_', ' ');
        }
    }

    private void listar() {
        clear();
        String[] labels = {
            "Todos",
            "Comprobantes por revisar",
            "Abonos verificados",
            "Con incidencia",
            "Devoluciones pendientes",
            "Devueltos",
            "Rechazados"
        };
        String[] codes = {
            "",
            "PROCESANDO",
            "APROBADO",
            "EN_REVISION",
            "REEMBOLSO_PENDIENTE",
            "REEMBOLSADO",
            "RECHAZADO"
        };
        var box = card();
        Spinner filter = new Spinner(requireContext());
        filter.setAdapter(
                new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, labels));
        filter.setSelection(estado);
        add(box, filter);
        button(
                box,
                "Filtrar",
                () -> {
                    estado = filter.getSelectedItemPosition();
                    pagina = 0;
                    refresh();
                });
        if (role() == Rol.ADMINISTRADOR)
            button(box, "Cuentas receptoras", () -> host().flujo("cuentas-cobro", 0));
        request(
                "GET",
                "pagos?pagina=" + pagina + (estado == 0 ? "" : "&estado=" + codes[estado]),
                null,
                false,
                d -> {
                    JSONArray rows = d.optJSONArray("items");
                    label(content, d.optLong("total") + " registros de pago", false);
                    if (rows == null || rows.length() == 0)
                        label(card(), "No hay registros para esta consulta.", false);
                    if (rows != null)
                        for (int i = 0; i < rows.length(); i++) {
                            var p = rows.optJSONObject(i);
                            var c = card();
                            label(c, p.optString("servicio"), true);
                            label(
                                    c,
                                    p.optString("metodo")
                                            + " · "
                                            + estado(p.optString("estado"))
                                            + "\n"
                                            + money(p.optLong("importe_centimos"))
                                            + "\n"
                                            + date(p.optString("creado_en")),
                                    false);
                            button(c, "Ver pago", () -> host().flujo("pago", p.optLong("id")));
                        }
                    if (pagina > 0)
                        button(
                                content,
                                "Anterior",
                                () -> {
                                    pagina--;
                                    refresh();
                                    resetScroll();
                                });
                    if ((pagina + 1L) * 20 < d.optLong("total"))
                        button(
                                content,
                                "Siguiente",
                                () -> {
                                    pagina++;
                                    refresh();
                                    resetScroll();
                                });
                });
    }

    private void contrato(JSONObject d) {
        clear();
        var box = card();
        label(box, "Resumen del contrato", true);
        label(
                box,
                "Total: "
                        + money(d.optLong("total_centimos"))
                        + "\nAbonado: "
                        + money(d.optLong("abonado_centimos"))
                        + "\nDevuelto: "
                        + money(d.optLong("reembolsado_centimos"))
                        + "\nSaldo: "
                        + money(d.optLong("saldo_centimos")),
                false);
        label(
                box,
                "Usa únicamente las cuentas que aparecen aquí. Adjuntar una captura no confirma el"
                        + " ingreso ni reserva el horario: administración debe contrastarlo en la"
                        + " cuenta receptora.",
                false);
        JSONArray rows = d.optJSONArray("pagos");
        boolean pending = false;
        if (rows != null)
            for (int i = 0; i < rows.length(); i++) {
                var p = rows.optJSONObject(i);
                pending |=
                        java.util.Arrays.asList(
                                        "PENDIENTE",
                                        "PROCESANDO",
                                        "EN_REVISION",
                                        "REEMBOLSO_PENDIENTE")
                                .contains(p.optString("estado"));
                var c = card();
                label(c, p.optString("metodo") + " · " + estado(p.optString("estado")), true);
                label(c, money(p.optLong("importe_centimos")), false);
                button(c, "Ver pago", () -> host().flujo("pago", p.optLong("id")));
            }
        if (role() != Rol.CLIENTE
                || d.optLong("saldo_centimos") <= 0
                || "CANCELADO".equals(d.optString("estadoContrato"))) return;
        if (pending) {
            label(
                    content,
                    "Consulta el registro pendiente antes de realizar otra transferencia.",
                    false);
            return;
        }
        JSONArray accounts = d.optJSONArray("cuentas");
        if (accounts == null || accounts.length() == 0) {
            label(
                    card(),
                    "Administración aún no ha habilitado cuentas receptoras. Solicita su"
                            + " configuración antes de transferir.",
                    false);
            return;
        }
        var form = card();
        label(form, "Registrar un pago directo", true);
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < accounts.length(); i++) {
            JSONObject a = accounts.optJSONObject(i);
            labels.add(a.optString("banco") + " · " + a.optString("titular"));
        }
        Spinner cuenta = selector(form, labels),
                medio = selector(form, new ArrayList<>()),
                modalidad =
                        selector(
                                form, Arrays.asList("Saldo completo", "Mitad del saldo pendiente"));
        TextView amount = label(form, "", true);
        List<String> medios = new ArrayList<>();
        cuenta.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onNothingSelected(android.widget.AdapterView<?> p) {}

                    public void onItemSelected(
                            android.widget.AdapterView<?> p,
                            android.view.View v,
                            int pos,
                            long id) {
                        medios.clear();
                        JSONObject a = accounts.optJSONObject(pos);
                        if (a.optBoolean("yape")) medios.add("YAPE");
                        if (a.optBoolean("plin")) medios.add("PLIN");
                        if (a.optBoolean("transferencia")) medios.add("TRANSFERENCIA");
                        medio.setAdapter(
                                new ArrayAdapter<>(
                                        requireContext(),
                                        android.R.layout.simple_spinner_dropdown_item,
                                        new ArrayList<>(medios)));
                    }
                });
        modalidad.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onNothingSelected(android.widget.AdapterView<?> p) {}

                    public void onItemSelected(
                            android.widget.AdapterView<?> p,
                            android.view.View v,
                            int pos,
                            long id) {
                        long balance = d.optLong("saldo_centimos");
                        amount.setText("Importe: " + money(pos == 0 ? balance : (balance + 1) / 2));
                    }
                });
        button(
                form,
                "Ver instrucciones y registrar",
                () -> {
                    if (medios.isEmpty()) {
                        invalid("Selecciona un medio habilitado");
                        return;
                    }
                    request(
                            "POST",
                            "contratos/" + requireArguments().getLong("id") + "/pagos",
                            obj(
                                    "cuentaId",
                                    accounts.optJSONObject(cuenta.getSelectedItemPosition())
                                            .optLong("id"),
                                    "metodo",
                                    medios.get(medio.getSelectedItemPosition()),
                                    "modalidad",
                                    modalidad.getSelectedItemPosition() == 0
                                            ? "SALDO_COMPLETO"
                                            : "MITAD_SALDO",
                                    "clave",
                                    clave),
                            false,
                            r -> {
                                clave = java.util.UUID.randomUUID().toString();
                                host().flujo("pago", r.optLong("id"));
                            });
                });
    }

    private Spinner selector(LinearLayout target, List<String> values) {
        Spinner s = new Spinner(requireContext());
        s.setAdapter(
                new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, values));
        add(target, s);
        return s;
    }
}
