-- Santa Cecilia: modelo central. MySQL 8.0.30+ / InnoDB / conexiones en UTC.
-- Aplicar UNA vez en un esquema vacio; no incluye usuarios ni datos de demostracion.
SET time_zone = '+00:00';
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE rol (
    id SMALLINT NOT NULL PRIMARY KEY COMMENT 'Identificador estable del rol',
    codigo VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Codigo de autorizacion del servidor',
    CONSTRAINT uq_rol_codigo UNIQUE (codigo),
    CONSTRAINT ck_rol_codigo CHECK (codigo IN ('ADMINISTRADOR','CLIENTE','DIRECTOR','SOLISTA'))
) ENGINE=InnoDB COMMENT='Cuatro roles; no crea cuentas de usuario';

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador del usuario',
    id_rol SMALLINT NOT NULL COMMENT 'Rol validado por el servidor',
    nombres VARCHAR(100) NOT NULL COMMENT 'Nombres de la persona',
    apellidos VARCHAR(120) NOT NULL COMMENT 'Apellidos de la persona',
    correo VARCHAR(254) COLLATE utf8mb4_0900_as_cs NOT NULL COMMENT 'Correo capturado',
    correo_normalizado VARCHAR(254) COLLATE utf8mb4_0900_as_cs GENERATED ALWAYS AS (LOWER(TRIM(correo))) STORED COMMENT 'Identidad normalizada; conserva diferencias de acento',
    password_hash VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Hash adaptativo generado por backend; nunca contrasena',
    telefono VARCHAR(24) NULL COMMENT 'Telefono autorizado del perfil',
    ciudad VARCHAR(100) NULL COMMENT 'Ciudad del perfil',
    biografia VARCHAR(1000) NULL COMMENT 'Texto opcional del perfil',
    estado VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE_ACTIVACION' COMMENT 'Estado administrativo de acceso',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version para bloqueo optimista',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de creacion',
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante UTC de ultima modificacion',
    CONSTRAINT uq_usuario_correo UNIQUE (correo_normalizado),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('PENDIENTE_ACTIVACION','ACTIVO','SUSPENDIDO','DESACTIVADO')),
    CONSTRAINT ck_usuario_nombre CHECK (CHAR_LENGTH(TRIM(nombres)) > 0 AND CHAR_LENGTH(TRIM(apellidos)) > 0),
    CONSTRAINT ck_usuario_version CHECK (version >= 0),
    INDEX ix_usuario_rol_estado (id_rol,estado)
) ENGINE=InnoDB COMMENT='Cuentas reales; baja logica preserva historia';

CREATE TABLE servicio_musical (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador publico del servicio',
    id_responsable BIGINT NOT NULL COMMENT 'Director o solista responsable; rol validado en Service',
    tipo VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'BANDA o SOLISTA; no son tablas duplicadas',
    id_solista_unico BIGINT GENERATED ALWAYS AS (CASE WHEN tipo='SOLISTA' THEN id_responsable ELSE NULL END) STORED COMMENT 'Un solo servicio individual por solista',
    nombre VARCHAR(160) NOT NULL COMMENT 'Nombre publico',
    descripcion_historica TEXT NULL COMMENT 'Resena historica opcional; no campo Presentacion del servicio',
    ciudad VARCHAR(100) NOT NULL COMMENT 'Ubicacion publica sin direccion de eventos',
    experiencia VARCHAR(500) NULL COMMENT 'Experiencia publicada',
    incluye TEXT NULL COMMENT 'Contenido referencial sujeto a cotizacion',
    tarifa_centimos BIGINT NULL COMMENT 'PEN por hora; NULL significa A cotizar',
    estado_publicacion VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BORRADOR' COMMENT 'Ciclo de revision administrativa',
    motivo_revision VARCHAR(1000) NULL COMMENT 'Motivo privado de observacion o retiro',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version optimista',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Modificacion UTC',
    CONSTRAINT uq_servicio_solista UNIQUE (id_solista_unico),
    CONSTRAINT fk_servicio_responsable FOREIGN KEY (id_responsable) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_servicio_tipo CHECK (tipo IN ('BANDA','SOLISTA')),
    CONSTRAINT ck_servicio_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT ck_servicio_tarifa CHECK (tarifa_centimos IS NULL OR tarifa_centimos BETWEEN 0 AND 999999999999),
    CONSTRAINT ck_servicio_estado CHECK (estado_publicacion IN ('BORRADOR','EN_REVISION','OBSERVADO','PUBLICADO','RETIRADO')),
    CONSTRAINT ck_servicio_version CHECK (version >= 0),
    INDEX ix_servicio_responsable (id_responsable,estado_publicacion),
    INDEX ix_catalogo (estado_publicacion,tipo,ciudad,tarifa_centimos)
) ENGINE=InnoDB COMMENT='Banda o servicio individual de solista';

CREATE TABLE servicio_genero (
    id_servicio BIGINT NOT NULL COMMENT 'Servicio propietario',
    genero VARCHAR(80) NOT NULL COMMENT 'Genero musical normalizado por backend',
    PRIMARY KEY(id_servicio,genero),
    CONSTRAINT fk_genero_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_genero_busqueda(genero,id_servicio)
) ENGINE=InnoDB COMMENT='Lista de generos consultable por SQL';
CREATE TABLE servicio_instrumento (
    id_servicio BIGINT NOT NULL COMMENT 'Servicio propietario',
    instrumento VARCHAR(80) NOT NULL COMMENT 'Instrumento o especialidad',
    PRIMARY KEY(id_servicio,instrumento),
    CONSTRAINT fk_instrumento_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_instrumento_busqueda(instrumento,id_servicio)
) ENGINE=InnoDB COMMENT='Instrumentos publicos del servicio';
CREATE TABLE servicio_tipo_evento (
    id_servicio BIGINT NOT NULL COMMENT 'Servicio propietario',
    tipo_evento VARCHAR(100) NOT NULL COMMENT 'Tipo de evento atendido',
    PRIMARY KEY(id_servicio,tipo_evento),
    CONSTRAINT fk_tipo_evento_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB COMMENT='Tipos de evento del perfil publico';

CREATE TABLE integrante (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador del integrante',
    id_servicio BIGINT NOT NULL COMMENT 'Banda propietaria; tipo validado por Service',
    id_solista_vinculado BIGINT NULL COMMENT 'Usuario solista invitado; aceptacion requerida',
    nombre VARCHAR(180) NOT NULL COMMENT 'Nombre publico autorizado',
    instrumento VARCHAR(100) NOT NULL COMMENT 'Instrumento o funcion musical',
    funcion VARCHAR(120) NULL COMMENT 'Funcion dentro de banda',
    estado_vinculo VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'SIN_VINCULO' COMMENT 'Estado del consentimiento de vinculo',
    activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Baja logica conserva referencias historicas',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version optimista',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
    CONSTRAINT fk_integrante_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_integrante_solista FOREIGN KEY(id_solista_vinculado) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_integrante_vinculo UNIQUE(id_servicio,id_solista_vinculado),
    CONSTRAINT ck_integrante_vinculo CHECK ((id_solista_vinculado IS NULL AND estado_vinculo='SIN_VINCULO') OR (id_solista_vinculado IS NOT NULL AND estado_vinculo IN ('INVITADO','ACEPTADO','RECHAZADO','RETIRADO'))),
    CONSTRAINT ck_integrante_activo CHECK (activo IN (0,1)),
    INDEX ix_integrante_servicio(id_servicio,activo)
) ENGINE=InnoDB COMMENT='Contenido normalizado y vinculo de recurso compartido';

CREATE TABLE repertorio (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de cancion',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio propietario',
    titulo VARCHAR(180) NOT NULL COMMENT 'Titulo de la pieza',
    genero VARCHAR(80) NOT NULL COMMENT 'Genero de la pieza',
    descripcion VARCHAR(1000) NULL COMMENT 'Descripcion opcional',
    duracion_segundos INT NULL COMMENT 'Duracion opcional positiva',
    audio_ruta VARCHAR(500) NULL COMMENT 'Ruta controlada de audio, no secreto ni binario',
    orden INT NOT NULL DEFAULT 0 COMMENT 'Orden de presentacion',
    activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Retiro logico',
    CONSTRAINT fk_repertorio_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_repertorio_duracion CHECK (duracion_segundos IS NULL OR duracion_segundos > 0),
    CONSTRAINT ck_repertorio_orden CHECK (orden >= 0),
    CONSTRAINT ck_repertorio_activo CHECK (activo IN (0,1)),
    INDEX ix_repertorio_servicio(id_servicio,activo,orden)
) ENGINE=InnoDB COMMENT='Repertorio individual por servicio';
CREATE TABLE imagen_servicio (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de imagen',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio propietario',
    ruta_objeto VARCHAR(500) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Ruta del almacenamiento externo, no URL firmada permanente',
    mime VARCHAR(80) CHARACTER SET ascii NOT NULL COMMENT 'MIME validado en servidor',
    tamano_bytes BIGINT NOT NULL COMMENT 'Tamano validado del archivo',
    descripcion VARCHAR(300) NULL COMMENT 'Descripcion accesible',
    orden INT NOT NULL DEFAULT 0 COMMENT 'Orden en galeria',
    es_portada BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Marca de portada',
    servicio_portada BIGINT GENERATED ALWAYS AS (CASE WHEN es_portada=1 THEN id_servicio ELSE NULL END) STORED COMMENT 'Una portada por servicio',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Carga UTC',
    CONSTRAINT fk_imagen_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_imagen_portada UNIQUE(servicio_portada),
    CONSTRAINT ck_imagen_tamano CHECK (tamano_bytes > 0),
    CONSTRAINT ck_imagen_orden CHECK (orden >= 0),
    CONSTRAINT ck_imagen_portada CHECK (es_portada IN (0,1)),
    INDEX ix_imagen_servicio(id_servicio,orden)
) ENGINE=InnoDB COMMENT='Metadatos de imagen; archivo fuera de MySQL';

CREATE TABLE horario_atencion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador del intervalo recurrente',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio configurado',
    dia_semana SMALLINT NOT NULL COMMENT 'ISO lunes=1 a domingo=7; hora local America/Lima',
    hora_inicio TIME NOT NULL COMMENT 'Apertura local',
    hora_fin TIME NOT NULL COMMENT 'Cierre local el mismo dia',
    margen_previo_min SMALLINT NOT NULL DEFAULT 0 COMMENT 'Preparacion antes del evento',
    margen_posterior_min SMALLINT NOT NULL DEFAULT 0 COMMENT 'Preparacion despues del evento',
    CONSTRAINT fk_horario_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_horario_intervalo UNIQUE(id_servicio,dia_semana,hora_inicio,hora_fin),
    CONSTRAINT ck_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_horas CHECK (hora_inicio >= '00:00:00' AND hora_inicio < hora_fin AND hora_fin <= '24:00:00'),
    CONSTRAINT ck_horario_margen CHECK (margen_previo_min BETWEEN 0 AND 1440 AND margen_posterior_min BETWEEN 0 AND 1440)
) ENGINE=InnoDB COMMENT='Franjas recurrentes; cruzar medianoche requiere dos franjas';

CREATE TABLE solicitud_cotizacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de solicitud',
    id_cliente BIGINT NOT NULL COMMENT 'Cliente solicitante',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio elegido',
    id_proveedor BIGINT NOT NULL COMMENT 'Responsable al solicitar; se conserva historicamente',
    inicio_evento_utc DATETIME(6) NOT NULL COMMENT 'Inicio del evento UTC',
    fin_evento_utc DATETIME(6) NOT NULL COMMENT 'Fin exclusivo del evento UTC',
    lugar VARCHAR(500) NOT NULL COMMENT 'Direccion privada del evento',
    ciudad VARCHAR(100) NOT NULL COMMENT 'Ciudad del evento',
    tipo_evento VARCHAR(100) NOT NULL COMMENT 'Tipo de evento solicitado',
    asistentes INT NULL COMMENT 'Asistentes previstos',
    preferencias TEXT NULL COMMENT 'Preferencias musicales',
    requerimientos TEXT NULL COMMENT 'Necesidades del evento',
    notas TEXT NULL COMMENT 'Notas privadas',
    estado VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ENVIADA' COMMENT 'Estado de la solicitud',
    clave_idempotencia VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave de solicitud por cliente',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version optimista',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC para indicadores de cohorte',
    primera_respuesta_en DATETIME(6) NULL COMMENT 'Primera respuesta real UTC',
    CONSTRAINT fk_solicitud_cliente FOREIGN KEY(id_cliente) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_solicitud_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_solicitud_proveedor FOREIGN KEY(id_proveedor) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_solicitud_clave UNIQUE(id_cliente,clave_idempotencia),
    CONSTRAINT uq_solicitud_contexto UNIQUE(id,id_servicio,id_cliente,id_proveedor),
    CONSTRAINT ck_solicitud_intervalo CHECK (inicio_evento_utc < fin_evento_utc AND fin_evento_utc <= inicio_evento_utc + INTERVAL 24 HOUR),
    CONSTRAINT ck_solicitud_asistentes CHECK (asistentes IS NULL OR asistentes > 0),
    CONSTRAINT ck_solicitud_estado CHECK (estado IN ('ENVIADA','EN_REVISION','COTIZACION_RECIBIDA','RECHAZADA','CANCELADA','CONTRATADA','RESERVADA')),
    INDEX ix_solicitud_cliente(id_cliente,estado,creada_en),
    INDEX ix_solicitud_proveedor(id_proveedor,estado,creada_en),
    INDEX ix_solicitud_servicio(id_servicio,inicio_evento_utc,fin_evento_utc)
) ENGINE=InnoDB COMMENT='Solicitud sin reserva de horario';

CREATE TABLE cotizacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de version',
    id_solicitud BIGINT NOT NULL COMMENT 'Solicitud que agrupa versiones',
    numero_version INT NOT NULL COMMENT 'Consecutivo dentro de la solicitud',
    inicio_propuesto_utc DATETIME(6) NOT NULL COMMENT 'Inicio propuesto UTC',
    fin_propuesto_utc DATETIME(6) NOT NULL COMMENT 'Fin propuesto UTC',
    subtotal_centimos BIGINT NOT NULL COMMENT 'Suma exacta de conceptos; backend reconcilia',
    descuento_centimos BIGINT NOT NULL DEFAULT 0 COMMENT 'Descuento exacto no mayor que subtotal',
    total_centimos BIGINT GENERATED ALWAYS AS (subtotal_centimos-descuento_centimos) STORED COMMENT 'Total exacto en centimos PEN',
    condiciones TEXT NOT NULL COMMENT 'Condiciones versionadas del servicio',
    politica_cancelacion TEXT NOT NULL COMMENT 'Politica propuesta para aceptacion expresa',
    politica_version VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador de politica aceptable',
    vigente_hasta_utc DATETIME(6) NOT NULL COMMENT 'Caducidad de la propuesta',
    estado VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'BORRADOR' COMMENT 'Estado de version, no edicion de propuesta enviada',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
    CONSTRAINT fk_cotizacion_solicitud FOREIGN KEY(id_solicitud) REFERENCES solicitud_cotizacion(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_cotizacion_version UNIQUE(id_solicitud,numero_version),
    CONSTRAINT uq_cotizacion_solicitud UNIQUE(id,id_solicitud),
    CONSTRAINT ck_cotizacion_version CHECK (numero_version > 0),
    CONSTRAINT ck_cotizacion_intervalo CHECK (inicio_propuesto_utc < fin_propuesto_utc AND fin_propuesto_utc <= inicio_propuesto_utc + INTERVAL 24 HOUR),
    CONSTRAINT ck_cotizacion_importes CHECK (subtotal_centimos BETWEEN 1 AND 999999999999 AND descuento_centimos >= 0 AND descuento_centimos < subtotal_centimos),
    CONSTRAINT ck_cotizacion_vigencia CHECK (vigente_hasta_utc > creada_en),
    CONSTRAINT ck_cotizacion_estado CHECK (estado IN ('BORRADOR','ENVIADA','ACEPTADA','RECHAZADA','CAMBIOS','VENCIDA','SUSTITUIDA'))
) ENGINE=InnoDB COMMENT='Versiones historicas de propuesta economica';

CREATE TABLE detalle_cotizacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador del concepto',
    id_cotizacion BIGINT NOT NULL COMMENT 'Version propietaria',
    concepto VARCHAR(300) NOT NULL COMMENT 'Descripcion del concepto economico',
    cantidad DECIMAL(10,3) NOT NULL COMMENT 'Cantidad exacta, hasta tres decimales',
    precio_unitario_centimos BIGINT NOT NULL COMMENT 'Precio exacto unitario en centimos PEN',
    total_centimos BIGINT GENERATED ALWAYS AS (CAST(ROUND(cantidad * precio_unitario_centimos,0) AS SIGNED)) STORED COMMENT 'Redondeo decimal por concepto: mitad hacia arriba',
    orden INT NOT NULL DEFAULT 0 COMMENT 'Orden del desglose',
    CONSTRAINT fk_detalle_cotizacion FOREIGN KEY(id_cotizacion) REFERENCES cotizacion(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0 AND cantidad <= 1000000),
    CONSTRAINT ck_detalle_precio CHECK (precio_unitario_centimos BETWEEN 0 AND 999999999999),
    CONSTRAINT ck_detalle_total CHECK (total_centimos BETWEEN 0 AND 999999999999),
    CONSTRAINT ck_detalle_orden CHECK (orden >= 0),
    INDEX ix_detalle_cotizacion(id_cotizacion,orden)
) ENGINE=InnoDB COMMENT='Desglose exacto sin float o double';

CREATE TABLE contrato (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador oficial del contrato',
    codigo VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Codigo visible unico',
    id_cotizacion BIGINT NOT NULL COMMENT 'Version exacta aceptada',
    id_solicitud BIGINT NOT NULL COMMENT 'Solicitud unica aun entre distintas versiones',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio historico coherente con solicitud',
    id_cliente BIGINT NOT NULL COMMENT 'Cliente historico coherente con solicitud',
    id_proveedor BIGINT NOT NULL COMMENT 'Proveedor historico coherente con solicitud',
    inicio_evento_utc DATETIME(6) NOT NULL COMMENT 'Inicio aceptado UTC',
    fin_evento_utc DATETIME(6) NOT NULL COMMENT 'Fin exclusivo aceptado UTC',
    inicio_ocupacion_utc DATETIME(6) NOT NULL COMMENT 'Inicio con margen de preparacion incluido',
    fin_ocupacion_utc DATETIME(6) NOT NULL COMMENT 'Fin con margen posterior incluido',
    total_centimos BIGINT NOT NULL COMMENT 'Total historico exacto PEN',
    condiciones_aceptadas BOOLEAN NOT NULL COMMENT 'Aceptacion expresa obligatoria',
    politica_version VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Version exacta aceptada',
    snapshot JSON NOT NULL COMMENT 'Copia historica de perfil, actores, conceptos, recursos y politicas',
    clave_idempotencia VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave de aceptacion por cliente',
    huella_aceptacion BINARY(32) NOT NULL COMMENT 'SHA256 de entrada canonica para detectar reutilizacion de clave',
    estado VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE_PAGO' COMMENT 'Estado contractual independiente del proveedor financiero',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version optimista de estado',
    aceptado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Aceptacion UTC',
    confirmado_en DATETIME(6) NULL COMMENT 'Primer abono verificado UTC',
    finalizado_en DATETIME(6) NULL COMMENT 'Finalizacion despues del evento UTC',
    cancelado_en DATETIME(6) NULL COMMENT 'Cancelacion UTC',
    motivo_cancelacion VARCHAR(1000) NULL COMMENT 'Motivo conservado segun politica',
    CONSTRAINT uq_contrato_codigo UNIQUE(codigo),
    CONSTRAINT uq_contrato_cotizacion UNIQUE(id_cotizacion),
    CONSTRAINT uq_contrato_solicitud UNIQUE(id_solicitud),
    CONSTRAINT uq_contrato_aceptacion UNIQUE(id_cliente,clave_idempotencia),
    CONSTRAINT uq_contrato_cliente UNIQUE(id,id_cliente),
    CONSTRAINT uq_contrato_servicio UNIQUE(id,id_servicio),
    CONSTRAINT fk_contrato_cotizacion FOREIGN KEY(id_cotizacion,id_solicitud) REFERENCES cotizacion(id,id_solicitud) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_contrato_contexto FOREIGN KEY(id_solicitud,id_servicio,id_cliente,id_proveedor) REFERENCES solicitud_cotizacion(id,id_servicio,id_cliente,id_proveedor) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_contrato_intervalo CHECK (inicio_evento_utc < fin_evento_utc AND inicio_ocupacion_utc <= inicio_evento_utc AND fin_ocupacion_utc >= fin_evento_utc),
    CONSTRAINT ck_contrato_total CHECK (total_centimos BETWEEN 1 AND 999999999999),
    CONSTRAINT ck_contrato_aceptacion CHECK (condiciones_aceptadas=1 AND JSON_TYPE(snapshot)='OBJECT'),
    CONSTRAINT ck_contrato_estado CHECK (estado IN ('PENDIENTE_PAGO','CONFIRMADO','FINALIZADO','CANCELADO')),
    CONSTRAINT ck_contrato_fechas CHECK ((estado NOT IN ('CONFIRMADO','FINALIZADO') OR confirmado_en IS NOT NULL) AND (estado <> 'FINALIZADO' OR (finalizado_en IS NOT NULL AND finalizado_en >= fin_evento_utc)) AND (estado <> 'CANCELADO' OR (cancelado_en IS NOT NULL AND CHAR_LENGTH(TRIM(COALESCE(motivo_cancelacion,''))) >= 5))),
    INDEX ix_contrato_cliente(id_cliente,estado,aceptado_en),
    INDEX ix_contrato_proveedor(id_proveedor,estado,inicio_evento_utc),
    INDEX ix_contrato_confirmacion(confirmado_en,estado),
    INDEX ix_contrato_evento(inicio_evento_utc,id_servicio)
) ENGINE=InnoDB COMMENT='Aceptacion inmutable; no reserva hasta pago verificado';

CREATE TABLE pago (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de intento financiero',
    id_contrato BIGINT NOT NULL COMMENT 'Contrato propietario',
    id_cliente BIGINT NOT NULL COMMENT 'Pagador coherente con contrato',
    proveedor VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Integracion financiera concreta; aun no seleccionada',
    referencia_externa VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'ID de operacion en proveedor; NULL antes de crear checkout',
    clave_idempotencia VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave unica del intento dentro de contrato',
    huella_solicitud BINARY(32) NOT NULL COMMENT 'SHA256 de entrada canonica asociada a clave',
    modalidad VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Importe determinado por servidor segun saldo',
    metodo VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Metodo reportado por proveedor; no promete Yape o Plin',
    importe_centimos BIGINT NOT NULL COMMENT 'Importe exacto del intento PEN',
    moneda CHAR(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PEN' COMMENT 'Moneda esperada y verificada',
    estado VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Estado interno; separado de respuesta externa',
    estado_externo VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Estado original del proveedor sin reinterpretarlo',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Inicio UTC del intento',
    verificado_en DATETIME(6) NULL COMMENT 'Consulta/autenticacion del resultado con proveedor',
    cobrado_en DATETIME(6) NULL COMMENT 'Evidencia de cobro externo; no implica reserva compatible',
    aplicado_en DATETIME(6) NULL COMMENT 'Instante del abono aplicado al contrato; idempotente',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Version optimista de seguimiento',
    CONSTRAINT fk_pago_contrato FOREIGN KEY(id_contrato,id_cliente) REFERENCES contrato(id,id_cliente) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_pago_clave UNIQUE(id_contrato,clave_idempotencia),
    CONSTRAINT uq_pago_referencia UNIQUE(proveedor,referencia_externa),
    CONSTRAINT uq_pago_contrato UNIQUE(id,id_contrato),
    CONSTRAINT uq_pago_proveedor UNIQUE(id,proveedor),
    CONSTRAINT ck_pago_importe CHECK (importe_centimos BETWEEN 1 AND 999999999999),
    CONSTRAINT ck_pago_moneda CHECK (moneda='PEN'),
    CONSTRAINT ck_pago_modalidad CHECK (modalidad IN ('SALDO_COMPLETO','MITAD_SALDO')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE','PROCESANDO','APROBADO','RECHAZADO','CANCELADO','EXPIRADO','REEMBOLSADO','REEMBOLSO_PENDIENTE','EN_REVISION')),
    CONSTRAINT ck_pago_verificacion CHECK ((cobrado_en IS NULL OR (verificado_en IS NOT NULL AND referencia_externa IS NOT NULL)) AND (aplicado_en IS NULL OR cobrado_en IS NOT NULL) AND (estado <> 'APROBADO' OR aplicado_en IS NOT NULL)),
    INDEX ix_pago_pendiente(estado,creado_en),
    INDEX ix_pago_contrato_fecha(id_contrato,creado_en)
) ENGINE=InnoDB COMMENT='Intentos, cobro externo y aplicacion contractual diferenciados';

CREATE TABLE reserva (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de reserva materializada',
    id_contrato BIGINT NOT NULL COMMENT 'Contrato unico que origina la reserva',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio coherente con contrato',
    estado VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'CONFIRMADA' COMMENT 'Estado operativo de reserva',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Primer abono aplicado UTC',
    cancelada_en DATETIME(6) NULL COMMENT 'Liberacion de intervalo UTC segun politica',
    CONSTRAINT uq_reserva_contrato UNIQUE(id_contrato),
    CONSTRAINT fk_reserva_contrato FOREIGN KEY(id_contrato,id_servicio) REFERENCES contrato(id,id_servicio) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('CONFIRMADA','CANCELADA')),
    CONSTRAINT ck_reserva_cancelacion CHECK (estado <> 'CANCELADA' OR cancelada_en IS NOT NULL)
) ENGINE=InnoDB COMMENT='Una reserva por contrato; fechas derivadas de snapshot contractual';

CREATE TABLE recurso_agenda (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Fila estable de serializacion por recurso',
    id_servicio BIGINT NULL COMMENT 'Recurso del propio servicio',
    id_musico BIGINT NULL COMMENT 'Usuario musico compartido entre bandas/servicio individual',
    CONSTRAINT uq_recurso_servicio UNIQUE(id_servicio),
    CONSTRAINT uq_recurso_musico UNIQUE(id_musico),
    CONSTRAINT fk_recurso_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_recurso_musico FOREIGN KEY(id_musico) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_recurso_origen CHECK ((id_servicio IS NOT NULL) + (id_musico IS NOT NULL) = 1)
) ENGINE=InnoDB COMMENT='Mutex transaccional por servicio o musico; SELECT FOR UPDATE ordenado';
CREATE TABLE bloqueo_agenda (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador del bloqueo manual',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio que bloquea',
    id_actor BIGINT NOT NULL COMMENT 'Actor autorizado en backend',
    inicio_utc DATETIME(6) NOT NULL COMMENT 'Inicio ocupado UTC',
    fin_utc DATETIME(6) NOT NULL COMMENT 'Fin exclusivo UTC',
    motivo VARCHAR(1000) NOT NULL COMMENT 'Motivo privado nunca publicado',
    activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Retiro logico de bloqueo',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
    retirado_en DATETIME(6) NULL COMMENT 'Retiro UTC',
    CONSTRAINT fk_bloqueo_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bloqueo_actor FOREIGN KEY(id_actor) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_bloqueo_intervalo CHECK (inicio_utc < fin_utc),
    CONSTRAINT ck_bloqueo_activo CHECK (activo IN (0,1)),
    INDEX ix_bloqueo_servicio(id_servicio,activo,inicio_utc,fin_utc)
) ENGINE=InnoDB COMMENT='Bloqueo manual requiere protocolo comun de recursos';
CREATE TABLE retencion_agenda (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de retencion tecnica',
    id_contrato BIGINT NOT NULL COMMENT 'Contrato que inicia checkout',
    id_servicio BIGINT NOT NULL COMMENT 'Servicio coherente con contrato',
    id_pago BIGINT NOT NULL COMMENT 'Intento asociado coherente con contrato',
    inicio_utc DATETIME(6) NOT NULL COMMENT 'Inicio con margenes UTC',
    fin_utc DATETIME(6) NOT NULL COMMENT 'Fin con margenes UTC',
    vence_en DATETIME(6) NOT NULL COMMENT 'Caducidad tecnica UTC, nunca reserva confirmada',
    estado VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVA' COMMENT 'Estado de retencion tecnica',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
    CONSTRAINT fk_retencion_contrato FOREIGN KEY(id_contrato,id_servicio) REFERENCES contrato(id,id_servicio) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_retencion_pago FOREIGN KEY(id_pago,id_contrato) REFERENCES pago(id,id_contrato) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_retencion_pago UNIQUE(id_pago),
    CONSTRAINT ck_retencion_intervalo CHECK (inicio_utc < fin_utc AND vence_en > creada_en),
    CONSTRAINT ck_retencion_estado CHECK (estado IN ('ACTIVA','CONVERTIDA','EXPIRADA','CANCELADA')),
    INDEX ix_retencion_vencimiento(estado,vence_en)
) ENGINE=InnoDB COMMENT='Soporte opcional de checkout asincrono; no obliga a habilitarlo';
CREATE TABLE ocupacion_agenda (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador del intervalo por recurso',
    id_recurso BIGINT NOT NULL COMMENT 'Recurso bloqueado antes de consultar o insertar',
    id_reserva BIGINT NULL COMMENT 'Reserva confirmada que origina ocupacion',
    id_bloqueo BIGINT NULL COMMENT 'Bloqueo manual que origina ocupacion',
    id_retencion BIGINT NULL COMMENT 'Retencion tecnica que origina ocupacion',
    inicio_utc DATETIME(6) NOT NULL COMMENT 'Intervalo semiabierto con margenes; snapshot de recurso',
    fin_utc DATETIME(6) NOT NULL COMMENT 'Fin exclusivo de ocupacion UTC',
    vence_en DATETIME(6) NULL COMMENT 'Solo retenciones tienen caducidad tecnica',
    activa BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Desactivar al liberar o convertir en misma transaccion',
    CONSTRAINT fk_ocupacion_recurso FOREIGN KEY(id_recurso) REFERENCES recurso_agenda(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ocupacion_reserva FOREIGN KEY(id_reserva) REFERENCES reserva(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ocupacion_bloqueo FOREIGN KEY(id_bloqueo) REFERENCES bloqueo_agenda(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ocupacion_retencion FOREIGN KEY(id_retencion) REFERENCES retencion_agenda(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_ocupacion_reserva UNIQUE(id_recurso,id_reserva),
    CONSTRAINT uq_ocupacion_bloqueo UNIQUE(id_recurso,id_bloqueo),
    CONSTRAINT uq_ocupacion_retencion UNIQUE(id_recurso,id_retencion),
    CONSTRAINT ck_ocupacion_origen CHECK ((id_reserva IS NOT NULL)+(id_bloqueo IS NOT NULL)+(id_retencion IS NOT NULL)=1),
    CONSTRAINT ck_ocupacion_intervalo CHECK (inicio_utc < fin_utc),
    CONSTRAINT ck_ocupacion_caducidad CHECK ((id_retencion IS NULL AND vence_en IS NULL) OR (id_retencion IS NOT NULL AND vence_en IS NOT NULL)),
    CONSTRAINT ck_ocupacion_activa CHECK (activa IN (0,1)),
    INDEX ix_ocupacion_conflicto(id_recurso,activa,inicio_utc,fin_utc,vence_en)
) ENGINE=InnoDB COMMENT='Ocupacion por recurso; exclusion temporal depende del protocolo transaccional';

CREATE TABLE valoracion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de resena',
    id_contrato BIGINT NOT NULL COMMENT 'Unica valoracion por contrato finalizado',
    puntuacion SMALLINT NOT NULL COMMENT 'Escala operativa 1-5; no instrumento de encuesta',
    comentario VARCHAR(2000) NULL COMMENT 'Comentario autorizado',
    consentimiento_publicacion BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Permiso expreso de publicacion',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Registro UTC',
    CONSTRAINT uq_valoracion_contrato UNIQUE(id_contrato),
    CONSTRAINT fk_valoracion_contrato FOREIGN KEY(id_contrato) REFERENCES contrato(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_valoracion_puntuacion CHECK (puntuacion BETWEEN 1 AND 5),
    CONSTRAINT ck_valoracion_consentimiento CHECK (consentimiento_publicacion IN (0,1))
) ENGINE=InnoDB COMMENT='Valoracion; no se crea encuesta de RF-68 pendiente de conciliacion';
CREATE TABLE notificacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de notificacion interna',
    id_destinatario BIGINT NOT NULL COMMENT 'Usuario unico destinatario',
    tipo VARCHAR(40) CHARACTER SET ascii NOT NULL COMMENT 'Tipo de aviso controlado por backend',
    titulo VARCHAR(180) NOT NULL COMMENT 'Titulo seguro',
    mensaje VARCHAR(2000) NOT NULL COMMENT 'Contenido sin secretos',
    entidad VARCHAR(80) CHARACTER SET ascii NULL COMMENT 'Entidad de destino; validar permiso al navegar',
    identificador_entidad BIGINT NULL COMMENT 'Referencia logica de destino autorizada',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Creacion UTC',
    leida_en DATETIME(6) NULL COMMENT 'Lectura UTC',
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY(id_destinatario) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_notificacion_destinatario(id_destinatario,leida_en,creada_en)
) ENGINE=InnoDB COMMENT='Notificaciones persistentes por destinatario';
CREATE TABLE historial_operacion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de evento auditable',
    id_actor BIGINT NULL COMMENT 'Usuario actor; NULL si sistema',
    id_solicitud BIGINT NULL COMMENT 'Origen opcional de contratacion',
    entidad VARCHAR(80) CHARACTER SET ascii NOT NULL COMMENT 'Tipo de agregado auditado',
    identificador_entidad VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Referencia logica no es FK universal',
    accion VARCHAR(80) CHARACTER SET ascii NOT NULL COMMENT 'Accion realizada',
    detalle JSON NULL COMMENT 'Metadatos redactados sin claves ni datos financieros sensibles',
    correlation_id VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Correlacion tecnica de la operacion',
    ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Instante real UTC del servidor',
    CONSTRAINT fk_historial_actor FOREIGN KEY(id_actor) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_historial_solicitud FOREIGN KEY(id_solicitud) REFERENCES solicitud_cotizacion(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX ix_historial_fecha(ocurrido_en,entidad),
    INDEX ix_historial_solicitud(id_solicitud,ocurrido_en)
) ENGINE=InnoDB COMMENT='Auditoria conservada al cancelar o despublicar';

CREATE TABLE sesion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de sesion de usuario',
    id_usuario BIGINT NOT NULL COMMENT 'Titular autenticado',
    familia CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Familia de rotacion para revocar reuso',
    refresh_hash BINARY(32) NOT NULL COMMENT 'Hash SHA256 de token aleatorio; nunca token en claro',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Emision UTC',
    vence_en DATETIME(6) NOT NULL COMMENT 'Caducidad UTC',
    consumida_en DATETIME(6) NULL COMMENT 'Uso del refresh; conservar para detectar reuso',
    revocada_en DATETIME(6) NULL COMMENT 'Revocacion UTC',
    CONSTRAINT uq_sesion_refresh UNIQUE(refresh_hash),
    CONSTRAINT fk_sesion_usuario FOREIGN KEY(id_usuario) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_sesion_vigencia CHECK (vence_en > creada_en),
    INDEX ix_sesion_usuario(id_usuario,revocada_en,vence_en),
    INDEX ix_sesion_familia(familia)
) ENGINE=InnoDB COMMENT='Rotacion y revocacion futura de sesiones reales';
CREATE TABLE recuperacion_acceso (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de recuperacion de acceso',
    id_usuario BIGINT NOT NULL COMMENT 'Cuenta destinataria',
    token_hash BINARY(32) NOT NULL COMMENT 'Hash de token aleatorio de un solo uso',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Emision UTC',
    vence_en DATETIME(6) NOT NULL COMMENT 'Caducidad UTC',
    utilizada_en DATETIME(6) NULL COMMENT 'Consumo UTC',
    CONSTRAINT uq_recuperacion_token UNIQUE(token_hash),
    CONSTRAINT fk_recuperacion_usuario FOREIGN KEY(id_usuario) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_recuperacion_vigencia CHECK (vence_en > creada_en),
    INDEX ix_recuperacion_usuario(id_usuario,vence_en)
) ENGINE=InnoDB COMMENT='Recuperacion segura, sin guardar credenciales';
CREATE TABLE evento_pago (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de recepcion de webhook',
    proveedor VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Proveedor autenticado',
    id_evento_externo VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador deduplicado del evento oficial',
    id_pago BIGINT NULL COMMENT 'Intento correlacionado; puede llegar antes de correlacionar',
    payload_hash BINARY(32) NOT NULL COMMENT 'Huella del cuerpo recibido; no almacena tarjeta ni payload completo',
    autenticado_en DATETIME(6) NOT NULL COMMENT 'Validacion de firma oficial antes de persistir como evento confiable',
    recibido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Recepcion UTC',
    procesado_en DATETIME(6) NULL COMMENT 'Aplicacion o conciliacion UTC',
    estado VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'RECIBIDO' COMMENT 'Estado de procesamiento interno',
    intentos INT NOT NULL DEFAULT 0 COMMENT 'Numero de reintentos de verificacion',
    ultimo_error_codigo VARCHAR(80) CHARACTER SET ascii NULL COMMENT 'Codigo tecnico sin secretos',
    CONSTRAINT uq_evento_pago_externo UNIQUE(proveedor,id_evento_externo),
    CONSTRAINT fk_evento_pago FOREIGN KEY(id_pago) REFERENCES pago(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_evento_pago_estado CHECK (estado IN ('RECIBIDO','VERIFICANDO','APLICADO','IGNORADO','EN_REVISION')),
    CONSTRAINT ck_evento_pago_intentos CHECK (intentos >= 0),
    INDEX ix_evento_pago_pendiente(estado,recibido_en)
) ENGINE=InnoDB COMMENT='Bandeja de eventos autenticados; API del proveedor verifica el cobro';
CREATE TABLE reembolso (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de solicitud de devolucion',
    id_pago BIGINT NOT NULL COMMENT 'Intento de cobro original',
    proveedor VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Proveedor coherente con pago; deduplicacion de devolucion externa',
    id_contrato BIGINT NOT NULL COMMENT 'Contrato coherente con pago',
    clave_idempotencia VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave de solicitud por pago',
    referencia_externa VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Referencia de devolucion en proveedor',
    importe_centimos BIGINT NOT NULL COMMENT 'Importe exacto solicitado PEN',
    motivo VARCHAR(1000) NOT NULL COMMENT 'Motivo autorizado de devolucion',
    estado VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Confirmar con proveedor, no con rollback SQL',
    solicitado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Solicitud UTC',
    confirmado_en DATETIME(6) NULL COMMENT 'Confirmacion externa UTC',
    CONSTRAINT fk_reembolso_pago FOREIGN KEY(id_pago,id_contrato) REFERENCES pago(id,id_contrato) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uq_reembolso_clave UNIQUE(id_pago,clave_idempotencia),
    CONSTRAINT uq_reembolso_externo UNIQUE(proveedor,referencia_externa),
    CONSTRAINT fk_reembolso_proveedor FOREIGN KEY(id_pago,proveedor) REFERENCES pago(id,proveedor) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_reembolso_importe CHECK (importe_centimos BETWEEN 1 AND 999999999999),
    CONSTRAINT ck_reembolso_estado CHECK (estado IN ('PENDIENTE','PROCESANDO','APROBADO','RECHAZADO')),
    CONSTRAINT ck_reembolso_confirmacion CHECK (estado <> 'APROBADO' OR (confirmado_en IS NOT NULL AND referencia_externa IS NOT NULL)),
    INDEX ix_reembolso_pendiente(estado,solicitado_en)
) ENGINE=InnoDB COMMENT='Devoluciones externas idempotentes y conciliables';
CREATE TABLE incidencia_pago (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de incidencia financiera',
    id_pago BIGINT NOT NULL COMMENT 'Pago afectado',
    id_contrato BIGINT NOT NULL COMMENT 'Contrato coherente con pago',
    tipo VARCHAR(50) CHARACTER SET ascii NOT NULL COMMENT 'Conflicto, cobro tardio o error de conciliacion',
    estado VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ABIERTA' COMMENT 'Estado de resolucion humana/automatica',
    detalle VARCHAR(2000) NOT NULL COMMENT 'Detalle sin instrumentos financieros sensibles',
    creada_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Deteccion UTC',
    resuelta_en DATETIME(6) NULL COMMENT 'Resolucion UTC',
    CONSTRAINT fk_incidencia_pago FOREIGN KEY(id_pago,id_contrato) REFERENCES pago(id,id_contrato) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_incidencia_estado CHECK (estado IN ('ABIERTA','EN_REVISION','RESUELTA')),
    CONSTRAINT ck_incidencia_resuelta CHECK (estado <> 'RESUELTA' OR resuelta_en IS NOT NULL),
    INDEX ix_incidencia_estado(estado,creada_en)
) ENGINE=InnoDB COMMENT='Cobro externo no aplicado y otras incidencias auditables';
CREATE TABLE evento_medicion (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de evento de medicion consentida',
    sesion_medicion CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador pseudonimo de sesion; no cuenta personal',
    clave_evento CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Identificador idempotente de evento',
    id_servicio BIGINT NULL COMMENT 'Servicio publico consultado, cuando corresponda',
    accion VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Inicio voluntario, consulta o fin',
    consentimiento_version VARCHAR(80) CHARACTER SET ascii NOT NULL COMMENT 'Version informada de consentimiento expreso',
    ocurrido_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Hora del servidor para indicadores',
    CONSTRAINT uq_medicion_evento UNIQUE(clave_evento),
    CONSTRAINT fk_medicion_servicio FOREIGN KEY(id_servicio) REFERENCES servicio_musical(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT ck_medicion_accion CHECK (accion IN ('INICIO','PERFIL','FIN')),
    CONSTRAINT ck_medicion_perfil CHECK (accion <> 'PERFIL' OR id_servicio IS NOT NULL),
    INDEX ix_medicion_sesion(sesion_medicion,ocurrido_en)
) ENGINE=InnoDB COMMENT='Eventos minimos de busqueda; no encuesta ni direccion privada';
CREATE TABLE evento_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador de evento transaccional de salida',
    clave_evento CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Clave estable de entrega idempotente',
    tipo VARCHAR(80) CHARACTER SET ascii NOT NULL COMMENT 'Tipo de notificacion o integracion',
    entidad VARCHAR(80) CHARACTER SET ascii NOT NULL COMMENT 'Agregado de origen',
    identificador_entidad BIGINT NOT NULL COMMENT 'ID logico del agregado',
    payload JSON NOT NULL COMMENT 'Datos minimos redactados del evento',
    estado VARCHAR(12) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDIENTE' COMMENT 'Estado de entrega',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Commit del hecho de negocio UTC',
    disponible_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Proximo intento UTC',
    enviado_en DATETIME(6) NULL COMMENT 'Confirmacion de entrega UTC',
    intentos INT NOT NULL DEFAULT 0 COMMENT 'Reintentos de entrega',
    CONSTRAINT uq_outbox_clave UNIQUE(clave_evento),
    CONSTRAINT ck_outbox_estado CHECK (estado IN ('PENDIENTE','PROCESANDO','ENVIADO','ERROR')),
    CONSTRAINT ck_outbox_intentos CHECK (intentos >= 0),
    INDEX ix_outbox_pendiente(estado,disponible_en,id)
) ENGINE=InnoDB COMMENT='Salida fiable: escritura con negocio y entrega posterior';
