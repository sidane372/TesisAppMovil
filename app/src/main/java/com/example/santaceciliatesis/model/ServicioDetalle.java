package com.example.santaceciliatesis.model;

import java.util.*;

public final class ServicioDetalle {
    public final ServicioMusical resumen;
    public final long responsableId, version;
    public final String descripcion, experiencia, incluye, estado;
    public final List<String> generos, instrumentos, tiposEvento, repertorio;
    public final List<Cancion> canciones;

    public ServicioDetalle(
            ServicioMusical resumen,
            long responsable,
            long version,
            String estado,
            String descripcion,
            String experiencia,
            String incluye,
            List<String> generos,
            List<String> instrumentos,
            List<String> eventos,
            List<Cancion> canciones) {
        this.resumen = resumen;
        this.responsableId = responsable;
        this.version = version;
        this.estado = estado;
        this.descripcion = descripcion;
        this.experiencia = experiencia;
        this.incluye = incluye;
        this.generos = List.copyOf(generos);
        this.instrumentos = List.copyOf(instrumentos);
        this.tiposEvento = List.copyOf(eventos);
        this.canciones = List.copyOf(canciones);
        List<String> titles = new ArrayList<>();
        for (Cancion c : canciones) titles.add(c.titulo + " · " + c.genero);
        this.repertorio = List.copyOf(titles);
    }
}
