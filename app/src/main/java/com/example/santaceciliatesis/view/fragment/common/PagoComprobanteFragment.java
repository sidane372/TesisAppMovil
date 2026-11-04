package com.example.santaceciliatesis.view.fragment.common;

import android.net.Uri;
import android.os.Bundle;

import com.example.santaceciliatesis.data.remote.*;
import com.example.santaceciliatesis.data.repository.Callback;
import com.google.android.material.textfield.TextInputEditText;

import org.json.*;

import java.time.LocalDate;
import java.util.*;

/**
 * El selector del sistema concede acceso solo al archivo elegido. No se guardan imágenes en Bundle.
 */
public final class PagoComprobanteFragment extends FlujoBaseFragment implements PrivateScreen {
    private boolean devolucion;
    private JSONObject data, draft = new JSONObject();
    private Uri uri;
    private RequestHandle preparation;
    private final Map<String, TextInputEditText> fields = new LinkedHashMap<>();
    private final androidx.activity.result.ActivityResultLauncher<String[]> picker =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                    value -> {
                        if (value != null) {
                            uri = value;
                            try {
                                requireContext()
                                        .getContentResolver()
                                        .takePersistableUriPermission(
                                                value,
                                                android.content.Intent
                                                        .FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException ignored) {
                            }
                            host().mostrarMensaje("Imagen seleccionada");
                            if (getView() != null)
                                text(
                                        com.example.santaceciliatesis.R.id.flujo_estado,
                                        "Comprobante seleccionado. Revisa los datos antes de"
                                            + " enviar.");
                        }
                    });

    public static PagoComprobanteFragment nuevo(long id, boolean devolucion) {
        var f = new PagoComprobanteFragment();
        Bundle b = new Bundle();
        b.putLong("id", id);
        b.putBoolean("devolucion", devolucion);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(android.view.View view, Bundle saved) {
        super.onViewCreated(view, saved);
        devolucion = requireArguments().getBoolean("devolucion");
        if (saved != null) {
            String path = saved.getString("uri");
            if (path != null) uri = Uri.parse(path);
            try {
                draft = new JSONObject(saved.getString("draft", "{}"));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void refresh() {
        title(devolucion ? "Registrar devolución realizada" : "Enviar comprobante");
        if (data == null)
            request(
                    "GET",
                    (devolucion ? "reembolsos/" : "pagos/") + requireArguments().getLong("id"),
                    null,
                    false,
                    d -> {
                        data = d;
                        render();
                    });
        else render();
    }

    private void render() {
        clear();
        fields.clear();
        var box = card();
        label(
                box,
                devolucion
                        ? "La transferencia de devolución se realiza en la banca o billetera."
                              + " Registra aquí su comprobante y contrasta que llegó al"
                              + " destinatario correcto."
                        : "Adjuntar un comprobante no acredita el ingreso ni reserva el horario."
                              + " Administración contrastará la operación en la cuenta receptora.",
                false);
        label(box, "Importe previsto: " + money(data.optLong("importe_centimos")), true);
        fields.put(
                "operacion",
                field(box, "Número de operación", draft.optString("operacion"), 100, 1));
        fields.put(
                "fecha",
                dayField(
                        box,
                        "Fecha de la operación",
                        draft.optString("fecha", LocalDate.now(PERU).toString())));
        fields.put(
                "importe",
                field(
                        box,
                        "Importe transferido (S/)",
                        draft.optString("importe", decimal(data.optLong("importe_centimos"))),
                        14,
                        8194));
        if (!devolucion)
            fields.put(
                    "titular",
                    field(
                            box,
                            "Nombre del titular que realizó el pago",
                            draft.optString("titular"),
                            180,
                            1));
        label(
                box,
                "Selecciona una captura JPEG o PNG legible. No adjuntes claves, PIN, CVV ni tu"
                    + " estado de cuenta completo.",
                false);
        button(
                box,
                uri == null ? "Seleccionar comprobante" : "Cambiar comprobante seleccionado",
                () -> picker.launch(new String[] {"image/jpeg", "image/png"}));
        var consent =
                check(
                        box,
                        devolucion
                                ? "He contrastado la transferencia de devolución, su importe y"
                                      + " destinatario en la cuenta bancaria o billetera."
                                : "Los datos corresponden al pago que realicé para este contrato.",
                        false);
        var submit =
                button(
                        box,
                        devolucion
                                ? "Registrar devolución contrastada"
                                : "Enviar comprobante a revisión",
                        () -> {
                            capture();
                            if (uri == null) {
                                invalid("Selecciona el comprobante antes de enviar");
                                return;
                            }
                            final JSONObject body;
                            try {
                                LocalDate.parse(draft.optString("fecha"));
                                if (draft.optString("operacion").length() < 3)
                                    throw new Exception();
                                body =
                                        obj(
                                                "version",
                                                data.optLong("version"),
                                                "operacion",
                                                draft.optString("operacion"),
                                                "fecha",
                                                draft.optString("fecha"),
                                                "importeCentimos",
                                                cents(draft.optString("importe")));
                                if (devolucion) body.put("contrasteEnCuenta", consent.isChecked());
                                else body.put("titularOrigen", draft.optString("titular"));
                            } catch (Exception e) {
                                invalid(
                                        "Completa la operación, fecha e importe con hasta dos"
                                            + " decimales");
                                return;
                            }
                            ocupado(true);
                            preparation =
                                    deps().getContratacionRepository()
                                            .prepararImagen(
                                                    requireContext().getApplicationContext(),
                                                    uri,
                                                    new Callback<String>() {
                                                        public void onFailure(ApiFailure e) {
                                                            if (getView() == null) return;
                                                            ocupado(false);
                                                            error(e);
                                                        }

                                                        public void onSuccess(String image) {
                                                            if (getView() == null) return;
                                                            ocupado(false);
                                                            try {
                                                                body.put("imagenBase64", image);
                                                            } catch (JSONException e) {
                                                                invalid(
                                                                        "No se pudo preparar el"
                                                                            + " comprobante");
                                                                return;
                                                            }
                                                            request(
                                                                    devolucion ? "POST" : "PUT",
                                                                    devolucion
                                                                            ? "admin/reembolsos/"
                                                                                    + data.optLong(
                                                                                            "id")
                                                                                    + "/confirmacion"
                                                                            : "pagos/"
                                                                                    + data.optLong(
                                                                                            "id")
                                                                                    + "/comprobante",
                                                                    body,
                                                                    false,
                                                                    r -> {
                                                                        liberarUri();
                                                                        host().terminarFormulario();
                                                                    });
                                                        }
                                                    });
                        });
        requireConsent(submit, consent);
    }

    private void capture() {
        for (var e : fields.entrySet())
            try {
                draft.put(e.getKey(), value(e.getValue()));
            } catch (JSONException ignored) {
            }
    }

    private void liberarUri() {
        if (uri != null)
            try {
                requireContext()
                        .getContentResolver()
                        .releasePersistableUriPermission(
                                uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
        uri = null;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        capture();
        out.putString("draft", draft.toString());
        if (uri != null) out.putString("uri", uri.toString());
        super.onSaveInstanceState(out);
    }

    @Override
    public void onStop() {
        capture();
        if (preparation != null) preparation.cancel();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        fields.clear();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (isRemoving() || (getActivity() != null && getActivity().isFinishing())) liberarUri();
        super.onDestroy();
    }
}
