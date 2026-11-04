package com.example.santaceciliatesis.service;

import java.util.*;

/** Selección pública de esta navegación, hasta tres IDs; no contiene datos privados. */
public final class ComparacionStore {
    private final LinkedHashSet<Long> ids = new LinkedHashSet<>();

    public boolean toggle(long id) {
        if (ids.remove(id)) return true;
        if (ids.size() == 3) return false;
        ids.add(id);
        return true;
    }

    public boolean contains(long id) {
        return ids.contains(id);
    }

    public int size() {
        return ids.size();
    }

    public List<Long> ids() {
        return List.copyOf(ids);
    }

    public void remove(long id) {
        ids.remove(id);
    }

    public void clear() {
        ids.clear();
    }

    public void restore(long[] saved) {
        ids.clear();
        if (saved != null) for (long id : saved) if (id > 0 && ids.size() < 3) ids.add(id);
    }
}
