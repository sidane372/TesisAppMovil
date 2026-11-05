-- Etapa 3: tokens opacos de acceso y bloqueo estable para familias de refresh.
-- No altera V001-V004 ni crea cuentas de demostracion.
CREATE TABLE auth_familia (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revocada_en DATETIME(6) NULL,
    CONSTRAINT uq_auth_familia_usuario UNIQUE(id,id_usuario),
    CONSTRAINT fk_auth_familia_usuario FOREIGN KEY(id_usuario) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_auth_familia_usuario(id_usuario,revocada_en)
) ENGINE=InnoDB COMMENT='Revocacion y serializacion de una familia de sesiones';
-- Backfill para sesiones preexistentes; la FK posterior rechaza familias con titulares mezclados.
INSERT INTO auth_familia(id,id_usuario,creada_en)
SELECT familia,MIN(id_usuario),MIN(creada_en) FROM sesion GROUP BY familia;
ALTER TABLE sesion ADD CONSTRAINT fk_sesion_familia_usuario
    FOREIGN KEY(familia,id_usuario) REFERENCES auth_familia(id,id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT;
CREATE TABLE token_acceso (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_sesion BIGINT NOT NULL,
    token_hash BINARY(32) NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    vence_en DATETIME(6) NOT NULL,
    CONSTRAINT uq_token_acceso_hash UNIQUE(token_hash),
    CONSTRAINT fk_token_acceso_sesion FOREIGN KEY(id_sesion) REFERENCES sesion(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_token_acceso_vigencia CHECK(vence_en>creado_en),
    INDEX ix_token_acceso_sesion(id_sesion,vence_en)
) ENGINE=InnoDB COMMENT='Solo huella del bearer corto; nunca el token en claro';
ALTER TABLE usuario
    ADD privacidad_version VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD privacidad_aceptada_en DATETIME(6) NULL,
    ADD CONSTRAINT ck_usuario_privacidad CHECK (
      (privacidad_version IS NULL AND privacidad_aceptada_en IS NULL)
      OR (privacidad_version IS NOT NULL AND privacidad_aceptada_en IS NOT NULL));
