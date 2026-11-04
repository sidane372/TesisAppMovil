package com.example.santaceciliatesis.model;

import java.util.*;

/** Resumen público. Una tarifa referencial no acredita precio contractual ni disponibilidad. */
public final class ServicioMusical {
    private final long id;
    private final String nombre, ciudad;
    private final TipoServicio tipo;
    private final Long tarifaCentimos;
    private final List<String> generos, instrumentos;

    public ServicioMusical(long id, String nombre, TipoServicio tipo, String ciudad, Long tarifa) {
        this(id, nombre, tipo, ciudad, tarifa, Collections.emptyList(), Collections.emptyList());
    }

    public ServicioMusical(
            long id,
            String nombre,
            TipoServicio tipo,
            String ciudad,
            Long tarifa,
            List<String> generos,
            List<String> instrumentos) {
        if (id <= 0 || nombre == null || nombre.trim().isEmpty() || (tarifa != null && tarifa < 0))
            throw new IllegalArgumentException("Resumen inválido");
        this.id = id;
        this.nombre = nombre.trim();
        this.tipo = Objects.requireNonNull(tipo);
        this.ciudad = Objects.requireNonNull(ciudad);
        this.tarifaCentimos = tarifa;
        this.generos = List.copyOf(generos);
        this.instrumentos = List.copyOf(instrumentos);
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoServicio getTipo() {
        return tipo;
    }

    public String getCiudad() {
        return ciudad;
    }

    public Long getTarifaCentimos() {
        return tarifaCentimos;
    }

    public List<String> getGeneros() {
        return generos;
    }

    public List<String> getInstrumentos() {
        return instrumentos;
    }
}
