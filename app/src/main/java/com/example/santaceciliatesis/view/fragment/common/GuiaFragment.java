package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class GuiaFragment extends BaseFragment {
    public GuiaFragment() {
        super(R.layout.fragment_guia);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        v(R.id.guia_explorar).setOnClickListener(v -> host().abrir("catalogo"));
        v(R.id.guia_cuenta).setOnClickListener(v -> host().mostrarCuenta());
        v(R.id.guia_privacidad)
                .setOnClickListener(
                        v ->
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.aviso_privacidad)
                                        .setMessage(R.string.aviso_privacidad_texto)
                                        .setPositiveButton(R.string.entendido, null)
                                        .show());
        v(R.id.guia_licencias)
                .setOnClickListener(
                        v -> {
                            StringBuilder text = new StringBuilder();
                            for (String name : new String[] {"Inter-OFL.txt", "Poppins-OFL.txt"})
                                try (java.io.BufferedReader reader =
                                        new java.io.BufferedReader(
                                                new java.io.InputStreamReader(
                                                        requireContext()
                                                                .getAssets()
                                                                .open("licenses/" + name),
                                                        java.nio.charset.StandardCharsets.UTF_8))) {
                                    String line;
                                    while ((line = reader.readLine()) != null)
                                        text.append(line).append('\n');
                                } catch (java.io.IOException e) {
                                    host().mostrarMensaje("No se pudo abrir la licencia.");
                                    return;
                                }
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.licencias)
                                    .setMessage(text.toString())
                                    .setPositiveButton(R.string.entendido, null)
                                    .show();
                        });
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.guia));
    }
}
