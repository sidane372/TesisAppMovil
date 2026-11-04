package com.example.santaceciliatesis.controller;

import com.example.santaceciliatesis.data.remote.RequestHandle;
import com.example.santaceciliatesis.data.repository.ServicioRepository;
import com.example.santaceciliatesis.model.ServicioMusical;
import com.example.santaceciliatesis.util.Resource;

import java.math.BigDecimal;
import java.util.*;

public final class CatalogoController {
    public interface View {
        void render(Resource<List<ServicioMusical>> value);

        default void generos(List<String> values) {}

        default void filtroError(String error) {}
    }

    private final ServicioRepository repo;
    private View view;
    private RequestHandle task;
    private int generation;
    private Resource<List<ServicioMusical>> source;
    private String query = "", tipo = "", genero = "";
    private Long max;

    public CatalogoController(ServicioRepository repo) {
        this.repo = repo;
    }

    public void attach(View value) {
        view = value;
    }

    public void cargar() {
        if (view == null) return;
        if (task != null) task.cancel();
        int current = ++generation;
        view.render(new Resource<>(Resource.State.LOADING, Collections.emptyList()));
        task =
                repo.cargar(
                        result -> {
                            if (view == null || generation != current) return;
                            source = result;
                            TreeSet<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                            for (ServicioMusical s : result.getData())
                                genres.addAll(s.getGeneros());
                            view.generos(new ArrayList<>(genres));
                            emit();
                        });
    }

    public void filtrar(String query, String tipo, String genero, String maximo) {
        if (view == null) return;
        Long cents = null;
        try {
            if (!maximo.trim().isEmpty()) {
                cents =
                        new BigDecimal(maximo.trim().replace(',', '.'))
                                .movePointRight(2)
                                .longValueExact();
                if (cents < 0 || cents > 999999999999L) throw new ArithmeticException();
            }
        } catch (Exception e) {
            view.filtroError("Usa un importe desde cero, con hasta dos decimales.");
            return;
        }
        view.filtroError(null);
        this.query = query.trim().toLowerCase(Locale.ROOT);
        this.tipo = tipo;
        this.genero = genero;
        max = cents;
        emit();
    }

    private void emit() {
        if (view == null || source == null) return;
        List<ServicioMusical> filtered = new ArrayList<>();
        for (ServicioMusical s : source.getData()) {
            if (!tipo.isEmpty() && !s.getTipo().name().equals(tipo)) continue;
            if (!genero.isEmpty()
                    && s.getGeneros().stream().noneMatch(g -> g.equalsIgnoreCase(genero))) continue;
            if (max != null && (s.getTarifaCentimos() == null || s.getTarifaCentimos() > max))
                continue;
            if (!query.isEmpty()) {
                List<String> words = new ArrayList<>(List.of(s.getNombre(), s.getCiudad()));
                words.addAll(s.getGeneros());
                words.addAll(s.getInstrumentos());
                if (words.stream().noneMatch(w -> w.toLowerCase(Locale.ROOT).contains(query)))
                    continue;
            }
            filtered.add(s);
        }
        view.render(new Resource<>(source.getState(), filtered));
    }

    public void detach() {
        generation++;
        view = null;
        if (task != null) task.cancel();
        source = null;
    }
}
