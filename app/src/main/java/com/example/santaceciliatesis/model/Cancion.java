package com.example.santaceciliatesis.model;

public final class Cancion {
    public final long id;
    public final String titulo, genero, descripcion;
    public final Integer duracionSegundos;
    public final int orden;

    public Cancion(
            long id,
            String titulo,
            String genero,
            String descripcion,
            Integer duracion,
            int orden) {
        this.id = id;
        this.titulo = titulo;
        this.genero = genero;
        this.descripcion = descripcion;
        this.duracionSegundos = duracion;
        this.orden = orden;
    }

    public String duracion() {
        return duracionSegundos == null
                ? "Duración por informar"
                : String.format(
                        java.util.Locale.ROOT,
                        "%d:%02d",
                        duracionSegundos / 60,
                        duracionSegundos % 60);
    }
}
