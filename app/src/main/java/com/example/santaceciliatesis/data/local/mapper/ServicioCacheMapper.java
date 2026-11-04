package com.example.santaceciliatesis.data.local.mapper;

import com.example.santaceciliatesis.data.local.entity.ServicioCacheEntity;
import com.example.santaceciliatesis.model.ServicioMusical;
import com.example.santaceciliatesis.model.TipoServicio;

import java.util.ArrayList;
import java.util.List;

public final class ServicioCacheMapper {
    private ServicioCacheMapper() {}

    private static List<String> lista(String json) {
        try {
            return com.example.santaceciliatesis.data.remote.mapper.ServicioDetalleMapper.strings(
                    new org.json.JSONArray(json));
        } catch (org.json.JSONException e) {
            return java.util.Collections.emptyList();
        }
    }

    public static List<ServicioMusical> toDomain(List<ServicioCacheEntity> entities) {
        List<ServicioMusical> result = new ArrayList<>();
        for (ServicioCacheEntity e : entities)
            result.add(
                    new ServicioMusical(
                            e.id,
                            e.nombre,
                            TipoServicio.valueOf(e.tipo),
                            e.ciudad,
                            e.tarifaCentimos,
                            lista(e.generosJson),
                            lista(e.instrumentosJson)));
        return result;
    }

    public static List<ServicioCacheEntity> toCache(List<ServicioMusical> servicios, long fecha) {
        List<ServicioCacheEntity> result = new ArrayList<>();
        for (ServicioMusical s : servicios) {
            ServicioCacheEntity e = new ServicioCacheEntity();
            e.id = s.getId();
            e.nombre = s.getNombre();
            e.tipo = s.getTipo().name();
            e.ciudad = s.getCiudad();
            e.tarifaCentimos = s.getTarifaCentimos();
            e.sincronizadoEn = fecha;
            e.generosJson = new org.json.JSONArray(s.getGeneros()).toString();
            e.instrumentosJson = new org.json.JSONArray(s.getInstrumentos()).toString();
            result.add(e);
        }
        return result;
    }
}
