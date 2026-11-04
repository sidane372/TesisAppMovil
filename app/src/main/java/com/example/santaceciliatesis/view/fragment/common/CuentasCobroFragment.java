package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import org.json.*;

import java.util.*;

public final class CuentasCobroFragment extends FlujoBaseFragment implements PrivateScreen {
    private JSONObject account, draft = new JSONObject();
    private final Map<String, TextInputEditText> fields = new LinkedHashMap<>();
    private final Map<String, MaterialCheckBox> flags = new LinkedHashMap<>();

    public static CuentasCobroFragment nuevo(long id) {
        var f = new CuentasCobroFragment();
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
                draft = new JSONObject(saved.getString("draft", "{}"));
            } catch (Exception ignored) {
            }
    }

    @Override
    public void refresh() {
        title("Cuentas receptoras");
        long id = requireArguments().getLong("id");
        if (id == 0) {
            if (account == null) account = obj("version", 0, "activo", true);
            render();
            return;
        }
        request(
                "GET",
                "admin/cuentas-cobro",
                null,
                false,
                d -> {
                    JSONArray rows = d.optJSONArray("servicios");
                    if (rows == null) rows = new JSONArray();
                    if (id < 0) {
                        clear();
                        button(
                                content,
                                "Agregar cuenta receptora",
                                () -> host().flujo("cuenta-cobro", 0));
                        label(
                                content,
                                "Registra una sola cuenta por destino bancario. Si Yape, Plin y"
                                    + " transferencias llegan a esa misma cuenta, habilita los"
                                    + " medios en el mismo registro.",
                                false);
                        if (rows.length() == 0)
                            label(card(), "No hay cuentas receptoras configuradas.", false);
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.optJSONObject(i);
                            var box = card();
                            label(
                                    box,
                                    row.optString("banco") + " · " + row.optString("titular"),
                                    true);
                            label(
                                    box,
                                    row.optBoolean("activo") ? "Habilitada" : "Desactivada",
                                    false);
                            button(
                                    box,
                                    "Administrar cuenta",
                                    () -> host().flujo("cuenta-cobro", row.optLong("id")));
                        }
                    } else {
                        for (int i = 0; i < rows.length(); i++)
                            if (rows.optJSONObject(i).optLong("id") == id) {
                                account = rows.optJSONObject(i);
                                render();
                                return;
                            }
                        invalid("La cuenta ya no está disponible");
                    }
                });
    }

    private void render() {
        clear();
        fields.clear();
        flags.clear();
        var box = card();
        label(box, "Datos que verá el cliente para pagar", true);
        label(
                box,
                "Si la cuenta ya tiene historial, sus datos receptores quedan conservados. Puedes"
                    + " desactivarla y registrar otra para un nuevo destino.",
                false);
        for (String[] f :
                new String[][] {
                    {"titular", "Titular receptor", "180"},
                    {"banco", "Banco o entidad", "100"},
                    {"telefono", "Celular de Yape / Plin (9 dígitos)", "9"},
                    {"numeroCuenta", "Número de cuenta (opcional)", "40"},
                    {"cci", "CCI de 20 dígitos (opcional)", "20"}
                }) {
            String db = f[0].equals("numeroCuenta") ? "numero_cuenta" : f[0];
            String value =
                    draft.has(f[0])
                            ? draft.optString(f[0])
                            : account.isNull(db) ? "" : account.optString(db);
            fields.put(
                    f[0],
                    field(
                            box,
                            f[1],
                            value,
                            Integer.parseInt(f[2]),
                            f[0].equals("titular") || f[0].equals("banco") ? 1 : 2));
        }
        for (String[] f :
                new String[][] {
                    {"yape", "Recibir Yape"},
                    {"plin", "Recibir Plin"},
                    {"transferencia", "Recibir transferencias bancarias"},
                    {"activo", "Cuenta habilitada para nuevos pagos"}
                })
            flags.put(
                    f[0],
                    check(
                            box,
                            f[1],
                            draft.has(f[0]) ? draft.optBoolean(f[0]) : account.optBoolean(f[0])));
        button(
                box,
                "Guardar cuenta receptora",
                () -> {
                    capture();
                    JSONObject body = obj("version", account.optLong("version"));
                    try {
                        for (var f : fields.entrySet()) {
                            String value = value(f.getValue());
                            if (!value.isEmpty()) body.put(f.getKey(), value);
                        }
                        for (var f : flags.entrySet())
                            body.put(f.getKey(), f.getValue().isChecked());
                    } catch (JSONException e) {
                        return;
                    }
                    long id = requireArguments().getLong("id");
                    request(
                            id == 0 ? "POST" : "PUT",
                            "admin/cuentas-cobro" + (id == 0 ? "" : "/" + id),
                            body,
                            false,
                            d -> host().terminarFormulario());
                });
    }

    private void capture() {
        for (var f : fields.entrySet())
            try {
                draft.put(f.getKey(), value(f.getValue()));
            } catch (JSONException ignored) {
            }
        for (var f : flags.entrySet())
            try {
                draft.put(f.getKey(), f.getValue().isChecked());
            } catch (JSONException ignored) {
            }
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        capture();
        out.putString("draft", draft.toString());
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
        flags.clear();
        super.onDestroyView();
    }
}
