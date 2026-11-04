package com.example.santaceciliatesis.data.remote.mapper;

import com.example.santaceciliatesis.data.remote.dto.ServicioResumenDto;
import com.example.santaceciliatesis.model.ServicioMusical;
import com.example.santaceciliatesis.model.TipoServicio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ServicioJsonMapper {
    private ServicioJsonMapper() {}

    /** Exige snapshot completo para no borrar datos al recibir una página parcial. */
    public static List<ServicioMusical> parse(JSONObject response) throws JSONException {
        if (!Boolean.TRUE.equals(response.get("completo")))
            throw new JSONException("Catálogo incompleto");
        JSONArray data = response.getJSONArray("data");
        List<ServicioMusical> result = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject json = data.getJSONObject(i);
                long id = new BigDecimal(json.get("id").toString()).longValueExact();
                Long tarifa =
                        json.isNull("tarifaCentimos")
                                ? null
                                : new BigDecimal(json.get("tarifaCentimos").toString())
                                        .longValueExact();
                ServicioResumenDto dto =
                        new ServicioResumenDto(
                                id,
                                json.getString("nombre"),
                                json.getString("tipo"),
                                json.getString("ciudad"),
                                tarifa);
                if (!ids.add(dto.id)) throw new IllegalArgumentException("ID repetido");
                result.add(
                        new ServicioMusical(
                                dto.id,
                                dto.nombre,
                                TipoServicio.valueOf(dto.tipo),
                                dto.ciudad,
                                dto.tarifaCentimos,
                                ServicioDetalleMapper.strings(json.optJSONArray("generos")),
                                ServicioDetalleMapper.strings(json.optJSONArray("instrumentos"))));
            }
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new JSONException("Resumen de servicio inválido");
        }
        return result;
    }
}
