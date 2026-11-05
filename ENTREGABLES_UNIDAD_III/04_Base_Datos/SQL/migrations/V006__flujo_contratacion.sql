-- Claves estables para reintentos y control independiente de la configuración de agenda.
ALTER TABLE solicitud_cotizacion ADD huella_creacion BINARY(32) NULL;
ALTER TABLE cotizacion ADD clave_envio VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD huella_envio BINARY(32) NULL,
    ADD CONSTRAINT uq_cotizacion_envio UNIQUE(id_solicitud,clave_envio);
ALTER TABLE servicio_musical ADD agenda_version BIGINT NOT NULL DEFAULT 0;
INSERT INTO recurso_agenda(id_servicio)
SELECT s.id FROM servicio_musical s LEFT JOIN recurso_agenda r ON r.id_servicio=s.id WHERE r.id IS NULL;
INSERT INTO recurso_agenda(id_musico)
SELECT DISTINCT s.id_responsable FROM servicio_musical s LEFT JOIN recurso_agenda r ON r.id_musico=s.id_responsable WHERE r.id IS NULL;
INSERT INTO recurso_agenda(id_musico)
SELECT DISTINCT i.id_solista_vinculado FROM integrante i LEFT JOIN recurso_agenda r ON r.id_musico=i.id_solista_vinculado
WHERE i.activo=1 AND i.estado_vinculo='ACEPTADO' AND r.id IS NULL;
