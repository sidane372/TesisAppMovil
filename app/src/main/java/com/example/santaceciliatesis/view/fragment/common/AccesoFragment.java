package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;
import androidx.fragment.app.Fragment;

import com.example.santaceciliatesis.*;
import com.example.santaceciliatesis.controller.AuthController;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.view.activity.NavigationHost;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.*;

public final class AccesoFragment extends Fragment implements AuthController.View {
    private String modo;
    private AuthController controller;
    private TextInputLayout correo, password, nombres, apellidos;
    private MaterialCheckBox privacy;
    private MaterialButton submit;
    private View progress;
    private TextView status;

    public AccesoFragment() {
        super(R.layout.fragment_acceso);
    }

    public static AccesoFragment nuevo(String modo) {
        AccesoFragment f = new AccesoFragment();
        Bundle args = new Bundle();
        args.putString("modo", modo);
        f.setArguments(args);
        return f;
    }

    private NavigationHost host() {
        return (NavigationHost) requireActivity();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        modo = requireArguments().getString("modo", "LOGIN");
        view.findViewById(R.id.acceso_volver)
                .setOnClickListener(
                        v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.acceso_ya_tengo_cuenta)
                .setOnClickListener(v -> host().mostrarAcceso("LOGIN"));
        controller =
                new AuthController(
                        ((SantaCeciliaApplication) requireActivity().getApplication())
                                .getContainer()
                                .getAuthRepository());
        correo = view.findViewById(R.id.acceso_correo);
        password = view.findViewById(R.id.acceso_password);
        nombres = view.findViewById(R.id.acceso_nombres);
        apellidos = view.findViewById(R.id.acceso_apellidos);
        privacy = view.findViewById(R.id.acceso_privacidad);
        submit = view.findViewById(R.id.acceso_submit);
        progress = view.findViewById(R.id.acceso_progress);
        status = view.findViewById(R.id.acceso_estado);
        boolean registro = "REGISTRO".equals(modo), forgot = "RECUPERAR".equals(modo);
        ((TextView) view.findViewById(R.id.acceso_titulo))
                .setText(
                        registro
                                ? R.string.registro_titulo
                                : forgot ? R.string.recuperar_titulo : R.string.login_titulo);
        ((TextView) view.findViewById(R.id.acceso_descripcion))
                .setText(
                        registro
                                ? R.string.registro_descripcion
                                : forgot
                                        ? R.string.recuperar_descripcion
                                        : R.string.login_descripcion);
        view.findViewById(R.id.acceso_ya_tengo_cuenta)
                .setVisibility(registro || forgot ? View.VISIBLE : View.GONE);
        password.getEditText().setAutofillHints(registro ? "newPassword" : "password");
        nombres.setVisibility(registro ? View.VISIBLE : View.GONE);
        apellidos.setVisibility(registro ? View.VISIBLE : View.GONE);
        privacy.setVisibility(registro ? View.VISIBLE : View.GONE);
        password.setVisibility(forgot ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.acceso_aviso).setVisibility(registro ? View.VISIBLE : View.GONE);
        submit.setText(
                registro
                        ? R.string.crear_cuenta
                        : forgot ? R.string.enviar_enlace : R.string.iniciar_sesion);
        view.findViewById(R.id.acceso_registro)
                .setVisibility("LOGIN".equals(modo) ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.acceso_recuperar)
                .setVisibility("LOGIN".equals(modo) ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.acceso_registro)
                .setOnClickListener(v -> host().mostrarAcceso("REGISTRO"));
        view.findViewById(R.id.acceso_recuperar)
                .setOnClickListener(v -> host().mostrarAcceso("RECUPERAR"));
        view.findViewById(R.id.acceso_aviso)
                .setOnClickListener(
                        v ->
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle(R.string.aviso_privacidad)
                                        .setMessage(R.string.aviso_privacidad_texto)
                                        .setPositiveButton(R.string.entendido, null)
                                        .show());
        submit.setOnClickListener(
                v -> {
                    for (TextInputLayout field :
                            Arrays.asList(correo, password, nombres, apellidos))
                        field.setError(null);
                    privacy.setError(null);
                    status.setText("");
                    if (!host().solicitarRedLocal()) return;
                    String email = text(correo), secret = text(password);
                    if (registro)
                        controller.registrar(
                                text(nombres), text(apellidos), email, secret, privacy.isChecked());
                    else if (forgot) controller.recuperar(email);
                    else controller.login(email, secret);
                });
    }

    private String text(TextInputLayout field) {
        return Objects.requireNonNull(field.getEditText()).getText().toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().getWindow().setBackgroundDrawableResource(R.color.sc_access_background);
        controller.attach(this);
    }

    @Override
    public void onStop() {
        controller.detach();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (password != null && password.getEditText() != null) password.getEditText().setText("");
        correo = password = nombres = apellidos = null;
        privacy = null;
        submit = null;
        progress = null;
        status = null;
        super.onDestroyView();
    }

    @Override
    public void onBusy(boolean busy) {
        submit.setEnabled(!busy);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        for (TextInputLayout field : Arrays.asList(correo, password, nombres, apellidos))
            field.setEnabled(!busy);
        privacy.setEnabled(!busy);
    }

    @Override
    public void onError(ApiFailure error) {
        status.setText(error.getMessage());
        Map<String, String> fields = error.getFields();
        correo.setError(fields.get("correo"));
        password.setError(fields.get("password"));
        nombres.setError(fields.get("nombres"));
        apellidos.setError(fields.get("apellidos"));
        if (fields.containsKey("aceptaPrivacidad")) {
            privacy.setError(fields.get("aceptaPrivacidad"));
            status.setText(fields.get("aceptaPrivacidad"));
        }
    }

    @Override
    public void onLoggedIn() {
        Objects.requireNonNull(password.getEditText()).setText("");
        host().mostrarInicio();
    }

    @Override
    public void onRegistered() {
        Objects.requireNonNull(password.getEditText()).setText("");
        // Volver al login puede retirar este Fragment al vaciar la pila de navegación.
        NavigationHost navigation = host();
        String message = getString(R.string.cuenta_creada);
        navigation.mostrarAcceso("LOGIN");
        navigation.mostrarMensaje(message);
    }

    @Override
    public void onRecoveryRequested(String message) {
        status.setText(message);
    }
}
