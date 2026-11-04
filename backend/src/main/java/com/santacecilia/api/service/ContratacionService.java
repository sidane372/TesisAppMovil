package com.santacecilia.api.service;

import static com.santacecilia.api.repository.ContratacionRepository.*;

import com.santacecilia.api.dto.ContratacionDtos.*;
import com.santacecilia.api.entity.Rol;
import com.santacecilia.api.exception.ApiException;
import com.santacecilia.api.repository.ContratacionRepository;
import com.santacecilia.api.security.AuthPrincipal;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

@Service
public class ContratacionService {
    public static final String POLITICA =
            "En caso de cancelación por parte del cliente, no se devolverán los importes abonados."
                + " El rechazo de una cotización antes del pago no genera ningún cobro.";
    public static final String POLITICA_VERSION = "cancelacion-cliente-v1";
    private final ContratacionRepository db;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final Clock clock;
    private static final ZoneId PERU = ZoneId.of("America/Lima");
    private static final Set<String> ABIERTAS =
            Set.of("ENVIADA", "EN_REVISION", "COTIZACION_RECIBIDA");

    public ContratacionService(
            ContratacionRepository db,
            PlatformTransactionManager manager,
            ObjectMapper json,
            Clock clock) {
        this.db = db;
        this.tx = new TransactionTemplate(manager);
        this.tx.setIsolationLevel(
                org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.json = json;
        this.clock = clock;
    }

    private <T> T write(Supplier<T> work) {
        for (int intento = 0; ; intento++)
            try {
                return tx.execute(status -> work.get());
            } catch (PessimisticLockingFailureException e) {
                if (intento >= 2)
                    throw ApiException.conflict(
                            "La agenda está siendo actualizada. Reintenta con la misma operación");
            }
    }

    private boolean admin(AuthPrincipal a) {
        return a != null && a.rol() == Rol.ADMINISTRADOR;
    }

    private void cliente(AuthPrincipal a) {
        if (a.rol() != Rol.CLIENTE) throw ApiException.notFound();
    }

    private Map<String, Object> servicio(long id, boolean lock) {
        return db.one(
                "SELECT s.*,u.estado AS cuenta_estado FROM servicio_musical s JOIN usuario u ON"
                    + " u.id=s.id_responsable WHERE s.id=?"
                        + (lock ? " FOR UPDATE" : ""),
                id);
    }

    private void publicado(Map<String, Object> s) {
        if (!"PUBLICADO".equals(s.get("estado_publicacion"))
                || !"ACTIVO".equals(s.get("cuenta_estado")))
            throw ApiException.conflict("El servicio debe estar publicado y su responsable activo");
    }

    private void recursosVigentes(long servicio, List<Long> bloqueados) {
        if (!db.recursos(servicio).equals(bloqueados))
            throw ApiException.conflict(
                    "Los recursos musicales cambiaron. Actualiza la agenda y reintenta");
    }

    private void propietario(Map<String, Object> s, AuthPrincipal a) {
        if (a == null || (!admin(a) && number(s, "id_responsable") != a.usuarioId()))
            throw ApiException.notFound();
    }

    private void permitido(Map<String, Object> row, AuthPrincipal a) {
        if (!admin(a)
                && number(row, "id_cliente") != a.usuarioId()
                && number(row, "id_proveedor") != a.usuarioId()) throw ApiException.notFound();
    }

    private void version(Map<String, Object> row, Long value) {
        if (value == null || number(row, "version") != value)
            throw ApiException.conflict("La información cambió. Actualiza antes de continuar");
    }

    private void abierta(Map<String, Object> s) {
        if (!ABIERTAS.contains(s.get("estado"))
                || !db.rows("SELECT id FROM contrato WHERE id_solicitud=?", s.get("id")).isEmpty())
            throw ApiException.conflict("La solicitud ya está cerrada o tiene contrato");
    }

    private void motivo(String text) {
        if (text == null || text.trim().length() < 5)
            throw ApiException.invalid("Escribe un motivo de al menos cinco caracteres");
    }

    private void intervalo(Instant inicio, Instant fin) {
        if (!inicio.equals(inicio.truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                || !fin.equals(fin.truncatedTo(java.time.temporal.ChronoUnit.SECONDS)))
            throw ApiException.invalid("Utiliza fechas con precisión de segundos");
        if (!inicio.isAfter(clock.instant())
                || !fin.isAfter(inicio)
                || Duration.between(inicio, fin).compareTo(Duration.ofHours(24)) > 0)
            throw ApiException.invalid(
                    "El evento debe ser futuro y durar entre un segundo y 24 horas");
    }

    private String encode(Object value) {
        return json.writeValueAsString(value);
    }

    private byte[] hash(Object value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(encode(value).getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void mismaHuella(Object stored, byte[] input) {
        if (!(stored instanceof byte[] bytes) || !MessageDigest.isEqual(bytes, input))
            throw ApiException.conflict("La clave ya fue utilizada para otra operación");
    }

    private void historial(
            AuthPrincipal a, long solicitud, String entidad, long id, String accion, String text) {
        db.update(
                "INSERT INTO"
                    + " historial_operacion(id_actor,id_solicitud,entidad,identificador_entidad,accion,detalle,correlation_id)"
                    + " VALUES(?,?,?,?,?,JSON_OBJECT('mensaje',?),?)",
                a.usuarioId(),
                solicitud,
                entidad,
                String.valueOf(id),
                accion,
                text,
                UUID.randomUUID().toString());
    }

    private void notificar(long usuario, String titulo, String entidad, long id) {
        db.update(
                "INSERT INTO"
                    + " notificacion(id_destinatario,tipo,titulo,mensaje,entidad,identificador_entidad)"
                    + " VALUES(?,'CONTRATACION',?,'Consulta el detalle para revisar la información"
                    + " actualizada',?,?)",
                usuario,
                titulo,
                entidad,
                id);
    }

    private List<Map<String, Object>> clean(List<Map<String, Object>> rows) {
        return rows.stream().map(ContratacionRepository::salida).toList();
    }

    private Map<String, Object> solicitud(long id, AuthPrincipal actor, boolean lock) {
        var row =
                db.one(
                        "SELECT * FROM solicitud_cotizacion WHERE id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        id);
        permitido(row, actor);
        return row;
    }

    private Map<String, Object> contrato(long id, AuthPrincipal actor, boolean lock) {
        var row = db.one("SELECT * FROM contrato WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
        permitido(row, actor);
        return row;
    }

    /** Unión de franjas locales, incluyendo cruces de medianoche en dos días. */
    private Instant[] atencion(long servicio, Instant inicio, Instant fin) {
        intervalo(inicio, fin);
        var franjas =
                db.rows(
                        "SELECT"
                            + " dia_semana,margen_previo_min,margen_posterior_min,TIME_FORMAT(hora_inicio,'%H:%i:%s')"
                            + " AS hora_inicio,TIME_FORMAT(hora_fin,'%H:%i:%s') AS hora_fin FROM"
                            + " horario_atencion WHERE id_servicio=? ORDER BY"
                            + " dia_semana,hora_inicio",
                        servicio);
        record Span(Instant start, Instant end, int before, int after) {}
        List<Span> spans = new ArrayList<>();
        var date = inicio.atZone(PERU).toLocalDate();
        for (int d = 0; d <= 1; d++) {
            LocalDate day = date.plusDays(d);
            for (var f : franjas)
                if (number(f, "dia_semana") == day.getDayOfWeek().getValue()) {
                    LocalTime start = LocalTime.parse(f.get("hora_inicio").toString());
                    LocalTime end =
                            LocalTime.parse(
                                    f.get("hora_fin").toString().equals("24:00:00")
                                            ? "00:00:00"
                                            : f.get("hora_fin").toString());
                    spans.add(
                            new Span(
                                    day.atTime(start).atZone(PERU).toInstant(),
                                    (end.equals(LocalTime.MIDNIGHT) ? day.plusDays(1) : day)
                                            .atTime(end)
                                            .atZone(PERU)
                                            .toInstant(),
                                    (int) number(f, "margen_previo_min"),
                                    (int) number(f, "margen_posterior_min")));
                }
        }
        spans.sort(Comparator.comparing(Span::start));
        Instant cursor = inicio;
        int before = 0, after = 0;
        for (var span : spans)
            if (!span.start().isAfter(cursor)
                    && span.end().isAfter(cursor)
                    && cursor.isBefore(fin)) {
                cursor = span.end();
                before = Math.max(before, span.before());
                after = Math.max(after, span.after());
            }
        if (cursor.isBefore(fin))
            throw ApiException.conflict(
                    "El intervalo no está cubierto por el horario de atención del servicio");
        return new Instant[] {inicio.minusSeconds(before * 60L), fin.plusSeconds(after * 60L)};
    }

    public Map<String, Object> solicitar(Solicitar b, AuthPrincipal a) {
        cliente(a);
        return write(
                () -> {
                    var resources = db.recursos(b.servicioId());
                    db.lock(resources);
                    var existing =
                            db.rows(
                                    "SELECT id,huella_creacion FROM solicitud_cotizacion WHERE"
                                        + " id_cliente=? AND clave_idempotencia=? FOR UPDATE",
                                    a.usuarioId(),
                                    b.clave());
                    if (!existing.isEmpty()) {
                        mismaHuella(existing.getFirst().get("huella_creacion"), hash(b));
                        return detalleSolicitud(number(existing.getFirst(), "id"), a);
                    }
                    var s = servicio(b.servicioId(), true);
                    recursosVigentes(b.servicioId(), resources);
                    publicado(s);
                    var range = atencion(b.servicioId(), b.inicio(), b.fin());
                    db.libre(resources, range[0], range[1]);
                    if (!db.rows(
                                    "SELECT id FROM solicitud_cotizacion WHERE id_cliente=? AND"
                                        + " id_servicio=? AND inicio_evento_utc=? AND"
                                        + " fin_evento_utc=? AND estado IN"
                                        + " ('ENVIADA','EN_REVISION','COTIZACION_RECIBIDA','CONTRATADA','RESERVADA')"
                                        + " FOR UPDATE",
                                    a.usuarioId(),
                                    b.servicioId(),
                                    utc(b.inicio()),
                                    utc(b.fin()))
                            .isEmpty())
                        throw ApiException.conflict(
                                "Ya tienes una solicitud abierta para ese servicio e intervalo");
                    long id =
                            db.insert(
                                    "INSERT INTO"
                                        + " solicitud_cotizacion(id_cliente,id_servicio,id_proveedor,inicio_evento_utc,fin_evento_utc,lugar,ciudad,tipo_evento,asistentes,preferencias,requerimientos,notas,clave_idempotencia,huella_creacion)"
                                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                    a.usuarioId(),
                                    b.servicioId(),
                                    s.get("id_responsable"),
                                    utc(b.inicio()),
                                    utc(b.fin()),
                                    b.lugar().trim(),
                                    b.ciudad().trim(),
                                    b.tipoEvento().trim(),
                                    b.asistentes(),
                                    b.preferencias(),
                                    b.requerimientos(),
                                    b.notas(),
                                    b.clave(),
                                    hash(b));
                    historial(a, id, "Solicitud", id, "SOLICITUD_ENVIADA", "Solicitud sin reserva");
                    notificar(
                            number(s, "id_responsable"),
                            "Nueva solicitud de cotización",
                            "Solicitud",
                            id);
                    return detalleSolicitud(id, a);
                });
    }

    public Map<String, Object> listado(
            String entidad, int pagina, int tamano, String estado, String q, AuthPrincipal a) {
        if (pagina < 0 || pagina > 100000 || tamano < 1 || tamano > 100)
            throw ApiException.invalid("Paginación inválida");
        boolean contratos = "contrato".equals(entidad);
        String table = contratos ? "contrato" : "solicitud_cotizacion";
        String scope =
                admin(a) ? "1=1" : a.rol() == Rol.CLIENTE ? "t.id_cliente=?" : "t.id_proveedor=?";
        List<Object> args = new ArrayList<>();
        if (!admin(a)) args.add(a.usuarioId());
        if (estado != null && !estado.isBlank()) {
            scope += " AND t.estado=?";
            args.add(estado);
        }
        if (q != null && !q.isBlank()) {
            if (q.length() > 100) throw ApiException.invalid("Búsqueda demasiado larga");
            scope += " AND (s.nombre LIKE ? OR r.tipo_evento LIKE ?)";
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        String from =
                " FROM "
                        + table
                        + " t JOIN servicio_musical s ON s.id=t.id_servicio JOIN"
                        + " solicitud_cotizacion r ON r.id="
                        + (contratos ? "t.id_solicitud" : "t.id")
                        + " WHERE "
                        + scope;
        long total = number(db.one("SELECT COUNT(*) AS total" + from, args.toArray()), "total");
        args.add(tamano);
        args.add(pagina * tamano);
        String columns =
                contratos
                        ? "t.id,t.codigo,t.estado,t.version,t.inicio_evento_utc,t.fin_evento_utc,t.total_centimos,t.aceptado_en"
                        : "t.id,t.estado,t.version,t.inicio_evento_utc,t.fin_evento_utc,t.creada_en,(SELECT"
                              + " c.total_centimos FROM cotizacion c WHERE c.id_solicitud=t.id"
                              + " ORDER BY c.numero_version DESC LIMIT 1) AS total_centimos";
        return Map.of(
                "items",
                clean(
                        db.rows(
                                "SELECT "
                                        + columns
                                        + ",s.nombre AS servicio,r.tipo_evento,r.ciudad"
                                        + from
                                        + " ORDER BY t.id DESC LIMIT ? OFFSET ?",
                                args.toArray())),
                "pagina",
                pagina,
                "tamano",
                tamano,
                "total",
                total);
    }

    public Map<String, Object> detalleSolicitud(long id, AuthPrincipal a) {
        var r = solicitud(id, a, false);
        var result = salida(r);
        result.remove("huella_creacion");
        result.remove("clave_idempotencia");
        result.put("servicio", servicio(number(r, "id_servicio"), false).get("nombre"));
        result.put(
                "cliente",
                db.one(
                                "SELECT CONCAT(nombres,' ',apellidos) AS nombre FROM usuario WHERE"
                                    + " id=?",
                                r.get("id_cliente"))
                        .get("nombre"));
        var versions =
                db.rows(
                        "SELECT"
                            + " id,numero_version,inicio_propuesto_utc,fin_propuesto_utc,subtotal_centimos,descuento_centimos,total_centimos,condiciones,politica_cancelacion,politica_version,vigente_hasta_utc,estado,creada_en"
                            + " FROM cotizacion WHERE id_solicitud=? ORDER BY numero_version DESC",
                        id);
        for (var c : versions) {
            if ("ENVIADA".equals(c.get("estado"))
                    && !instant(c.get("vigente_hasta_utc")).isAfter(clock.instant()))
                c.put("estado", "VENCIDA");
            c.put(
                    "conceptos",
                    db.rows(
                            "SELECT concepto,cantidad,precio_unitario_centimos,total_centimos FROM"
                                + " detalle_cotizacion WHERE id_cotizacion=? ORDER BY orden,id",
                            c.get("id")));
        }
        result.put("cotizaciones", clean(versions));
        result.put(
                "historial",
                clean(
                        db.rows(
                                "SELECT accion,detalle,ocurrido_en FROM historial_operacion WHERE"
                                    + " id_solicitud=? ORDER BY id DESC",
                                id)));
        result.put(
                "contratoId",
                db.rows("SELECT id FROM contrato WHERE id_solicitud=?", id).stream()
                        .map(x -> x.get("id"))
                        .findFirst()
                        .orElse(null));
        result.put("ahora", clock.instant().toString());
        return result;
    }

    public Map<String, Object> cambiarSolicitud(long id, Cambiar b, AuthPrincipal a) {
        return write(
                () -> {
                    var r = solicitud(id, a, true);
                    version(r, b.version());
                    abierta(r);
                    String state;
                    if ("CANCELAR".equals(b.accion()) && number(r, "id_cliente") == a.usuarioId()) {
                        motivo(b.motivo());
                        state = "CANCELADA";
                    } else if (number(r, "id_proveedor") == a.usuarioId()
                            && "REVISION".equals(b.accion())) state = "EN_REVISION";
                    else if (number(r, "id_proveedor") == a.usuarioId()
                            && "RECHAZAR".equals(b.accion())) {
                        motivo(b.motivo());
                        state = "RECHAZADA";
                    } else throw ApiException.notFound();
                    db.update(
                            "UPDATE solicitud_cotizacion SET"
                                + " estado=?,version=version+1,primera_respuesta_en=IF(id_proveedor=?,COALESCE(primera_respuesta_en,UTC_TIMESTAMP(6)),primera_respuesta_en)"
                                + " WHERE id=?",
                            state,
                            a.usuarioId(),
                            id);
                    if (!ABIERTAS.contains(state))
                        db.update(
                                "UPDATE cotizacion SET estado='RECHAZADA' WHERE id_solicitud=? AND"
                                    + " estado='ENVIADA'",
                                id);
                    historial(
                            a,
                            id,
                            "Solicitud",
                            id,
                            "SOLICITUD_" + state,
                            b.motivo() == null ? "Solicitud en revisión" : b.motivo());
                    notificar(
                            number(
                                    r,
                                    number(r, "id_cliente") == a.usuarioId()
                                            ? "id_proveedor"
                                            : "id_cliente"),
                            "Solicitud actualizada",
                            "Solicitud",
                            id);
                    return detalleSolicitud(id, a);
                });
    }

    public Map<String, Object> cotizar(long id, Cotizar b, AuthPrincipal a) {
        return write(
                () -> {
                    var initial = solicitud(id, a, false);
                    var resources = db.recursos(number(initial, "id_servicio"));
                    db.lock(resources);
                    var r = solicitud(id, a, true);
                    if (number(r, "id_proveedor") != a.usuarioId()) throw ApiException.notFound();
                    var repeated =
                            db.rows(
                                    "SELECT huella_envio FROM cotizacion WHERE id_solicitud=? AND"
                                        + " clave_envio=?",
                                    id,
                                    b.clave());
                    if (!repeated.isEmpty()) {
                        mismaHuella(repeated.getFirst().get("huella_envio"), hash(b));
                        return detalleSolicitud(id, a);
                    }
                    version(r, b.version());
                    abierta(r);
                    publicado(servicio(number(r, "id_servicio"), true));
                    recursosVigentes(number(r, "id_servicio"), resources);
                    var range = atencion(number(r, "id_servicio"), b.inicio(), b.fin());
                    db.libre(resources, range[0], range[1]);
                    if (!b.vigenteHasta().isAfter(clock.instant())
                            || b.vigenteHasta().isAfter(b.inicio()))
                        throw ApiException.invalid(
                                "La vigencia debe ser futura y no superar el inicio del evento");
                    long subtotal = 0;
                    for (var line : b.conceptos())
                        subtotal =
                                Math.addExact(
                                        subtotal,
                                        Math.multiplyExact(line.cantidad(), line.precioCentimos()));
                    long total = subtotal - b.descuentoCentimos();
                    if (total <= 0 || total > 100000000)
                        throw ApiException.invalid(
                                "El total debe ser positivo y no superar S/ 1 000 000,00");
                    long next =
                            number(
                                    db.one(
                                            "SELECT COALESCE(MAX(numero_version),0)+1 AS n FROM"
                                                + " cotizacion WHERE id_solicitud=?",
                                            id),
                                    "n");
                    db.update(
                            "UPDATE cotizacion SET"
                                + " estado=IF(vigente_hasta_utc<=UTC_TIMESTAMP(6),'VENCIDA','SUSTITUIDA')"
                                + " WHERE id_solicitud=? AND estado IN ('ENVIADA','CAMBIOS')",
                            id);
                    long quote =
                            db.insert(
                                    "INSERT INTO"
                                        + " cotizacion(id_solicitud,numero_version,inicio_propuesto_utc,fin_propuesto_utc,subtotal_centimos,descuento_centimos,condiciones,politica_cancelacion,politica_version,vigente_hasta_utc,clave_envio,huella_envio)"
                                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                                    id,
                                    next,
                                    utc(b.inicio()),
                                    utc(b.fin()),
                                    subtotal,
                                    b.descuentoCentimos(),
                                    b.condiciones().trim(),
                                    POLITICA,
                                    POLITICA_VERSION,
                                    utc(b.vigenteHasta()),
                                    b.clave(),
                                    hash(b));
                    int orden = 0;
                    for (var line : b.conceptos())
                        db.update(
                                "INSERT INTO"
                                    + " detalle_cotizacion(id_cotizacion,concepto,cantidad,precio_unitario_centimos,orden)"
                                    + " VALUES(?,?,?,?,?)",
                                quote,
                                line.concepto().trim(),
                                line.cantidad(),
                                line.precioCentimos(),
                                orden++);
                    db.update("UPDATE cotizacion SET estado='ENVIADA' WHERE id=?", quote);
                    db.update(
                            "UPDATE solicitud_cotizacion SET"
                                + " estado='COTIZACION_RECIBIDA',version=version+1,primera_respuesta_en=COALESCE(primera_respuesta_en,UTC_TIMESTAMP(6))"
                                + " WHERE id=?",
                            id);
                    historial(a, id, "Cotizacion", quote, "COTIZACION_ENVIADA", "Versión " + next);
                    notificar(
                            number(r, "id_cliente"),
                            "Nueva versión de cotización",
                            "Solicitud",
                            id);
                    return detalleSolicitud(id, a);
                });
    }

    private Map<String, Object> vigente(long quote, long solicitud) {
        var c =
                db.one(
                        "SELECT * FROM cotizacion WHERE id=? AND id_solicitud=? FOR UPDATE",
                        quote,
                        solicitud);
        if (!"ENVIADA".equals(c.get("estado"))
                || !instant(c.get("vigente_hasta_utc")).isAfter(clock.instant())
                || number(c, "numero_version")
                        != number(
                                db.one(
                                        "SELECT MAX(numero_version) AS n FROM cotizacion WHERE"
                                            + " id_solicitud=?",
                                        solicitud),
                                "n"))
            throw ApiException.conflict("Esta propuesta fue respondida, sustituida o venció");
        return c;
    }

    public Map<String, Object> responder(long id, long quote, Responder b, AuthPrincipal a) {
        cliente(a);
        return write(
                () -> {
                    var r = solicitud(id, a, true);
                    if (number(r, "id_cliente") != a.usuarioId()) throw ApiException.notFound();
                    version(r, b.version());
                    abierta(r);
                    vigente(quote, id);
                    if (!Set.of("CAMBIOS", "RECHAZAR").contains(b.accion()))
                        throw ApiException.invalid("Respuesta inválida");
                    if ("CAMBIOS".equals(b.accion())) motivo(b.motivo());
                    db.update(
                            "UPDATE cotizacion SET estado=? WHERE id=?",
                            "CAMBIOS".equals(b.accion()) ? "CAMBIOS" : "RECHAZADA",
                            quote);
                    db.update(
                            "UPDATE solicitud_cotizacion SET estado='EN_REVISION',version=version+1"
                                + " WHERE id=?",
                            id);
                    historial(
                            a,
                            id,
                            "Cotizacion",
                            quote,
                            "COTIZACION_" + b.accion(),
                            b.motivo() == null ? "Cotización rechazada" : b.motivo());
                    notificar(
                            number(r, "id_proveedor"),
                            "Respuesta a tu cotización",
                            "Solicitud",
                            id);
                    return detalleSolicitud(id, a);
                });
    }

    public Map<String, Object> aceptar(long id, long quote, Aceptar b, AuthPrincipal a) {
        cliente(a);
        return write(
                () -> {
                    var initial = solicitud(id, a, false);
                    if (number(initial, "id_cliente") != a.usuarioId())
                        throw ApiException.notFound();
                    var resources = db.recursos(number(initial, "id_servicio"));
                    db.lock(resources);
                    byte[] fingerprint = hash(List.of(id, quote, b));
                    var repeated =
                            db.rows(
                                    "SELECT id,huella_aceptacion FROM contrato WHERE id_cliente=?"
                                        + " AND clave_idempotencia=? FOR UPDATE",
                                    a.usuarioId(),
                                    b.clave());
                    if (!repeated.isEmpty()) {
                        mismaHuella(repeated.getFirst().get("huella_aceptacion"), fingerprint);
                        return detalleContrato(number(repeated.getFirst(), "id"), a);
                    }
                    var r = solicitud(id, a, true);
                    version(r, b.version());
                    abierta(r);
                    var c = vigente(quote, id);
                    var s = servicio(number(r, "id_servicio"), true);
                    recursosVigentes(number(r, "id_servicio"), resources);
                    publicado(s);
                    var inicio = instant(c.get("inicio_propuesto_utc"));
                    var fin = instant(c.get("fin_propuesto_utc"));
                    var range = atencion(number(r, "id_servicio"), inicio, fin);
                    db.libre(resources, range[0], range[1]);
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("servicio", salida(s));
                    snapshot.put(
                            "cliente",
                            db.one(
                                    "SELECT id,nombres,apellidos,correo FROM usuario WHERE id=?",
                                    r.get("id_cliente")));
                    snapshot.put(
                            "proveedor",
                            db.one(
                                    "SELECT id,nombres,apellidos,correo FROM usuario WHERE id=?",
                                    r.get("id_proveedor")));
                    var solicitud = salida(r);
                    solicitud.remove("huella_creacion");
                    solicitud.remove("clave_idempotencia");
                    snapshot.put("solicitud", solicitud);
                    var propuesta = salida(c);
                    propuesta.remove("huella_envio");
                    propuesta.remove("clave_envio");
                    snapshot.put("cotizacion", propuesta);
                    snapshot.put(
                            "conceptos",
                            db.rows(
                                    "SELECT"
                                        + " concepto,cantidad,precio_unitario_centimos,total_centimos"
                                        + " FROM detalle_cotizacion WHERE id_cotizacion=? ORDER BY"
                                        + " orden,id",
                                    quote));
                    snapshot.put(
                            "integrantes",
                            db.rows(
                                    "SELECT"
                                        + " nombre,instrumento,funcion,id_solista_vinculado,estado_vinculo"
                                        + " FROM integrante WHERE id_servicio=? AND activo=1 ORDER"
                                        + " BY id",
                                    r.get("id_servicio")));
                    snapshot.put("recursos", resources);
                    snapshot.put("zonaHoraria", "America/Lima");
                    snapshot.put("versionSnapshot", 1);
                    long contract =
                            db.insert(
                                    "INSERT INTO"
                                        + " contrato(codigo,id_cotizacion,id_solicitud,id_servicio,id_cliente,id_proveedor,inicio_evento_utc,fin_evento_utc,inicio_ocupacion_utc,fin_ocupacion_utc,total_centimos,condiciones_aceptadas,politica_version,snapshot,clave_idempotencia,huella_aceptacion)"
                                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?,1,?,?,?,?)",
                                    "SC-" + UUID.randomUUID(),
                                    quote,
                                    id,
                                    r.get("id_servicio"),
                                    r.get("id_cliente"),
                                    r.get("id_proveedor"),
                                    utc(inicio),
                                    utc(fin),
                                    utc(range[0]),
                                    utc(range[1]),
                                    c.get("total_centimos"),
                                    c.get("politica_version"),
                                    encode(snapshot),
                                    b.clave(),
                                    fingerprint);
                    db.update("UPDATE cotizacion SET estado='ACEPTADA' WHERE id=?", quote);
                    db.update(
                            "UPDATE solicitud_cotizacion SET estado='CONTRATADA',version=version+1"
                                + " WHERE id=?",
                            id);
                    historial(
                            a,
                            id,
                            "Contrato",
                            contract,
                            "CONTRATO_ACEPTADO",
                            "Pendiente de pago; no se crea reserva ni retención");
                    notificar(
                            number(r, "id_proveedor"),
                            "Contrato pendiente de pago",
                            "Contrato",
                            contract);
                    notificar(
                            a.usuarioId(),
                            "Contrato creado: falta pagar para reservar",
                            "Contrato",
                            contract);
                    return detalleContrato(contract, a);
                });
    }

    public Map<String, Object> detalleContrato(long id, AuthPrincipal a) {
        var r = contrato(id, a, false);
        var out = salida(r);
        out.remove("huella_aceptacion");
        out.remove("clave_idempotencia");
        out.put("snapshot", json.readValue(r.get("snapshot").toString(), Map.class));
        out.put("ahora", clock.instant().toString());
        out.put(
                "historial",
                clean(
                        db.rows(
                                "SELECT accion,detalle,ocurrido_en FROM historial_operacion WHERE"
                                    + " id_solicitud=? ORDER BY id DESC",
                                r.get("id_solicitud"))));
        return out;
    }

    public Map<String, Object> cambiarContrato(long id, Cambiar b, AuthPrincipal a) {
        return write(
                () -> {
                    var initial = contrato(id, a, false);
                    Map<?, ?> snapshot =
                            json.readValue(initial.get("snapshot").toString(), Map.class);
                    List<Long> resources =
                            ((List<?>) snapshot.get("recursos"))
                                    .stream().map(x -> ((Number) x).longValue()).sorted().toList();
                    db.lock(resources);
                    var r = contrato(id, a, true);
                    version(r, b.version());
                    if (!"CONFIRMADO".equals(r.get("estado")))
                        throw ApiException.conflict(
                                "Esta acción requiere un contrato confirmado por un pago"
                                    + " verificado");
                    String state;
                    if ("CANCELAR".equals(b.accion())) {
                        motivo(b.motivo());
                        state = "CANCELADO";
                        db.update(
                                "UPDATE contrato SET"
                                    + " estado='CANCELADO',version=version+1,cancelado_en=UTC_TIMESTAMP(6),motivo_cancelacion=?"
                                    + " WHERE id=?",
                                b.motivo().trim(),
                                id);
                    } else if ("FINALIZAR".equals(b.accion())
                            && (admin(a) || number(r, "id_proveedor") == a.usuarioId())) {
                        if (clock.instant().isBefore(instant(r.get("fin_evento_utc"))))
                            throw ApiException.conflict("El horario del evento aún no terminó");
                        state = "FINALIZADO";
                        db.update(
                                "UPDATE contrato SET"
                                    + " estado='FINALIZADO',version=version+1,finalizado_en=UTC_TIMESTAMP(6)"
                                    + " WHERE id=?",
                                id);
                    } else throw ApiException.notFound();
                    db.update(
                            "UPDATE ocupacion_agenda SET activa=0 WHERE id_reserva IN (SELECT id"
                                + " FROM reserva WHERE id_contrato=?) OR id_retencion IN (SELECT id"
                                + " FROM retencion_agenda WHERE id_contrato=?)",
                            id,
                            id);
                    if ("CANCELADO".equals(state))
                        db.update(
                                "UPDATE reserva SET"
                                    + " estado='CANCELADA',cancelada_en=UTC_TIMESTAMP(6) WHERE"
                                    + " id_contrato=? AND estado='CONFIRMADA'",
                                id);
                    db.update(
                            "UPDATE retencion_agenda SET estado='CANCELADA' WHERE id_contrato=? AND"
                                + " estado='ACTIVA'",
                            id);
                    historial(
                            a,
                            number(r, "id_solicitud"),
                            "Contrato",
                            id,
                            "CONTRATO_" + state,
                            b.motivo() == null
                                    ? "Finalización registrada con hora del servidor"
                                    : b.motivo());
                    notificar(
                            number(r, "id_cliente"),
                            "Contrato " + state.toLowerCase(Locale.ROOT),
                            "Contrato",
                            id);
                    notificar(
                            number(r, "id_proveedor"),
                            "Contrato " + state.toLowerCase(Locale.ROOT),
                            "Contrato",
                            id);
                    return detalleContrato(id, a);
                });
    }

    public Map<String, Object> agenda(
            long id, LocalDate desde, LocalDate hasta, AuthPrincipal a, boolean privada) {
        if (desde == null
                || hasta == null
                || hasta.isBefore(desde)
                || java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) > 62)
            throw ApiException.invalid("Consulta hasta 63 días de agenda");
        var s = servicio(id, false);
        if (privada) propietario(s, a);
        else if (!"PUBLICADO".equals(s.get("estado_publicacion"))
                || !"ACTIVO".equals(s.get("cuenta_estado"))) propietario(s, a);
        var resources = db.recursos(id);
        List<Map<String, Object>> ocupaciones = new ArrayList<>();
        var from = utc(desde.atStartOfDay(PERU).toInstant());
        var to = utc(hasta.plusDays(1).atStartOfDay(PERU).toInstant());
        Set<String> seen = new HashSet<>();
        for (long resource : resources)
            for (var row :
                    db.rows(
                            "SELECT inicio_utc,fin_utc,IF(id_reserva IS NOT"
                                + " NULL,'RESERVADO','OCUPADO') AS estado FROM ocupacion_agenda"
                                + " WHERE id_recurso=? AND activa=1 AND (vence_en IS NULL OR"
                                + " vence_en>UTC_TIMESTAMP(6)) AND inicio_utc<? AND fin_utc>? ORDER"
                                + " BY inicio_utc",
                            resource,
                            to,
                            from)) {
                if (seen.add(row.toString())) ocupaciones.add(salida(row));
            }
        var out = new LinkedHashMap<String, Object>();
        out.put("version", s.get("agenda_version"));
        out.put("zonaHoraria", "America/Lima");
        out.put("ahora", clock.instant().toString());
        out.put(
                "horarios",
                db.rows(
                        "SELECT dia_semana,TIME_FORMAT(hora_inicio,'%H:%i:%s') AS"
                            + " hora_inicio,TIME_FORMAT(hora_fin,'%H:%i:%s') AS"
                            + " hora_fin,margen_previo_min,margen_posterior_min FROM"
                            + " horario_atencion WHERE id_servicio=? ORDER BY"
                            + " dia_semana,hora_inicio",
                        id));
        out.put("ocupaciones", ocupaciones);
        if (privada)
            out.put(
                    "bloqueos",
                    clean(
                            db.rows(
                                    "SELECT id,inicio_utc,fin_utc,motivo FROM bloqueo_agenda WHERE"
                                        + " id_servicio=? AND activo=1 AND inicio_utc<? AND"
                                        + " fin_utc>? ORDER BY inicio_utc",
                                    id,
                                    to,
                                    from)));
        return out;
    }

    private int endSecond(String value) {
        return value.startsWith("24:") ? 86400 : LocalTime.parse(value).toSecondOfDay();
    }

    public Map<String, Object> horarios(long id, Horarios b, AuthPrincipal a) {
        return write(
                () -> {
                    var resources = db.recursos(id);
                    db.lock(resources);
                    var s = servicio(id, true);
                    recursosVigentes(id, resources);
                    propietario(s, a);
                    if (number(s, "agenda_version") != b.version())
                        throw ApiException.conflict("La configuración de agenda cambió. Actualiza");
                    for (var f : b.franjas())
                        if (endSecond(f.hasta()) <= f.desde().toSecondOfDay())
                            throw ApiException.invalid(
                                    "Cada franja termina el mismo día; divide los cruces de"
                                        + " medianoche en dos franjas");
                    var sorted =
                            b.franjas().stream()
                                    .sorted(
                                            Comparator.comparingInt(Franja::dia)
                                                    .thenComparing(Franja::desde))
                                    .toList();
                    for (int n = 1; n < sorted.size(); n++)
                        if (sorted.get(n).dia() == sorted.get(n - 1).dia()
                                && sorted.get(n).desde().toSecondOfDay()
                                        < endSecond(sorted.get(n - 1).hasta()))
                            throw ApiException.invalid("Las franjas no deben superponerse");
                    db.update("DELETE FROM horario_atencion WHERE id_servicio=?", id);
                    for (var f : sorted)
                        db.update(
                                "INSERT INTO"
                                    + " horario_atencion(id_servicio,dia_semana,hora_inicio,hora_fin,margen_previo_min,margen_posterior_min)"
                                    + " VALUES(?,?,?,?,?,?)",
                                id,
                                f.dia(),
                                f.desde(),
                                f.hasta(),
                                f.previoMin(),
                                f.posteriorMin());
                    db.update(
                            "UPDATE servicio_musical SET agenda_version=agenda_version+1 WHERE"
                                + " id=?",
                            id);
                    db.update(
                            "INSERT INTO"
                                + " historial_operacion(id_actor,entidad,identificador_entidad,accion,correlation_id)"
                                + " VALUES(?,'Agenda',?,'HORARIOS_ACTUALIZADOS',?)",
                            a.usuarioId(),
                            String.valueOf(id),
                            UUID.randomUUID().toString());
                    return Map.of("version", b.version() + 1);
                });
    }

    public Map<String, Object> bloquear(long id, Bloquear b, AuthPrincipal a) {
        return write(
                () -> {
                    var resources = db.recursos(id);
                    db.lock(resources);
                    propietario(servicio(id, true), a);
                    recursosVigentes(id, resources);
                    intervalo(b.inicio(), b.fin());
                    db.libre(resources, b.inicio(), b.fin());
                    long block =
                            db.insert(
                                    "INSERT INTO"
                                        + " bloqueo_agenda(id_servicio,id_actor,inicio_utc,fin_utc,motivo)"
                                        + " VALUES(?,?,?,?,?)",
                                    id,
                                    a.usuarioId(),
                                    utc(b.inicio()),
                                    utc(b.fin()),
                                    b.motivo().trim());
                    for (long resource : resources)
                        db.update(
                                "INSERT INTO"
                                    + " ocupacion_agenda(id_recurso,id_bloqueo,inicio_utc,fin_utc)"
                                    + " VALUES(?,?,?,?)",
                                resource,
                                block,
                                utc(b.inicio()),
                                utc(b.fin()));
                    auditarAgenda(a, id, "BLOQUEO_CREADO");
                    return Map.of("id", block);
                });
    }

    public void quitarBloqueo(long id, long block, AuthPrincipal a) {
        write(
                () -> {
                    // Lock the original frozen set as well as the current set: later roster changes
                    // never release another musician's lock early.
                    var resources = new TreeSet<>(db.recursos(id));
                    for (var row :
                            db.rows(
                                    "SELECT id_recurso FROM ocupacion_agenda WHERE id_bloqueo=?",
                                    block)) resources.add(number(row, "id_recurso"));
                    db.lock(new ArrayList<>(resources));
                    propietario(servicio(id, true), a);
                    var row =
                            db.one(
                                    "SELECT id FROM bloqueo_agenda WHERE id=? AND id_servicio=? FOR"
                                        + " UPDATE",
                                    block,
                                    id);
                    db.update("UPDATE ocupacion_agenda SET activa=0 WHERE id_bloqueo=?", block);
                    db.update(
                            "UPDATE bloqueo_agenda SET"
                                + " activo=0,retirado_en=COALESCE(retirado_en,UTC_TIMESTAMP(6))"
                                + " WHERE id=?",
                            block);
                    auditarAgenda(a, id, "BLOQUEO_RETIRADO");
                    return true;
                });
    }

    private void auditarAgenda(AuthPrincipal a, long id, String accion) {
        db.update(
                "INSERT INTO"
                    + " historial_operacion(id_actor,entidad,identificador_entidad,accion,correlation_id)"
                    + " VALUES(?,'Agenda',?,?,?)",
                a.usuarioId(),
                String.valueOf(id),
                accion,
                UUID.randomUUID().toString());
    }

    public Map<String, Object> resumen(AuthPrincipal a) {
        String scope =
                admin(a) ? "1=1" : a.rol() == Rol.CLIENTE ? "id_cliente=?" : "id_proveedor=?";
        Object[] args = admin(a) ? new Object[] {} : new Object[] {a.usuarioId()};
        return Map.of(
                "solicitudes",
                number(
                        db.one(
                                "SELECT COUNT(*) AS n FROM solicitud_cotizacion WHERE "
                                        + scope
                                        + " AND estado IN"
                                        + " ('ENVIADA','EN_REVISION','COTIZACION_RECIBIDA')",
                                args),
                        "n"),
                "contratos",
                number(db.one("SELECT COUNT(*) AS n FROM contrato WHERE " + scope, args), "n"),
                "eventos",
                number(
                        db.one(
                                "SELECT COUNT(*) AS n FROM contrato WHERE "
                                        + scope
                                        + " AND estado='CONFIRMADO' AND"
                                        + " fin_evento_utc>UTC_TIMESTAMP(6)",
                                args),
                        "n"));
    }
}
