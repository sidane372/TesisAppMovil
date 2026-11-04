package com.example.santaceciliatesis.data.remote.dto;

/** Contrato de transporte independiente del esquema SQLite. */
public final class ServicioResumenDto {
    public final long id;
    public final String nombre, tipo, ciudad;
    public final Long tarifaCentimos;

    public ServicioResumenDto(
            long id, String nombre, String tipo, String ciudad, Long tarifaCentimos) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.ciudad = ciudad;
        this.tarifaCentimos = tarifaCentimos;
    }
}
