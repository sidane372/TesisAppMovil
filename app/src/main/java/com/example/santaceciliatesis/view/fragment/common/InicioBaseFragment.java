package com.example.santaceciliatesis.view.fragment.common;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.*;

import com.example.santaceciliatesis.R;
import com.example.santaceciliatesis.controller.*;
import com.example.santaceciliatesis.data.remote.ApiFailure;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.util.*;
import com.example.santaceciliatesis.view.adapter.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class InicioBaseFragment extends BaseFragment
        implements CatalogoController.View, Refreshable {
    private CatalogoController catalog;
    private ConsultaController<Long> users;
    private ConsultaController<org.json.JSONObject> resumen;
    private ConsultaController<Pagina<ServicioDetalle>> own;
    private ServicioAdapter publicAdapter;
    private GestionServicioAdapter privateAdapter;
    private Rol role;

    protected InicioBaseFragment() {
        super(R.layout.fragment_inicio);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        var session = deps().getSessionManager().snapshot().session;
        Usuario user = session == null ? null : session.getUsuario();
        role = user == null ? null : user.getRol();
        boolean admin = role == Rol.ADMINISTRADOR,
                provider = role == Rol.DIRECTOR || role == Rol.SOLISTA;
        text(
                R.id.inicio_saludo,
                user == null
                        ? getString(R.string.bienvenida_publica)
                        : getString(R.string.saludo, user.getNombres().split(" ")[0]));
        text(
                R.id.inicio_contexto,
                (user == null ? getString(R.string.catalogo_subtitulo) : Presentacion.rol(role))
                        + " · "
                        + ZonedDateTime.now(ZoneId.of("America/Lima"))
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "d 'de' MMMM", new Locale("es", "PE"))));
        text(
                R.id.inicio_eyebrow,
                getString(
                        admin
                                ? R.string.dashboard_admin_eyebrow
                                : provider
                                        ? R.string.dashboard_proveedor_eyebrow
                                        : R.string.dashboard_cliente_eyebrow));
        text(
                R.id.hero_uno,
                getString(
                        admin
                                ? R.string.hero_admin_uno
                                : provider
                                        ? R.string.hero_proveedor_uno
                                        : R.string.hero_cliente_uno));
        text(
                R.id.hero_dos,
                getString(
                        admin
                                ? R.string.hero_admin_dos
                                : provider
                                        ? R.string.hero_proveedor_dos
                                        : R.string.hero_cliente_dos));
        text(
                R.id.hero_descripcion,
                getString(
                        admin
                                ? R.string.hero_admin_descripcion
                                : provider
                                        ? R.string.hero_proveedor_descripcion
                                        : R.string.hero_cliente_descripcion));
        text(
                R.id.hero_accion,
                getString(
                        admin
                                ? R.string.revisar_servicios
                                : provider
                                        ? R.string.administrar_mis_servicios
                                        : R.string.explorar_musicos));
        v(R.id.hero_accion)
                .setOnClickListener(
                        v -> host().abrir(admin ? "servicios" : provider ? "propios" : "catalogo"));
        visible(R.id.inicio_admin_acciones, admin);
        v(R.id.inicio_cuentas).setOnClickListener(v -> host().abrir("usuarios"));
        v(R.id.inicio_servicios).setOnClickListener(v -> host().abrir("servicios"));
        v(R.id.inicio_ver_todos)
                .setOnClickListener(v -> host().abrir(provider ? "propios" : "catalogo"));
        v(R.id.inicio_reintentar)
                .setOnClickListener(
                        v -> {
                            if (host().solicitarRedLocal()) refresh();
                        });
        text(
                R.id.stat_label_uno,
                getString(
                        admin
                                ? R.string.cuentas_registradas
                                : provider
                                        ? R.string.mis_servicios
                                        : R.string.servicios_publicados));
        if (getResources().getConfiguration().fontScale >= 1.3f) {
            int[] labels = {R.id.stat_label_uno, R.id.stat_label_dos, R.id.stat_label_tres};
            int[] shorts = {
                admin ? R.string.estadistica_cuentas_corta : R.string.estadistica_servicios_corta,
                R.string.estadistica_solicitudes_corta,
                R.string.estadistica_contratos_corta
            };
            int horizontal = Math.round(6 * getResources().getDisplayMetrics().density);
            for (int i = 0; i < labels.length; i++) {
                android.widget.TextView label = v(labels[i]);
                label.setContentDescription(label.getText());
                label.setText(shorts[i]);
                View card = (View) label.getParent();
                card.setPadding(
                        horizontal, card.getPaddingTop(), horizontal, card.getPaddingBottom());
            }
        }
        v(R.id.stat_dos)
                .setContentDescription(
                        getString(R.string.solicitudes_proceso)
                                + ": "
                                + getString(R.string.estadistica_sin_datos));
        v(R.id.stat_tres)
                .setContentDescription(
                        getString(R.string.contratos_confirmados)
                                + ": "
                                + getString(R.string.estadistica_sin_datos));
        publicAdapter = new ServicioAdapter(s -> host().mostrarServicio(s.getId()));
        privateAdapter =
                new GestionServicioAdapter(s -> host().gestionarServicio(s.resumen.getId()));
        list(R.id.inicio_lista, provider ? privateAdapter : publicAdapter);
        if (provider) {
            text(R.id.inicio_seccion_eyebrow, getString(R.string.mis_servicios));
            text(
                    R.id.inicio_seccion_titulo,
                    getString(
                            role == Rol.DIRECTOR
                                    ? R.string.mis_bandas
                                    : R.string.mi_perfil_artistico));
            own =
                    new ConsultaController<>(
                            cb -> deps().getComunidadRepository().servicios(false, 0, cb));
        } else catalog = new CatalogoController(deps().getServicioRepository());
        if (admin)
            users =
                    new ConsultaController<>(
                            cb -> deps().getComunidadRepository().cuentasVigentes(cb));
    }

    public void render(Resource<List<ServicioMusical>> result) {
        boolean loading = result.getState() == Resource.State.LOADING;
        visible(R.id.inicio_progress, loading);
        List<ServicioMusical> rows = result.getData();
        publicAdapter.submitList(rows.subList(0, Math.min(3, rows.size())));
        visible(R.id.inicio_vacio, !loading && rows.isEmpty());
        if (role != Rol.ADMINISTRADOR)
            text(
                    R.id.stat_uno,
                    result.getState() == Resource.State.REMOTE || !rows.isEmpty()
                            ? Integer.toString(rows.size())
                            : "—");
        text(
                R.id.inicio_estado,
                getString(
                        loading
                                ? R.string.inicio_cargando
                                : result.getState() == Resource.State.REMOTE
                                        ? R.string.catalogo_actualizado
                                        : result.getState() == Resource.State.NOT_CONFIGURED
                                                ? R.string.catalogo_no_disponible
                                                : result.getState()
                                                                == Resource.State.LOCAL_PERMISSION
                                                        ? R.string.catalogo_permiso_local
                                                        : rows.isEmpty()
                                                                ? R.string.catalogo_error
                                                                : R.string.catalogo_cache));
    }

    @Override
    public void onStart() {
        super.onStart();
        host().titulo(getString(R.string.inicio));
        if (catalog != null) catalog.attach(this);
        if (users != null)
            users.attach(
                    new ConsultaController.View<Long>() {
                        public void cargando() {
                            text(R.id.stat_uno, "—");
                        }

                        public void mostrar(Long count) {
                            text(R.id.stat_uno, Long.toString(count));
                        }

                        public void error(ApiFailure e) {
                            text(R.id.stat_uno, "—");
                            text(R.id.inicio_estado, e.getMessage());
                        }
                    });
        if (own != null)
            own.attach(
                    new ConsultaController.View<Pagina<ServicioDetalle>>() {
                        public void cargando() {
                            visible(R.id.inicio_progress, true);
                            privateAdapter.submit(Collections.emptyList());
                            text(R.id.inicio_estado, getString(R.string.inicio_cargando));
                        }

                        public void mostrar(Pagina<ServicioDetalle> page) {
                            visible(R.id.inicio_progress, false);
                            text(R.id.stat_uno, Long.toString(page.total));
                            privateAdapter.submit(
                                    page.items.subList(0, Math.min(3, page.items.size())));
                            visible(R.id.inicio_vacio, page.items.isEmpty());
                            text(R.id.inicio_vacio, getString(R.string.sin_servicios_propios));
                            text(R.id.inicio_estado, getString(R.string.detalle_actualizado));
                        }

                        public void error(ApiFailure e) {
                            visible(R.id.inicio_progress, false);
                            text(R.id.stat_uno, "—");
                            text(R.id.inicio_estado, e.getMessage());
                            privateAdapter.submit(Collections.emptyList());
                        }
                    });
        if (role != null) {
            resumen =
                    new ConsultaController<>(
                            cb ->
                                    deps().getContratacionRepository()
                                            .request("GET", "mi-resumen", null, false, cb));
            resumen.attach(
                    new ConsultaController.View<org.json.JSONObject>() {
                        public void cargando() {
                            text(R.id.stat_dos, "—");
                            text(R.id.stat_tres, "—");
                        }

                        public void mostrar(org.json.JSONObject d) {
                            text(R.id.stat_dos, d.optString("solicitudes"));
                            text(R.id.stat_tres, d.optString("eventos"));
                            v(R.id.stat_dos)
                                    .setContentDescription(
                                            "Solicitudes abiertas: " + d.optLong("solicitudes"));
                            v(R.id.stat_tres)
                                    .setContentDescription(
                                            "Próximos eventos confirmados: "
                                                    + d.optLong("eventos"));
                        }

                        public void error(ApiFailure e) {
                            text(R.id.stat_dos, "—");
                            text(R.id.stat_tres, "—");
                        }
                    });
        }
        refresh();
    }

    @Override
    public void refresh() {
        if (catalog != null) catalog.cargar();
        if (users != null) users.cargar();
        if (own != null) own.cargar();
        if (resumen != null) resumen.cargar();
    }

    @Override
    public void onStop() {
        if (catalog != null) catalog.detach();
        if (users != null) users.detach();
        if (own != null) own.detach();
        if (resumen != null) resumen.detach();
        privateAdapter.submit(Collections.emptyList());
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        ((androidx.recyclerview.widget.RecyclerView) v(R.id.inicio_lista)).setAdapter(null);
        publicAdapter = null;
        privateAdapter = null;
        super.onDestroyView();
    }
}
