package com.example.santaceciliatesis.view.dialog;

import android.view.*;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.util.Presentacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.*;

public final class EstadoDialog {
    public interface Action {
        void aceptar(String estado, String motivo);
    }

    private EstadoDialog() {}

    public static void show(Fragment fragment, String title, List<String> estados, Action action) {
        View body = fragment.getLayoutInflater().inflate(R.layout.dialog_estado, null);
        Spinner select = body.findViewById(R.id.estado_selector);
        ((TextView) body.findViewById(R.id.estado_ayuda))
                .setText(
                        estados.contains("ACTIVO") || estados.contains("SUSPENDIDO")
                                ? R.string.estado_cuenta_ayuda
                                : R.string.estado_servicio_ayuda);
        List<String> labels = new ArrayList<>();
        for (String state : estados) labels.add(Presentacion.estado(state));
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(fragment.requireContext(), R.layout.item_selector, labels);
        adapter.setDropDownViewResource(R.layout.item_selector);
        select.setAdapter(adapter);
        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(fragment.requireContext())
                        .setTitle(title)
                        .setView(body)
                        .setNegativeButton(R.string.cancelar, null)
                        .setPositiveButton(R.string.confirmar, null)
                        .show();
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            EditText input = body.findViewById(R.id.edit_motivo);
                            String reason = input.getText().toString().trim();
                            if (reason.length() < 5) {
                                ((TextInputLayout) body.findViewById(R.id.field_motivo))
                                        .setError("Indica un motivo de al menos cinco caracteres.");
                                return;
                            }
                            String state = estados.get(select.getSelectedItemPosition());
                            dialog.dismiss();
                            action.aceptar(state, reason);
                        });
    }
}
