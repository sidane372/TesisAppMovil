-- SANTA CECILIA - TABLAS MYSQL DEL ESQUEMA ACTUAL
-- MySQL 8.0.30 o superior. Estructura V001-V007.
-- 36 tablas de negocio, relaciones PK/FK, CHECK, indices, triggers y vista de saldos.
-- Incluye unicamente los cuatro roles de referencia; no copia usuarios, sesiones,
-- contratos, pagos, comprobantes, contrasenas ni credenciales de MySQL.
-- Importar como base NUEVA de consulta/entrega, no encima de la base de la app.
-- Este archivo no instala el historial Flyway. El backend habitual utiliza
-- INICIAR_SANTA_CECILIA.cmd y sus migraciones, no esta copia de consulta.

CREATE DATABASE `santa_cecilia_tablas`
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `santa_cecilia_tablas`;
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+00:00';


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_familia` (
  `id` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `id_usuario` bigint NOT NULL,
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `revocada_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_auth_familia_usuario` (`id`,`id_usuario`),
  KEY `ix_auth_familia_usuario` (`id_usuario`,`revocada_en`),
  CONSTRAINT `fk_auth_familia_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Revocacion y serializacion de una familia de sesiones';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bloqueo_agenda` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador del bloqueo manual',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio que bloquea',
  `id_actor` bigint NOT NULL COMMENT 'Actor autorizado en backend',
  `inicio_utc` datetime(6) NOT NULL COMMENT 'Inicio ocupado UTC',
  `fin_utc` datetime(6) NOT NULL COMMENT 'Fin exclusivo UTC',
  `motivo` varchar(1000) NOT NULL COMMENT 'Motivo privado nunca publicado',
  `activo` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Retiro logico de bloqueo',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
  `retirado_en` datetime(6) DEFAULT NULL COMMENT 'Retiro UTC',
  PRIMARY KEY (`id`),
  KEY `fk_bloqueo_actor` (`id_actor`),
  KEY `ix_bloqueo_servicio` (`id_servicio`,`activo`,`inicio_utc`,`fin_utc`),
  CONSTRAINT `fk_bloqueo_actor` FOREIGN KEY (`id_actor`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_bloqueo_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_bloqueo_activo` CHECK ((`activo` in (0,1))),
  CONSTRAINT `ck_bloqueo_intervalo` CHECK ((`inicio_utc` < `fin_utc`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Bloqueo manual requiere protocolo comun de recursos';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comprobante_directo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `id_pago` bigint DEFAULT NULL,
  `id_reembolso` bigint DEFAULT NULL,
  `imagen` mediumblob NOT NULL,
  `imagen_hash` binary(32) NOT NULL,
  `tipo` varchar(30) NOT NULL DEFAULT 'image/jpeg',
  `id_actor` bigint NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_comprobante_pago` (`id_pago`),
  UNIQUE KEY `uq_comprobante_reembolso` (`id_reembolso`),
  KEY `fk_comprobante_actor` (`id_actor`),
  CONSTRAINT `fk_comprobante_actor` FOREIGN KEY (`id_actor`) REFERENCES `usuario` (`id`),
  CONSTRAINT `fk_comprobante_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id`),
  CONSTRAINT `fk_comprobante_reembolso` FOREIGN KEY (`id_reembolso`) REFERENCES `reembolso` (`id`),
  CONSTRAINT `ck_comprobante_origen` CHECK ((((`id_pago` is not null) + (`id_reembolso` is not null)) = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contrato` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador oficial del contrato',
  `codigo` varchar(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Codigo visible unico',
  `id_cotizacion` bigint NOT NULL COMMENT 'Version exacta aceptada',
  `id_solicitud` bigint NOT NULL COMMENT 'Solicitud unica aun entre distintas versiones',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio historico coherente con solicitud',
  `id_cliente` bigint NOT NULL COMMENT 'Cliente historico coherente con solicitud',
  `id_proveedor` bigint NOT NULL COMMENT 'Proveedor historico coherente con solicitud',
  `inicio_evento_utc` datetime(6) NOT NULL COMMENT 'Inicio aceptado UTC',
  `fin_evento_utc` datetime(6) NOT NULL COMMENT 'Fin exclusivo aceptado UTC',
  `inicio_ocupacion_utc` datetime(6) NOT NULL COMMENT 'Inicio con margen de preparacion incluido',
  `fin_ocupacion_utc` datetime(6) NOT NULL COMMENT 'Fin con margen posterior incluido',
  `total_centimos` bigint NOT NULL COMMENT 'Total historico exacto PEN',
  `condiciones_aceptadas` tinyint(1) NOT NULL COMMENT 'Aceptacion expresa obligatoria',
  `politica_version` varchar(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Version exacta aceptada',
  `snapshot` json NOT NULL COMMENT 'Copia historica de perfil, actores, conceptos, recursos y politicas',
  `clave_idempotencia` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave de aceptacion por cliente',
  `huella_aceptacion` binary(32) NOT NULL COMMENT 'SHA256 de entrada canonica para detectar reutilizacion de clave',
  `estado` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE_PAGO' COMMENT 'Estado contractual independiente del proveedor financiero',
  `version` bigint NOT NULL DEFAULT '0' COMMENT 'Version optimista de estado',
  `aceptado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Aceptacion UTC',
  `confirmado_en` datetime(6) DEFAULT NULL COMMENT 'Primer abono verificado UTC',
  `finalizado_en` datetime(6) DEFAULT NULL COMMENT 'Finalizacion despues del evento UTC',
  `cancelado_en` datetime(6) DEFAULT NULL COMMENT 'Cancelacion UTC',
  `motivo_cancelacion` varchar(1000) DEFAULT NULL COMMENT 'Motivo conservado segun politica',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_contrato_codigo` (`codigo`),
  UNIQUE KEY `uq_contrato_cotizacion` (`id_cotizacion`),
  UNIQUE KEY `uq_contrato_solicitud` (`id_solicitud`),
  UNIQUE KEY `uq_contrato_aceptacion` (`id_cliente`,`clave_idempotencia`),
  UNIQUE KEY `uq_contrato_cliente` (`id`,`id_cliente`),
  UNIQUE KEY `uq_contrato_servicio` (`id`,`id_servicio`),
  KEY `fk_contrato_cotizacion` (`id_cotizacion`,`id_solicitud`),
  KEY `fk_contrato_contexto` (`id_solicitud`,`id_servicio`,`id_cliente`,`id_proveedor`),
  KEY `ix_contrato_cliente` (`id_cliente`,`estado`,`aceptado_en`),
  KEY `ix_contrato_proveedor` (`id_proveedor`,`estado`,`inicio_evento_utc`),
  KEY `ix_contrato_confirmacion` (`confirmado_en`,`estado`),
  KEY `ix_contrato_evento` (`inicio_evento_utc`,`id_servicio`),
  CONSTRAINT `fk_contrato_contexto` FOREIGN KEY (`id_solicitud`, `id_servicio`, `id_cliente`, `id_proveedor`) REFERENCES `solicitud_cotizacion` (`id`, `id_servicio`, `id_cliente`, `id_proveedor`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_contrato_cotizacion` FOREIGN KEY (`id_cotizacion`, `id_solicitud`) REFERENCES `cotizacion` (`id`, `id_solicitud`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_contrato_aceptacion` CHECK (((`condiciones_aceptadas` = 1) and (json_type(`snapshot`) = _utf8mb4'OBJECT'))),
  CONSTRAINT `ck_contrato_estado` CHECK ((`estado` in (_utf8mb4'PENDIENTE_PAGO',_utf8mb4'CONFIRMADO',_utf8mb4'FINALIZADO',_utf8mb4'CANCELADO'))),
  CONSTRAINT `ck_contrato_fechas` CHECK ((((`estado` not in (_utf8mb4'CONFIRMADO',_utf8mb4'FINALIZADO')) or (`confirmado_en` is not null)) and ((`estado` <> _utf8mb4'FINALIZADO') or ((`finalizado_en` is not null) and (`finalizado_en` >= `fin_evento_utc`))) and ((`estado` <> _utf8mb4'CANCELADO') or ((`cancelado_en` is not null) and (char_length(trim(coalesce(`motivo_cancelacion`,_utf8mb4''))) >= 5))))),
  CONSTRAINT `ck_contrato_intervalo` CHECK (((`inicio_evento_utc` < `fin_evento_utc`) and (`inicio_ocupacion_utc` <= `inicio_evento_utc`) and (`fin_ocupacion_utc` >= `fin_evento_utc`))),
  CONSTRAINT `ck_contrato_total` CHECK ((`total_centimos` between 1 and 999999999999))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Aceptacion inmutable; no reserva hasta pago verificado';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_contrato_insertar` BEFORE INSERT ON `contrato` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_contrato_actualizar` BEFORE UPDATE ON `contrato` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_contrato_eliminar` BEFORE DELETE ON `contrato` FOR EACH ROW BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='CONTRATO_HISTORICO_NO_SE_ELIMINA';
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cotizacion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de version',
  `id_solicitud` bigint NOT NULL COMMENT 'Solicitud que agrupa versiones',
  `numero_version` int NOT NULL COMMENT 'Consecutivo dentro de la solicitud',
  `inicio_propuesto_utc` datetime(6) NOT NULL COMMENT 'Inicio propuesto UTC',
  `fin_propuesto_utc` datetime(6) NOT NULL COMMENT 'Fin propuesto UTC',
  `subtotal_centimos` bigint NOT NULL COMMENT 'Suma exacta de conceptos; backend reconcilia',
  `descuento_centimos` bigint NOT NULL DEFAULT '0' COMMENT 'Descuento exacto no mayor que subtotal',
  `total_centimos` bigint GENERATED ALWAYS AS ((`subtotal_centimos` - `descuento_centimos`)) STORED COMMENT 'Total exacto en centimos PEN',
  `condiciones` text NOT NULL COMMENT 'Condiciones versionadas del servicio',
  `politica_cancelacion` text NOT NULL COMMENT 'Politica propuesta para aceptacion expresa',
  `politica_version` varchar(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador de politica aceptable',
  `vigente_hasta_utc` datetime(6) NOT NULL COMMENT 'Caducidad de la propuesta',
  `estado` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BORRADOR' COMMENT 'Estado de version, no edicion de propuesta enviada',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
  `clave_envio` varchar(100) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `huella_envio` binary(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cotizacion_version` (`id_solicitud`,`numero_version`),
  UNIQUE KEY `uq_cotizacion_solicitud` (`id`,`id_solicitud`),
  UNIQUE KEY `uq_cotizacion_envio` (`id_solicitud`,`clave_envio`),
  CONSTRAINT `fk_cotizacion_solicitud` FOREIGN KEY (`id_solicitud`) REFERENCES `solicitud_cotizacion` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_cotizacion_estado` CHECK ((`estado` in (_ascii'BORRADOR',_ascii'ENVIADA',_ascii'ACEPTADA',_ascii'RECHAZADA',_ascii'CAMBIOS',_ascii'VENCIDA',_ascii'SUSTITUIDA'))),
  CONSTRAINT `ck_cotizacion_importes` CHECK (((`subtotal_centimos` between 1 and 999999999999) and (`descuento_centimos` >= 0) and (`descuento_centimos` < `subtotal_centimos`))),
  CONSTRAINT `ck_cotizacion_intervalo` CHECK (((`inicio_propuesto_utc` < `fin_propuesto_utc`) and (`fin_propuesto_utc` <= (`inicio_propuesto_utc` + interval 24 hour)))),
  CONSTRAINT `ck_cotizacion_version` CHECK ((`numero_version` > 0)),
  CONSTRAINT `ck_cotizacion_vigencia` CHECK ((`vigente_hasta_utc` > `creada_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Versiones historicas de propuesta economica';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_cotizacion_actualizar` BEFORE UPDATE ON `cotizacion` FOR EACH ROW BEGIN
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
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_cotizacion_eliminar` BEFORE DELETE ON `cotizacion` FOR EACH ROW BEGIN
    IF OLD.estado<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='COTIZACION_ENVIADA_NO_SE_ELIMINA';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cuenta_cobro` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `titular` varchar(180) NOT NULL,
  `banco` varchar(100) NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `numero_cuenta` varchar(40) DEFAULT NULL,
  `cci` varchar(20) DEFAULT NULL,
  `yape` tinyint(1) NOT NULL DEFAULT '0',
  `plin` tinyint(1) NOT NULL DEFAULT '0',
  `transferencia` tinyint(1) NOT NULL DEFAULT '0',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint NOT NULL DEFAULT '0',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cobro_cuenta` (`banco`,`numero_cuenta`),
  UNIQUE KEY `uq_cobro_cci` (`cci`),
  UNIQUE KEY `uq_cobro_telefono` (`banco`,`telefono`),
  CONSTRAINT `ck_cobro_destino` CHECK (((((`yape` = 0) and (`plin` = 0)) or (`telefono` is not null)) and ((`transferencia` = 0) or (`numero_cuenta` is not null) or (`cci` is not null)))),
  CONSTRAINT `ck_cobro_medios` CHECK (((`yape` = 1) or (`plin` = 1) or (`transferencia` = 1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `detalle_cotizacion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador del concepto',
  `id_cotizacion` bigint NOT NULL COMMENT 'Version propietaria',
  `concepto` varchar(300) NOT NULL COMMENT 'Descripcion del concepto economico',
  `cantidad` decimal(10,3) NOT NULL COMMENT 'Cantidad exacta, hasta tres decimales',
  `precio_unitario_centimos` bigint NOT NULL COMMENT 'Precio exacto unitario en centimos PEN',
  `total_centimos` bigint GENERATED ALWAYS AS (cast(round((`cantidad` * `precio_unitario_centimos`),0) as signed)) STORED COMMENT 'Redondeo decimal por concepto: mitad hacia arriba',
  `orden` int NOT NULL DEFAULT '0' COMMENT 'Orden del desglose',
  PRIMARY KEY (`id`),
  KEY `ix_detalle_cotizacion` (`id_cotizacion`,`orden`),
  CONSTRAINT `fk_detalle_cotizacion` FOREIGN KEY (`id_cotizacion`) REFERENCES `cotizacion` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_detalle_cantidad` CHECK (((`cantidad` > 0) and (`cantidad` <= 1000000))),
  CONSTRAINT `ck_detalle_orden` CHECK ((`orden` >= 0)),
  CONSTRAINT `ck_detalle_precio` CHECK ((`precio_unitario_centimos` between 0 and 999999999999)),
  CONSTRAINT `ck_detalle_total` CHECK ((`total_centimos` between 0 and 999999999999))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Desglose exacto sin float o double';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_detalle_insertar` BEFORE INSERT ON `detalle_cotizacion` FOR EACH ROW BEGIN
    DECLARE estado_padre VARCHAR(16);
    SELECT estado INTO estado_padre FROM cotizacion WHERE id=NEW.id_cotizacion FOR UPDATE;
    IF estado_padre IS NOT NULL AND estado_padre<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DETALLE_DE_VERSION_ENVIADA_INMUTABLE';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_detalle_actualizar` BEFORE UPDATE ON `detalle_cotizacion` FOR EACH ROW BEGIN
    DECLARE estado_padre VARCHAR(16);
    SELECT estado INTO estado_padre FROM cotizacion WHERE id=OLD.id_cotizacion FOR UPDATE;
    IF NEW.id_cotizacion<>OLD.id_cotizacion OR estado_padre<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DETALLE_DE_VERSION_ENVIADA_INMUTABLE';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_detalle_eliminar` BEFORE DELETE ON `detalle_cotizacion` FOR EACH ROW BEGIN
    DECLARE estado_padre VARCHAR(16);
    SELECT estado INTO estado_padre FROM cotizacion WHERE id=OLD.id_cotizacion FOR UPDATE;
    IF estado_padre<>'BORRADOR' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='DETALLE_DE_VERSION_ENVIADA_INMUTABLE';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evento_medicion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de evento de medicion consentida',
  `sesion_medicion` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador pseudonimo de sesion; no cuenta personal',
  `clave_evento` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador idempotente de evento',
  `id_servicio` bigint DEFAULT NULL COMMENT 'Servicio publico consultado, cuando corresponda',
  `accion` varchar(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Inicio voluntario, consulta o fin',
  `consentimiento_version` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Version informada de consentimiento expreso',
  `ocurrido_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Hora del servidor para indicadores',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_medicion_evento` (`clave_evento`),
  KEY `fk_medicion_servicio` (`id_servicio`),
  KEY `ix_medicion_sesion` (`sesion_medicion`,`ocurrido_en`),
  CONSTRAINT `fk_medicion_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_medicion_accion` CHECK ((`accion` in (_utf8mb4'INICIO',_utf8mb4'PERFIL',_utf8mb4'FIN'))),
  CONSTRAINT `ck_medicion_perfil` CHECK (((`accion` <> _utf8mb4'PERFIL') or (`id_servicio` is not null)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Eventos minimos de busqueda; no encuesta ni direccion privada';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evento_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de evento transaccional de salida',
  `clave_evento` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave estable de entrega idempotente',
  `tipo` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Tipo de notificacion o integracion',
  `entidad` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Agregado de origen',
  `identificador_entidad` bigint NOT NULL COMMENT 'ID logico del agregado',
  `payload` json NOT NULL COMMENT 'Datos minimos redactados del evento',
  `estado` varchar(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Estado de entrega',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Commit del hecho de negocio UTC',
  `disponible_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Proximo intento UTC',
  `enviado_en` datetime(6) DEFAULT NULL COMMENT 'Confirmacion de entrega UTC',
  `intentos` int NOT NULL DEFAULT '0' COMMENT 'Reintentos de entrega',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_outbox_clave` (`clave_evento`),
  KEY `ix_outbox_pendiente` (`estado`,`disponible_en`,`id`),
  CONSTRAINT `ck_outbox_estado` CHECK ((`estado` in (_utf8mb4'PENDIENTE',_utf8mb4'PROCESANDO',_utf8mb4'ENVIADO',_utf8mb4'ERROR'))),
  CONSTRAINT `ck_outbox_intentos` CHECK ((`intentos` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Salida fiable: escritura con negocio y entrega posterior';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evento_pago` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de recepcion de webhook',
  `proveedor` varchar(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Proveedor autenticado',
  `id_evento_externo` varchar(150) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador deduplicado del evento oficial',
  `id_pago` bigint DEFAULT NULL COMMENT 'Intento correlacionado; puede llegar antes de correlacionar',
  `payload_hash` binary(32) NOT NULL COMMENT 'Huella del cuerpo recibido; no almacena tarjeta ni payload completo',
  `autenticado_en` datetime(6) NOT NULL COMMENT 'Validacion de firma oficial antes de persistir como evento confiable',
  `recibido_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Recepcion UTC',
  `procesado_en` datetime(6) DEFAULT NULL COMMENT 'Aplicacion o conciliacion UTC',
  `estado` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'RECIBIDO' COMMENT 'Estado de procesamiento interno',
  `intentos` int NOT NULL DEFAULT '0' COMMENT 'Numero de reintentos de verificacion',
  `ultimo_error_codigo` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL COMMENT 'Codigo tecnico sin secretos',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_evento_pago_externo` (`proveedor`,`id_evento_externo`),
  KEY `fk_evento_pago` (`id_pago`),
  KEY `ix_evento_pago_pendiente` (`estado`,`recibido_en`),
  CONSTRAINT `fk_evento_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_evento_pago_estado` CHECK ((`estado` in (_utf8mb4'RECIBIDO',_utf8mb4'VERIFICANDO',_utf8mb4'APLICADO',_utf8mb4'IGNORADO',_utf8mb4'EN_REVISION'))),
  CONSTRAINT `ck_evento_pago_intentos` CHECK ((`intentos` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Bandeja de eventos autenticados; API del proveedor verifica el cobro';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `historial_operacion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de evento auditable',
  `id_actor` bigint DEFAULT NULL COMMENT 'Usuario actor; NULL si sistema',
  `id_solicitud` bigint DEFAULT NULL COMMENT 'Origen opcional de contratacion',
  `entidad` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Tipo de agregado auditado',
  `identificador_entidad` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Referencia logica no es FK universal',
  `accion` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Accion realizada',
  `detalle` json DEFAULT NULL COMMENT 'Metadatos redactados sin claves ni datos financieros sensibles',
  `correlation_id` varchar(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Correlacion tecnica de la operacion',
  `ocurrido_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante real UTC del servidor',
  PRIMARY KEY (`id`),
  KEY `fk_historial_actor` (`id_actor`),
  KEY `ix_historial_fecha` (`ocurrido_en`,`entidad`),
  KEY `ix_historial_solicitud` (`id_solicitud`,`ocurrido_en`),
  CONSTRAINT `fk_historial_actor` FOREIGN KEY (`id_actor`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_historial_solicitud` FOREIGN KEY (`id_solicitud`) REFERENCES `solicitud_cotizacion` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Auditoria conservada al cancelar o despublicar';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `horario_atencion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador del intervalo recurrente',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio configurado',
  `dia_semana` smallint NOT NULL COMMENT 'ISO lunes=1 a domingo=7; hora local America/Lima',
  `hora_inicio` time NOT NULL COMMENT 'Apertura local',
  `hora_fin` time NOT NULL COMMENT 'Cierre local el mismo dia',
  `margen_previo_min` smallint NOT NULL DEFAULT '0' COMMENT 'Preparacion antes del evento',
  `margen_posterior_min` smallint NOT NULL DEFAULT '0' COMMENT 'Preparacion despues del evento',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_horario_intervalo` (`id_servicio`,`dia_semana`,`hora_inicio`,`hora_fin`),
  CONSTRAINT `fk_horario_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_horario_dia` CHECK ((`dia_semana` between 1 and 7)),
  CONSTRAINT `ck_horario_horas` CHECK (((`hora_inicio` >= _utf8mb4'00:00:00') and (`hora_inicio` < `hora_fin`) and (`hora_fin` <= _utf8mb4'24:00:00'))),
  CONSTRAINT `ck_horario_margen` CHECK (((`margen_previo_min` between 0 and 1440) and (`margen_posterior_min` between 0 and 1440)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Franjas recurrentes; cruzar medianoche requiere dos franjas';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `imagen_servicio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de imagen',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio propietario',
  `ruta_objeto` varchar(500) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Ruta del almacenamiento externo, no URL firmada permanente',
  `mime` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'MIME validado en servidor',
  `tamano_bytes` bigint NOT NULL COMMENT 'Tamano validado del archivo',
  `descripcion` varchar(300) DEFAULT NULL COMMENT 'Descripcion accesible',
  `orden` int NOT NULL DEFAULT '0' COMMENT 'Orden en galeria',
  `es_portada` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Marca de portada',
  `servicio_portada` bigint GENERATED ALWAYS AS ((case when (`es_portada` = 1) then `id_servicio` else NULL end)) STORED COMMENT 'Una portada por servicio',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Carga UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_imagen_portada` (`servicio_portada`),
  KEY `ix_imagen_servicio` (`id_servicio`,`orden`),
  CONSTRAINT `fk_imagen_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_imagen_orden` CHECK ((`orden` >= 0)),
  CONSTRAINT `ck_imagen_portada` CHECK ((`es_portada` in (0,1))),
  CONSTRAINT `ck_imagen_tamano` CHECK ((`tamano_bytes` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Metadatos de imagen; archivo fuera de MySQL';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incidencia_pago` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de incidencia financiera',
  `id_pago` bigint NOT NULL COMMENT 'Pago afectado',
  `id_contrato` bigint NOT NULL COMMENT 'Contrato coherente con pago',
  `tipo` varchar(50) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Conflicto, cobro tardio o error de conciliacion',
  `estado` varchar(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ABIERTA' COMMENT 'Estado de resolucion humana/automatica',
  `detalle` varchar(2000) NOT NULL COMMENT 'Detalle sin instrumentos financieros sensibles',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Deteccion UTC',
  `resuelta_en` datetime(6) DEFAULT NULL COMMENT 'Resolucion UTC',
  PRIMARY KEY (`id`),
  KEY `fk_incidencia_pago` (`id_pago`,`id_contrato`),
  KEY `ix_incidencia_estado` (`estado`,`creada_en`),
  CONSTRAINT `fk_incidencia_pago` FOREIGN KEY (`id_pago`, `id_contrato`) REFERENCES `pago` (`id`, `id_contrato`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_incidencia_estado` CHECK ((`estado` in (_utf8mb4'ABIERTA',_utf8mb4'EN_REVISION',_utf8mb4'RESUELTA'))),
  CONSTRAINT `ck_incidencia_resuelta` CHECK (((`estado` <> _utf8mb4'RESUELTA') or (`resuelta_en` is not null)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cobro externo no aplicado y otras incidencias auditables';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `integrante` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador del integrante',
  `id_servicio` bigint NOT NULL COMMENT 'Banda propietaria; tipo validado por Service',
  `id_solista_vinculado` bigint DEFAULT NULL COMMENT 'Usuario solista invitado; aceptacion requerida',
  `nombre` varchar(180) NOT NULL COMMENT 'Nombre publico autorizado',
  `instrumento` varchar(100) NOT NULL COMMENT 'Instrumento o funcion musical',
  `funcion` varchar(120) DEFAULT NULL COMMENT 'Funcion dentro de banda',
  `estado_vinculo` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'SIN_VINCULO' COMMENT 'Estado del consentimiento de vinculo',
  `activo` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Baja logica conserva referencias historicas',
  `version` bigint NOT NULL DEFAULT '0' COMMENT 'Version optimista',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_integrante_vinculo` (`id_servicio`,`id_solista_vinculado`),
  KEY `fk_integrante_solista` (`id_solista_vinculado`),
  KEY `ix_integrante_servicio` (`id_servicio`,`activo`),
  CONSTRAINT `fk_integrante_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_integrante_solista` FOREIGN KEY (`id_solista_vinculado`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_integrante_activo` CHECK ((`activo` in (0,1))),
  CONSTRAINT `ck_integrante_vinculo` CHECK ((((`id_solista_vinculado` is null) and (`estado_vinculo` = _utf8mb4'SIN_VINCULO')) or ((`id_solista_vinculado` is not null) and (`estado_vinculo` in (_utf8mb4'INVITADO',_utf8mb4'ACEPTADO',_utf8mb4'RECHAZADO',_utf8mb4'RETIRADO')))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Contenido normalizado y vinculo de recurso compartido';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `movimiento_cuenta` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `id_cuenta` bigint NOT NULL,
  `id_pago` bigint DEFAULT NULL,
  `id_reembolso` bigint DEFAULT NULL,
  `operacion` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `fecha` date NOT NULL,
  `importe_centimos` bigint NOT NULL,
  `id_verificador` bigint NOT NULL,
  `registrado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_movimiento_bancario` (`id_cuenta`,`fecha`,`operacion`),
  UNIQUE KEY `uq_movimiento_pago` (`id_pago`),
  UNIQUE KEY `uq_movimiento_reembolso` (`id_reembolso`),
  KEY `fk_movimiento_verificador` (`id_verificador`),
  CONSTRAINT `fk_movimiento_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cuenta_cobro` (`id`),
  CONSTRAINT `fk_movimiento_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id`),
  CONSTRAINT `fk_movimiento_reembolso` FOREIGN KEY (`id_reembolso`) REFERENCES `reembolso` (`id`),
  CONSTRAINT `fk_movimiento_verificador` FOREIGN KEY (`id_verificador`) REFERENCES `usuario` (`id`),
  CONSTRAINT `ck_movimiento_importe` CHECK ((`importe_centimos` > 0)),
  CONSTRAINT `ck_movimiento_origen` CHECK ((((`id_pago` is not null) + (`id_reembolso` is not null)) = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificacion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de notificacion interna',
  `id_destinatario` bigint NOT NULL COMMENT 'Usuario unico destinatario',
  `tipo` varchar(40) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Tipo de aviso controlado por backend',
  `titulo` varchar(180) NOT NULL COMMENT 'Titulo seguro',
  `mensaje` varchar(2000) NOT NULL COMMENT 'Contenido sin secretos',
  `entidad` varchar(80) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL COMMENT 'Entidad de destino; validar permiso al navegar',
  `identificador_entidad` bigint DEFAULT NULL COMMENT 'Referencia logica de destino autorizada',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
  `leida_en` datetime(6) DEFAULT NULL COMMENT 'Lectura UTC',
  PRIMARY KEY (`id`),
  KEY `ix_notificacion_destinatario` (`id_destinatario`,`leida_en`,`creada_en`),
  CONSTRAINT `fk_notificacion_usuario` FOREIGN KEY (`id_destinatario`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Notificaciones persistentes por destinatario';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ocupacion_agenda` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador del intervalo por recurso',
  `id_recurso` bigint NOT NULL COMMENT 'Recurso bloqueado antes de consultar o insertar',
  `id_reserva` bigint DEFAULT NULL COMMENT 'Reserva confirmada que origina ocupacion',
  `id_bloqueo` bigint DEFAULT NULL COMMENT 'Bloqueo manual que origina ocupacion',
  `id_retencion` bigint DEFAULT NULL COMMENT 'Retencion tecnica que origina ocupacion',
  `inicio_utc` datetime(6) NOT NULL COMMENT 'Intervalo semiabierto con margenes; snapshot de recurso',
  `fin_utc` datetime(6) NOT NULL COMMENT 'Fin exclusivo de ocupacion UTC',
  `vence_en` datetime(6) DEFAULT NULL COMMENT 'Solo retenciones tienen caducidad tecnica',
  `activa` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Desactivar al liberar o convertir en misma transaccion',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ocupacion_reserva` (`id_recurso`,`id_reserva`),
  UNIQUE KEY `uq_ocupacion_bloqueo` (`id_recurso`,`id_bloqueo`),
  UNIQUE KEY `uq_ocupacion_retencion` (`id_recurso`,`id_retencion`),
  KEY `fk_ocupacion_reserva` (`id_reserva`),
  KEY `fk_ocupacion_bloqueo` (`id_bloqueo`),
  KEY `fk_ocupacion_retencion` (`id_retencion`),
  KEY `ix_ocupacion_conflicto` (`id_recurso`,`activa`,`inicio_utc`,`fin_utc`,`vence_en`),
  CONSTRAINT `fk_ocupacion_bloqueo` FOREIGN KEY (`id_bloqueo`) REFERENCES `bloqueo_agenda` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ocupacion_recurso` FOREIGN KEY (`id_recurso`) REFERENCES `recurso_agenda` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ocupacion_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reserva` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ocupacion_retencion` FOREIGN KEY (`id_retencion`) REFERENCES `retencion_agenda` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_ocupacion_activa` CHECK ((`activa` in (0,1))),
  CONSTRAINT `ck_ocupacion_caducidad` CHECK ((((`id_retencion` is null) and (`vence_en` is null)) or ((`id_retencion` is not null) and (`vence_en` is not null)))),
  CONSTRAINT `ck_ocupacion_intervalo` CHECK ((`inicio_utc` < `fin_utc`)),
  CONSTRAINT `ck_ocupacion_origen` CHECK (((((`id_reserva` is not null) + (`id_bloqueo` is not null)) + (`id_retencion` is not null)) = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Ocupacion por recurso; exclusion temporal depende del protocolo transaccional';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pago` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de intento financiero',
  `id_contrato` bigint NOT NULL COMMENT 'Contrato propietario',
  `id_cliente` bigint NOT NULL COMMENT 'Pagador coherente con contrato',
  `proveedor` varchar(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Integracion financiera concreta; aun no seleccionada',
  `referencia_externa` varchar(150) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'ID de operacion en proveedor; NULL antes de crear checkout',
  `clave_idempotencia` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave unica del intento dentro de contrato',
  `huella_solicitud` binary(32) NOT NULL COMMENT 'SHA256 de entrada canonica asociada a clave',
  `modalidad` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Importe determinado por servidor segun saldo',
  `metodo` varchar(40) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'Metodo reportado por proveedor; no promete Yape o Plin',
  `importe_centimos` bigint NOT NULL COMMENT 'Importe exacto del intento PEN',
  `moneda` char(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PEN' COMMENT 'Moneda esperada y verificada',
  `estado` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Estado interno; separado de respuesta externa',
  `estado_externo` varchar(80) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'Estado original del proveedor sin reinterpretarlo',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Inicio UTC del intento',
  `verificado_en` datetime(6) DEFAULT NULL COMMENT 'Consulta/autenticacion del resultado con proveedor',
  `cobrado_en` datetime(6) DEFAULT NULL COMMENT 'Evidencia de cobro externo; no implica reserva compatible',
  `aplicado_en` datetime(6) DEFAULT NULL COMMENT 'Instante del abono aplicado al contrato; idempotente',
  `version` bigint NOT NULL DEFAULT '0' COMMENT 'Version optimista de seguimiento',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_pago_clave` (`id_contrato`,`clave_idempotencia`),
  UNIQUE KEY `uq_pago_contrato` (`id`,`id_contrato`),
  UNIQUE KEY `uq_pago_proveedor` (`id`,`proveedor`),
  UNIQUE KEY `uq_pago_referencia` (`proveedor`,`referencia_externa`),
  KEY `fk_pago_contrato` (`id_contrato`,`id_cliente`),
  KEY `ix_pago_pendiente` (`estado`,`creado_en`),
  KEY `ix_pago_contrato_fecha` (`id_contrato`,`creado_en`),
  CONSTRAINT `fk_pago_contrato` FOREIGN KEY (`id_contrato`, `id_cliente`) REFERENCES `contrato` (`id`, `id_cliente`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_pago_estado` CHECK ((`estado` in (_utf8mb4'PENDIENTE',_utf8mb4'PROCESANDO',_utf8mb4'APROBADO',_utf8mb4'RECHAZADO',_utf8mb4'CANCELADO',_utf8mb4'EXPIRADO',_utf8mb4'REEMBOLSADO',_utf8mb4'REEMBOLSO_PENDIENTE',_utf8mb4'EN_REVISION'))),
  CONSTRAINT `ck_pago_importe` CHECK ((`importe_centimos` between 1 and 999999999999)),
  CONSTRAINT `ck_pago_modalidad` CHECK ((`modalidad` in (_utf8mb4'SALDO_COMPLETO',_utf8mb4'MITAD_SALDO'))),
  CONSTRAINT `ck_pago_moneda` CHECK ((`moneda` = _utf8mb4'PEN')),
  CONSTRAINT `ck_pago_verificacion` CHECK ((((`cobrado_en` is null) or ((`verificado_en` is not null) and (`referencia_externa` is not null))) and ((`aplicado_en` is null) or (`cobrado_en` is not null)) and ((`estado` <> _utf8mb4'APROBADO') or (`aplicado_en` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Intentos, cobro externo y aplicacion contractual diferenciados';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pago_directo` (
  `id_pago` bigint NOT NULL,
  `id_cuenta` bigint NOT NULL,
  `destino_snapshot` json NOT NULL,
  `operacion_reportada` varchar(100) DEFAULT NULL,
  `fecha_reportada` date DEFAULT NULL,
  `importe_reportado_centimos` bigint DEFAULT NULL,
  `titular_origen` varchar(180) DEFAULT NULL,
  `reportado_en` datetime(6) DEFAULT NULL,
  `huella_reporte` binary(32) DEFAULT NULL,
  `motivo_revision` varchar(1000) DEFAULT NULL,
  `huella_revision` binary(32) DEFAULT NULL,
  `id_revisor` bigint DEFAULT NULL,
  PRIMARY KEY (`id_pago`),
  KEY `fk_directo_cuenta` (`id_cuenta`),
  KEY `fk_directo_revisor` (`id_revisor`),
  CONSTRAINT `fk_directo_cuenta` FOREIGN KEY (`id_cuenta`) REFERENCES `cuenta_cobro` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_directo_pago` FOREIGN KEY (`id_pago`) REFERENCES `pago` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_directo_revisor` FOREIGN KEY (`id_revisor`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recuperacion_acceso` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de recuperacion de acceso',
  `id_usuario` bigint NOT NULL COMMENT 'Cuenta destinataria',
  `token_hash` binary(32) NOT NULL COMMENT 'Hash de token aleatorio de un solo uso',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Emision UTC',
  `vence_en` datetime(6) NOT NULL COMMENT 'Caducidad UTC',
  `utilizada_en` datetime(6) DEFAULT NULL COMMENT 'Consumo UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_recuperacion_token` (`token_hash`),
  KEY `ix_recuperacion_usuario` (`id_usuario`,`vence_en`),
  CONSTRAINT `fk_recuperacion_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_recuperacion_vigencia` CHECK ((`vence_en` > `creada_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Recuperacion segura, sin guardar credenciales';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recurso_agenda` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Fila estable de serializacion por recurso',
  `id_servicio` bigint DEFAULT NULL COMMENT 'Recurso del propio servicio',
  `id_musico` bigint DEFAULT NULL COMMENT 'Usuario musico compartido entre bandas/servicio individual',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_recurso_servicio` (`id_servicio`),
  UNIQUE KEY `uq_recurso_musico` (`id_musico`),
  CONSTRAINT `fk_recurso_musico` FOREIGN KEY (`id_musico`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_recurso_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_recurso_origen` CHECK ((((`id_servicio` is not null) + (`id_musico` is not null)) = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Mutex transaccional por servicio o musico; SELECT FOR UPDATE ordenado';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reembolso` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de solicitud de devolucion',
  `id_pago` bigint NOT NULL COMMENT 'Intento de cobro original',
  `proveedor` varchar(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Proveedor coherente con pago; deduplicacion de devolucion externa',
  `id_contrato` bigint NOT NULL COMMENT 'Contrato coherente con pago',
  `clave_idempotencia` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave de solicitud por pago',
  `referencia_externa` varchar(150) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT 'Referencia de devolucion en proveedor',
  `importe_centimos` bigint NOT NULL COMMENT 'Importe exacto solicitado PEN',
  `motivo` varchar(1000) NOT NULL COMMENT 'Motivo autorizado de devolucion',
  `estado` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Confirmar con proveedor, no con rollback SQL',
  `solicitado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Solicitud UTC',
  `confirmado_en` datetime(6) DEFAULT NULL COMMENT 'Confirmacion externa UTC',
  `version` bigint NOT NULL DEFAULT '0',
  `huella_solicitud` binary(32) DEFAULT NULL,
  `huella_confirmacion` binary(32) DEFAULT NULL,
  `id_solicitante` bigint DEFAULT NULL,
  `id_verificador` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reembolso_clave` (`id_pago`,`clave_idempotencia`),
  UNIQUE KEY `uq_reembolso_externo` (`proveedor`,`referencia_externa`),
  KEY `fk_reembolso_pago` (`id_pago`,`id_contrato`),
  KEY `fk_reembolso_proveedor` (`id_pago`,`proveedor`),
  KEY `ix_reembolso_pendiente` (`estado`,`solicitado_en`),
  KEY `fk_reembolso_solicitante` (`id_solicitante`),
  KEY `fk_reembolso_verificador` (`id_verificador`),
  CONSTRAINT `fk_reembolso_pago` FOREIGN KEY (`id_pago`, `id_contrato`) REFERENCES `pago` (`id`, `id_contrato`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_reembolso_proveedor` FOREIGN KEY (`id_pago`, `proveedor`) REFERENCES `pago` (`id`, `proveedor`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_reembolso_solicitante` FOREIGN KEY (`id_solicitante`) REFERENCES `usuario` (`id`),
  CONSTRAINT `fk_reembolso_verificador` FOREIGN KEY (`id_verificador`) REFERENCES `usuario` (`id`),
  CONSTRAINT `ck_reembolso_confirmacion` CHECK (((`estado` <> _ascii'APROBADO') or ((`confirmado_en` is not null) and (`referencia_externa` is not null)))),
  CONSTRAINT `ck_reembolso_estado` CHECK ((`estado` in (_ascii'PENDIENTE',_ascii'PROCESANDO',_ascii'APROBADO',_ascii'RECHAZADO'))),
  CONSTRAINT `ck_reembolso_importe` CHECK ((`importe_centimos` between 1 and 999999999999))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Devoluciones externas idempotentes y conciliables';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repertorio` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de cancion',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio propietario',
  `titulo` varchar(180) NOT NULL COMMENT 'Titulo de la pieza',
  `genero` varchar(80) NOT NULL COMMENT 'Genero de la pieza',
  `descripcion` varchar(1000) DEFAULT NULL COMMENT 'Descripcion opcional',
  `duracion_segundos` int DEFAULT NULL COMMENT 'Duracion opcional positiva',
  `audio_ruta` varchar(500) DEFAULT NULL COMMENT 'Ruta controlada de audio, no secreto ni binario',
  `orden` int NOT NULL DEFAULT '0' COMMENT 'Orden de presentacion',
  `activo` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Retiro logico',
  PRIMARY KEY (`id`),
  KEY `ix_repertorio_servicio` (`id_servicio`,`activo`,`orden`),
  CONSTRAINT `fk_repertorio_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_repertorio_activo` CHECK ((`activo` in (0,1))),
  CONSTRAINT `ck_repertorio_duracion` CHECK (((`duracion_segundos` is null) or (`duracion_segundos` > 0))),
  CONSTRAINT `ck_repertorio_orden` CHECK ((`orden` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Repertorio individual por servicio';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reserva` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de reserva materializada',
  `id_contrato` bigint NOT NULL COMMENT 'Contrato unico que origina la reserva',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio coherente con contrato',
  `estado` varchar(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'CONFIRMADA' COMMENT 'Estado operativo de reserva',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Primer abono aplicado UTC',
  `cancelada_en` datetime(6) DEFAULT NULL COMMENT 'Liberacion de intervalo UTC segun politica',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_reserva_contrato` (`id_contrato`),
  KEY `fk_reserva_contrato` (`id_contrato`,`id_servicio`),
  CONSTRAINT `fk_reserva_contrato` FOREIGN KEY (`id_contrato`, `id_servicio`) REFERENCES `contrato` (`id`, `id_servicio`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_reserva_cancelacion` CHECK (((`estado` <> _utf8mb4'CANCELADA') or (`cancelada_en` is not null))),
  CONSTRAINT `ck_reserva_estado` CHECK ((`estado` in (_utf8mb4'CONFIRMADA',_utf8mb4'CANCELADA')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Una reserva por contrato; fechas derivadas de snapshot contractual';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `retencion_agenda` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de retencion tecnica',
  `id_contrato` bigint NOT NULL COMMENT 'Contrato que inicia checkout',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio coherente con contrato',
  `id_pago` bigint NOT NULL COMMENT 'Intento asociado coherente con contrato',
  `inicio_utc` datetime(6) NOT NULL COMMENT 'Inicio con margenes UTC',
  `fin_utc` datetime(6) NOT NULL COMMENT 'Fin con margenes UTC',
  `vence_en` datetime(6) NOT NULL COMMENT 'Caducidad tecnica UTC, nunca reserva confirmada',
  `estado` varchar(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVA' COMMENT 'Estado de retencion tecnica',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_retencion_pago` (`id_pago`),
  KEY `fk_retencion_contrato` (`id_contrato`,`id_servicio`),
  KEY `fk_retencion_pago` (`id_pago`,`id_contrato`),
  KEY `ix_retencion_vencimiento` (`estado`,`vence_en`),
  CONSTRAINT `fk_retencion_contrato` FOREIGN KEY (`id_contrato`, `id_servicio`) REFERENCES `contrato` (`id`, `id_servicio`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_retencion_pago` FOREIGN KEY (`id_pago`, `id_contrato`) REFERENCES `pago` (`id`, `id_contrato`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_retencion_estado` CHECK ((`estado` in (_utf8mb4'ACTIVA',_utf8mb4'CONVERTIDA',_utf8mb4'EXPIRADA',_utf8mb4'CANCELADA'))),
  CONSTRAINT `ck_retencion_intervalo` CHECK (((`inicio_utc` < `fin_utc`) and (`vence_en` > `creada_en`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Soporte opcional de checkout asincrono; no obliga a habilitarlo';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rol` (
  `id` smallint NOT NULL COMMENT 'Identificador estable del rol',
  `codigo` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Codigo de autorizacion del servidor',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_rol_codigo` (`codigo`),
  CONSTRAINT `ck_rol_codigo` CHECK ((`codigo` in (_utf8mb4'ADMINISTRADOR',_utf8mb4'CLIENTE',_utf8mb4'DIRECTOR',_utf8mb4'SOLISTA')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cuatro roles; no crea cuentas de usuario';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servicio_genero` (
  `id_servicio` bigint NOT NULL COMMENT 'Servicio propietario',
  `genero` varchar(80) NOT NULL COMMENT 'Genero musical normalizado por backend',
  PRIMARY KEY (`id_servicio`,`genero`),
  KEY `ix_genero_busqueda` (`genero`,`id_servicio`),
  CONSTRAINT `fk_genero_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Lista de generos consultable por SQL';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servicio_instrumento` (
  `id_servicio` bigint NOT NULL COMMENT 'Servicio propietario',
  `instrumento` varchar(80) NOT NULL COMMENT 'Instrumento o especialidad',
  PRIMARY KEY (`id_servicio`,`instrumento`),
  KEY `ix_instrumento_busqueda` (`instrumento`,`id_servicio`),
  CONSTRAINT `fk_instrumento_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Instrumentos publicos del servicio';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servicio_musical` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador publico del servicio',
  `id_responsable` bigint NOT NULL COMMENT 'Director o solista responsable; rol validado en Service',
  `tipo` varchar(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'BANDA o SOLISTA; no son tablas duplicadas',
  `id_solista_unico` bigint GENERATED ALWAYS AS ((case when (`tipo` = _ascii'SOLISTA') then `id_responsable` else NULL end)) STORED COMMENT 'Un solo servicio individual por solista',
  `nombre` varchar(160) NOT NULL COMMENT 'Nombre publico',
  `descripcion_historica` text COMMENT 'Resena historica opcional; no campo Presentacion del servicio',
  `ciudad` varchar(100) NOT NULL COMMENT 'Ubicacion publica sin direccion de eventos',
  `experiencia` varchar(500) DEFAULT NULL COMMENT 'Experiencia publicada',
  `incluye` text COMMENT 'Contenido referencial sujeto a cotizacion',
  `tarifa_centimos` bigint DEFAULT NULL COMMENT 'PEN por hora; NULL significa A cotizar',
  `estado_publicacion` varchar(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BORRADOR' COMMENT 'Ciclo de revision administrativa',
  `motivo_revision` varchar(1000) DEFAULT NULL COMMENT 'Motivo privado de observacion o retiro',
  `version` bigint NOT NULL DEFAULT '0' COMMENT 'Version optimista',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Modificacion UTC',
  `agenda_version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_servicio_solista` (`id_solista_unico`),
  KEY `ix_servicio_responsable` (`id_responsable`,`estado_publicacion`),
  KEY `ix_catalogo` (`estado_publicacion`,`tipo`,`ciudad`,`tarifa_centimos`),
  CONSTRAINT `fk_servicio_responsable` FOREIGN KEY (`id_responsable`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_servicio_estado` CHECK ((`estado_publicacion` in (_ascii'BORRADOR',_ascii'EN_REVISION',_ascii'OBSERVADO',_ascii'PUBLICADO',_ascii'RETIRADO'))),
  CONSTRAINT `ck_servicio_nombre` CHECK ((char_length(trim(`nombre`)) > 0)),
  CONSTRAINT `ck_servicio_tarifa` CHECK (((`tarifa_centimos` is null) or (`tarifa_centimos` between 0 and 999999999999))),
  CONSTRAINT `ck_servicio_tipo` CHECK ((`tipo` in (_ascii'BANDA',_ascii'SOLISTA'))),
  CONSTRAINT `ck_servicio_version` CHECK ((`version` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Banda o servicio individual de solista';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servicio_tipo_evento` (
  `id_servicio` bigint NOT NULL COMMENT 'Servicio propietario',
  `tipo_evento` varchar(100) NOT NULL COMMENT 'Tipo de evento atendido',
  PRIMARY KEY (`id_servicio`,`tipo_evento`),
  CONSTRAINT `fk_tipo_evento_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Tipos de evento del perfil publico';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de sesion de usuario',
  `id_usuario` bigint NOT NULL COMMENT 'Titular autenticado',
  `familia` char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Familia de rotacion para revocar reuso',
  `refresh_hash` binary(32) NOT NULL COMMENT 'Hash SHA256 de token aleatorio; nunca token en claro',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Emision UTC',
  `vence_en` datetime(6) NOT NULL COMMENT 'Caducidad UTC',
  `consumida_en` datetime(6) DEFAULT NULL COMMENT 'Uso del refresh; conservar para detectar reuso',
  `revocada_en` datetime(6) DEFAULT NULL COMMENT 'Revocacion UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sesion_refresh` (`refresh_hash`),
  KEY `ix_sesion_usuario` (`id_usuario`,`revocada_en`,`vence_en`),
  KEY `ix_sesion_familia` (`familia`),
  KEY `fk_sesion_familia_usuario` (`familia`,`id_usuario`),
  CONSTRAINT `fk_sesion_familia_usuario` FOREIGN KEY (`familia`, `id_usuario`) REFERENCES `auth_familia` (`id`, `id_usuario`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_sesion_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_sesion_vigencia` CHECK ((`vence_en` > `creada_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Rotacion y revocacion futura de sesiones reales';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitud_cotizacion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de solicitud',
  `id_cliente` bigint NOT NULL COMMENT 'Cliente solicitante',
  `id_servicio` bigint NOT NULL COMMENT 'Servicio elegido',
  `id_proveedor` bigint NOT NULL COMMENT 'Responsable al solicitar; se conserva historicamente',
  `inicio_evento_utc` datetime(6) NOT NULL COMMENT 'Inicio del evento UTC',
  `fin_evento_utc` datetime(6) NOT NULL COMMENT 'Fin exclusivo del evento UTC',
  `lugar` varchar(500) NOT NULL COMMENT 'Direccion privada del evento',
  `ciudad` varchar(100) NOT NULL COMMENT 'Ciudad del evento',
  `tipo_evento` varchar(100) NOT NULL COMMENT 'Tipo de evento solicitado',
  `asistentes` int DEFAULT NULL COMMENT 'Asistentes previstos',
  `preferencias` text COMMENT 'Preferencias musicales',
  `requerimientos` text COMMENT 'Necesidades del evento',
  `notas` text COMMENT 'Notas privadas',
  `estado` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ENVIADA' COMMENT 'Estado de la solicitud',
  `clave_idempotencia` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave de solicitud por cliente',
  `version` bigint NOT NULL DEFAULT '0' COMMENT 'Version optimista',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC para indicadores de cohorte',
  `primera_respuesta_en` datetime(6) DEFAULT NULL COMMENT 'Primera respuesta real UTC',
  `huella_creacion` binary(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_solicitud_clave` (`id_cliente`,`clave_idempotencia`),
  UNIQUE KEY `uq_solicitud_contexto` (`id`,`id_servicio`,`id_cliente`,`id_proveedor`),
  KEY `ix_solicitud_cliente` (`id_cliente`,`estado`,`creada_en`),
  KEY `ix_solicitud_proveedor` (`id_proveedor`,`estado`,`creada_en`),
  KEY `ix_solicitud_servicio` (`id_servicio`,`inicio_evento_utc`,`fin_evento_utc`),
  CONSTRAINT `fk_solicitud_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_solicitud_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `usuario` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_solicitud_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicio_musical` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_solicitud_asistentes` CHECK (((`asistentes` is null) or (`asistentes` > 0))),
  CONSTRAINT `ck_solicitud_estado` CHECK ((`estado` in (_ascii'ENVIADA',_ascii'EN_REVISION',_ascii'COTIZACION_RECIBIDA',_ascii'RECHAZADA',_ascii'CANCELADA',_ascii'CONTRATADA',_ascii'RESERVADA'))),
  CONSTRAINT `ck_solicitud_intervalo` CHECK (((`inicio_evento_utc` < `fin_evento_utc`) and (`fin_evento_utc` <= (`inicio_evento_utc` + interval 24 hour))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Solicitud sin reserva de horario';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `token_acceso` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `id_sesion` bigint NOT NULL,
  `token_hash` binary(32) NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `vence_en` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_token_acceso_hash` (`token_hash`),
  KEY `ix_token_acceso_sesion` (`id_sesion`,`vence_en`),
  CONSTRAINT `fk_token_acceso_sesion` FOREIGN KEY (`id_sesion`) REFERENCES `sesion` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_token_acceso_vigencia` CHECK ((`vence_en` > `creado_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Solo huella del bearer corto; nunca el token en claro';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador del usuario',
  `id_rol` smallint NOT NULL COMMENT 'Rol validado por el servidor',
  `nombres` varchar(100) NOT NULL COMMENT 'Nombres de la persona',
  `apellidos` varchar(120) NOT NULL COMMENT 'Apellidos de la persona',
  `correo` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'Correo capturado',
  `correo_normalizado` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs GENERATED ALWAYS AS (lower(trim(`correo`))) STORED COMMENT 'Identidad normalizada; conserva diferencias de acento',
  `password_hash` varchar(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Hash adaptativo generado por backend; nunca contrasena',
  `telefono` varchar(24) DEFAULT NULL COMMENT 'Telefono autorizado del perfil',
  `ciudad` varchar(100) DEFAULT NULL COMMENT 'Ciudad del perfil',
  `biografia` varchar(1000) DEFAULT NULL COMMENT 'Texto opcional del perfil',
  `estado` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE_ACTIVACION' COMMENT 'Estado administrativo de acceso',
  `version` bigint NOT NULL DEFAULT '0' COMMENT 'Version para bloqueo optimista',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de creacion',
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de ultima modificacion',
  `privacidad_version` varchar(80) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `privacidad_aceptada_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_usuario_correo` (`correo_normalizado`),
  KEY `ix_usuario_rol_estado` (`id_rol`,`estado`),
  CONSTRAINT `fk_usuario_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_usuario_estado` CHECK ((`estado` in (_ascii'PENDIENTE_ACTIVACION',_ascii'ACTIVO',_ascii'SUSPENDIDO',_ascii'DESACTIVADO'))),
  CONSTRAINT `ck_usuario_nombre` CHECK (((char_length(trim(`nombres`)) > 0) and (char_length(trim(`apellidos`)) > 0))),
  CONSTRAINT `ck_usuario_privacidad` CHECK ((((`privacidad_version` is null) and (`privacidad_aceptada_en` is null)) or ((`privacidad_version` is not null) and (`privacidad_aceptada_en` is not null)))),
  CONSTRAINT `ck_usuario_version` CHECK ((`version` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Cuentas reales; baja logica preserva historia';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `valoracion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de resena',
  `id_contrato` bigint NOT NULL COMMENT 'Unica valoracion por contrato finalizado',
  `puntuacion` smallint NOT NULL COMMENT 'Escala operativa 1-5; no instrumento de encuesta',
  `comentario` varchar(2000) DEFAULT NULL COMMENT 'Comentario autorizado',
  `consentimiento_publicacion` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Permiso expreso de publicacion',
  `creada_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Registro UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_valoracion_contrato` (`id_contrato`),
  CONSTRAINT `fk_valoracion_contrato` FOREIGN KEY (`id_contrato`) REFERENCES `contrato` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_valoracion_consentimiento` CHECK ((`consentimiento_publicacion` in (0,1))),
  CONSTRAINT `ck_valoracion_puntuacion` CHECK ((`puntuacion` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Valoracion; no se crea encuesta de RF-68 pendiente de conciliacion';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/  /*!50003 TRIGGER `tr_valoracion_insertar` BEFORE INSERT ON `valoracion` FOR EACH ROW BEGIN
    DECLARE estado_contrato VARCHAR(20);
    SELECT estado INTO estado_contrato FROM contrato WHERE id=NEW.id_contrato FOR UPDATE;
    IF estado_contrato IS NOT NULL AND estado_contrato<>'FINALIZADO' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VALORACION_REQUIERE_CONTRATO_FINALIZADO';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- Roles del sistema; no son cuentas de acceso.
INSERT INTO `rol` (`id`,`codigo`) VALUES
(1,'ADMINISTRADOR'),
(2,'CLIENTE'),
(3,'DIRECTOR'),
(4,'SOLISTA');

-- Vista contable, con permisos del usuario que la consulta.
CREATE ALGORITHM=UNDEFINED SQL SECURITY INVOKER VIEW `v_saldo_contrato` AS select `c`.`id` AS `id_contrato`,`c`.`total_centimos` AS `total_centimos`,coalesce(`a`.`abonado_centimos`,0) AS `abonado_centimos`,coalesce(`a`.`reembolsado_centimos`,0) AS `reembolsado_centimos`,((`c`.`total_centimos` - coalesce(`a`.`abonado_centimos`,0)) + coalesce(`a`.`reembolsado_centimos`,0)) AS `saldo_centimos` from (`contrato` `c` left join (select `p`.`id_contrato` AS `id_contrato`,sum(`p`.`importe_centimos`) AS `abonado_centimos`,sum(coalesce(`r`.`importe_centimos`,0)) AS `reembolsado_centimos` from (`pago` `p` left join (select `reembolso`.`id_pago` AS `id_pago`,sum(`reembolso`.`importe_centimos`) AS `importe_centimos` from `reembolso` where (`reembolso`.`estado` = 'APROBADO') group by `reembolso`.`id_pago`) `r` on((`r`.`id_pago` = `p`.`id`))) where (`p`.`aplicado_en` is not null) group by `p`.`id_contrato`) `a` on((`a`.`id_contrato` = `c`.`id`)));
