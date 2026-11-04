package com.example.santaceciliatesis.data.remote.mapper;

import com.example.santaceciliatesis.model.*;

import org.json.*;

import java.util.*;

public final class ServicioDetalleMapper {
    private ServicioDetalleMapper() {}

    public static List<String> strings(JSONArray values) throws JSONException {
        List<String> result = new ArrayList<>();
        if (values != null)
            for (int i = 0; i < values.length(); i++) result.add(values.getString(i));
        return result;
    }

    private static String optional(JSONObject data, String key) throws JSONException {
        return data.isNull(key) ? "" : data.getString(key);
    }

    public static ServicioDetalle parse(JSONObject response) throws JSONException {
        return objeto(response.getJSONObject("data"));
    }

    public static ServicioDetalle objeto(JSONObject data) throws JSONException {
        try {
            List<String> generos = strings(data.getJSONArray("generos")),
                    instrumentos = strings(data.getJSONArray("instrumentos"));
            ServicioMusical base =
                    new ServicioMusical(
                            data.getLong("id"),
                            data.getString("nombre"),
                            TipoServicio.valueOf(data.getString("tipo")),
                            data.getString("ciudad"),
                            data.isNull("tarifaCentimos") ? null : data.getLong("tarifaCentimos"),
                            generos,
                            instrumentos);
            List<Cancion> canciones = new ArrayList<>();
            JSONArray rows = data.getJSONArray("repertorio");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject c = rows.getJSONObject(i);
                canciones.add(
                        new Cancion(
                                c.getLong("id"),
                                c.getString("titulo"),
                                c.getString("genero"),
                                optional(c, "descripcion"),
                                c.isNull("duracionSegundos") ? null : c.getInt("duracionSegundos"),
                                c.getInt("orden")));
            }
            return new ServicioDetalle(
                    base,
                    data.getLong("responsableId"),
                    data.getLong("version"),
                    data.getString("estadoPublicacion"),
                    optional(data, "descripcionHistorica"),
                    optional(data, "experiencia"),
                    optional(data, "incluye"),
                    generos,
                    instrumentos,
                    strings(data.getJSONArray("tiposEvento")),
                    canciones);
        } catch (IllegalArgumentException e) {
            throw new JSONException("Detalle no válido");
        }
    }
}
