-- Decisión del usuario: Yape, Plin y transferencia sin pasarela comercial.
-- Ningún comprobante del cliente constituye por sí solo un ingreso verificado.
CREATE TABLE cuenta_cobro (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titular VARCHAR(180) NOT NULL,
    banco VARCHAR(100) NOT NULL,
    telefono VARCHAR(20) NULL,
    numero_cuenta VARCHAR(40) NULL,
    cci VARCHAR(20) NULL,
    yape BOOLEAN NOT NULL DEFAULT FALSE,
    plin BOOLEAN NOT NULL DEFAULT FALSE,
    transferencia BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_cobro_cuenta UNIQUE(banco,numero_cuenta),
    CONSTRAINT uq_cobro_cci UNIQUE(cci),
    CONSTRAINT uq_cobro_telefono UNIQUE(banco,telefono),
    CONSTRAINT ck_cobro_medios CHECK (yape=1 OR plin=1 OR transferencia=1),
    CONSTRAINT ck_cobro_destino CHECK ((yape=0 AND plin=0 OR telefono IS NOT NULL) AND (transferencia=0 OR numero_cuenta IS NOT NULL OR cci IS NOT NULL))
) ENGINE=InnoDB;

CREATE TABLE pago_directo (
    id_pago BIGINT NOT NULL PRIMARY KEY,
    id_cuenta BIGINT NOT NULL,
    destino_snapshot JSON NOT NULL,
    operacion_reportada VARCHAR(100) NULL,
    fecha_reportada DATE NULL,
    importe_reportado_centimos BIGINT NULL,
    titular_origen VARCHAR(180) NULL,
    reportado_en DATETIME(6) NULL,
    huella_reporte BINARY(32) NULL,
    motivo_revision VARCHAR(1000) NULL,
    huella_revision BINARY(32) NULL,
    id_revisor BIGINT NULL,
    CONSTRAINT fk_directo_pago FOREIGN KEY(id_pago) REFERENCES pago(id) ON DELETE RESTRICT,
    CONSTRAINT fk_directo_cuenta FOREIGN KEY(id_cuenta) REFERENCES cuenta_cobro(id) ON DELETE RESTRICT,
    CONSTRAINT fk_directo_revisor FOREIGN KEY(id_revisor) REFERENCES usuario(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

ALTER TABLE reembolso ADD version BIGINT NOT NULL DEFAULT 0,
    ADD huella_solicitud BINARY(32) NULL,
    ADD huella_confirmacion BINARY(32) NULL,
    ADD id_solicitante BIGINT NULL,
    ADD id_verificador BIGINT NULL,
    ADD CONSTRAINT fk_reembolso_solicitante FOREIGN KEY(id_solicitante) REFERENCES usuario(id),
    ADD CONSTRAINT fk_reembolso_verificador FOREIGN KEY(id_verificador) REFERENCES usuario(id);

CREATE TABLE comprobante_directo (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_pago BIGINT NULL,
    id_reembolso BIGINT NULL,
    imagen MEDIUMBLOB NOT NULL,
    imagen_hash BINARY(32) NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'image/jpeg',
    id_actor BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_comprobante_pago UNIQUE(id_pago),
    CONSTRAINT uq_comprobante_reembolso UNIQUE(id_reembolso),
    CONSTRAINT fk_comprobante_pago FOREIGN KEY(id_pago) REFERENCES pago(id),
    CONSTRAINT fk_comprobante_reembolso FOREIGN KEY(id_reembolso) REFERENCES reembolso(id),
    CONSTRAINT fk_comprobante_actor FOREIGN KEY(id_actor) REFERENCES usuario(id),
    CONSTRAINT ck_comprobante_origen CHECK ((id_pago IS NOT NULL)+(id_reembolso IS NOT NULL)=1)
) ENGINE=InnoDB;

CREATE TABLE movimiento_cuenta (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_cuenta BIGINT NOT NULL,
    id_pago BIGINT NULL,
    id_reembolso BIGINT NULL,
    operacion VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    fecha DATE NOT NULL,
    importe_centimos BIGINT NOT NULL,
    id_verificador BIGINT NOT NULL,
    registrado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_movimiento_bancario UNIQUE(id_cuenta,fecha,operacion),
    CONSTRAINT uq_movimiento_pago UNIQUE(id_pago),
    CONSTRAINT uq_movimiento_reembolso UNIQUE(id_reembolso),
    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY(id_cuenta) REFERENCES cuenta_cobro(id),
    CONSTRAINT fk_movimiento_pago FOREIGN KEY(id_pago) REFERENCES pago(id),
    CONSTRAINT fk_movimiento_reembolso FOREIGN KEY(id_reembolso) REFERENCES reembolso(id),
    CONSTRAINT fk_movimiento_verificador FOREIGN KEY(id_verificador) REFERENCES usuario(id),
    CONSTRAINT ck_movimiento_origen CHECK ((id_pago IS NOT NULL)+(id_reembolso IS NOT NULL)=1),
    CONSTRAINT ck_movimiento_importe CHECK (importe_centimos>0)
) ENGINE=InnoDB;
