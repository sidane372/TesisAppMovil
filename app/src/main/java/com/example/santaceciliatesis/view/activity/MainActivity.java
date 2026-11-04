package com.example.santaceciliatesis.view.activity;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.santaceciliatesis.*;
import com.example.santaceciliatesis.model.*;
import com.example.santaceciliatesis.service.SessionManager;
import com.example.santaceciliatesis.util.Presentacion;
import com.example.santaceciliatesis.view.fragment.common.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

public final class MainActivity extends AppCompatActivity implements NavigationHost {
    private BottomNavigationView navigation;
    private NavigationView drawerNavigation;
    private DrawerLayout drawer;
    private MaterialToolbar toolbar;
    private SessionManager sessions;
    private long userId;
    private Rol role;
    private String root = "inicio";
    private boolean permissionRequested;
    private boolean menuReady;
    private final SessionManager.Listener sessionListener =
            value -> {
                if (!value.loaded) return;
                Usuario user = value.session == null ? null : value.session.getUsuario();
                long next = user == null ? 0 : user.getId();
                Rol nextRole = user == null ? null : user.getRol();
                boolean changed = next != userId || nextRole != role;
                boolean previous = userId != 0;
                userId = next;
                role = nextRole;
                configurarMenu();
                Fragment active =
                        getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                boolean forbidden =
                        (root.equals("usuarios") || root.equals("servicios"))
                                        && role != Rol.ADMINISTRADOR
                                || root.equals("propios")
                                        && role != Rol.DIRECTOR
                                        && role != Rol.SOLISTA
                                || next == 0 && active instanceof PrivateScreen
                                || next != 0 && active instanceof AccesoFragment;
                if (changed || forbidden) {
                    getSupportFragmentManager()
                            .popBackStackImmediate(
                                    null,
                                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    abrir("inicio");
                }
                if (previous && next == 0 && value.notice != null) mostrarMensaje(value.notice);
                findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
            };
    private final androidx.activity.result.ActivityResultLauncher<String> networkPermission =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts
                            .RequestPermission(),
                    granted -> {
                        if (granted) refrescar();
                    });

    @Override
    public boolean solicitarRedLocal() {
        if (android.os.Build.VERSION.SDK_INT < 37
                || !BuildConfig.DEBUG
                || !new com.example.santaceciliatesis.data.remote.ApiConfig(
                                BuildConfig.API_BASE_URL)
                        .isLocalEndpoint()) return true;
        if (checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK")
                == android.content.pm.PackageManager.PERMISSION_GRANTED) return true;
        permissionRequested = true;
        networkPermission.launch("android.permission.ACCESS_LOCAL_NETWORK");
        return false;
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        var container = ((SantaCeciliaApplication) getApplication()).getContainer();
        sessions = container.getSessionManager();
        var state = sessions.snapshot();
        userId = state.session == null ? 0 : state.session.getUsuario().getId();
        role = state.session == null ? null : state.session.getUsuario().getRol();
        if (saved != null) {
            root = saved.getString("root", "inicio");
            permissionRequested = saved.getBoolean("localPermissionRequested", false);
            container.getComparacion().restore(saved.getLongArray("comparison"));
        }
        navigation = findViewById(R.id.bottom_navigation);
        navigation.setBackgroundColor(getColor(R.color.sc_white));
        drawer = findViewById(R.id.drawer);
        drawerNavigation = findViewById(R.id.drawer_navigation);
        toolbar = findViewById(R.id.shell_toolbar);
        toolbar.setLogo(R.drawable.bg_logo);
        ViewCompat.setOnApplyWindowInsetsListener(
                navigation, (v, i) -> WindowInsetsCompat.CONSUMED);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                            ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    view.setPadding(
                            bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
                    boolean editing = getCurrentFocus() instanceof android.widget.EditText;
                    Fragment current =
                            getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    navigation.setVisibility(
                            current instanceof AccesoFragment || editing && ime.bottom > bars.bottom
                                    ? View.GONE
                                    : View.VISIBLE);
                    return insets;
                });
        ViewCompat.setOnApplyWindowInsetsListener(
                drawerNavigation,
                (v, i) -> {
                    Insets bars = i.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
        toolbar.setNavigationOnClickListener(
                v -> {
                    if (getSupportFragmentManager().getBackStackEntryCount() > 0)
                        getSupportFragmentManager().popBackStack();
                    else drawer.openDrawer(GravityCompat.START);
                });
        toolbar.getMenu()
                .add(0, R.id.action_refresh, 0, R.string.actualizar_seccion)
                .setIcon(R.drawable.ic_refresh)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        toolbar.getMenu()
                .add(0, R.id.action_guia, 1, R.string.guia)
                .setIcon(R.drawable.ic_help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        toolbar.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_refresh) {
                        if (solicitarRedLocal()) refrescar();
                    } else abrir("guia");
                    return true;
                });
        drawerNavigation.setNavigationItemSelectedListener(
                item -> {
                    drawer.closeDrawer(GravityCompat.START);
                    abrir(ruta(item.getItemId()));
                    return true;
                });
        getSupportFragmentManager().addOnBackStackChangedListener(this::actualizarBarra);
        configurarMenu();
        if (saved == null) abrir("inicio");
        else actualizarBarra();
        findViewById(R.id.fragment_container)
                .setVisibility(state.loaded ? View.VISIBLE : View.INVISIBLE);
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (drawer.isDrawerOpen(GravityCompat.START))
                                    drawer.closeDrawer(GravityCompat.START);
                                else if (getSupportFragmentManager().getBackStackEntryCount() > 0)
                                    getSupportFragmentManager().popBackStack();
                                else if (!"inicio".equals(root)) abrir("inicio");
                                else {
                                    setEnabled(false);
                                    getOnBackPressedDispatcher().onBackPressed();
                                }
                            }
                        });
    }

    private void add(Menu menu, int id, int text, int icon) {
        menu.add(0, id, menu.size(), text).setIcon(icon).setCheckable(true);
    }

    private void configurarMenu() {
        navigation.setOnItemSelectedListener(null);
        Menu bottom = navigation.getMenu();
        bottom.clear();
        add(bottom, R.id.nav_inicio, R.string.inicio, R.drawable.ic_inicio);
        if (role == Rol.ADMINISTRADOR) {
            add(bottom, R.id.nav_servicios, R.string.servicios, R.drawable.ic_music);
            add(bottom, R.id.nav_solicitudes, R.string.solicitudes, R.drawable.ic_request);
            add(bottom, R.id.nav_contratos, R.string.contratos, R.drawable.ic_contract);
        } else if (role == Rol.DIRECTOR || role == Rol.SOLISTA) {
            add(
                    bottom,
                    R.id.nav_propios,
                    role == Rol.DIRECTOR ? R.string.mis_bandas : R.string.mi_servicio,
                    R.drawable.ic_music);
            add(bottom, R.id.nav_solicitudes, R.string.solicitudes, R.drawable.ic_request);
            add(bottom, R.id.nav_calendario, R.string.calendario, R.drawable.ic_calendar);
        } else if (role == Rol.CLIENTE) {
            add(bottom, R.id.nav_catalogo, R.string.catalogo, R.drawable.ic_catalogo);
            add(bottom, R.id.nav_solicitudes, R.string.solicitudes, R.drawable.ic_request);
            add(bottom, R.id.nav_contratos, R.string.contratos, R.drawable.ic_contract);
        } else {
            add(bottom, R.id.nav_catalogo, R.string.catalogo, R.drawable.ic_catalogo);
            add(bottom, R.id.nav_comparar, R.string.comparar, R.drawable.ic_compare);
            add(bottom, R.id.nav_guia, R.string.guia, R.drawable.ic_help);
        }
        add(
                bottom,
                R.id.nav_cuenta,
                role == null ? R.string.ingresar : R.string.perfil,
                R.drawable.ic_cuenta);
        navigation.setItemHorizontalTranslationEnabled(
                getResources().getConfiguration().fontScale >= 1.3f);
        navigation.setLabelVisibilityMode(
                getResources().getConfiguration().fontScale >= 1.3f
                        ? com.google.android.material.navigation.NavigationBarView
                                .LABEL_VISIBILITY_SELECTED
                        : com.google.android.material.navigation.NavigationBarView
                                .LABEL_VISIBILITY_LABELED);
        Menu full = drawerNavigation.getMenu();
        full.clear();
        for (int i = 0; i < bottom.size(); i++) {
            MenuItem item = bottom.getItem(i);
            full.add(0, item.getItemId(), i, item.getTitle())
                    .setIcon(item.getIcon())
                    .setCheckable(true);
        }
        if (full.findItem(R.id.nav_guia) == null)
            add(full, R.id.nav_guia, R.string.guia, R.drawable.ic_help);
        if (full.findItem(R.id.nav_catalogo) == null)
            add(full, R.id.nav_catalogo, R.string.catalogo, R.drawable.ic_catalogo);
        if (full.findItem(R.id.nav_comparar) == null)
            add(full, R.id.nav_comparar, R.string.comparar, R.drawable.ic_compare);
        if (role == Rol.ADMINISTRADOR)
            add(full, R.id.nav_usuarios, R.string.usuarios, R.drawable.ic_users);
        if (role != null && full.findItem(R.id.nav_contratos) == null)
            add(full, R.id.nav_contratos, R.string.contratos, R.drawable.ic_contract);
        if (role == Rol.ADMINISTRADOR)
            add(full, R.id.nav_calendario, R.string.calendario, R.drawable.ic_calendar);
        if (role != null)
            add(full, R.id.nav_pagos, R.string.pagos_directos, R.drawable.ic_contract);
        if (role == Rol.ADMINISTRADOR)
            add(full, R.id.nav_cobros, R.string.cuentas_cobro, R.drawable.ic_contract);
        var state = sessions.snapshot();
        Usuario user = state.session == null ? null : state.session.getUsuario();
        View header = drawerNavigation.getHeaderView(0);
        ((TextView) header.findViewById(R.id.drawer_nombre))
                .setText(
                        user == null
                                ? getString(R.string.bienvenida_publica)
                                : user.getNombres() + " " + user.getApellidos());
        ((TextView) header.findViewById(R.id.drawer_rol))
                .setText(
                        user == null
                                ? getString(R.string.catalogo_subtitulo)
                                : Presentacion.rol(user.getRol()));
        navigation.setOnItemSelectedListener(
                item -> {
                    abrir(ruta(item.getItemId()));
                    return true;
                });
        menuReady = true;
        marcarMenu();
    }

    private String ruta(int id) {
        if (id == R.id.nav_pagos) return "pagos";
        if (id == R.id.nav_cobros) return "cuentas-cobro";
        if (id == R.id.nav_solicitudes) return "solicitudes";
        if (id == R.id.nav_contratos) return "contratos";
        if (id == R.id.nav_calendario) return "calendario";
        if (id == R.id.nav_catalogo) return "catalogo";
        if (id == R.id.nav_usuarios) return "usuarios";
        if (id == R.id.nav_servicios) return "servicios";
        if (id == R.id.nav_propios) return "propios";
        if (id == R.id.nav_comparar) return "comparar";
        if (id == R.id.nav_guia) return "guia";
        if (id == R.id.nav_cuenta) return "cuenta";
        return "inicio";
    }

    private void marcarMenu() {
        if (!menuReady) return;
        for (int i = 0; i < navigation.getMenu().size(); i++) {
            MenuItem item = navigation.getMenu().getItem(i);
            item.setChecked(root.equals(ruta(item.getItemId())));
        }
        for (int i = 0; i < drawerNavigation.getMenu().size(); i++) {
            MenuItem item = drawerNavigation.getMenu().getItem(i);
            item.setChecked(root.equals(ruta(item.getItemId())));
        }
    }

    @Override
    public void abrir(String route) {
        if (getSupportFragmentManager().isStateSaved()) return;
        if ((route.equals("usuarios") || route.equals("servicios")) && role != Rol.ADMINISTRADOR) {
            mostrarCuenta();
            return;
        }
        if (route.equals("propios") && role != Rol.DIRECTOR && role != Rol.SOLISTA) {
            mostrarCuenta();
            return;
        }
        if ((route.equals("solicitudes") || route.equals("contratos") || route.equals("calendario"))
                && role == null) {
            mostrarCuenta();
            return;
        }
        root = route;
        getSupportFragmentManager()
                .popBackStackImmediate(
                        null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment fragment;
        switch (route) {
            case "pagos":
                fragment = PagosFragment.nuevo(0);
                break;
            case "cuentas-cobro":
                fragment = CuentasCobroFragment.nuevo(-1);
                break;
            case "solicitudes":
                fragment = BandejaFragment.nuevo(false);
                break;
            case "contratos":
                fragment = BandejaFragment.nuevo(true);
                break;
            case "calendario":
                fragment = AgendaFragment.nuevo(0, true);
                break;
            case "catalogo":
                fragment = new CatalogoFragment();
                break;
            case "comparar":
                fragment = new CompararFragment();
                break;
            case "guia":
                fragment = new GuiaFragment();
                break;
            case "usuarios":
                fragment =
                        new com.example.santaceciliatesis.view.fragment.administrador
                                .UsuariosFragment();
                break;
            case "servicios":
                fragment = ServiciosGestionFragment.nuevo(true);
                break;
            case "propios":
                fragment = ServiciosGestionFragment.nuevo(false);
                break;
            case "cuenta":
                fragment =
                        sessions.snapshot().session == null
                                ? AccesoFragment.nuevo("LOGIN")
                                : new CuentaFragment();
                break;
            default:
                fragment =
                        role == Rol.ADMINISTRADOR
                                ? new com.example.santaceciliatesis.view.fragment.administrador
                                        .InicioAdministradorFragment()
                                : role == Rol.DIRECTOR
                                        ? new com.example.santaceciliatesis.view.fragment.director
                                                .InicioDirectorFragment()
                                        : role == Rol.SOLISTA
                                                ? new com.example.santaceciliatesis.view.fragment
                                                        .solista.InicioSolistaFragment()
                                                : new com.example.santaceciliatesis.view.fragment
                                                        .cliente.InicioClienteFragment();
        }
        replace(fragment, false);
        marcarMenu();
    }

    private void replace(Fragment fragment, boolean back) {
        if (getSupportFragmentManager().isStateSaved()) return;
        if (!(fragment instanceof AccesoFragment)) {
            View focus = getCurrentFocus();
            if (focus != null) focus.clearFocus();
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                    .hide(WindowInsetsCompat.Type.ime());
            navigation.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.main)
                .setBackgroundResource(
                        fragment instanceof AccesoFragment
                                ? R.color.sc_access_background
                                : R.color.sc_background);
        navigation.setVisibility(fragment instanceof AccesoFragment ? View.GONE : View.VISIBLE);
        toolbar.setVisibility(fragment instanceof AccesoFragment ? View.GONE : View.VISIBLE);
        getWindow()
                .setBackgroundDrawableResource(
                        fragment instanceof AccesoFragment
                                ? R.color.sc_access_background
                                : R.color.sc_background);
        var transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment);
        if (back) transaction.addToBackStack(null);
        else transaction.runOnCommit(this::actualizarBarra);
        transaction.commit();
    }

    private void actualizarBarra() {
        Fragment active = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        findViewById(R.id.main)
                .setBackgroundResource(
                        active instanceof AccesoFragment
                                ? R.color.sc_access_background
                                : R.color.sc_background);
        WindowInsetsCompat currentInsets = ViewCompat.getRootWindowInsets(findViewById(R.id.main));
        boolean editingWithKeyboard =
                getCurrentFocus() instanceof android.widget.EditText
                        && currentInsets != null
                        && currentInsets.isVisible(WindowInsetsCompat.Type.ime());
        navigation.setVisibility(
                active instanceof AccesoFragment || editingWithKeyboard ? View.GONE : View.VISIBLE);
        toolbar.setVisibility(active instanceof AccesoFragment ? View.GONE : View.VISIBLE);
        toolbar.setNavigationIcon(
                getSupportFragmentManager().getBackStackEntryCount() > 0
                        ? R.drawable.ic_back
                        : R.drawable.ic_menu);
        toolbar.setNavigationContentDescription(
                getSupportFragmentManager().getBackStackEntryCount() > 0
                        ? R.string.volver
                        : R.string.menu_abrir);
    }

    private void refrescar() {
        Fragment active = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (active instanceof Refreshable) ((Refreshable) active).refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessions.addListener(sessionListener);
        if (!permissionRequested) solicitarRedLocal();
    }

    @Override
    protected void onStop() {
        sessions.removeListener(sessionListener);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        out.putString("root", root);
        out.putBoolean("localPermissionRequested", permissionRequested);
        out.putLongArray(
                "comparison",
                ((SantaCeciliaApplication) getApplication())
                        .getContainer().getComparacion().ids().stream()
                                .mapToLong(Long::longValue)
                                .toArray());
        super.onSaveInstanceState(out);
    }

    @Override
    public void mostrarInicio() {
        abrir("inicio");
    }

    @Override
    public void mostrarAcceso(String modo) {
        if ("LOGIN".equals(modo)) {
            abrir("cuenta");
            return;
        }
        replace(AccesoFragment.nuevo(modo), true);
    }

    @Override
    public void mostrarCuenta() {
        abrir("cuenta");
    }

    @Override
    public void mostrarServicio(long id) {
        replace(DetalleServicioFragment.nuevo(id), true);
    }

    @Override
    public void gestionarServicio(long id) {
        if (role == Rol.ADMINISTRADOR || role == Rol.DIRECTOR || role == Rol.SOLISTA)
            replace(GestionServicioFragment.nuevo(id), true);
        else mostrarCuenta();
    }

    @Override
    public void mostrarUsuario(long id) {
        if (role == Rol.ADMINISTRADOR)
            replace(
                    com.example.santaceciliatesis.view.fragment.administrador.UsuarioDetalleFragment
                            .nuevo(id),
                    true);
    }

    @Override
    public void formulario(String modo, long id, long cancion) {
        if (sessions.snapshot().session == null) {
            mostrarCuenta();
            return;
        }
        replace(FormularioFragment.nuevo(modo, id, cancion), true);
    }

    @Override
    public void terminarFormulario() {
        getSupportFragmentManager().popBackStack();
        mostrarMensaje(getString(R.string.guardado));
    }

    @Override
    public void flujo(String pantalla, long id) {
        if (!pantalla.equals("disponibilidad") && sessions.snapshot().session == null) {
            mostrarCuenta();
            return;
        }
        Fragment f;
        switch (pantalla) {
            case "pagos":
                f = PagosFragment.nuevo(id);
                break;
            case "pago":
                f = PagoDetalleFragment.nuevo(id);
                break;
            case "comprobante":
                f = PagoComprobanteFragment.nuevo(id, false);
                break;
            case "devolucion":
                f = PagoComprobanteFragment.nuevo(id, true);
                break;
            case "cuentas-cobro":
                f = CuentasCobroFragment.nuevo(-1);
                break;
            case "cuenta-cobro":
                f = CuentasCobroFragment.nuevo(id);
                break;
            case "solicitud":
                f = SolicitudDetalleFragment.nuevo(id);
                break;
            case "contrato":
                f = ContratoDetalleFragment.nuevo(id);
                break;
            case "solicitar":
            case "cotizar":
                f = ContratacionFormFragment.nuevo(pantalla, id);
                break;
            case "horarios":
                f = HorariosFragment.nuevo(id);
                break;
            default:
                f = AgendaFragment.nuevo(id, !pantalla.equals("disponibilidad"));
        }
        replace(f, true);
    }

    @Override
    public void solicitar(long servicio, String fecha) {
        if (role != Rol.CLIENTE) {
            mostrarCuenta();
            return;
        }
        Fragment f = ContratacionFormFragment.nuevo("solicitar", servicio);
        f.requireArguments().putString("fecha", fecha);
        replace(f, true);
    }

    @Override
    public void titulo(String title) {
        toolbar.setSubtitle(title);
        actualizarBarra();
    }

    @Override
    public void mostrarMensaje(String message) {
        Snackbar bar = Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG);
        if (navigation.getVisibility() == View.VISIBLE) bar.setAnchorView(navigation);
        bar.show();
    }
}
