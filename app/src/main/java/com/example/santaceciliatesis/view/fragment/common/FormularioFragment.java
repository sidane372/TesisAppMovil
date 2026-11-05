package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.FormularioController;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.Usuario;
import com.example.santaceciliatesis.util.Presentacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.*;

public final class FormularioFragment extends BaseFragment
        implements PrivateScreen, FormularioController.View, Refreshable {
    private FormularioController controller;
    private String mode, version = "";
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private final Map<String, TextInputLayout> labels = new LinkedHashMap<>();
    private List<Usuario> owners = Collections.emptyList();
    private boolean ready, busy;
    private Bundle draft;
    private long savedOwner;

    public FormularioFragment() {
        super(R.layout.fragment_formulario);
    }

    public static FormularioFragment nuevo(String mode, long id, long song) {
        FormularioFragment f = new FormularioFragment();
        Bundle b = new Bundle();
        b.putString("mode", mode);
        b.putLong("id", id);
        b.putLong("song", song);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        mode = requireArguments().getString("mode");
        controller =
                new FormularioController(
                        mode,
                        requireArguments().getLong("id"),
                        requireArguments().getLong("song"),
                        deps().getComunidadRepository(),
                        deps().getAuthRepository());
        int layout =
                mode.startsWith("USUARIO") || mode.equals("PERFIL")
                        ? R.layout.form_usuario
                        : mode.startsWith("SERVICIO")
                                ? R.layout.form_servicio
                                : mode.equals("TARIFA")
                                        ? R.layout.form_tarifa
                                        : R.layout.form_cancion;
        LinearLayout panel = v(R.id.formulario_campos);
        getLayoutInflater().inflate(layout, panel, true);
        Map<String, Integer> ids = new LinkedHashMap<>();
        ids.put("nombres", R.id.edit_nombres);
        ids.put("apellidos", R.id.edit_apellidos);
        ids.put("correo", R.id.edit_correo);
        ids.put("password", R.id.edit_password);
        ids.put("telefono", R.id.edit_telefono);
        ids.put("ciudad", R.id.edit_ciudad);
        ids.put("biografia", R.id.edit_biografia);
        ids.put("nombre", R.id.edit_nombre);
        ids.put("generos", R.id.edit_generos);
        ids.put("instrumentos", R.id.edit_instrumentos);
        ids.put("tiposEvento", R.id.edit_tiposEvento);
        ids.put("experiencia", R.id.edit_experiencia);
        ids.put("incluye", R.id.edit_incluye);
        ids.put("tarifa", R.id.edit_tarifa);
        ids.put("titulo", R.id.edit_titulo);
        ids.put("genero", R.id.edit_genero);
        ids.put("descripcion", R.id.edit_descripcion);
        ids.put("duracion", R.id.edit_duracion);
        ids.put("orden", R.id.edit_orden);
        for (var entry : ids.entrySet()) {
            EditText input = panel.findViewById(entry.getValue());
            if (input != null) {
                fields.put(entry.getKey(), input);
                View parent = (View) input.getParent();
                while (parent != null && !(parent instanceof TextInputLayout))
                    parent = (View) parent.getParent();
                labels.put(entry.getKey(), (TextInputLayout) parent);
            }
        }
        if (mode.equals("PERFIL") || mode.equals("USUARIO_EDITAR")) {
            labels.get("correo").setVisibility(View.GONE);
            labels.get("password").setVisibility(View.GONE);
            v(R.id.form_selector).setVisibility(View.GONE);
            v(R.id.form_rol_label).setVisibility(View.GONE);
        }
        if (mode.equals("USUARIO_NUEVO")) {
            for (String key : List.of("telefono", "ciudad", "biografia"))
                labels.get(key).setVisibility(View.GONE);
            spinner(
                    R.id.form_selector,
                    List.of(getString(R.string.rol_director), getString(R.string.solista),
                            getString(R.string.rol_administrador), getString(R.string.rol_cliente)));
        }
        if (mode.equals("SERVICIO_EDITAR")) {
            v(R.id.form_selector).setVisibility(View.GONE);
            text(R.id.form_rol_label, getString(R.string.responsable_fijo));
        }
        int title =
                mode.equals("PERFIL")
                        ? R.string.editar_perfil
                        : mode.equals("USUARIO_NUEVO")
                                ? R.string.crear_musico
                                : mode.equals("USUARIO_EDITAR")
                                        ? R.string.detalle_usuario
                                        : mode.equals("SERVICIO_NUEVO")
                                                ? R.string.crear_servicio
                                                : mode.equals("SERVICIO_EDITAR")
                                                        ? R.string.editar_informacion
                                                        : mode.equals("TARIFA")
                                                                ? R.string.editar_tarifa
                                                                : R.string.editar_cancion;
        text(R.id.formulario_titulo, getString(title));
        text(
                R.id.formulario_ayuda,
                getString(
                        mode.equals("USUARIO_NUEVO")
                                ? R.string.nueva_cuenta_info
                                : mode.startsWith("SERVICIO")
                                        ? R.string.listas_comas
                                        : mode.equals("TARIFA")
                                                ? R.string.tarifa_guardada
                                                : R.string.datos_privados_sin_cache));
        text(
                R.id.formulario_guardar,
                getString(
                        mode.equals("SERVICIO_NUEVO")
                                ? R.string.crear_borrador
                                : mode.equals("USUARIO_NUEVO")
                                        ? R.string.crear_cuenta
                                        : mode.equals("CANCION")
                                                        && requireArguments().getLong("song") == 0
                                                ? R.string.crear_cancion
                                                : R.string.guardar_cambios));
        if (saved != null) {
            draft = saved.getBundle("draft");
            savedOwner = saved.getLong("owner", 0);
            if (mode.equals("USUARIO_NUEVO"))
                ((Spinner) v(R.id.form_selector)).setSelection(saved.getInt("role", 0));
        }
        v(R.id.formulario_guardar)
                .setOnClickListener(
                        v -> {
                            for (TextInputLayout label : labels.values()) label.setError(null);
                            Usuario owner = null;
                            String role = "DIRECTOR";
                            Spinner select = requireView().findViewById(R.id.form_selector);
                            if (select != null) {
                                int index = select.getSelectedItemPosition();
                                if (mode.equals("SERVICIO_NUEVO")
                                        && index > 0
                                        && index <= owners.size()) owner = owners.get(index - 1);
                                if (mode.equals("USUARIO_NUEVO"))
                                    role = List.of("DIRECTOR", "SOLISTA", "ADMINISTRADOR", "CLIENTE").get(index);
                            }
                            controller.guardar(values(), owner, role);
                        });
        v(R.id.formulario_recargar)
                .setOnClickListener(
                        v ->
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setMessage(R.string.restaurar_datos)
                                        .setNegativeButton(R.string.cancelar, null)
                                        .setPositiveButton(
                                                R.string.confirmar,
                                                (d, w) -> {
                                                    draft = null;
                                                    controller.cargar();
                                                })
                                        .show());
    }

    private Map<String, String> values() {
        Map<String, String> map = new LinkedHashMap<>();
        for (var entry : fields.entrySet())
            map.put(entry.getKey(), entry.getValue().getText().toString());
        return map;
    }

    public void preparando() {
        ready = false;
        visible(R.id.formulario_campos, false);
        visible(R.id.formulario_progress, true);
        text(R.id.formulario_estado, getString(R.string.cargando_datos));
        enabled(R.id.formulario_guardar, false);
        enabled(R.id.formulario_recargar, false);
    }

    public void preparar(Map<String, String> data, List<Usuario> owners) {
        this.owners = List.copyOf(owners);
        version = data.getOrDefault("_version", "");
        ready = true;
        visible(R.id.formulario_campos, true);
        visible(R.id.formulario_progress, false);
        text(R.id.formulario_estado, "");
        Map<String, String> source = new LinkedHashMap<>(data);
        if (draft != null) {
            if (version.equals(draft.getString("_version", ""))) {
                for (String key : fields.keySet())
                    if (!key.equals("password") && draft.containsKey(key))
                        source.put(key, draft.getString(key, ""));
            } else
                text(
                        R.id.formulario_estado,
                        "El registro cambió desde tu última edición. Revisa los datos actuales"
                                + " antes de guardar.");
            draft = null;
        }
        for (var entry : fields.entrySet())
            if (!entry.getKey().equals("password")) {
                // No truncar datos existentes al cargarlos con filtros de longitud de entrada.
                android.text.InputFilter[] filters = entry.getValue().getFilters();
                entry.getValue().setFilters(new android.text.InputFilter[0]);
                entry.getValue().setText(source.getOrDefault(entry.getKey(), ""));
                entry.getValue().setFilters(filters);
            }
        if (mode.equals("SERVICIO_NUEVO")) {
            List<String> names = new ArrayList<>();
            names.add(getString(R.string.selecciona_responsable));
            for (Usuario u : owners)
                names.add(
                        u.getNombres()
                                + " "
                                + u.getApellidos()
                                + " · "
                                + Presentacion.rol(u.getRol()));
            spinner(R.id.form_selector, names);
            for (int i = 0; i < owners.size(); i++)
                if (owners.get(i).getId() == savedOwner)
                    ((Spinner) v(R.id.form_selector)).setSelection(i + 1);
            if (owners.isEmpty()) {
                ready = false;
                text(R.id.formulario_estado, getString(R.string.sin_responsables));
            }
        }
        busy(false);
    }

    public void busy(boolean value) {
        busy = value;
        visible(R.id.formulario_progress, value);
        enabled(R.id.formulario_guardar, !value && ready);
        enabled(R.id.formulario_recargar, !value);
        for (EditText field : fields.values()) field.setEnabled(!value);
        View select = requireView().findViewById(R.id.form_selector);
        if (select != null) select.setEnabled(!value);
        if (value) text(R.id.formulario_estado, getString(R.string.guardando));
    }

    public void error(ApiFailure error) {
        visible(R.id.formulario_progress, false);
        text(R.id.formulario_estado, error.getMessage());
        ((androidx.core.widget.NestedScrollView) v(R.id.pagina_scroll)).smoothScrollTo(0, 0);
        for (var e : error.getFields().entrySet()) {
            String key =
                    e.getKey().equals("tarifaCentimos")
                            ? "tarifa"
                            : e.getKey().equals("duracionSegundos") ? "duracion" : e.getKey();
            if (labels.containsKey(key)) labels.get(key).setError(e.getValue());
        }
        enabled(R.id.formulario_guardar, ready);
        enabled(R.id.formulario_recargar, true);
    }

    public void guardado() {
        if (fields.containsKey("password")) fields.get("password").setText("");
        host().terminarFormulario();
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(((TextView) v(R.id.formulario_titulo)).getText().toString());
        controller.attach(this);
        if (!ready || busy) controller.cargar();
    }

    @Override
    public void onStop() {
        controller.detach();
        super.onStop();
    }

    @Override
    public void refresh() {
        if (!busy) v(R.id.formulario_recargar).performClick();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        if (getView() != null) {
            Bundle fieldsBundle = new Bundle();
            for (var entry : values().entrySet())
                if (!entry.getKey().equals("password"))
                    fieldsBundle.putString(entry.getKey(), entry.getValue());
            fieldsBundle.putString("_version", version);
            out.putBundle("draft", fieldsBundle);
            Spinner select = requireView().findViewById(R.id.form_selector);
            if (mode.equals("USUARIO_NUEVO") && select != null)
                out.putInt("role", select.getSelectedItemPosition());
            if (mode.equals("SERVICIO_NUEVO")
                    && select != null
                    && select.getSelectedItemPosition() > 0)
                out.putLong("owner", owners.get(select.getSelectedItemPosition() - 1).getId());
        }
        super.onSaveInstanceState(out);
    }

    @Override
    public void onDestroyView() {
        if (fields.containsKey("password")) fields.get("password").setText("");
        fields.clear();
        labels.clear();
        owners = Collections.emptyList();
        ready = false;
        super.onDestroyView();
    }
}
