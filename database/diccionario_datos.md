# Diccionario físico MySQL — Etapa 2

Actualización Etapa 3: este inventario describe V004. La extensión vigente V005 y las dos tablas nuevas se documentan en [autenticacion_etapa3.md](autenticacion_etapa3.md). El backend aplica ahora cinco migraciones.

Generado a partir de INFORMATION_SCHEMA de MySQL 8.0.30, ejecución 2026-11-03T19:52:55.148102+00:00. La evidencia y los SHA-256 de migraciones están en tests/evidence/resultado_mysql.json.

Alcance: 30 tablas, 276 columnas físicas y 1 vista. Los valores mostrados son definición de esquema, no registros de personas. Room conserva un esquema independiente v1 descrito al final.

Convenciones: importes BIGINT en céntimos PEN, cantidades DECIMAL(10,3), fechas DATETIME(6) en UTC salvo TIME de horario recurrente local America/Lima. Las filas con FK usan ON DELETE/UPDATE RESTRICT; la validación de roles/propietarios del actor corresponde al backend. Claves financieras en ascii_bin; correo normalizado sensible a acentos. Las columnas calculadas no se escriben desde el backend.

## bloqueo_agenda

Bloqueo manual requiere protocolo comun de recursos

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador del bloqueo manual |
| id_servicio | bigint | NO | MUL | Sin valor explícito | Servicio que bloquea |
| id_actor | bigint | NO | MUL | Sin valor explícito | Actor autorizado en backend |
| inicio_utc | datetime(6) | NO | — | Sin valor explícito | Inicio ocupado UTC |
| fin_utc | datetime(6) | NO | — | Sin valor explícito | Fin exclusivo UTC |
| motivo | varchar(1000) | NO | — | Sin valor explícito | Motivo privado nunca publicado |
| activo | tinyint(1) | NO | — | 1 | Retiro logico de bloqueo |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC |
| retirado_en | datetime(6) | YES | — | NULL | Retiro UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_bloqueo_actor | INDEX | id_actor |
| ix_bloqueo_servicio | INDEX | id_servicio, activo, inicio_utc, fin_utc |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_bloqueo_actor | (id_actor) → usuario(id) | RESTRICT / RESTRICT |
| fk_bloqueo_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_bloqueo_activo | (activo in (0,1)) |
| ck_bloqueo_intervalo | (inicio_utc < fin_utc) |

## contrato

Aceptacion inmutable; no reserva hasta pago verificado

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador oficial del contrato |
| codigo | varchar(50) | NO | UNI | Sin valor explícito | Codigo visible unico |
| id_cotizacion | bigint | NO | UNI | Sin valor explícito | Version exacta aceptada |
| id_solicitud | bigint | NO | UNI | Sin valor explícito | Solicitud unica aun entre distintas versiones |
| id_servicio | bigint | NO | — | Sin valor explícito | Servicio historico coherente con solicitud |
| id_cliente | bigint | NO | MUL | Sin valor explícito | Cliente historico coherente con solicitud |
| id_proveedor | bigint | NO | MUL | Sin valor explícito | Proveedor historico coherente con solicitud |
| inicio_evento_utc | datetime(6) | NO | MUL | Sin valor explícito | Inicio aceptado UTC |
| fin_evento_utc | datetime(6) | NO | — | Sin valor explícito | Fin exclusivo aceptado UTC |
| inicio_ocupacion_utc | datetime(6) | NO | — | Sin valor explícito | Inicio con margen de preparacion incluido |
| fin_ocupacion_utc | datetime(6) | NO | — | Sin valor explícito | Fin con margen posterior incluido |
| total_centimos | bigint | NO | — | Sin valor explícito | Total historico exacto PEN |
| condiciones_aceptadas | tinyint(1) | NO | — | Sin valor explícito | Aceptacion expresa obligatoria |
| politica_version | varchar(80) | NO | — | Sin valor explícito | Version exacta aceptada |
| snapshot | json | NO | — | Sin valor explícito | Copia historica de perfil, actores, conceptos, recursos y politicas |
| clave_idempotencia | varchar(100) | NO | — | Sin valor explícito | Clave de aceptacion por cliente |
| huella_aceptacion | binary(32) | NO | — | Sin valor explícito | SHA256 de entrada canonica para detectar reutilizacion de clave |
| estado | varchar(20) | NO | — | PENDIENTE_PAGO | Estado contractual independiente del proveedor financiero |
| version | bigint | NO | — | 0 | Version optimista de estado |
| aceptado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Aceptacion UTC |
| confirmado_en | datetime(6) | YES | MUL | NULL | Primer abono verificado UTC |
| finalizado_en | datetime(6) | YES | — | NULL | Finalizacion despues del evento UTC |
| cancelado_en | datetime(6) | YES | — | NULL | Cancelacion UTC |
| motivo_cancelacion | varchar(1000) | YES | — | NULL | Motivo conservado segun politica |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_contrato_contexto | INDEX | id_solicitud, id_servicio, id_cliente, id_proveedor |
| fk_contrato_cotizacion | INDEX | id_cotizacion, id_solicitud |
| ix_contrato_cliente | INDEX | id_cliente, estado, aceptado_en |
| ix_contrato_confirmacion | INDEX | confirmado_en, estado |
| ix_contrato_evento | INDEX | inicio_evento_utc, id_servicio |
| ix_contrato_proveedor | INDEX | id_proveedor, estado, inicio_evento_utc |
| uq_contrato_aceptacion | UNIQUE | id_cliente, clave_idempotencia |
| uq_contrato_cliente | UNIQUE | id, id_cliente |
| uq_contrato_codigo | UNIQUE | codigo |
| uq_contrato_cotizacion | UNIQUE | id_cotizacion |
| uq_contrato_servicio | UNIQUE | id, id_servicio |
| uq_contrato_solicitud | UNIQUE | id_solicitud |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_contrato_contexto | (id_solicitud, id_servicio, id_cliente, id_proveedor) → solicitud_cotizacion(id, id_servicio, id_cliente, id_proveedor) | RESTRICT / RESTRICT |
| fk_contrato_cotizacion | (id_cotizacion, id_solicitud) → cotizacion(id, id_solicitud) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_contrato_aceptacion | ((condiciones_aceptadas = 1) and (json_type(snapshot) = _utf8mb4\'OBJECT\')) |
| ck_contrato_estado | (estado in (_utf8mb4\'PENDIENTE_PAGO\',_utf8mb4\'CONFIRMADO\',_utf8mb4\'FINALIZADO\',_utf8mb4\'CANCELADO\')) |
| ck_contrato_fechas | (((estado not in (_utf8mb4\'CONFIRMADO\',_utf8mb4\'FINALIZADO\')) or (confirmado_en is not null)) and ((estado <> _utf8mb4\'FINALIZADO\') or ((finalizado_en is not null) and (finalizado_en >= fin_evento_utc))) and ((estado <> _utf8mb4\'CANCELADO\') or ((cancelado_en is not null) and (char_length(trim(coalesce(motivo_cancelacion,_utf8mb4\'\'))) >= 5)))) |
| ck_contrato_intervalo | ((inicio_evento_utc < fin_evento_utc) and (inicio_ocupacion_utc <= inicio_evento_utc) and (fin_ocupacion_utc >= fin_evento_utc)) |
| ck_contrato_total | (total_centimos between 1 and 999999999999) |

## cotizacion

Versiones historicas de propuesta economica

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de version |
| id_solicitud | bigint | NO | MUL | Sin valor explícito | Solicitud que agrupa versiones |
| numero_version | int | NO | — | Sin valor explícito | Consecutivo dentro de la solicitud |
| inicio_propuesto_utc | datetime(6) | NO | — | Sin valor explícito | Inicio propuesto UTC |
| fin_propuesto_utc | datetime(6) | NO | — | Sin valor explícito | Fin propuesto UTC |
| subtotal_centimos | bigint | NO | — | Sin valor explícito | Suma exacta de conceptos; backend reconcilia |
| descuento_centimos | bigint | NO | — | 0 | Descuento exacto no mayor que subtotal |
| total_centimos | bigint | YES | — | (subtotal_centimos - descuento_centimos) | Total exacto en centimos PEN |
| condiciones | text | NO | — | Sin valor explícito | Condiciones versionadas del servicio |
| politica_cancelacion | text | NO | — | Sin valor explícito | Politica propuesta para aceptacion expresa |
| politica_version | varchar(80) | NO | — | Sin valor explícito | Identificador de politica aceptable |
| vigente_hasta_utc | datetime(6) | NO | — | Sin valor explícito | Caducidad de la propuesta |
| estado | varchar(16) | NO | — | BORRADOR | Estado de version, no edicion de propuesta enviada |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| uq_cotizacion_solicitud | UNIQUE | id, id_solicitud |
| uq_cotizacion_version | UNIQUE | id_solicitud, numero_version |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_cotizacion_solicitud | (id_solicitud) → solicitud_cotizacion(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_cotizacion_estado | (estado in (_utf8mb4\'BORRADOR\',_utf8mb4\'ENVIADA\',_utf8mb4\'ACEPTADA\',_utf8mb4\'RECHAZADA\',_utf8mb4\'CAMBIOS\',_utf8mb4\'VENCIDA\',_utf8mb4\'SUSTITUIDA\')) |
| ck_cotizacion_importes | ((subtotal_centimos between 1 and 999999999999) and (descuento_centimos >= 0) and (descuento_centimos < subtotal_centimos)) |
| ck_cotizacion_intervalo | ((inicio_propuesto_utc < fin_propuesto_utc) and (fin_propuesto_utc <= (inicio_propuesto_utc + interval 24 hour))) |
| ck_cotizacion_version | (numero_version > 0) |
| ck_cotizacion_vigencia | (vigente_hasta_utc > creada_en) |

## detalle_cotizacion

Desglose exacto sin float o double

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador del concepto |
| id_cotizacion | bigint | NO | MUL | Sin valor explícito | Version propietaria |
| concepto | varchar(300) | NO | — | Sin valor explícito | Descripcion del concepto economico |
| cantidad | decimal(10,3) | NO | — | Sin valor explícito | Cantidad exacta, hasta tres decimales |
| precio_unitario_centimos | bigint | NO | — | Sin valor explícito | Precio exacto unitario en centimos PEN |
| total_centimos | bigint | YES | — | cast(round((cantidad * precio_unitario_centimos),0) as signed) | Redondeo decimal por concepto: mitad hacia arriba |
| orden | int | NO | — | 0 | Orden del desglose |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_detalle_cotizacion | INDEX | id_cotizacion, orden |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_detalle_cotizacion | (id_cotizacion) → cotizacion(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_detalle_cantidad | ((cantidad > 0) and (cantidad <= 1000000)) |
| ck_detalle_orden | (orden >= 0) |
| ck_detalle_precio | (precio_unitario_centimos between 0 and 999999999999) |
| ck_detalle_total | (total_centimos between 0 and 999999999999) |

## evento_medicion

Eventos minimos de busqueda; no encuesta ni direccion privada

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de evento de medicion consentida |
| sesion_medicion | char(36) | NO | MUL | Sin valor explícito | Identificador pseudonimo de sesion; no cuenta personal |
| clave_evento | char(36) | NO | UNI | Sin valor explícito | Identificador idempotente de evento |
| id_servicio | bigint | YES | MUL | NULL | Servicio publico consultado, cuando corresponda |
| accion | varchar(12) | NO | — | Sin valor explícito | Inicio voluntario, consulta o fin |
| consentimiento_version | varchar(80) | NO | — | Sin valor explícito | Version informada de consentimiento expreso |
| ocurrido_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Hora del servidor para indicadores |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_medicion_servicio | INDEX | id_servicio |
| ix_medicion_sesion | INDEX | sesion_medicion, ocurrido_en |
| uq_medicion_evento | UNIQUE | clave_evento |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_medicion_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_medicion_accion | (accion in (_utf8mb4\'INICIO\',_utf8mb4\'PERFIL\',_utf8mb4\'FIN\')) |
| ck_medicion_perfil | ((accion <> _utf8mb4\'PERFIL\') or (id_servicio is not null)) |

## evento_outbox

Salida fiable: escritura con negocio y entrega posterior

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de evento transaccional de salida |
| clave_evento | char(36) | NO | UNI | Sin valor explícito | Clave estable de entrega idempotente |
| tipo | varchar(80) | NO | — | Sin valor explícito | Tipo de notificacion o integracion |
| entidad | varchar(80) | NO | — | Sin valor explícito | Agregado de origen |
| identificador_entidad | bigint | NO | — | Sin valor explícito | ID logico del agregado |
| payload | json | NO | — | Sin valor explícito | Datos minimos redactados del evento |
| estado | varchar(12) | NO | MUL | PENDIENTE | Estado de entrega |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Commit del hecho de negocio UTC |
| disponible_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Proximo intento UTC |
| enviado_en | datetime(6) | YES | — | NULL | Confirmacion de entrega UTC |
| intentos | int | NO | — | 0 | Reintentos de entrega |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_outbox_pendiente | INDEX | estado, disponible_en, id |
| uq_outbox_clave | UNIQUE | clave_evento |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_outbox_estado | (estado in (_utf8mb4\'PENDIENTE\',_utf8mb4\'PROCESANDO\',_utf8mb4\'ENVIADO\',_utf8mb4\'ERROR\')) |
| ck_outbox_intentos | (intentos >= 0) |

## evento_pago

Bandeja de eventos autenticados; API del proveedor verifica el cobro

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de recepcion de webhook |
| proveedor | varchar(50) | NO | MUL | Sin valor explícito | Proveedor autenticado |
| id_evento_externo | varchar(150) | NO | — | Sin valor explícito | Identificador deduplicado del evento oficial |
| id_pago | bigint | YES | MUL | NULL | Intento correlacionado; puede llegar antes de correlacionar |
| payload_hash | binary(32) | NO | — | Sin valor explícito | Huella del cuerpo recibido; no almacena tarjeta ni payload completo |
| autenticado_en | datetime(6) | NO | — | Sin valor explícito | Validacion de firma oficial antes de persistir como evento confiable |
| recibido_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Recepcion UTC |
| procesado_en | datetime(6) | YES | — | NULL | Aplicacion o conciliacion UTC |
| estado | varchar(16) | NO | MUL | RECIBIDO | Estado de procesamiento interno |
| intentos | int | NO | — | 0 | Numero de reintentos de verificacion |
| ultimo_error_codigo | varchar(80) | YES | — | NULL | Codigo tecnico sin secretos |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_evento_pago | INDEX | id_pago |
| ix_evento_pago_pendiente | INDEX | estado, recibido_en |
| uq_evento_pago_externo | UNIQUE | proveedor, id_evento_externo |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_evento_pago | (id_pago) → pago(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_evento_pago_estado | (estado in (_utf8mb4\'RECIBIDO\',_utf8mb4\'VERIFICANDO\',_utf8mb4\'APLICADO\',_utf8mb4\'IGNORADO\',_utf8mb4\'EN_REVISION\')) |
| ck_evento_pago_intentos | (intentos >= 0) |

## historial_operacion

Auditoria conservada al cancelar o despublicar

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de evento auditable |
| id_actor | bigint | YES | MUL | NULL | Usuario actor; NULL si sistema |
| id_solicitud | bigint | YES | MUL | NULL | Origen opcional de contratacion |
| entidad | varchar(80) | NO | — | Sin valor explícito | Tipo de agregado auditado |
| identificador_entidad | varchar(100) | NO | — | Sin valor explícito | Referencia logica no es FK universal |
| accion | varchar(80) | NO | — | Sin valor explícito | Accion realizada |
| detalle | json | YES | — | NULL | Metadatos redactados sin claves ni datos financieros sensibles |
| correlation_id | varchar(80) | NO | — | Sin valor explícito | Correlacion tecnica de la operacion |
| ocurrido_en | datetime(6) | NO | MUL | CURRENT_TIMESTAMP(6) | Instante real UTC del servidor |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_historial_actor | INDEX | id_actor |
| ix_historial_fecha | INDEX | ocurrido_en, entidad |
| ix_historial_solicitud | INDEX | id_solicitud, ocurrido_en |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_historial_actor | (id_actor) → usuario(id) | RESTRICT / RESTRICT |
| fk_historial_solicitud | (id_solicitud) → solicitud_cotizacion(id) | RESTRICT / RESTRICT |

## horario_atencion

Franjas recurrentes; cruzar medianoche requiere dos franjas

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador del intervalo recurrente |
| id_servicio | bigint | NO | MUL | Sin valor explícito | Servicio configurado |
| dia_semana | smallint | NO | — | Sin valor explícito | ISO lunes=1 a domingo=7; hora local America/Lima |
| hora_inicio | time | NO | — | Sin valor explícito | Apertura local |
| hora_fin | time | NO | — | Sin valor explícito | Cierre local el mismo dia |
| margen_previo_min | smallint | NO | — | 0 | Preparacion antes del evento |
| margen_posterior_min | smallint | NO | — | 0 | Preparacion despues del evento |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| uq_horario_intervalo | UNIQUE | id_servicio, dia_semana, hora_inicio, hora_fin |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_horario_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_horario_dia | (dia_semana between 1 and 7) |
| ck_horario_horas | ((hora_inicio >= _utf8mb4\'00:00:00\') and (hora_inicio < hora_fin) and (hora_fin <= _utf8mb4\'24:00:00\')) |
| ck_horario_margen | ((margen_previo_min between 0 and 1440) and (margen_posterior_min between 0 and 1440)) |

## imagen_servicio

Metadatos de imagen; archivo fuera de MySQL

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de imagen |
| id_servicio | bigint | NO | MUL | Sin valor explícito | Servicio propietario |
| ruta_objeto | varchar(500) | NO | — | Sin valor explícito | Ruta del almacenamiento externo, no URL firmada permanente |
| mime | varchar(80) | NO | — | Sin valor explícito | MIME validado en servidor |
| tamano_bytes | bigint | NO | — | Sin valor explícito | Tamano validado del archivo |
| descripcion | varchar(300) | YES | — | NULL | Descripcion accesible |
| orden | int | NO | — | 0 | Orden en galeria |
| es_portada | tinyint(1) | NO | — | 0 | Marca de portada |
| servicio_portada | bigint | YES | UNI | (case when (es_portada = 1) then id_servicio else NULL end) | Una portada por servicio |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Carga UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_imagen_servicio | INDEX | id_servicio, orden |
| uq_imagen_portada | UNIQUE | servicio_portada |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_imagen_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_imagen_orden | (orden >= 0) |
| ck_imagen_portada | (es_portada in (0,1)) |
| ck_imagen_tamano | (tamano_bytes > 0) |

## incidencia_pago

Cobro externo no aplicado y otras incidencias auditables

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de incidencia financiera |
| id_pago | bigint | NO | MUL | Sin valor explícito | Pago afectado |
| id_contrato | bigint | NO | — | Sin valor explícito | Contrato coherente con pago |
| tipo | varchar(50) | NO | — | Sin valor explícito | Conflicto, cobro tardio o error de conciliacion |
| estado | varchar(12) | NO | MUL | ABIERTA | Estado de resolucion humana/automatica |
| detalle | varchar(2000) | NO | — | Sin valor explícito | Detalle sin instrumentos financieros sensibles |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Deteccion UTC |
| resuelta_en | datetime(6) | YES | — | NULL | Resolucion UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_incidencia_pago | INDEX | id_pago, id_contrato |
| ix_incidencia_estado | INDEX | estado, creada_en |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_incidencia_pago | (id_pago, id_contrato) → pago(id, id_contrato) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_incidencia_estado | (estado in (_utf8mb4\'ABIERTA\',_utf8mb4\'EN_REVISION\',_utf8mb4\'RESUELTA\')) |
| ck_incidencia_resuelta | ((estado <> _utf8mb4\'RESUELTA\') or (resuelta_en is not null)) |

## integrante

Contenido normalizado y vinculo de recurso compartido

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador del integrante |
| id_servicio | bigint | NO | MUL | Sin valor explícito | Banda propietaria; tipo validado por Service |
| id_solista_vinculado | bigint | YES | MUL | NULL | Usuario solista invitado; aceptacion requerida |
| nombre | varchar(180) | NO | — | Sin valor explícito | Nombre publico autorizado |
| instrumento | varchar(100) | NO | — | Sin valor explícito | Instrumento o funcion musical |
| funcion | varchar(120) | YES | — | NULL | Funcion dentro de banda |
| estado_vinculo | varchar(16) | NO | — | SIN_VINCULO | Estado del consentimiento de vinculo |
| activo | tinyint(1) | NO | — | 1 | Baja logica conserva referencias historicas |
| version | bigint | NO | — | 0 | Version optimista |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_integrante_solista | INDEX | id_solista_vinculado |
| ix_integrante_servicio | INDEX | id_servicio, activo |
| uq_integrante_vinculo | UNIQUE | id_servicio, id_solista_vinculado |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_integrante_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |
| fk_integrante_solista | (id_solista_vinculado) → usuario(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_integrante_activo | (activo in (0,1)) |
| ck_integrante_vinculo | (((id_solista_vinculado is null) and (estado_vinculo = _utf8mb4\'SIN_VINCULO\')) or ((id_solista_vinculado is not null) and (estado_vinculo in (_utf8mb4\'INVITADO\',_utf8mb4\'ACEPTADO\',_utf8mb4\'RECHAZADO\',_utf8mb4\'RETIRADO\')))) |

## notificacion

Notificaciones persistentes por destinatario

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de notificacion interna |
| id_destinatario | bigint | NO | MUL | Sin valor explícito | Usuario unico destinatario |
| tipo | varchar(40) | NO | — | Sin valor explícito | Tipo de aviso controlado por backend |
| titulo | varchar(180) | NO | — | Sin valor explícito | Titulo seguro |
| mensaje | varchar(2000) | NO | — | Sin valor explícito | Contenido sin secretos |
| entidad | varchar(80) | YES | — | NULL | Entidad de destino; validar permiso al navegar |
| identificador_entidad | bigint | YES | — | NULL | Referencia logica de destino autorizada |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC |
| leida_en | datetime(6) | YES | — | NULL | Lectura UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_notificacion_destinatario | INDEX | id_destinatario, leida_en, creada_en |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_notificacion_usuario | (id_destinatario) → usuario(id) | RESTRICT / RESTRICT |

## ocupacion_agenda

Ocupacion por recurso; exclusion temporal depende del protocolo transaccional

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador del intervalo por recurso |
| id_recurso | bigint | NO | MUL | Sin valor explícito | Recurso bloqueado antes de consultar o insertar |
| id_reserva | bigint | YES | MUL | NULL | Reserva confirmada que origina ocupacion |
| id_bloqueo | bigint | YES | MUL | NULL | Bloqueo manual que origina ocupacion |
| id_retencion | bigint | YES | MUL | NULL | Retencion tecnica que origina ocupacion |
| inicio_utc | datetime(6) | NO | — | Sin valor explícito | Intervalo semiabierto con margenes; snapshot de recurso |
| fin_utc | datetime(6) | NO | — | Sin valor explícito | Fin exclusivo de ocupacion UTC |
| vence_en | datetime(6) | YES | — | NULL | Solo retenciones tienen caducidad tecnica |
| activa | tinyint(1) | NO | — | 1 | Desactivar al liberar o convertir en misma transaccion |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_ocupacion_bloqueo | INDEX | id_bloqueo |
| fk_ocupacion_reserva | INDEX | id_reserva |
| fk_ocupacion_retencion | INDEX | id_retencion |
| ix_ocupacion_conflicto | INDEX | id_recurso, activa, inicio_utc, fin_utc, vence_en |
| uq_ocupacion_bloqueo | UNIQUE | id_recurso, id_bloqueo |
| uq_ocupacion_reserva | UNIQUE | id_recurso, id_reserva |
| uq_ocupacion_retencion | UNIQUE | id_recurso, id_retencion |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_ocupacion_bloqueo | (id_bloqueo) → bloqueo_agenda(id) | RESTRICT / RESTRICT |
| fk_ocupacion_recurso | (id_recurso) → recurso_agenda(id) | RESTRICT / RESTRICT |
| fk_ocupacion_reserva | (id_reserva) → reserva(id) | RESTRICT / RESTRICT |
| fk_ocupacion_retencion | (id_retencion) → retencion_agenda(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_ocupacion_activa | (activa in (0,1)) |
| ck_ocupacion_caducidad | (((id_retencion is null) and (vence_en is null)) or ((id_retencion is not null) and (vence_en is not null))) |
| ck_ocupacion_intervalo | (inicio_utc < fin_utc) |
| ck_ocupacion_origen | ((((id_reserva is not null) + (id_bloqueo is not null)) + (id_retencion is not null)) = 1) |

## pago

Intentos, cobro externo y aplicacion contractual diferenciados

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de intento financiero |
| id_contrato | bigint | NO | MUL | Sin valor explícito | Contrato propietario |
| id_cliente | bigint | NO | — | Sin valor explícito | Pagador coherente con contrato |
| proveedor | varchar(50) | NO | MUL | Sin valor explícito | Integracion financiera concreta; aun no seleccionada |
| referencia_externa | varchar(150) | YES | — | NULL | ID de operacion en proveedor; NULL antes de crear checkout |
| clave_idempotencia | varchar(100) | NO | — | Sin valor explícito | Clave unica del intento dentro de contrato |
| huella_solicitud | binary(32) | NO | — | Sin valor explícito | SHA256 de entrada canonica asociada a clave |
| modalidad | varchar(20) | NO | — | Sin valor explícito | Importe determinado por servidor segun saldo |
| metodo | varchar(40) | YES | — | NULL | Metodo reportado por proveedor; no promete Yape o Plin |
| importe_centimos | bigint | NO | — | Sin valor explícito | Importe exacto del intento PEN |
| moneda | char(3) | NO | — | PEN | Moneda esperada y verificada |
| estado | varchar(24) | NO | MUL | PENDIENTE | Estado interno; separado de respuesta externa |
| estado_externo | varchar(80) | YES | — | NULL | Estado original del proveedor sin reinterpretarlo |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Inicio UTC del intento |
| verificado_en | datetime(6) | YES | — | NULL | Consulta/autenticacion del resultado con proveedor |
| cobrado_en | datetime(6) | YES | — | NULL | Evidencia de cobro externo; no implica reserva compatible |
| aplicado_en | datetime(6) | YES | — | NULL | Instante del abono aplicado al contrato; idempotente |
| version | bigint | NO | — | 0 | Version optimista de seguimiento |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_pago_contrato | INDEX | id_contrato, id_cliente |
| ix_pago_contrato_fecha | INDEX | id_contrato, creado_en |
| ix_pago_pendiente | INDEX | estado, creado_en |
| uq_pago_clave | UNIQUE | id_contrato, clave_idempotencia |
| uq_pago_contrato | UNIQUE | id, id_contrato |
| uq_pago_proveedor | UNIQUE | id, proveedor |
| uq_pago_referencia | UNIQUE | proveedor, referencia_externa |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_pago_contrato | (id_contrato, id_cliente) → contrato(id, id_cliente) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_pago_estado | (estado in (_utf8mb4\'PENDIENTE\',_utf8mb4\'PROCESANDO\',_utf8mb4\'APROBADO\',_utf8mb4\'RECHAZADO\',_utf8mb4\'CANCELADO\',_utf8mb4\'EXPIRADO\',_utf8mb4\'REEMBOLSADO\',_utf8mb4\'REEMBOLSO_PENDIENTE\',_utf8mb4\'EN_REVISION\')) |
| ck_pago_importe | (importe_centimos between 1 and 999999999999) |
| ck_pago_modalidad | (modalidad in (_utf8mb4\'SALDO_COMPLETO\',_utf8mb4\'MITAD_SALDO\')) |
| ck_pago_moneda | (moneda = _utf8mb4\'PEN\') |
| ck_pago_verificacion | (((cobrado_en is null) or ((verificado_en is not null) and (referencia_externa is not null))) and ((aplicado_en is null) or (cobrado_en is not null)) and ((estado <> _utf8mb4\'APROBADO\') or (aplicado_en is not null))) |

## recuperacion_acceso

Recuperacion segura, sin guardar credenciales

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de recuperacion de acceso |
| id_usuario | bigint | NO | MUL | Sin valor explícito | Cuenta destinataria |
| token_hash | binary(32) | NO | UNI | Sin valor explícito | Hash de token aleatorio de un solo uso |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Emision UTC |
| vence_en | datetime(6) | NO | — | Sin valor explícito | Caducidad UTC |
| utilizada_en | datetime(6) | YES | — | NULL | Consumo UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_recuperacion_usuario | INDEX | id_usuario, vence_en |
| uq_recuperacion_token | UNIQUE | token_hash |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_recuperacion_usuario | (id_usuario) → usuario(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_recuperacion_vigencia | (vence_en > creada_en) |

## recurso_agenda

Mutex transaccional por servicio o musico; SELECT FOR UPDATE ordenado

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Fila estable de serializacion por recurso |
| id_servicio | bigint | YES | UNI | NULL | Recurso del propio servicio |
| id_musico | bigint | YES | UNI | NULL | Usuario musico compartido entre bandas/servicio individual |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| uq_recurso_musico | UNIQUE | id_musico |
| uq_recurso_servicio | UNIQUE | id_servicio |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_recurso_musico | (id_musico) → usuario(id) | RESTRICT / RESTRICT |
| fk_recurso_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_recurso_origen | (((id_servicio is not null) + (id_musico is not null)) = 1) |

## reembolso

Devoluciones externas idempotentes y conciliables

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de solicitud de devolucion |
| id_pago | bigint | NO | MUL | Sin valor explícito | Intento de cobro original |
| proveedor | varchar(50) | NO | MUL | Sin valor explícito | Proveedor coherente con pago; deduplicacion de devolucion externa |
| id_contrato | bigint | NO | — | Sin valor explícito | Contrato coherente con pago |
| clave_idempotencia | varchar(100) | NO | — | Sin valor explícito | Clave de solicitud por pago |
| referencia_externa | varchar(150) | YES | — | NULL | Referencia de devolucion en proveedor |
| importe_centimos | bigint | NO | — | Sin valor explícito | Importe exacto solicitado PEN |
| motivo | varchar(1000) | NO | — | Sin valor explícito | Motivo autorizado de devolucion |
| estado | varchar(16) | NO | MUL | PENDIENTE | Confirmar con proveedor, no con rollback SQL |
| solicitado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Solicitud UTC |
| confirmado_en | datetime(6) | YES | — | NULL | Confirmacion externa UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_reembolso_pago | INDEX | id_pago, id_contrato |
| fk_reembolso_proveedor | INDEX | id_pago, proveedor |
| ix_reembolso_pendiente | INDEX | estado, solicitado_en |
| uq_reembolso_clave | UNIQUE | id_pago, clave_idempotencia |
| uq_reembolso_externo | UNIQUE | proveedor, referencia_externa |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_reembolso_pago | (id_pago, id_contrato) → pago(id, id_contrato) | RESTRICT / RESTRICT |
| fk_reembolso_proveedor | (id_pago, proveedor) → pago(id, proveedor) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_reembolso_confirmacion | ((estado <> _utf8mb4\'APROBADO\') or ((confirmado_en is not null) and (referencia_externa is not null))) |
| ck_reembolso_estado | (estado in (_utf8mb4\'PENDIENTE\',_utf8mb4\'PROCESANDO\',_utf8mb4\'APROBADO\',_utf8mb4\'RECHAZADO\')) |
| ck_reembolso_importe | (importe_centimos between 1 and 999999999999) |

## repertorio

Repertorio individual por servicio

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de cancion |
| id_servicio | bigint | NO | MUL | Sin valor explícito | Servicio propietario |
| titulo | varchar(180) | NO | — | Sin valor explícito | Titulo de la pieza |
| genero | varchar(80) | NO | — | Sin valor explícito | Genero de la pieza |
| descripcion | varchar(1000) | YES | — | NULL | Descripcion opcional |
| duracion_segundos | int | YES | — | NULL | Duracion opcional positiva |
| audio_ruta | varchar(500) | YES | — | NULL | Ruta controlada de audio, no secreto ni binario |
| orden | int | NO | — | 0 | Orden de presentacion |
| activo | tinyint(1) | NO | — | 1 | Retiro logico |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_repertorio_servicio | INDEX | id_servicio, activo, orden |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_repertorio_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_repertorio_activo | (activo in (0,1)) |
| ck_repertorio_duracion | ((duracion_segundos is null) or (duracion_segundos > 0)) |
| ck_repertorio_orden | (orden >= 0) |

## reserva

Una reserva por contrato; fechas derivadas de snapshot contractual

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de reserva materializada |
| id_contrato | bigint | NO | UNI | Sin valor explícito | Contrato unico que origina la reserva |
| id_servicio | bigint | NO | — | Sin valor explícito | Servicio coherente con contrato |
| estado | varchar(12) | NO | — | CONFIRMADA | Estado operativo de reserva |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Primer abono aplicado UTC |
| cancelada_en | datetime(6) | YES | — | NULL | Liberacion de intervalo UTC segun politica |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_reserva_contrato | INDEX | id_contrato, id_servicio |
| uq_reserva_contrato | UNIQUE | id_contrato |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_reserva_contrato | (id_contrato, id_servicio) → contrato(id, id_servicio) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_reserva_cancelacion | ((estado <> _utf8mb4\'CANCELADA\') or (cancelada_en is not null)) |
| ck_reserva_estado | (estado in (_utf8mb4\'CONFIRMADA\',_utf8mb4\'CANCELADA\')) |

## retencion_agenda

Soporte opcional de checkout asincrono; no obliga a habilitarlo

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de retencion tecnica |
| id_contrato | bigint | NO | MUL | Sin valor explícito | Contrato que inicia checkout |
| id_servicio | bigint | NO | — | Sin valor explícito | Servicio coherente con contrato |
| id_pago | bigint | NO | UNI | Sin valor explícito | Intento asociado coherente con contrato |
| inicio_utc | datetime(6) | NO | — | Sin valor explícito | Inicio con margenes UTC |
| fin_utc | datetime(6) | NO | — | Sin valor explícito | Fin con margenes UTC |
| vence_en | datetime(6) | NO | — | Sin valor explícito | Caducidad tecnica UTC, nunca reserva confirmada |
| estado | varchar(12) | NO | MUL | ACTIVA | Estado de retencion tecnica |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| fk_retencion_contrato | INDEX | id_contrato, id_servicio |
| fk_retencion_pago | INDEX | id_pago, id_contrato |
| ix_retencion_vencimiento | INDEX | estado, vence_en |
| uq_retencion_pago | UNIQUE | id_pago |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_retencion_contrato | (id_contrato, id_servicio) → contrato(id, id_servicio) | RESTRICT / RESTRICT |
| fk_retencion_pago | (id_pago, id_contrato) → pago(id, id_contrato) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_retencion_estado | (estado in (_utf8mb4\'ACTIVA\',_utf8mb4\'CONVERTIDA\',_utf8mb4\'EXPIRADA\',_utf8mb4\'CANCELADA\')) |
| ck_retencion_intervalo | ((inicio_utc < fin_utc) and (vence_en > creada_en)) |

## rol

Cuatro roles; no crea cuentas de usuario

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | smallint | NO | PRI | Sin valor explícito | Identificador estable del rol |
| codigo | varchar(24) | NO | UNI | Sin valor explícito | Codigo de autorizacion del servidor |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| uq_rol_codigo | UNIQUE | codigo |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_rol_codigo | (codigo in (_utf8mb4\'ADMINISTRADOR\',_utf8mb4\'CLIENTE\',_utf8mb4\'DIRECTOR\',_utf8mb4\'SOLISTA\')) |

## servicio_genero

Lista de generos consultable por SQL

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id_servicio | bigint | NO | PRI | Sin valor explícito | Servicio propietario |
| genero | varchar(80) | NO | PRI | Sin valor explícito | Genero musical normalizado por backend |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id_servicio, genero |
| ix_genero_busqueda | INDEX | genero, id_servicio |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_genero_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

## servicio_instrumento

Instrumentos publicos del servicio

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id_servicio | bigint | NO | PRI | Sin valor explícito | Servicio propietario |
| instrumento | varchar(80) | NO | PRI | Sin valor explícito | Instrumento o especialidad |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id_servicio, instrumento |
| ix_instrumento_busqueda | INDEX | instrumento, id_servicio |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_instrumento_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

## servicio_musical

Banda o servicio individual de solista

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador publico del servicio |
| id_responsable | bigint | NO | MUL | Sin valor explícito | Director o solista responsable; rol validado en Service |
| tipo | varchar(12) | NO | — | Sin valor explícito | BANDA o SOLISTA; no son tablas duplicadas |
| id_solista_unico | bigint | YES | UNI | (case when (tipo = _utf8mb4\'SOLISTA\') then id_responsable else NULL end) | Un solo servicio individual por solista |
| nombre | varchar(160) | NO | — | Sin valor explícito | Nombre publico |
| descripcion_historica | text | YES | — | NULL | Resena historica opcional; no campo Presentacion del servicio |
| ciudad | varchar(100) | NO | — | Sin valor explícito | Ubicacion publica sin direccion de eventos |
| experiencia | varchar(500) | YES | — | NULL | Experiencia publicada |
| incluye | text | YES | — | NULL | Contenido referencial sujeto a cotizacion |
| tarifa_centimos | bigint | YES | — | NULL | PEN por hora; NULL significa A cotizar |
| estado_publicacion | varchar(20) | NO | MUL | BORRADOR | Ciclo de revision administrativa |
| motivo_revision | varchar(1000) | YES | — | NULL | Motivo privado de observacion o retiro |
| version | bigint | NO | — | 0 | Version optimista |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC |
| actualizado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Modificacion UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_catalogo | INDEX | estado_publicacion, tipo, ciudad, tarifa_centimos |
| ix_servicio_responsable | INDEX | id_responsable, estado_publicacion |
| uq_servicio_solista | UNIQUE | id_solista_unico |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_servicio_responsable | (id_responsable) → usuario(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_servicio_estado | (estado_publicacion in (_utf8mb4\'BORRADOR\',_utf8mb4\'EN_REVISION\',_utf8mb4\'OBSERVADO\',_utf8mb4\'PUBLICADO\',_utf8mb4\'RETIRADO\')) |
| ck_servicio_nombre | (char_length(trim(nombre)) > 0) |
| ck_servicio_tarifa | ((tarifa_centimos is null) or (tarifa_centimos between 0 and 999999999999)) |
| ck_servicio_tipo | (tipo in (_utf8mb4\'BANDA\',_utf8mb4\'SOLISTA\')) |
| ck_servicio_version | (version >= 0) |

## servicio_tipo_evento

Tipos de evento del perfil publico

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id_servicio | bigint | NO | PRI | Sin valor explícito | Servicio propietario |
| tipo_evento | varchar(100) | NO | PRI | Sin valor explícito | Tipo de evento atendido |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id_servicio, tipo_evento |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_tipo_evento_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

## sesion

Rotacion y revocacion futura de sesiones reales

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de sesion de usuario |
| id_usuario | bigint | NO | MUL | Sin valor explícito | Titular autenticado |
| familia | char(36) | NO | MUL | Sin valor explícito | Familia de rotacion para revocar reuso |
| refresh_hash | binary(32) | NO | UNI | Sin valor explícito | Hash SHA256 de token aleatorio; nunca token en claro |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Emision UTC |
| vence_en | datetime(6) | NO | — | Sin valor explícito | Caducidad UTC |
| consumida_en | datetime(6) | YES | — | NULL | Uso del refresh; conservar para detectar reuso |
| revocada_en | datetime(6) | YES | — | NULL | Revocacion UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_sesion_familia | INDEX | familia |
| ix_sesion_usuario | INDEX | id_usuario, revocada_en, vence_en |
| uq_sesion_refresh | UNIQUE | refresh_hash |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_sesion_usuario | (id_usuario) → usuario(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_sesion_vigencia | (vence_en > creada_en) |

## solicitud_cotizacion

Solicitud sin reserva de horario

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de solicitud |
| id_cliente | bigint | NO | MUL | Sin valor explícito | Cliente solicitante |
| id_servicio | bigint | NO | MUL | Sin valor explícito | Servicio elegido |
| id_proveedor | bigint | NO | MUL | Sin valor explícito | Responsable al solicitar; se conserva historicamente |
| inicio_evento_utc | datetime(6) | NO | — | Sin valor explícito | Inicio del evento UTC |
| fin_evento_utc | datetime(6) | NO | — | Sin valor explícito | Fin exclusivo del evento UTC |
| lugar | varchar(500) | NO | — | Sin valor explícito | Direccion privada del evento |
| ciudad | varchar(100) | NO | — | Sin valor explícito | Ciudad del evento |
| tipo_evento | varchar(100) | NO | — | Sin valor explícito | Tipo de evento solicitado |
| asistentes | int | YES | — | NULL | Asistentes previstos |
| preferencias | text | YES | — | NULL | Preferencias musicales |
| requerimientos | text | YES | — | NULL | Necesidades del evento |
| notas | text | YES | — | NULL | Notas privadas |
| estado | varchar(24) | NO | — | ENVIADA | Estado de la solicitud |
| clave_idempotencia | varchar(100) | NO | — | Sin valor explícito | Clave de solicitud por cliente |
| version | bigint | NO | — | 0 | Version optimista |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Creacion UTC para indicadores de cohorte |
| primera_respuesta_en | datetime(6) | YES | — | NULL | Primera respuesta real UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_solicitud_cliente | INDEX | id_cliente, estado, creada_en |
| ix_solicitud_proveedor | INDEX | id_proveedor, estado, creada_en |
| ix_solicitud_servicio | INDEX | id_servicio, inicio_evento_utc, fin_evento_utc |
| uq_solicitud_clave | UNIQUE | id_cliente, clave_idempotencia |
| uq_solicitud_contexto | UNIQUE | id, id_servicio, id_cliente, id_proveedor |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_solicitud_cliente | (id_cliente) → usuario(id) | RESTRICT / RESTRICT |
| fk_solicitud_proveedor | (id_proveedor) → usuario(id) | RESTRICT / RESTRICT |
| fk_solicitud_servicio | (id_servicio) → servicio_musical(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_solicitud_asistentes | ((asistentes is null) or (asistentes > 0)) |
| ck_solicitud_estado | (estado in (_utf8mb4\'ENVIADA\',_utf8mb4\'EN_REVISION\',_utf8mb4\'COTIZACION_RECIBIDA\',_utf8mb4\'RECHAZADA\',_utf8mb4\'CANCELADA\',_utf8mb4\'CONTRATADA\',_utf8mb4\'RESERVADA\')) |
| ck_solicitud_intervalo | ((inicio_evento_utc < fin_evento_utc) and (fin_evento_utc <= (inicio_evento_utc + interval 24 hour))) |

## usuario

Cuentas reales; baja logica preserva historia

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador del usuario |
| id_rol | smallint | NO | MUL | Sin valor explícito | Rol validado por el servidor |
| nombres | varchar(100) | NO | — | Sin valor explícito | Nombres de la persona |
| apellidos | varchar(120) | NO | — | Sin valor explícito | Apellidos de la persona |
| correo | varchar(254) | NO | — | Sin valor explícito | Correo capturado |
| correo_normalizado | varchar(254) | YES | UNI | lower(trim(correo)) | Identidad normalizada; conserva diferencias de acento |
| password_hash | varchar(255) | NO | — | Sin valor explícito | Hash adaptativo generado por backend; nunca contrasena |
| telefono | varchar(24) | YES | — | NULL | Telefono autorizado del perfil |
| ciudad | varchar(100) | YES | — | NULL | Ciudad del perfil |
| biografia | varchar(1000) | YES | — | NULL | Texto opcional del perfil |
| estado | varchar(24) | NO | — | PENDIENTE_ACTIVACION | Estado administrativo de acceso |
| version | bigint | NO | — | 0 | Version para bloqueo optimista |
| creado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Instante UTC de creacion |
| actualizado_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Instante UTC de ultima modificacion |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| ix_usuario_rol_estado | INDEX | id_rol, estado |
| uq_usuario_correo | UNIQUE | correo_normalizado |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_usuario_rol | (id_rol) → rol(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_usuario_estado | (estado in (_utf8mb4\'PENDIENTE_ACTIVACION\',_utf8mb4\'ACTIVO\',_utf8mb4\'SUSPENDIDO\',_utf8mb4\'DESACTIVADO\')) |
| ck_usuario_nombre | ((char_length(trim(nombres)) > 0) and (char_length(trim(apellidos)) > 0)) |
| ck_usuario_version | (version >= 0) |

## valoracion

Valoracion; no se crea encuesta de RF-68 pendiente de conciliacion

| Columna | Tipo MySQL | NULL | Clave | Predeterminado / cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI | Sin valor explícito | Identificador de resena |
| id_contrato | bigint | NO | UNI | Sin valor explícito | Unica valoracion por contrato finalizado |
| puntuacion | smallint | NO | — | Sin valor explícito | Escala operativa 1-5; no instrumento de encuesta |
| comentario | varchar(2000) | YES | — | NULL | Comentario autorizado |
| consentimiento_publicacion | tinyint(1) | NO | — | 0 | Permiso expreso de publicacion |
| creada_en | datetime(6) | NO | — | CURRENT_TIMESTAMP(6) | Registro UTC |

**Claves e índices**

| Nombre | Clase | Columnas en orden |
|---|---|---|
| PRIMARY | PK | id |
| uq_valoracion_contrato | UNIQUE | id_contrato |

**Claves foráneas**

| Restricción | Columnas → destino | Borrado / actualización |
|---|---|---|
| fk_valoracion_contrato | (id_contrato) → contrato(id) | RESTRICT / RESTRICT |

**Restricciones CHECK ejecutadas**

| Nombre | Expresión MySQL |
|---|---|
| ck_valoracion_consentimiento | (consentimiento_publicacion in (0,1)) |
| ck_valoracion_puntuacion | (puntuacion between 1 and 5) |

## Vista v_saldo_contrato

Separa abonos aplicados y reembolsos confirmados. Los cobros externos no aplicados por incidencia y los intentos rechazados no reducen el saldo. Primero agrega devoluciones por pago y luego pagos por contrato para evitar multiplicaciones al hacer joins. No oculta un saldo negativo con GREATEST: el Service debe impedir sobrepago bajo bloqueo del contrato. SQL SECURITY INVOKER usa permisos del consultante.

| Campo | Definición |
|---|---|
| id_contrato | Contrato consultado, sujeto a autorización por propietario |
| total_centimos | Total del snapshot aceptado |
| abonado_centimos | Suma de intentos con aplicado_en no nulo |
| reembolsado_centimos | Suma de reembolsos APROBADO asociados a pagos aplicados |
| saldo_centimos | Total − abonado + reembolsado |

Las sumas de MySQL sobre BIGINT se devuelven con precisión decimal; el backend debe mapearlas a entero exacto/BigDecimal con escala cero, sin float/double. La política comercial después de una cancelación se interpreta con el estado del contrato; la vista es contabilidad de abonos, no una orden de seguir cobrando un contrato cancelado.

## Triggers

| Trigger | Tabla | Momento / evento |
|---|---|---|
| tr_contrato_actualizar | contrato | BEFORE UPDATE |
| tr_contrato_eliminar | contrato | BEFORE DELETE |
| tr_contrato_insertar | contrato | BEFORE INSERT |
| tr_cotizacion_actualizar | cotizacion | BEFORE UPDATE |
| tr_cotizacion_eliminar | cotizacion | BEFORE DELETE |
| tr_detalle_actualizar | detalle_cotizacion | BEFORE UPDATE |
| tr_detalle_eliminar | detalle_cotizacion | BEFORE DELETE |
| tr_detalle_insertar | detalle_cotizacion | BEFORE INSERT |
| tr_valoracion_insertar | valoracion | BEFORE INSERT |

## Room local existente

| Columna servicio_cache | Tipo SQLite / Java | Regla |
|---|---|---|
| id | INTEGER / long | PK oficial del servicio |
| nombre | TEXT / String | NOT NULL, nombre público |
| tipo | TEXT / String | BANDA o SOLISTA en el mapper |
| ciudad | TEXT / String | Ciudad pública |
| tarifaCentimos | INTEGER / Long | NULL = A cotizar; entero en céntimos |
| sincronizadoEn | INTEGER / long | epoch millis del refresco local |

Room no se migra en esta etapa; ninguna tabla financiera central se copia automáticamente a Android. Contraseñas, tokens, direcciones de eventos y datos de tarjeta no forman parte de esta caché.

## Caché Android actualizada en Etapa 5

Room v3 incorpora generosJson e instrumentosJson en servicio_cache, con migración explícita 2→3. Continúa siendo caché pública separada de MySQL; no contiene cuentas, contratos o pagos. Ver ../docs/interfaz.md y app/schemas en la raíz del proyecto. Los apartados de Room anteriores conservan el corte inicial.

## Extensión V006 — Etapa 6

- solicitud_cotizacion.huella_creacion: BINARY(32), nullable para filas históricas, SHA-256 de la entrada para su clave por cliente.
- cotizacion.clave_envio: VARCHAR(100) ASCII binario, nullable para historia; única por id_solicitud.
- cotizacion.huella_envio: BINARY(32), nullable para historia; detecta reutilización de una clave con otra propuesta.
- servicio_musical.agenda_version: BIGINT NOT NULL DEFAULT 0, control optimista independiente del perfil.

Los recursos existentes se completan en la migración sin crear horarios de demostración. No cambian las 32 tablas de negocio del corte V005.

## Extensión V007 — pagos directos

| Tabla | Datos y restricciones principales |
|---|---|
| cuenta_cobro | Identidad receptora: titular/banco/celular/cuenta/CCI, medios y activación, versión optimista. Número/CCI/celular por entidad deduplicados |
| pago_directo | PK/FK id_pago, FK id_cuenta, destino_snapshot JSON, reporte del cliente (operación/fecha/importe/titular), huellas e identidad/motivo del revisor |
| comprobante_directo | Imagen JPEG privada MEDIUMBLOB, hash BINARY(32), actor y fecha; origen único pago o devolución, una imagen por origen |
| movimiento_cuenta | Ingreso o devolución contrastados: cuenta, origen único pago/reembolso, operación ASCII normalizada, fecha, importe exacto y administrador; UNIQUE(cuenta,fecha,operación) |

reembolso añade version, huella_solicitud, huella_confirmacion e identidades de solicitante/verificador. Pago mantiene separado su importe previsto, cobrado/verificado y aplicado. El importe realmente contrastado aparece en movimiento_cuenta; una diferencia no genera abono. La vista v_saldo_contrato solo suma pagos aplicados y resta devoluciones confirmadas de esos pagos. Son 36 tablas de negocio, además del historial Flyway.
