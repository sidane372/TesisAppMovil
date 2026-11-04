package com.example.santaceciliatesis.model;

import java.util.*;

public final class Pagina<T> {
    public final List<T> items;
    public final int pagina, tamano;
    public final long total;

    public Pagina(List<T> items, int pagina, int tamano, long total) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.pagina = pagina;
        this.tamano = tamano;
        this.total = total;
    }

    public boolean haySiguiente() {
        return ((long) pagina + 1) * tamano < total;
    }
}
