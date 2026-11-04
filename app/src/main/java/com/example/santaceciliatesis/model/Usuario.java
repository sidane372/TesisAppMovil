package com.example.santaceciliatesis.model;

import java.util.Objects;

public final class Usuario {
    private final long id, version;
    private final String nombres, apellidos, correo, estado, telefono, ciudad, biografia;
    private final Rol rol;

    public Usuario(
            long id,
            String nombres,
            String apellidos,
            String correo,
            Rol rol,
            String estado,
            long version) {
        this(id, nombres, apellidos, correo, rol, estado, version, "", "", "");
    }

    public Usuario(
            long id,
            String nombres,
            String apellidos,
            String correo,
            Rol rol,
            String estado,
            long version,
            String telefono,
            String ciudad,
            String biografia) {
        if (id <= 0
                || version < 0
                || nombres.trim().isEmpty()
                || apellidos.trim().isEmpty()
                || correo.trim().isEmpty()) throw new IllegalArgumentException("Perfil inválido");
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.rol = Objects.requireNonNull(rol);
        this.estado = estado;
        this.version = version;
        this.telefono = telefono;
        this.ciudad = ciudad;
        this.biografia = biografia;
    }

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public Rol getRol() {
        return rol;
    }

    public String getEstado() {
        return estado;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getBiografia() {
        return biografia;
    }
}
