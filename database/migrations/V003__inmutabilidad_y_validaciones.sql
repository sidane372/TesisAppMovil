-- Inmutabilidad de versiones enviadas y del snapshot aceptado.
-- DELIMITER es una directiva del cliente mysql; no es SQL enviado al servidor.
DELIMITER $$
CREATE TRIGGER tr_cotizacion_actualizar BEFORE UPDATE ON cotizacion FOR EACH ROW
BEGIN
    IF OLD.estado <> 'BORRADOR' AND (
        NEW.estado='BORRADOR' OR NEW.id_solicitud<>OLD.id_solicitud OR NEW.numero_version<>OLD.numero_version
        OR NEW.inicio_propuesto_utc<>OLD.inicio_propuesto_utc OR NEW.fin_propuesto_utc<>OLD.fin_propuesto_utc
        OR NEW.subtotal_centimos<>OLD.subtotal_centimos OR NEW.descuento_centimos<>OLD.descuento_centimos
        OR NOT(NEW.condiciones <=> OLD.condiciones) OR NOT(NEW.politica_cancelacion <=> OLD.politica_cancelacion)
        OR NEW.politica_version<>OLD.politica_version OR NEW.vigente_hasta_utc<>OLD.vigente_hasta_utc
        OR NEW.creada_en<>OLD.creada_en) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='COTIZACION_ENVIADA_INMUTABLE';
    END IF;
    IF OLD.estado='BORRADOR' AND NEW.estado='ENVIADA' THEN
        IF (SELECT COUNT(*) FROM detalle_cotizacion WHERE id_cotizacion=OLD.id)=0
           OR NEW.subtotal_centimos<>(SELECT COALESCE(SUM(total_centimos),0) FROM detalle_cotizacion WHERE id_cotizacion=OLD.id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DESGLOSE_NO_COINCIDE_CON_SUBTOTAL';
        END IF;
    END IF;
END$$
CREATE TRIGGER tr_cotizacion_eliminar BEFORE DELETE ON cotizacion FOR EACH ROW
BEGIN
    IF OLD.estado<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='COTIZACION_ENVIADA_NO_SE_ELIMINA';
    END IF;
END$$
CREATE TRIGGER tr_detalle_insertar BEFORE INSERT ON detalle_cotizacion FOR EACH ROW
BEGIN
    DECLARE estado_padre VARCHAR(16);
    SELECT estado INTO estado_padre FROM cotizacion WHERE id=NEW.id_cotizacion FOR UPDATE;
    IF estado_padre IS NOT NULL AND estado_padre<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DETALLE_DE_VERSION_ENVIADA_INMUTABLE';
    END IF;
END$$
CREATE TRIGGER tr_detalle_actualizar BEFORE UPDATE ON detalle_cotizacion FOR EACH ROW
BEGIN
    DECLARE estado_padre VARCHAR(16);
    SELECT estado INTO estado_padre FROM cotizacion WHERE id=OLD.id_cotizacion FOR UPDATE;
    IF NEW.id_cotizacion<>OLD.id_cotizacion OR estado_padre<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DETALLE_DE_VERSION_ENVIADA_INMUTABLE';
    END IF;
END$$
CREATE TRIGGER tr_detalle_eliminar BEFORE DELETE ON detalle_cotizacion FOR EACH ROW
BEGIN
    DECLARE estado_padre VARCHAR(16);
    SELECT estado INTO estado_padre FROM cotizacion WHERE id=OLD.id_cotizacion FOR UPDATE;
    IF estado_padre<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DETALLE_DE_VERSION_ENVIADA_INMUTABLE';
    END IF;
END$$
CREATE TRIGGER tr_contrato_insertar BEFORE INSERT ON contrato FOR EACH ROW
BEGIN
    DECLARE estado_propuesta VARCHAR(16);
    DECLARE total_propuesta BIGINT;
    DECLARE inicio_propuesta DATETIME(6);
    DECLARE fin_propuesta DATETIME(6);
    DECLARE vencimiento DATETIME(6);
    DECLARE politica VARCHAR(80);
    SELECT estado,total_centimos,inicio_propuesto_utc,fin_propuesto_utc,vigente_hasta_utc,politica_version
      INTO estado_propuesta,total_propuesta,inicio_propuesta,fin_propuesta,vencimiento,politica
      FROM cotizacion WHERE id=NEW.id_cotizacion FOR UPDATE;
    IF estado_propuesta IS NOT NULL AND (estado_propuesta<>'ENVIADA' OR vencimiento<=UTC_TIMESTAMP(6)
        OR NEW.total_centimos<>total_propuesta OR NEW.inicio_evento_utc<>inicio_propuesta
        OR NEW.fin_evento_utc<>fin_propuesta OR NEW.politica_version<>politica) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ACEPTACION_NO_COINCIDE_CON_PROPUESTA_VIGENTE';
    END IF;
END$$
CREATE TRIGGER tr_contrato_actualizar BEFORE UPDATE ON contrato FOR EACH ROW
BEGIN
    IF NEW.codigo<>OLD.codigo OR NEW.id_cotizacion<>OLD.id_cotizacion OR NEW.id_solicitud<>OLD.id_solicitud
       OR NEW.id_servicio<>OLD.id_servicio OR NEW.id_cliente<>OLD.id_cliente OR NEW.id_proveedor<>OLD.id_proveedor
       OR NEW.inicio_evento_utc<>OLD.inicio_evento_utc OR NEW.fin_evento_utc<>OLD.fin_evento_utc
       OR NEW.inicio_ocupacion_utc<>OLD.inicio_ocupacion_utc OR NEW.fin_ocupacion_utc<>OLD.fin_ocupacion_utc
       OR NEW.total_centimos<>OLD.total_centimos OR NEW.condiciones_aceptadas<>OLD.condiciones_aceptadas
       OR NEW.politica_version<>OLD.politica_version OR NOT(NEW.snapshot <=> OLD.snapshot)
       OR NEW.clave_idempotencia<>OLD.clave_idempotencia OR NEW.huella_aceptacion<>OLD.huella_aceptacion
       OR NEW.aceptado_en<>OLD.aceptado_en THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='SNAPSHOT_CONTRACTUAL_INMUTABLE';
    END IF;
    IF NEW.estado='FINALIZADO' AND UTC_TIMESTAMP(6)<OLD.fin_evento_utc THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='SERVICIO_AUN_NO_FINALIZA';
    END IF;
END$$
CREATE TRIGGER tr_contrato_eliminar BEFORE DELETE ON contrato FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CONTRATO_HISTORICO_NO_SE_ELIMINA';
END$$
CREATE TRIGGER tr_valoracion_insertar BEFORE INSERT ON valoracion FOR EACH ROW
BEGIN
    DECLARE estado_contrato VARCHAR(20);
    SELECT estado INTO estado_contrato FROM contrato WHERE id=NEW.id_contrato FOR UPDATE;
    IF estado_contrato IS NOT NULL AND estado_contrato<>'FINALIZADO' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VALORACION_REQUIERE_CONTRATO_FINALIZADO';
    END IF;
END$$
DELIMITER ;
