package com.example.santaceciliatesis.view.activity;

public interface NavigationHost {
    boolean solicitarRedLocal();

    void mostrarAcceso(String modo);

    void mostrarCuenta();

    void mostrarServicio(long id);

    void mostrarMensaje(String mensaje);

    void abrir(String ruta);

    void mostrarInicio();

    void gestionarServicio(long id);

    void mostrarUsuario(long id);

    void formulario(String modo, long id, long cancion);

    void terminarFormulario();

    void titulo(String title);

    void flujo(String pantalla, long id);

    void solicitar(long servicio, String fecha);
}
