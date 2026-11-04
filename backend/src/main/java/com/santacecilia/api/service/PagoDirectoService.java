package com.santacecilia.api.service;

import static com.santacecilia.api.repository.ContratacionRepository.*;

import com.santacecilia.api.dto.PagoDirectoDtos.*;
import com.santacecilia.api.entity.Rol;
import com.santacecilia.api.exception.ApiException;
import com.santacecilia.api.repository.ContratacionRepository;
import com.santacecilia.api.security.AuthPrincipal;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * Pagos directos: reporte del cliente, conciliación humana y aplicación contable son hechos
 * distintos.
 */
@Service
public class PagoDirectoService {
    private final ContratacionRepository db;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final ComprobanteImagenService imagen;
    private final Clock clock;
    private static final ZoneId PERU = ZoneId.of("America/Lima");

    public PagoDirectoService(
            ContratacionRepository db,
            PlatformTransactionManager manager,
            ObjectMapper json,
            ComprobanteImagenService imagen,
            Clock clock) {
        this.db = db;
        this.tx = new TransactionTemplate(manager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.json = json;
        this.imagen = imagen;
        this.clock = clock;
    }

    private <T> T write(Supplier<T> work) {
        for (int n = 0; ; n++)
            try {
                return tx.execute(s -> work.get());
            } catch (PessimisticLockingFailureException e) {
                if (n >= 2)
                    throw ApiException.conflict(
                            "Otra operación está modificando este pago. Actualiza y reintenta");
            }
    }

    private boolean admin(AuthPrincipal a) {
        return a.rol() == Rol.ADMINISTRADOR;
    }

    private void soloAdmin(AuthPrincipal a) {
        if (!admin(a)) throw ApiException.notFound();
    }

    private String encode(Object o) {
        return json.writeValueAsString(o);
    }

    private byte[] sha(byte[] raw) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] hash(Object o) {
        return sha(encode(o).getBytes(StandardCharsets.UTF_8));
    }

    private void same(Object stored, byte[] expected) {
        if (!(stored instanceof byte[] bytes) || !MessageDigest.isEqual(bytes, expected))
            throw ApiException.conflict("Esta operación ya se registró con otros datos");
    }

    private void version(Map<String, Object> row, long version) {
        if (number(row, "version") != version)
            throw ApiException.conflict("El registro cambió. Actualiza antes de continuar");
    }

    private boolean flag(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return Boolean.TRUE.equals(v) || (v instanceof Number n && n.intValue() != 0);
    }

    private void motivo(String value) {
        if (value == null || value.trim().length() < 5)
            throw ApiException.invalid("Escribe un motivo de al menos cinco caracteres");
    }

    private Map<String, Object> contrato(long id, AuthPrincipal a, boolean lock) {
        var c = db.one("SELECT * FROM contrato WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
        if (!admin(a)
                && number(c, "id_cliente") != a.usuarioId()
                && number(c, "id_proveedor") != a.usuarioId()) throw ApiException.notFound();
        return c;
    }

    private void cliente(Map<String, Object> c, AuthPrincipal a) {
        if (a.rol() != Rol.CLIENTE || number(c, "id_cliente") != a.usuarioId())
            throw ApiException.notFound();
    }

    private List<Long> recursos(Map<String, Object> c) {
        Map<?, ?> snap = json.readValue(c.get("snapshot").toString(), Map.class);
        return ((List<?>) snap.get("recursos"))
                .stream().map(x -> ((Number) x).longValue()).sorted().toList();
    }

    private Map<String, Object> bloquearContrato(long id, AuthPrincipal a) {
        var initial = contrato(id, a, false);
        db.lock(recursos(initial));
        return contrato(id, a, true);
    }

    private Map<String, Object> pago(long id, AuthPrincipal a, boolean lock) {
        var p =
                db.one(
                        "SELECT * FROM pago WHERE id=? AND proveedor='DIRECTO'"
                                + (lock ? " FOR UPDATE" : ""),
                        id);
        contrato(number(p, "id_contrato"), a, false);
        return p;
    }

    private Map<String, Object> saldo(long id) {
        return db.one("SELECT * FROM v_saldo_contrato WHERE id_contrato=?", id);
    }

    private Map<String, Object> destino(long id) {
        return db.one("SELECT * FROM cuenta_cobro WHERE id=?", id);
    }

    private void fecha(LocalDate date) {
        if (date == null
                || date.isAfter(LocalDate.now(clock.withZone(PERU)))
                || date.isBefore(LocalDate.of(2000, 1, 1)))
            throw ApiException.invalid("Indica una fecha real de operación, no futura");
    }

    private String operacion(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{3,100}"))
            throw ApiException.invalid(
                    "Indica el número de operación, con letras, números o guion");
        return value.toUpperCase(Locale.ROOT);
    }

    private void evento(
            Map<String, Object> c,
            AuthPrincipal a,
            String entidad,
            long id,
            String accion,
            String mensaje) {
        db.update(
                "INSERT INTO"
                    + " historial_operacion(id_actor,id_solicitud,entidad,identificador_entidad,accion,detalle,correlation_id)"
                    + " VALUES(?,?,?,?,?,JSON_OBJECT('mensaje',?),?)",
                a.usuarioId(),
                c.get("id_solicitud"),
                entidad,
                String.valueOf(id),
                accion,
                mensaje,
                UUID.randomUUID().toString());
        db.update(
                "INSERT INTO evento_outbox(clave_evento,tipo,entidad,identificador_entidad,payload)"
                    + " VALUES(?,'AVISO_INTERNO',?,?,?)",
                UUID.randomUUID().toString(),
                entidad,
                id,
                encode(
                        Map.of(
                                "cliente",
                                c.get("id_cliente"),
                                "proveedor",
                                c.get("id_proveedor"),
                                "titulo",
                                mensaje.substring(
                                        0,
                                        mensaje.offsetByCodePoints(
                                                0,
                                                Math.min(
                                                        160,
                                                        mensaje.codePointCount(
                                                                0, mensaje.length())))),
                                "entidad",
                                entidad,
                                "id",
                                id,
                                "avisarAdmin",
                                Set.of("COMPROBANTE_REPORTADO", "INGRESO_CON_INCIDENCIA")
                                        .contains(accion))));
    }

    private void auditarCuenta(AuthPrincipal a, long id, String accion) {
        db.update(
                "INSERT INTO"
                    + " historial_operacion(id_actor,entidad,identificador_entidad,accion,correlation_id)"
                    + " VALUES(?,'CuentaCobro',?,?,?)",
                a.usuarioId(),
                String.valueOf(id),
                accion,
                UUID.randomUUID().toString());
    }

    public List<Map<String, Object>> cuentas(AuthPrincipal a) {
        soloAdmin(a);
        return db.rows("SELECT * FROM cuenta_cobro ORDER BY activo DESC,id").stream()
                .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                .toList();
    }

    public Map<String, Object> guardarCuenta(Long id, Cuenta b, AuthPrincipal a) {
        soloAdmin(a);
        return write(
                () -> {
                    if (!b.yape() && !b.plin() && !b.transferencia())
                        throw ApiException.invalid("Habilita al menos un medio de pago");
                    if ((b.yape() || b.plin()) && b.telefono() == null)
                        throw ApiException.invalid("Yape y Plin requieren un celular receptor");
                    if (b.transferencia() && b.numeroCuenta() == null && b.cci() == null)
                        throw ApiException.invalid("Indica la cuenta o el CCI para transferencia");
                    if (id == null) {
                        long created =
                                db.insert(
                                        "INSERT INTO"
                                            + " cuenta_cobro(titular,banco,telefono,numero_cuenta,cci,yape,plin,transferencia,activo)"
                                            + " VALUES(?,?,?,?,?,?,?,?,?)",
                                        b.titular(),
                                        b.banco(),
                                        b.telefono(),
                                        b.numeroCuenta(),
                                        b.cci(),
                                        b.yape(),
                                        b.plin(),
                                        b.transferencia(),
                                        b.activo());
                        auditarCuenta(a, created, "CUENTA_COBRO_CREADA");
                        return salida(destino(created));
                    }
                    var old = db.one("SELECT * FROM cuenta_cobro WHERE id=? FOR UPDATE", id);
                    version(old, b.version());
                    boolean used =
                            !db.rows(
                                            "SELECT id_pago FROM pago_directo WHERE id_cuenta=?"
                                                + " LIMIT 1",
                                            id)
                                    .isEmpty();
                    if (used
                            && (!Objects.equals(old.get("titular"), b.titular())
                                    || !Objects.equals(old.get("banco"), b.banco())
                                    || !Objects.equals(old.get("telefono"), b.telefono())
                                    || !Objects.equals(old.get("numero_cuenta"), b.numeroCuenta())
                                    || !Objects.equals(old.get("cci"), b.cci())))
                        throw ApiException.conflict(
                                "Esta cuenta tiene historial. Desactívala y registra otra para"
                                    + " cambiar sus datos receptores");
                    db.update(
                            "UPDATE cuenta_cobro SET"
                                + " titular=?,banco=?,telefono=?,numero_cuenta=?,cci=?,yape=?,plin=?,transferencia=?,activo=?,version=version+1"
                                + " WHERE id=?",
                            b.titular(),
                            b.banco(),
                            b.telefono(),
                            b.numeroCuenta(),
                            b.cci(),
                            b.yape(),
                            b.plin(),
                            b.transferencia(),
                            b.activo(),
                            id);
                    auditarCuenta(a, id, "CUENTA_COBRO_ACTUALIZADA");
                    return salida(destino(id));
                });
    }

    public Map<String, Object> resumen(long contrato, AuthPrincipal a) {
        contrato(contrato, a, false);
        var out = salida(saldo(contrato));
        out.put("estadoContrato", contrato(contrato, a, false).get("estado"));
        out.put(
                "cuentas",
                db.rows("SELECT * FROM cuenta_cobro WHERE activo=1 ORDER BY id").stream()
                        .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                        .toList());
        out.put(
                "pagos",
                db
                        .rows(
                                "SELECT"
                                    + " id,metodo,modalidad,importe_centimos,estado,version,creado_en,aplicado_en"
                                    + " FROM pago WHERE id_contrato=? AND proveedor='DIRECTO' ORDER"
                                    + " BY id DESC",
                                contrato)
                        .stream()
                        .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                        .toList());
        return out;
    }

    public Map<String, Object> iniciar(long id, Iniciar b, AuthPrincipal a) {
        return write(
                () -> {
                    var c = bloquearContrato(id, a);
                    cliente(c, a);
                    byte[] fingerprint = hash(b);
                    var old =
                            db.rows(
                                    "SELECT id,huella_solicitud FROM pago WHERE id_contrato=? AND"
                                        + " clave_idempotencia=? FOR UPDATE",
                                    id,
                                    b.clave());
                    if (!old.isEmpty()) {
                        same(old.getFirst().get("huella_solicitud"), fingerprint);
                        return detalle(number(old.getFirst(), "id"), a);
                    }
                    if (!Set.of("PENDIENTE_PAGO", "CONFIRMADO", "FINALIZADO")
                            .contains(c.get("estado")))
                        throw ApiException.conflict("El contrato no admite nuevos pagos");
                    if (!db.rows(
                                    "SELECT id FROM pago WHERE id_contrato=? AND estado IN"
                                        + " ('PENDIENTE','PROCESANDO','EN_REVISION','REEMBOLSO_PENDIENTE')"
                                        + " FOR UPDATE",
                                    id)
                            .isEmpty())
                        throw ApiException.conflict(
                                "Hay un pago pendiente o una incidencia. Consulta ese registro"
                                    + " antes de transferir otra vez");
                    long balance = number(saldo(id), "saldo_centimos");
                    if (balance <= 0)
                        throw ApiException.conflict("El contrato ya no tiene saldo pendiente");
                    if ("PENDIENTE_PAGO".equals(c.get("estado"))) {
                        if (!instant(c.get("inicio_evento_utc")).isAfter(clock.instant()))
                            throw ApiException.conflict(
                                    "El evento ya comenzó; no se puede iniciar su reserva");
                        db.libre(
                                recursos(c),
                                instant(c.get("inicio_ocupacion_utc")),
                                instant(c.get("fin_ocupacion_utc")));
                    }
                    long importe =
                            switch (b.modalidad()) {
                                case "SALDO_COMPLETO" -> balance;
                                case "MITAD_SALDO" -> (balance + 1) / 2;
                                default ->
                                        throw ApiException.invalid(
                                                "Elige saldo completo o mitad del saldo");
                            };
                    if (!Set.of("YAPE", "PLIN", "TRANSFERENCIA").contains(b.metodo()))
                        throw ApiException.invalid("Medio de pago no válido");
                    var account =
                            db.one(
                                    "SELECT * FROM cuenta_cobro WHERE id=? FOR UPDATE",
                                    b.cuentaId());
                    if (!flag(account, "activo")
                            || !flag(account, b.metodo().toLowerCase(Locale.ROOT)))
                        throw ApiException.conflict("La cuenta no está habilitada para ese medio");
                    long p =
                            db.insert(
                                    "INSERT INTO"
                                        + " pago(id_contrato,id_cliente,proveedor,clave_idempotencia,huella_solicitud,modalidad,metodo,importe_centimos)"
                                        + " VALUES(?,?,'DIRECTO',?,?,?,?,?)",
                                    id,
                                    a.usuarioId(),
                                    b.clave(),
                                    fingerprint,
                                    b.modalidad(),
                                    b.metodo(),
                                    importe);
                    db.update(
                            "INSERT INTO pago_directo(id_pago,id_cuenta,destino_snapshot)"
                                + " VALUES(?,?,?)",
                            p,
                            b.cuentaId(),
                            encode(salida(account)));
                    evento(
                            c,
                            a,
                            "Pago",
                            p,
                            "PAGO_INICIADO",
                            "Instrucciones de pago directo registradas");
                    return detalle(p, a);
                });
    }

    public Map<String, Object> detalle(long id, AuthPrincipal a) {
        var p = pago(id, a, false);
        var out = salida(p);
        out.remove("huella_solicitud");
        out.remove("clave_idempotencia");
        var direct =
                salida(
                        db.one(
                                "SELECT"
                                    + " id_cuenta,destino_snapshot,operacion_reportada,DATE_FORMAT(fecha_reportada,'%Y-%m-%d')"
                                    + " AS fecha_reportada,importe_reportado_centimos,titular_origen,reportado_en,motivo_revision,id_revisor"
                                    + " FROM pago_directo WHERE id_pago=?",
                                id));
        direct.put(
                "destino_snapshot",
                json.readValue(direct.get("destino_snapshot").toString(), Map.class));
        out.put("directo", direct);
        out.put(
                "comprobante",
                !db.rows("SELECT id FROM comprobante_directo WHERE id_pago=?", id).isEmpty());
        out.put(
                "movimientos",
                db
                        .rows(
                                "SELECT operacion,DATE_FORMAT(fecha,'%Y-%m-%d') AS"
                                    + " fecha,importe_centimos,registrado_en FROM movimiento_cuenta"
                                    + " WHERE id_pago=?",
                                id)
                        .stream()
                        .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                        .toList());
        out.put(
                "reembolsos",
                db
                        .rows(
                                "SELECT"
                                    + " id,importe_centimos,motivo,estado,version,solicitado_en,confirmado_en"
                                    + " FROM reembolso WHERE id_pago=? ORDER BY id DESC",
                                id)
                        .stream()
                        .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                        .toList());
        out.put(
                "incidencias",
                db
                        .rows(
                                "SELECT tipo,estado,detalle,creada_en FROM incidencia_pago WHERE"
                                    + " id_pago=? ORDER BY id DESC",
                                id)
                        .stream()
                        .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                        .toList());
        out.put("saldo", salida(saldo(number(p, "id_contrato"))));
        out.put("contratoEstado", contrato(number(p, "id_contrato"), a, false).get("estado"));
        return out;
    }

    public Map<String, Object> listar(int pagina, String estado, AuthPrincipal a) {
        if (pagina < 0 || pagina > 100000) throw ApiException.invalid("Página inválida");
        String scope =
                admin(a) ? "1=1" : a.rol() == Rol.CLIENTE ? "c.id_cliente=?" : "c.id_proveedor=?";
        List<Object> args = new ArrayList<>();
        if (!admin(a)) args.add(a.usuarioId());
        if (estado != null && !estado.isBlank()) {
            scope += " AND p.estado=?";
            args.add(estado);
        }
        String from =
                " FROM pago p JOIN contrato c ON c.id=p.id_contrato JOIN servicio_musical s ON"
                    + " s.id=c.id_servicio WHERE p.proveedor='DIRECTO' AND "
                        + scope;
        long total = number(db.one("SELECT COUNT(*) AS n" + from, args.toArray()), "n");
        args.add(pagina * 20);
        return Map.of(
                "items",
                db
                        .rows(
                                "SELECT"
                                    + " p.id,p.id_contrato,p.metodo,p.importe_centimos,p.estado,p.creado_en,c.codigo,s.nombre"
                                    + " AS servicio"
                                        + from
                                        + " ORDER BY p.id DESC LIMIT 20 OFFSET ?",
                                args.toArray())
                        .stream()
                        .map(com.santacecilia.api.repository.ContratacionRepository::salida)
                        .toList(),
                "pagina",
                pagina,
                "total",
                total);
    }

    private void guardarImagen(Long pago, Long reembolso, byte[] bytes, AuthPrincipal a) {
        db.update(
                "INSERT INTO comprobante_directo(id_pago,id_reembolso,imagen,imagen_hash,id_actor)"
                    + " VALUES(?,?,?,?,?)",
                pago,
                reembolso,
                bytes,
                sha(bytes),
                a.usuarioId());
    }

    public Map<String, Object> reportar(long id, Comprobante b, AuthPrincipal a) {
        var autorizado = pago(id, a, false);
        cliente(contrato(number(autorizado, "id_contrato"), a, false), a);
        byte[] bytes = imagen.normalizar(b.imagenBase64());
        byte[] fingerprint =
                hash(
                        List.of(
                                b.version(),
                                b.operacion(),
                                b.fecha(),
                                b.importeCentimos(),
                                b.titularOrigen(),
                                Base64.getEncoder().encodeToString(sha(bytes))));
        return write(
                () -> {
                    var initial = pago(id, a, false);
                    var c = bloquearContrato(number(initial, "id_contrato"), a);
                    cliente(c, a);
                    var p = pago(id, a, true);
                    var d = db.one("SELECT * FROM pago_directo WHERE id_pago=? FOR UPDATE", id);
                    if (d.get("huella_reporte") != null) {
                        same(d.get("huella_reporte"), fingerprint);
                        return detalle(id, a);
                    }
                    version(p, b.version());
                    if (!"PENDIENTE".equals(p.get("estado")))
                        throw ApiException.conflict("Este registro ya no admite comprobantes");
                    fecha(b.fecha());
                    db.update(
                            "UPDATE pago_directo SET"
                                + " operacion_reportada=?,fecha_reportada=?,importe_reportado_centimos=?,titular_origen=?,reportado_en=UTC_TIMESTAMP(6),huella_reporte=?"
                                + " WHERE id_pago=?",
                            operacion(b.operacion()),
                            b.fecha(),
                            b.importeCentimos(),
                            b.titularOrigen(),
                            fingerprint,
                            id);
                    guardarImagen(id, null, bytes, a);
                    db.update(
                            "UPDATE pago SET estado='PROCESANDO',version=version+1 WHERE id=?", id);
                    evento(
                            c,
                            a,
                            "Pago",
                            id,
                            "COMPROBANTE_REPORTADO",
                            "Comprobante recibido; falta contrastar el ingreso en la cuenta"
                                + " receptora");
                    return detalle(id, a);
                });
    }

    public Map<String, Object> cancelar(long id, Cancelar b, AuthPrincipal a) {
        return write(
                () -> {
                    var initial = pago(id, a, false);
                    var c = bloquearContrato(number(initial, "id_contrato"), a);
                    cliente(c, a);
                    var p = pago(id, a, true);
                    if ("CANCELADO".equals(p.get("estado"))) return detalle(id, a);
                    version(p, b.version());
                    if (!"PENDIENTE".equals(p.get("estado")))
                        throw ApiException.conflict(
                                "Un comprobante reportado debe ser revisado por administración");
                    db.update(
                            "UPDATE pago SET estado='CANCELADO',version=version+1 WHERE id=?", id);
                    evento(
                            c,
                            a,
                            "Pago",
                            id,
                            "INTENTO_CANCELADO",
                            "Registro de pago cancelado; no revierte movimientos bancarios");
                    return detalle(id, a);
                });
    }

    public Map<String, Object> comprobante(long id, boolean reembolso, AuthPrincipal a) {
        long pago = id;
        if (reembolso)
            pago = number(db.one("SELECT id_pago FROM reembolso WHERE id=?", id), "id_pago");
        pago(pago, a, false);
        var proof =
                db.one(
                        "SELECT imagen,tipo FROM comprobante_directo WHERE "
                                + (reembolso ? "id_reembolso" : "id_pago")
                                + "=?",
                        id);
        return Map.of(
                "imagenBase64",
                Base64.getEncoder().encodeToString((byte[]) proof.get("imagen")),
                "tipo",
                proof.get("tipo"));
    }

    private long movimiento(
            long cuenta,
            Long pago,
            Long reembolso,
            String op,
            LocalDate date,
            long importe,
            AuthPrincipal a) {
        fecha(date);
        return db.insert(
                "INSERT INTO"
                    + " movimiento_cuenta(id_cuenta,id_pago,id_reembolso,operacion,fecha,importe_centimos,id_verificador)"
                    + " VALUES(?,?,?,?,?,?,?)",
                cuenta,
                pago,
                reembolso,
                operacion(op),
                date,
                importe,
                a.usuarioId());
    }

    private void incidencia(long id, long c, String tipo, String detalle) {
        db.update(
                "INSERT INTO incidencia_pago(id_pago,id_contrato,tipo,detalle) VALUES(?,?,?,?)",
                id,
                c,
                tipo,
                detalle);
    }

    public Map<String, Object> revisar(long id, Revision b, AuthPrincipal a) {
        soloAdmin(a);
        return write(
                () -> {
                    var initial = pago(id, a, false);
                    var c = bloquearContrato(number(initial, "id_contrato"), a);
                    var p = pago(id, a, true);
                    var d = db.one("SELECT * FROM pago_directo WHERE id_pago=? FOR UPDATE", id);
                    byte[] fingerprint = hash(b);
                    if (d.get("huella_revision") != null) {
                        same(d.get("huella_revision"), fingerprint);
                        return detalle(id, a);
                    }
                    version(p, b.version());
                    if (!"PROCESANDO".equals(p.get("estado")))
                        throw ApiException.conflict(
                                "Solo se revisan comprobantes enviados pendientes de conciliación");
                    if ("RECHAZAR".equals(b.accion())) {
                        motivo(b.motivo());
                        db.update(
                                "UPDATE pago_directo SET"
                                    + " motivo_revision=?,id_revisor=?,huella_revision=? WHERE"
                                    + " id_pago=?",
                                b.motivo(),
                                a.usuarioId(),
                                fingerprint,
                                id);
                        db.update(
                                "UPDATE pago SET estado='RECHAZADO',version=version+1 WHERE id=?",
                                id);
                        evento(c, a, "Pago", id, "COMPROBANTE_RECHAZADO", b.motivo());
                        return detalle(id, a);
                    }
                    if (!"VERIFICAR".equals(b.accion())
                            || !Boolean.TRUE.equals(b.contrasteEnCuenta())
                            || b.importeCentimos() == null
                            || b.importeCentimos() <= 0)
                        throw ApiException.invalid(
                                "Confirma que contrastaste el ingreso real en la cuenta receptora");
                    fecha(b.fecha());
                    if (b.fecha()
                            .isBefore(instant(c.get("aceptado_en")).atZone(PERU).toLocalDate()))
                        throw ApiException.invalid(
                                "El ingreso debe corresponder a este contrato y ser posterior a su"
                                    + " aceptación");
                    long mov =
                            movimiento(
                                    number(d, "id_cuenta"),
                                    id,
                                    null,
                                    b.operacion(),
                                    b.fecha(),
                                    b.importeCentimos(),
                                    a);
                    db.update(
                            "UPDATE pago_directo SET"
                                + " motivo_revision=?,id_revisor=?,huella_revision=? WHERE"
                                + " id_pago=?",
                            b.motivo(),
                            a.usuarioId(),
                            fingerprint,
                            id);
                    db.update(
                            "UPDATE pago SET"
                                + " referencia_externa=?,estado_externo='CONCILIADO_MANUALMENTE',verificado_en=UTC_TIMESTAMP(6),cobrado_en=UTC_TIMESTAMP(6),version=version+1"
                                + " WHERE id=?",
                            "DIRECTO-" + mov,
                            id);
                    String conflict = null;
                    if (b.importeCentimos() != number(p, "importe_centimos"))
                        conflict = "IMPORTE_DIFERENTE";
                    else if (Set.of("CANCELADO").contains(c.get("estado")))
                        conflict = "CONTRATO_CANCELADO";
                    else if (number(saldo(number(c, "id")), "saldo_centimos") < b.importeCentimos())
                        conflict = "SALDO_INSUFICIENTE";
                    else if ("PENDIENTE_PAGO".equals(c.get("estado"))) {
                        if (!instant(c.get("inicio_evento_utc")).isAfter(clock.instant()))
                            conflict = "INGRESO_TARDIO";
                        else
                            try {
                                db.libre(
                                        recursos(c),
                                        instant(c.get("inicio_ocupacion_utc")),
                                        instant(c.get("fin_ocupacion_utc")));
                            } catch (ApiException e) {
                                if (e.getStatus().value() != 409) throw e;
                                conflict = "CONFLICTO_AGENDA";
                            }
                    } else if (!Set.of("CONFIRMADO", "FINALIZADO").contains(c.get("estado")))
                        conflict = "ESTADO_CONTRACTUAL";
                    else if (db.rows(
                                    "SELECT id FROM reserva WHERE id_contrato=? AND"
                                        + " estado='CONFIRMADA' FOR UPDATE",
                                    c.get("id"))
                            .isEmpty()) conflict = "RESERVA_INCONSISTENTE";
                    if (conflict != null) {
                        db.update("UPDATE pago SET estado='EN_REVISION' WHERE id=?", id);
                        incidencia(
                                id,
                                number(c, "id"),
                                conflict,
                                "Ingreso contrastado en cuenta, no aplicado al contrato. Requiere"
                                    + " regularización o devolución documentada.");
                        evento(
                                c,
                                a,
                                "Pago",
                                id,
                                "INGRESO_CON_INCIDENCIA",
                                "Ingreso verificado con incidencia; no confirma una reserva");
                        return detalle(id, a);
                    }
                    db.update(
                            "UPDATE pago SET estado='APROBADO',aplicado_en=UTC_TIMESTAMP(6) WHERE"
                                + " id=?",
                            id);
                    if ("PENDIENTE_PAGO".equals(c.get("estado"))) {
                        long reserva =
                                db.insert(
                                        "INSERT INTO reserva(id_contrato,id_servicio) VALUES(?,?)",
                                        c.get("id"),
                                        c.get("id_servicio"));
                        for (long resource : recursos(c))
                            db.update(
                                    "INSERT INTO"
                                        + " ocupacion_agenda(id_recurso,id_reserva,inicio_utc,fin_utc)"
                                        + " VALUES(?,?,?,?)",
                                    resource,
                                    reserva,
                                    c.get("inicio_ocupacion_utc"),
                                    c.get("fin_ocupacion_utc"));
                        db.update(
                                "UPDATE contrato SET"
                                    + " estado='CONFIRMADO',confirmado_en=UTC_TIMESTAMP(6),version=version+1"
                                    + " WHERE id=?",
                                c.get("id"));
                        db.update(
                                "UPDATE solicitud_cotizacion SET"
                                    + " estado='RESERVADA',version=version+1 WHERE id=?",
                                c.get("id_solicitud"));
                    }
                    evento(
                            c,
                            a,
                            "Pago",
                            id,
                            "ABONO_APLICADO",
                            "Ingreso conciliado y aplicado al contrato");
                    return detalle(id, a);
                });
    }

    public Map<String, Object> devolver(long id, Devolver b, AuthPrincipal a) {
        soloAdmin(a);
        return write(
                () -> {
                    var initial = pago(id, a, false);
                    var c = bloquearContrato(number(initial, "id_contrato"), a);
                    var p = pago(id, a, true);
                    var previous =
                            db.rows(
                                    "SELECT id,huella_solicitud FROM reembolso WHERE id_pago=? AND"
                                        + " clave_idempotencia=? FOR UPDATE",
                                    id,
                                    b.clave());
                    if (!previous.isEmpty()) {
                        same(previous.getFirst().get("huella_solicitud"), hash(b));
                        return detalle(id, a);
                    }
                    var m =
                            db.rows(
                                    "SELECT importe_centimos FROM movimiento_cuenta WHERE"
                                        + " id_pago=?",
                                    id);
                    if (m.isEmpty())
                        throw ApiException.conflict(
                                "Solo se devuelve un ingreso contrastado en cuenta");
                    if (p.get("aplicado_en") != null && !"CANCELADO".equals(c.get("estado")))
                        throw ApiException.conflict(
                                "Antes de devolver un abono aplicado, cancela el contrato con el"
                                    + " motivo correspondiente");
                    long comprometido =
                            number(
                                    db.one(
                                            "SELECT COALESCE(SUM(importe_centimos),0) AS n FROM"
                                                + " reembolso WHERE id_pago=? AND estado IN"
                                                + " ('PENDIENTE','PROCESANDO','APROBADO')",
                                            id),
                                    "n");
                    if (b.importeCentimos()
                            > number(m.getFirst(), "importe_centimos") - comprometido)
                        throw ApiException.conflict(
                                "El importe supera lo disponible para devolver");
                    long refund =
                            db.insert(
                                    "INSERT INTO"
                                        + " reembolso(id_pago,proveedor,id_contrato,clave_idempotencia,importe_centimos,motivo,huella_solicitud,id_solicitante)"
                                        + " VALUES(?,'DIRECTO',?,?,?,?,?,?)",
                                    id,
                                    c.get("id"),
                                    b.clave(),
                                    b.importeCentimos(),
                                    b.motivo(),
                                    hash(b),
                                    a.usuarioId());
                    db.update(
                            "UPDATE pago SET estado='REEMBOLSO_PENDIENTE',version=version+1 WHERE"
                                + " id=?",
                            id);
                    evento(
                            c,
                            a,
                            "Pago",
                            id,
                            "DEVOLUCION_SOLICITADA",
                            "Devolución pendiente de transferencia y comprobación");
                    return detalle(id, a);
                });
    }

    public Map<String, Object> confirmarDevolucion(
            long id, ConfirmarDevolucion b, AuthPrincipal a) {
        soloAdmin(a);
        byte[] bytes = imagen.normalizar(b.imagenBase64());
        byte[] fingerprint =
                hash(
                        List.of(
                                b.version(),
                                b.operacion(),
                                b.fecha(),
                                b.importeCentimos(),
                                b.contrasteEnCuenta(),
                                Base64.getEncoder().encodeToString(sha(bytes))));
        return write(
                () -> {
                    var initial =
                            db.one(
                                    "SELECT * FROM reembolso WHERE id=? AND proveedor='DIRECTO'",
                                    id);
                    var c = bloquearContrato(number(initial, "id_contrato"), a);
                    var p = pago(number(initial, "id_pago"), a, true);
                    var r = db.one("SELECT * FROM reembolso WHERE id=? FOR UPDATE", id);
                    if (r.get("huella_confirmacion") != null) {
                        same(r.get("huella_confirmacion"), fingerprint);
                        return detalle(number(p, "id"), a);
                    }
                    version(r, b.version());
                    if (!"PENDIENTE".equals(r.get("estado")))
                        throw ApiException.conflict("La devolución ya fue resuelta");
                    if (!b.contrasteEnCuenta()
                            || b.importeCentimos() != number(r, "importe_centimos"))
                        throw ApiException.invalid(
                                "La devolución realizada debe coincidir con el importe solicitado");
                    var original =
                            db.one(
                                    "SELECT fecha FROM movimiento_cuenta WHERE id_pago=?",
                                    p.get("id"));
                    if (b.fecha().isBefore(LocalDate.parse(original.get("fecha").toString())))
                        throw ApiException.invalid(
                                "La devolución no puede ser anterior al ingreso original");
                    var d =
                            db.one(
                                    "SELECT id_cuenta FROM pago_directo WHERE id_pago=?",
                                    p.get("id"));
                    long mov =
                            movimiento(
                                    number(d, "id_cuenta"),
                                    null,
                                    id,
                                    b.operacion(),
                                    b.fecha(),
                                    b.importeCentimos(),
                                    a);
                    guardarImagen(null, id, bytes, a);
                    db.update(
                            "UPDATE reembolso SET"
                                + " estado='APROBADO',referencia_externa=?,confirmado_en=UTC_TIMESTAMP(6),huella_confirmacion=?,id_verificador=?,version=version+1"
                                + " WHERE id=?",
                            "DIRECTO-" + mov,
                            fingerprint,
                            a.usuarioId(),
                            id);
                    long refunded =
                            number(
                                    db.one(
                                            "SELECT COALESCE(SUM(importe_centimos),0) AS n FROM"
                                                + " reembolso WHERE id_pago=? AND"
                                                + " estado='APROBADO'",
                                            p.get("id")),
                                    "n");
                    long received =
                            number(
                                    db.one(
                                            "SELECT importe_centimos FROM movimiento_cuenta WHERE"
                                                + " id_pago=?",
                                            p.get("id")),
                                    "importe_centimos");
                    boolean pending =
                            !db.rows(
                                            "SELECT id FROM reembolso WHERE id_pago=? AND"
                                                + " estado='PENDIENTE'",
                                            p.get("id"))
                                    .isEmpty();
                    String state =
                            refunded >= received
                                    ? "REEMBOLSADO"
                                    : pending
                                            ? "REEMBOLSO_PENDIENTE"
                                            : p.get("aplicado_en") == null
                                                    ? "EN_REVISION"
                                                    : "APROBADO";
                    db.update(
                            "UPDATE pago SET estado=?,version=version+1 WHERE id=?",
                            state,
                            p.get("id"));
                    if (refunded >= received)
                        db.update(
                                "UPDATE incidencia_pago SET"
                                    + " estado='RESUELTA',resuelta_en=UTC_TIMESTAMP(6) WHERE"
                                    + " id_pago=? AND estado<>'RESUELTA'",
                                p.get("id"));
                    evento(
                            c,
                            a,
                            "Pago",
                            number(p, "id"),
                            "DEVOLUCION_CONFIRMADA",
                            "Devolución realizada y contrastada por administración");
                    return detalle(number(p, "id"), a);
                });
    }

    public Map<String, Object> detalleReembolso(long id, AuthPrincipal a) {
        var r =
                db.one(
                        "SELECT id,id_pago,importe_centimos,motivo,estado,version FROM reembolso"
                            + " WHERE id=? AND proveedor='DIRECTO'",
                        id);
        r.put("pago", detalle(number(r, "id_pago"), a));
        return salida(r);
    }

    public Map<String, Object> rechazarDevolucion(long id, Revision b, AuthPrincipal a) {
        soloAdmin(a);
        motivo(b.motivo());
        return write(
                () -> {
                    var initial =
                            db.one(
                                    "SELECT * FROM reembolso WHERE id=? AND proveedor='DIRECTO'",
                                    id);
                    var c = bloquearContrato(number(initial, "id_contrato"), a);
                    var p = pago(number(initial, "id_pago"), a, true);
                    var r = db.one("SELECT * FROM reembolso WHERE id=? FOR UPDATE", id);
                    if ("RECHAZADO".equals(r.get("estado"))) return detalle(number(p, "id"), a);
                    version(r, b.version());
                    if (!"PENDIENTE".equals(r.get("estado")))
                        throw ApiException.conflict(
                                "Solo se anula una solicitud que todavía no fue confirmada");
                    db.update(
                            "UPDATE reembolso SET estado='RECHAZADO',version=version+1 WHERE id=?",
                            id);
                    boolean pending =
                            !db.rows(
                                            "SELECT id FROM reembolso WHERE id_pago=? AND"
                                                + " estado='PENDIENTE'",
                                            p.get("id"))
                                    .isEmpty();
                    db.update(
                            "UPDATE pago SET estado=?,version=version+1 WHERE id=?",
                            pending
                                    ? "REEMBOLSO_PENDIENTE"
                                    : p.get("aplicado_en") == null ? "EN_REVISION" : "APROBADO",
                            p.get("id"));
                    evento(
                            c,
                            a,
                            "Pago",
                            number(p, "id"),
                            "SOLICITUD_DEVOLUCION_ANULADA",
                            b.motivo());
                    return detalle(number(p, "id"), a);
                });
    }
}
