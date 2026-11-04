package com.santacecilia.api.service;

import com.santacecilia.api.repository.ContratacionRepository;

import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Entrega interna durable: aviso y confirmación de outbox se comprometen juntos. No envía mensajes
 * externos.
 */
@Service
@EnableScheduling
public class PagoAvisosService {
    private final ContratacionRepository db;
    private final ObjectMapper json;

    public PagoAvisosService(ContratacionRepository db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    @Transactional
    public void entregar() {
        for (var event :
                db.rows(
                        "SELECT id,payload FROM evento_outbox WHERE tipo='AVISO_INTERNO' AND"
                            + " estado='PENDIENTE' ORDER BY id LIMIT 20 FOR UPDATE SKIP LOCKED")) {
            Map<?, ?> data = json.readValue(event.get("payload").toString(), Map.class);
            Set<Long> users = new HashSet<>();
            users.add(((Number) data.get("cliente")).longValue());
            users.add(((Number) data.get("proveedor")).longValue());
            if (Boolean.TRUE.equals(data.get("avisarAdmin")))
                for (var user :
                        db.rows(
                                "SELECT u.id FROM usuario u JOIN rol r ON r.id=u.id_rol WHERE"
                                    + " r.codigo='ADMINISTRADOR' AND u.estado='ACTIVO'"))
                    users.add(((Number) user.get("id")).longValue());
            for (long user : users)
                db.update(
                        "INSERT INTO"
                            + " notificacion(id_destinatario,tipo,titulo,mensaje,entidad,identificador_entidad)"
                            + " VALUES(?,'PAGO_DIRECTO',?,'Consulta el detalle del pago para"
                            + " revisar su estado',?,?)",
                        user,
                        data.get("titulo"),
                        data.get("entidad"),
                        data.get("id"));
            db.update(
                    "UPDATE evento_outbox SET"
                        + " estado='ENVIADO',enviado_en=UTC_TIMESTAMP(6),intentos=intentos+1 WHERE"
                        + " id=?",
                    event.get("id"));
        }
    }
}
