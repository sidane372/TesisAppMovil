package com.santacecilia.api.repository;

import com.santacecilia.api.exception.ApiException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/** SQL parametrizado; las filas de recurso se bloquean antes que los agregados contractuales. */
@Repository
public class ContratacionRepository {
    private final JdbcTemplate jdbc;

    public ContratacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.queryForList(sql, args);
    }

    public Map<String, Object> one(String sql, Object... args) {
        var rows = rows(sql, args);
        if (rows.isEmpty()) throw ApiException.notFound();
        return rows.getFirst();
    }

    public int update(String sql, Object... args) {
        return jdbc.update(sql, args);
    }

    public long insert(String sql, Object... args) {
        update(sql, args);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public static long number(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
    }

    public static Instant instant(Object value) {
        if (value instanceof Timestamp t) return t.toLocalDateTime().toInstant(ZoneOffset.UTC);
        return ((LocalDateTime) value).toInstant(ZoneOffset.UTC);
    }

    public static LocalDateTime utc(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    public List<Long> recursos(long servicio) {
        var s = one("SELECT id_responsable FROM servicio_musical WHERE id=?", servicio);
        var owners =
                rows(
                        "SELECT id FROM recurso_agenda WHERE id_servicio=? OR id_musico=? OR"
                            + " id_musico IN (SELECT id_solista_vinculado FROM integrante WHERE"
                            + " id_servicio=? AND activo=1 AND estado_vinculo='ACEPTADO') ORDER BY"
                            + " id",
                        servicio,
                        number(s, "id_responsable"),
                        servicio);
        long expected =
                1
                        + rows(
                                        "SELECT id_responsable AS id FROM servicio_musical WHERE"
                                            + " id=? UNION SELECT id_solista_vinculado FROM"
                                            + " integrante WHERE id_servicio=? AND activo=1 AND"
                                            + " estado_vinculo='ACEPTADO'",
                                        servicio,
                                        servicio)
                                .size();
        if (owners.size() != expected)
            throw ApiException.conflict(
                    "La agenda requiere registrar todos los recursos musicales");
        return owners.stream().map(r -> number(r, "id")).toList();
    }

    public void lock(List<Long> resources) {
        if (resources.isEmpty()) throw ApiException.conflict("No hay recursos de agenda");
        for (long id : new TreeSet<>(resources))
            one("SELECT id FROM recurso_agenda WHERE id=? FOR UPDATE", id);
    }

    public void libre(List<Long> resources, Instant inicio, Instant fin) {
        for (long id : resources)
            if (!rows(
                            "SELECT id FROM ocupacion_agenda WHERE id_recurso=? AND activa=1 AND"
                                + " (vence_en IS NULL OR vence_en>UTC_TIMESTAMP(6)) AND"
                                + " inicio_utc<? AND fin_utc>? FOR UPDATE",
                            id,
                            utc(fin),
                            utc(inicio))
                    .isEmpty())
                throw ApiException.conflict(
                        "El intervalo se superpone con un compromiso o bloqueo del servicio o de"
                            + " sus músicos");
    }

    public static Map<String, Object> salida(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach(
                (k, v) ->
                        result.put(
                                k,
                                v instanceof Timestamp || v instanceof LocalDateTime
                                        ? instant(v).toString()
                                        : v));
        return result;
    }
}
