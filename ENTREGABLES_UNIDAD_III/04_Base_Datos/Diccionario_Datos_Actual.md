# Diccionario físico del esquema actual

Metadatos de la instancia local persistente. No incluye filas de usuarios, sesiones, contratos, pagos ni comprobantes.

MySQL local con Flyway V001–V007: 36 tablas de negocio, 337 columnas de negocio y una tabla técnica de historial.

## auth_familia

Revocacion y serializacion de una familia de sesiones

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | char(36) | NO | PRI |  |  |
| id_usuario | bigint | NO | MUL |  |  |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) |  |
| revocada_en | datetime(6) | YES |  |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_auth_familia_usuario | id_usuario | usuario.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_auth_familia_usuario | id_usuario | 1 | No |
| ix_auth_familia_usuario | revocada_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_auth_familia_usuario | id | 1 | Sí |
| uq_auth_familia_usuario | id_usuario | 2 | Sí |

## bloqueo_agenda

Bloqueo manual requiere protocolo comun de recursos

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador del bloqueo manual |
| id_servicio | bigint | NO | MUL |  | Servicio que bloquea |
| id_actor | bigint | NO | MUL |  | Actor autorizado en backend |
| inicio_utc | datetime(6) | NO |  |  | Inicio ocupado UTC |
| fin_utc | datetime(6) | NO |  |  | Fin exclusivo UTC |
| motivo | varchar(1000) | NO |  |  | Motivo privado nunca publicado |
| activo | tinyint(1) | NO |  | 1 | Retiro logico de bloqueo |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC |
| retirado_en | datetime(6) | YES |  |  | Retiro UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_bloqueo_actor | id_actor | usuario.id | RESTRICT | RESTRICT |
| fk_bloqueo_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_bloqueo_actor | id_actor | 1 | No |
| ix_bloqueo_servicio | id_servicio | 1 | No |
| ix_bloqueo_servicio | activo | 2 | No |
| ix_bloqueo_servicio | inicio_utc | 3 | No |
| ix_bloqueo_servicio | fin_utc | 4 | No |
| PRIMARY | id | 1 | Sí |

## comprobante_directo



| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  |  |
| id_pago | bigint | YES | UNI |  |  |
| id_reembolso | bigint | YES | UNI |  |  |
| imagen | mediumblob | NO |  |  |  |
| imagen_hash | binary(32) | NO |  |  |  |
| tipo | varchar(30) | NO |  | image/jpeg |  |
| id_actor | bigint | NO | MUL |  |  |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_comprobante_actor | id_actor | usuario.id | NO ACTION | NO ACTION |
| fk_comprobante_pago | id_pago | pago.id | NO ACTION | NO ACTION |
| fk_comprobante_reembolso | id_reembolso | reembolso.id | NO ACTION | NO ACTION |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_comprobante_actor | id_actor | 1 | No |
| PRIMARY | id | 1 | Sí |
| uq_comprobante_pago | id_pago | 1 | Sí |
| uq_comprobante_reembolso | id_reembolso | 1 | Sí |

## contrato

Aceptacion inmutable; no reserva hasta pago verificado

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador oficial del contrato |
| codigo | varchar(50) | NO | UNI |  | Codigo visible unico |
| id_cotizacion | bigint | NO | UNI |  | Version exacta aceptada |
| id_solicitud | bigint | NO | UNI |  | Solicitud unica aun entre distintas versiones |
| id_servicio | bigint | NO |  |  | Servicio historico coherente con solicitud |
| id_cliente | bigint | NO | MUL |  | Cliente historico coherente con solicitud |
| id_proveedor | bigint | NO | MUL |  | Proveedor historico coherente con solicitud |
| inicio_evento_utc | datetime(6) | NO | MUL |  | Inicio aceptado UTC |
| fin_evento_utc | datetime(6) | NO |  |  | Fin exclusivo aceptado UTC |
| inicio_ocupacion_utc | datetime(6) | NO |  |  | Inicio con margen de preparacion incluido |
| fin_ocupacion_utc | datetime(6) | NO |  |  | Fin con margen posterior incluido |
| total_centimos | bigint | NO |  |  | Total historico exacto PEN |
| condiciones_aceptadas | tinyint(1) | NO |  |  | Aceptacion expresa obligatoria |
| politica_version | varchar(80) | NO |  |  | Version exacta aceptada |
| snapshot | json | NO |  |  | Copia historica de perfil, actores, conceptos, recursos y politicas |
| clave_idempotencia | varchar(100) | NO |  |  | Clave de aceptacion por cliente |
| huella_aceptacion | binary(32) | NO |  |  | SHA256 de entrada canonica para detectar reutilizacion de clave |
| estado | varchar(20) | NO |  | PENDIENTE_PAGO | Estado contractual independiente del proveedor financiero |
| version | bigint | NO |  | 0 | Version optimista de estado |
| aceptado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Aceptacion UTC |
| confirmado_en | datetime(6) | YES | MUL |  | Primer abono verificado UTC |
| finalizado_en | datetime(6) | YES |  |  | Finalizacion despues del evento UTC |
| cancelado_en | datetime(6) | YES |  |  | Cancelacion UTC |
| motivo_cancelacion | varchar(1000) | YES |  |  | Motivo conservado segun politica |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_contrato_contexto | id_solicitud | solicitud_cotizacion.id | RESTRICT | RESTRICT |
| fk_contrato_contexto | id_servicio | solicitud_cotizacion.id_servicio | RESTRICT | RESTRICT |
| fk_contrato_contexto | id_cliente | solicitud_cotizacion.id_cliente | RESTRICT | RESTRICT |
| fk_contrato_contexto | id_proveedor | solicitud_cotizacion.id_proveedor | RESTRICT | RESTRICT |
| fk_contrato_cotizacion | id_cotizacion | cotizacion.id | RESTRICT | RESTRICT |
| fk_contrato_cotizacion | id_solicitud | cotizacion.id_solicitud | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_contrato_contexto | id_solicitud | 1 | No |
| fk_contrato_contexto | id_servicio | 2 | No |
| fk_contrato_contexto | id_cliente | 3 | No |
| fk_contrato_contexto | id_proveedor | 4 | No |
| fk_contrato_cotizacion | id_cotizacion | 1 | No |
| fk_contrato_cotizacion | id_solicitud | 2 | No |
| ix_contrato_cliente | id_cliente | 1 | No |
| ix_contrato_cliente | estado | 2 | No |
| ix_contrato_cliente | aceptado_en | 3 | No |
| ix_contrato_confirmacion | confirmado_en | 1 | No |
| ix_contrato_confirmacion | estado | 2 | No |
| ix_contrato_evento | inicio_evento_utc | 1 | No |
| ix_contrato_evento | id_servicio | 2 | No |
| ix_contrato_proveedor | id_proveedor | 1 | No |
| ix_contrato_proveedor | estado | 2 | No |
| ix_contrato_proveedor | inicio_evento_utc | 3 | No |
| PRIMARY | id | 1 | Sí |
| uq_contrato_aceptacion | id_cliente | 1 | Sí |
| uq_contrato_aceptacion | clave_idempotencia | 2 | Sí |
| uq_contrato_cliente | id | 1 | Sí |
| uq_contrato_cliente | id_cliente | 2 | Sí |
| uq_contrato_codigo | codigo | 1 | Sí |
| uq_contrato_cotizacion | id_cotizacion | 1 | Sí |
| uq_contrato_servicio | id | 1 | Sí |
| uq_contrato_servicio | id_servicio | 2 | Sí |
| uq_contrato_solicitud | id_solicitud | 1 | Sí |

## cotizacion

Versiones historicas de propuesta economica

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de version |
| id_solicitud | bigint | NO | MUL |  | Solicitud que agrupa versiones |
| numero_version | int | NO |  |  | Consecutivo dentro de la solicitud |
| inicio_propuesto_utc | datetime(6) | NO |  |  | Inicio propuesto UTC |
| fin_propuesto_utc | datetime(6) | NO |  |  | Fin propuesto UTC |
| subtotal_centimos | bigint | NO |  |  | Suma exacta de conceptos; backend reconcilia |
| descuento_centimos | bigint | NO |  | 0 | Descuento exacto no mayor que subtotal |
| total_centimos | bigint | YES |  | (`subtotal_centimos` - `descuento_centimos`) | Total exacto en centimos PEN |
| condiciones | text | NO |  |  | Condiciones versionadas del servicio |
| politica_cancelacion | text | NO |  |  | Politica propuesta para aceptacion expresa |
| politica_version | varchar(80) | NO |  |  | Identificador de politica aceptable |
| vigente_hasta_utc | datetime(6) | NO |  |  | Caducidad de la propuesta |
| estado | varchar(16) | NO |  | BORRADOR | Estado de version, no edicion de propuesta enviada |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC |
| clave_envio | varchar(100) | YES |  |  |  |
| huella_envio | binary(32) | YES |  |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_cotizacion_solicitud | id_solicitud | solicitud_cotizacion.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id | 1 | Sí |
| uq_cotizacion_envio | id_solicitud | 1 | Sí |
| uq_cotizacion_envio | clave_envio | 2 | Sí |
| uq_cotizacion_solicitud | id | 1 | Sí |
| uq_cotizacion_solicitud | id_solicitud | 2 | Sí |
| uq_cotizacion_version | id_solicitud | 1 | Sí |
| uq_cotizacion_version | numero_version | 2 | Sí |

## cuenta_cobro



| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  |  |
| titular | varchar(180) | NO |  |  |  |
| banco | varchar(100) | NO | MUL |  |  |
| telefono | varchar(20) | YES |  |  |  |
| numero_cuenta | varchar(40) | YES |  |  |  |
| cci | varchar(20) | YES | UNI |  |  |
| yape | tinyint(1) | NO |  | 0 |  |
| plin | tinyint(1) | NO |  | 0 |  |
| transferencia | tinyint(1) | NO |  | 0 |  |
| activo | tinyint(1) | NO |  | 1 |  |
| version | bigint | NO |  | 0 |  |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) |  |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id | 1 | Sí |
| uq_cobro_cci | cci | 1 | Sí |
| uq_cobro_cuenta | banco | 1 | Sí |
| uq_cobro_cuenta | numero_cuenta | 2 | Sí |
| uq_cobro_telefono | banco | 1 | Sí |
| uq_cobro_telefono | telefono | 2 | Sí |

## detalle_cotizacion

Desglose exacto sin float o double

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador del concepto |
| id_cotizacion | bigint | NO | MUL |  | Version propietaria |
| concepto | varchar(300) | NO |  |  | Descripcion del concepto economico |
| cantidad | decimal(10,3) | NO |  |  | Cantidad exacta, hasta tres decimales |
| precio_unitario_centimos | bigint | NO |  |  | Precio exacto unitario en centimos PEN |
| total_centimos | bigint | YES |  | cast(round((`cantidad` * `precio_unitario_centimos`),0) as signed) | Redondeo decimal por concepto: mitad hacia arriba |
| orden | int | NO |  | 0 | Orden del desglose |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_detalle_cotizacion | id_cotizacion | cotizacion.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_detalle_cotizacion | id_cotizacion | 1 | No |
| ix_detalle_cotizacion | orden | 2 | No |
| PRIMARY | id | 1 | Sí |

## evento_medicion

Eventos minimos de busqueda; no encuesta ni direccion privada

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de evento de medicion consentida |
| sesion_medicion | char(36) | NO | MUL |  | Identificador pseudonimo de sesion; no cuenta personal |
| clave_evento | char(36) | NO | UNI |  | Identificador idempotente de evento |
| id_servicio | bigint | YES | MUL |  | Servicio publico consultado, cuando corresponda |
| accion | varchar(12) | NO |  |  | Inicio voluntario, consulta o fin |
| consentimiento_version | varchar(80) | NO |  |  | Version informada de consentimiento expreso |
| ocurrido_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Hora del servidor para indicadores |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_medicion_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_medicion_servicio | id_servicio | 1 | No |
| ix_medicion_sesion | sesion_medicion | 1 | No |
| ix_medicion_sesion | ocurrido_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_medicion_evento | clave_evento | 1 | Sí |

## evento_outbox

Salida fiable: escritura con negocio y entrega posterior

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de evento transaccional de salida |
| clave_evento | char(36) | NO | UNI |  | Clave estable de entrega idempotente |
| tipo | varchar(80) | NO |  |  | Tipo de notificacion o integracion |
| entidad | varchar(80) | NO |  |  | Agregado de origen |
| identificador_entidad | bigint | NO |  |  | ID logico del agregado |
| payload | json | NO |  |  | Datos minimos redactados del evento |
| estado | varchar(12) | NO | MUL | PENDIENTE | Estado de entrega |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Commit del hecho de negocio UTC |
| disponible_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Proximo intento UTC |
| enviado_en | datetime(6) | YES |  |  | Confirmacion de entrega UTC |
| intentos | int | NO |  | 0 | Reintentos de entrega |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_outbox_pendiente | estado | 1 | No |
| ix_outbox_pendiente | disponible_en | 2 | No |
| ix_outbox_pendiente | id | 3 | No |
| PRIMARY | id | 1 | Sí |
| uq_outbox_clave | clave_evento | 1 | Sí |

## evento_pago

Bandeja de eventos autenticados; API del proveedor verifica el cobro

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de recepcion de webhook |
| proveedor | varchar(50) | NO | MUL |  | Proveedor autenticado |
| id_evento_externo | varchar(150) | NO |  |  | Identificador deduplicado del evento oficial |
| id_pago | bigint | YES | MUL |  | Intento correlacionado; puede llegar antes de correlacionar |
| payload_hash | binary(32) | NO |  |  | Huella del cuerpo recibido; no almacena tarjeta ni payload completo |
| autenticado_en | datetime(6) | NO |  |  | Validacion de firma oficial antes de persistir como evento confiable |
| recibido_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Recepcion UTC |
| procesado_en | datetime(6) | YES |  |  | Aplicacion o conciliacion UTC |
| estado | varchar(16) | NO | MUL | RECIBIDO | Estado de procesamiento interno |
| intentos | int | NO |  | 0 | Numero de reintentos de verificacion |
| ultimo_error_codigo | varchar(80) | YES |  |  | Codigo tecnico sin secretos |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_evento_pago | id_pago | pago.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_evento_pago | id_pago | 1 | No |
| ix_evento_pago_pendiente | estado | 1 | No |
| ix_evento_pago_pendiente | recibido_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_evento_pago_externo | proveedor | 1 | Sí |
| uq_evento_pago_externo | id_evento_externo | 2 | Sí |

## flyway_schema_history



| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| installed_rank | int | NO | PRI |  |  |
| version | varchar(50) | YES |  |  |  |
| description | varchar(200) | NO |  |  |  |
| type | varchar(20) | NO |  |  |  |
| script | varchar(1000) | NO |  |  |  |
| checksum | int | YES |  |  |  |
| installed_by | varchar(100) | NO |  |  |  |
| installed_on | timestamp | NO |  | CURRENT_TIMESTAMP |  |
| execution_time | int | NO |  |  |  |
| success | tinyint(1) | NO | MUL |  |  |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| flyway_schema_history_s_idx | success | 1 | No |
| PRIMARY | installed_rank | 1 | Sí |

## historial_operacion

Auditoria conservada al cancelar o despublicar

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de evento auditable |
| id_actor | bigint | YES | MUL |  | Usuario actor; NULL si sistema |
| id_solicitud | bigint | YES | MUL |  | Origen opcional de contratacion |
| entidad | varchar(80) | NO |  |  | Tipo de agregado auditado |
| identificador_entidad | varchar(100) | NO |  |  | Referencia logica no es FK universal |
| accion | varchar(80) | NO |  |  | Accion realizada |
| detalle | json | YES |  |  | Metadatos redactados sin claves ni datos financieros sensibles |
| correlation_id | varchar(80) | NO |  |  | Correlacion tecnica de la operacion |
| ocurrido_en | datetime(6) | NO | MUL | CURRENT_TIMESTAMP(6) | Instante real UTC del servidor |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_historial_actor | id_actor | usuario.id | RESTRICT | RESTRICT |
| fk_historial_solicitud | id_solicitud | solicitud_cotizacion.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_historial_actor | id_actor | 1 | No |
| ix_historial_fecha | ocurrido_en | 1 | No |
| ix_historial_fecha | entidad | 2 | No |
| ix_historial_solicitud | id_solicitud | 1 | No |
| ix_historial_solicitud | ocurrido_en | 2 | No |
| PRIMARY | id | 1 | Sí |

## horario_atencion

Franjas recurrentes; cruzar medianoche requiere dos franjas

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador del intervalo recurrente |
| id_servicio | bigint | NO | MUL |  | Servicio configurado |
| dia_semana | smallint | NO |  |  | ISO lunes=1 a domingo=7; hora local America/Lima |
| hora_inicio | time | NO |  |  | Apertura local |
| hora_fin | time | NO |  |  | Cierre local el mismo dia |
| margen_previo_min | smallint | NO |  | 0 | Preparacion antes del evento |
| margen_posterior_min | smallint | NO |  | 0 | Preparacion despues del evento |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_horario_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id | 1 | Sí |
| uq_horario_intervalo | id_servicio | 1 | Sí |
| uq_horario_intervalo | dia_semana | 2 | Sí |
| uq_horario_intervalo | hora_inicio | 3 | Sí |
| uq_horario_intervalo | hora_fin | 4 | Sí |

## imagen_servicio

Metadatos de imagen; archivo fuera de MySQL

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de imagen |
| id_servicio | bigint | NO | MUL |  | Servicio propietario |
| ruta_objeto | varchar(500) | NO |  |  | Ruta del almacenamiento externo, no URL firmada permanente |
| mime | varchar(80) | NO |  |  | MIME validado en servidor |
| tamano_bytes | bigint | NO |  |  | Tamano validado del archivo |
| descripcion | varchar(300) | YES |  |  | Descripcion accesible |
| orden | int | NO |  | 0 | Orden en galeria |
| es_portada | tinyint(1) | NO |  | 0 | Marca de portada |
| servicio_portada | bigint | YES | UNI | (case when (`es_portada` = 1) then `id_servicio` else NULL end) | Una portada por servicio |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Carga UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_imagen_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_imagen_servicio | id_servicio | 1 | No |
| ix_imagen_servicio | orden | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_imagen_portada | servicio_portada | 1 | Sí |

## incidencia_pago

Cobro externo no aplicado y otras incidencias auditables

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de incidencia financiera |
| id_pago | bigint | NO | MUL |  | Pago afectado |
| id_contrato | bigint | NO |  |  | Contrato coherente con pago |
| tipo | varchar(50) | NO |  |  | Conflicto, cobro tardio o error de conciliacion |
| estado | varchar(12) | NO | MUL | ABIERTA | Estado de resolucion humana/automatica |
| detalle | varchar(2000) | NO |  |  | Detalle sin instrumentos financieros sensibles |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Deteccion UTC |
| resuelta_en | datetime(6) | YES |  |  | Resolucion UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_incidencia_pago | id_pago | pago.id | RESTRICT | RESTRICT |
| fk_incidencia_pago | id_contrato | pago.id_contrato | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_incidencia_pago | id_pago | 1 | No |
| fk_incidencia_pago | id_contrato | 2 | No |
| ix_incidencia_estado | estado | 1 | No |
| ix_incidencia_estado | creada_en | 2 | No |
| PRIMARY | id | 1 | Sí |

## integrante

Contenido normalizado y vinculo de recurso compartido

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador del integrante |
| id_servicio | bigint | NO | MUL |  | Banda propietaria; tipo validado por Service |
| id_solista_vinculado | bigint | YES | MUL |  | Usuario solista invitado; aceptacion requerida |
| nombre | varchar(180) | NO |  |  | Nombre publico autorizado |
| instrumento | varchar(100) | NO |  |  | Instrumento o funcion musical |
| funcion | varchar(120) | YES |  |  | Funcion dentro de banda |
| estado_vinculo | varchar(16) | NO |  | SIN_VINCULO | Estado del consentimiento de vinculo |
| activo | tinyint(1) | NO |  | 1 | Baja logica conserva referencias historicas |
| version | bigint | NO |  | 0 | Version optimista |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_integrante_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |
| fk_integrante_solista | id_solista_vinculado | usuario.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_integrante_solista | id_solista_vinculado | 1 | No |
| ix_integrante_servicio | id_servicio | 1 | No |
| ix_integrante_servicio | activo | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_integrante_vinculo | id_servicio | 1 | Sí |
| uq_integrante_vinculo | id_solista_vinculado | 2 | Sí |

## movimiento_cuenta



| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  |  |
| id_cuenta | bigint | NO | MUL |  |  |
| id_pago | bigint | YES | UNI |  |  |
| id_reembolso | bigint | YES | UNI |  |  |
| operacion | varchar(100) | NO |  |  |  |
| fecha | date | NO |  |  |  |
| importe_centimos | bigint | NO |  |  |  |
| id_verificador | bigint | NO | MUL |  |  |
| registrado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_movimiento_cuenta | id_cuenta | cuenta_cobro.id | NO ACTION | NO ACTION |
| fk_movimiento_pago | id_pago | pago.id | NO ACTION | NO ACTION |
| fk_movimiento_reembolso | id_reembolso | reembolso.id | NO ACTION | NO ACTION |
| fk_movimiento_verificador | id_verificador | usuario.id | NO ACTION | NO ACTION |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_movimiento_verificador | id_verificador | 1 | No |
| PRIMARY | id | 1 | Sí |
| uq_movimiento_bancario | id_cuenta | 1 | Sí |
| uq_movimiento_bancario | fecha | 2 | Sí |
| uq_movimiento_bancario | operacion | 3 | Sí |
| uq_movimiento_pago | id_pago | 1 | Sí |
| uq_movimiento_reembolso | id_reembolso | 1 | Sí |

## notificacion

Notificaciones persistentes por destinatario

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de notificacion interna |
| id_destinatario | bigint | NO | MUL |  | Usuario unico destinatario |
| tipo | varchar(40) | NO |  |  | Tipo de aviso controlado por backend |
| titulo | varchar(180) | NO |  |  | Titulo seguro |
| mensaje | varchar(2000) | NO |  |  | Contenido sin secretos |
| entidad | varchar(80) | YES |  |  | Entidad de destino; validar permiso al navegar |
| identificador_entidad | bigint | YES |  |  | Referencia logica de destino autorizada |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC |
| leida_en | datetime(6) | YES |  |  | Lectura UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_notificacion_usuario | id_destinatario | usuario.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_notificacion_destinatario | id_destinatario | 1 | No |
| ix_notificacion_destinatario | leida_en | 2 | No |
| ix_notificacion_destinatario | creada_en | 3 | No |
| PRIMARY | id | 1 | Sí |

## ocupacion_agenda

Ocupacion por recurso; exclusion temporal depende del protocolo transaccional

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador del intervalo por recurso |
| id_recurso | bigint | NO | MUL |  | Recurso bloqueado antes de consultar o insertar |
| id_reserva | bigint | YES | MUL |  | Reserva confirmada que origina ocupacion |
| id_bloqueo | bigint | YES | MUL |  | Bloqueo manual que origina ocupacion |
| id_retencion | bigint | YES | MUL |  | Retencion tecnica que origina ocupacion |
| inicio_utc | datetime(6) | NO |  |  | Intervalo semiabierto con margenes; snapshot de recurso |
| fin_utc | datetime(6) | NO |  |  | Fin exclusivo de ocupacion UTC |
| vence_en | datetime(6) | YES |  |  | Solo retenciones tienen caducidad tecnica |
| activa | tinyint(1) | NO |  | 1 | Desactivar al liberar o convertir en misma transaccion |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_ocupacion_bloqueo | id_bloqueo | bloqueo_agenda.id | RESTRICT | RESTRICT |
| fk_ocupacion_recurso | id_recurso | recurso_agenda.id | RESTRICT | RESTRICT |
| fk_ocupacion_reserva | id_reserva | reserva.id | RESTRICT | RESTRICT |
| fk_ocupacion_retencion | id_retencion | retencion_agenda.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_ocupacion_bloqueo | id_bloqueo | 1 | No |
| fk_ocupacion_reserva | id_reserva | 1 | No |
| fk_ocupacion_retencion | id_retencion | 1 | No |
| ix_ocupacion_conflicto | id_recurso | 1 | No |
| ix_ocupacion_conflicto | activa | 2 | No |
| ix_ocupacion_conflicto | inicio_utc | 3 | No |
| ix_ocupacion_conflicto | fin_utc | 4 | No |
| ix_ocupacion_conflicto | vence_en | 5 | No |
| PRIMARY | id | 1 | Sí |
| uq_ocupacion_bloqueo | id_recurso | 1 | Sí |
| uq_ocupacion_bloqueo | id_bloqueo | 2 | Sí |
| uq_ocupacion_reserva | id_recurso | 1 | Sí |
| uq_ocupacion_reserva | id_reserva | 2 | Sí |
| uq_ocupacion_retencion | id_recurso | 1 | Sí |
| uq_ocupacion_retencion | id_retencion | 2 | Sí |

## pago

Intentos, cobro externo y aplicacion contractual diferenciados

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de intento financiero |
| id_contrato | bigint | NO | MUL |  | Contrato propietario |
| id_cliente | bigint | NO |  |  | Pagador coherente con contrato |
| proveedor | varchar(50) | NO | MUL |  | Integracion financiera concreta; aun no seleccionada |
| referencia_externa | varchar(150) | YES |  |  | ID de operacion en proveedor; NULL antes de crear checkout |
| clave_idempotencia | varchar(100) | NO |  |  | Clave unica del intento dentro de contrato |
| huella_solicitud | binary(32) | NO |  |  | SHA256 de entrada canonica asociada a clave |
| modalidad | varchar(20) | NO |  |  | Importe determinado por servidor segun saldo |
| metodo | varchar(40) | YES |  |  | Metodo reportado por proveedor; no promete Yape o Plin |
| importe_centimos | bigint | NO |  |  | Importe exacto del intento PEN |
| moneda | char(3) | NO |  | PEN | Moneda esperada y verificada |
| estado | varchar(24) | NO | MUL | PENDIENTE | Estado interno; separado de respuesta externa |
| estado_externo | varchar(80) | YES |  |  | Estado original del proveedor sin reinterpretarlo |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Inicio UTC del intento |
| verificado_en | datetime(6) | YES |  |  | Consulta/autenticacion del resultado con proveedor |
| cobrado_en | datetime(6) | YES |  |  | Evidencia de cobro externo; no implica reserva compatible |
| aplicado_en | datetime(6) | YES |  |  | Instante del abono aplicado al contrato; idempotente |
| version | bigint | NO |  | 0 | Version optimista de seguimiento |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_pago_contrato | id_contrato | contrato.id | RESTRICT | RESTRICT |
| fk_pago_contrato | id_cliente | contrato.id_cliente | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_pago_contrato | id_contrato | 1 | No |
| fk_pago_contrato | id_cliente | 2 | No |
| ix_pago_contrato_fecha | id_contrato | 1 | No |
| ix_pago_contrato_fecha | creado_en | 2 | No |
| ix_pago_pendiente | estado | 1 | No |
| ix_pago_pendiente | creado_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_pago_clave | id_contrato | 1 | Sí |
| uq_pago_clave | clave_idempotencia | 2 | Sí |
| uq_pago_contrato | id | 1 | Sí |
| uq_pago_contrato | id_contrato | 2 | Sí |
| uq_pago_proveedor | id | 1 | Sí |
| uq_pago_proveedor | proveedor | 2 | Sí |
| uq_pago_referencia | proveedor | 1 | Sí |
| uq_pago_referencia | referencia_externa | 2 | Sí |

## pago_directo



| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id_pago | bigint | NO | PRI |  |  |
| id_cuenta | bigint | NO | MUL |  |  |
| destino_snapshot | json | NO |  |  |  |
| operacion_reportada | varchar(100) | YES |  |  |  |
| fecha_reportada | date | YES |  |  |  |
| importe_reportado_centimos | bigint | YES |  |  |  |
| titular_origen | varchar(180) | YES |  |  |  |
| reportado_en | datetime(6) | YES |  |  |  |
| huella_reporte | binary(32) | YES |  |  |  |
| motivo_revision | varchar(1000) | YES |  |  |  |
| huella_revision | binary(32) | YES |  |  |  |
| id_revisor | bigint | YES | MUL |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_directo_cuenta | id_cuenta | cuenta_cobro.id | RESTRICT | NO ACTION |
| fk_directo_pago | id_pago | pago.id | RESTRICT | NO ACTION |
| fk_directo_revisor | id_revisor | usuario.id | RESTRICT | NO ACTION |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_directo_cuenta | id_cuenta | 1 | No |
| fk_directo_revisor | id_revisor | 1 | No |
| PRIMARY | id_pago | 1 | Sí |

## recuperacion_acceso

Recuperacion segura, sin guardar credenciales

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de recuperacion de acceso |
| id_usuario | bigint | NO | MUL |  | Cuenta destinataria |
| token_hash | binary(32) | NO | UNI |  | Hash de token aleatorio de un solo uso |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Emision UTC |
| vence_en | datetime(6) | NO |  |  | Caducidad UTC |
| utilizada_en | datetime(6) | YES |  |  | Consumo UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_recuperacion_usuario | id_usuario | usuario.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_recuperacion_usuario | id_usuario | 1 | No |
| ix_recuperacion_usuario | vence_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_recuperacion_token | token_hash | 1 | Sí |

## recurso_agenda

Mutex transaccional por servicio o musico; SELECT FOR UPDATE ordenado

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Fila estable de serializacion por recurso |
| id_servicio | bigint | YES | UNI |  | Recurso del propio servicio |
| id_musico | bigint | YES | UNI |  | Usuario musico compartido entre bandas/servicio individual |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_recurso_musico | id_musico | usuario.id | RESTRICT | RESTRICT |
| fk_recurso_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id | 1 | Sí |
| uq_recurso_musico | id_musico | 1 | Sí |
| uq_recurso_servicio | id_servicio | 1 | Sí |

## reembolso

Devoluciones externas idempotentes y conciliables

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de solicitud de devolucion |
| id_pago | bigint | NO | MUL |  | Intento de cobro original |
| proveedor | varchar(50) | NO | MUL |  | Proveedor coherente con pago; deduplicacion de devolucion externa |
| id_contrato | bigint | NO |  |  | Contrato coherente con pago |
| clave_idempotencia | varchar(100) | NO |  |  | Clave de solicitud por pago |
| referencia_externa | varchar(150) | YES |  |  | Referencia de devolucion en proveedor |
| importe_centimos | bigint | NO |  |  | Importe exacto solicitado PEN |
| motivo | varchar(1000) | NO |  |  | Motivo autorizado de devolucion |
| estado | varchar(16) | NO | MUL | PENDIENTE | Confirmar con proveedor, no con rollback SQL |
| solicitado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Solicitud UTC |
| confirmado_en | datetime(6) | YES |  |  | Confirmacion externa UTC |
| version | bigint | NO |  | 0 |  |
| huella_solicitud | binary(32) | YES |  |  |  |
| huella_confirmacion | binary(32) | YES |  |  |  |
| id_solicitante | bigint | YES | MUL |  |  |
| id_verificador | bigint | YES | MUL |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_reembolso_pago | id_pago | pago.id | RESTRICT | RESTRICT |
| fk_reembolso_pago | id_contrato | pago.id_contrato | RESTRICT | RESTRICT |
| fk_reembolso_proveedor | id_pago | pago.id | RESTRICT | RESTRICT |
| fk_reembolso_proveedor | proveedor | pago.proveedor | RESTRICT | RESTRICT |
| fk_reembolso_solicitante | id_solicitante | usuario.id | NO ACTION | NO ACTION |
| fk_reembolso_verificador | id_verificador | usuario.id | NO ACTION | NO ACTION |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_reembolso_pago | id_pago | 1 | No |
| fk_reembolso_pago | id_contrato | 2 | No |
| fk_reembolso_proveedor | id_pago | 1 | No |
| fk_reembolso_proveedor | proveedor | 2 | No |
| fk_reembolso_solicitante | id_solicitante | 1 | No |
| fk_reembolso_verificador | id_verificador | 1 | No |
| ix_reembolso_pendiente | estado | 1 | No |
| ix_reembolso_pendiente | solicitado_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_reembolso_clave | id_pago | 1 | Sí |
| uq_reembolso_clave | clave_idempotencia | 2 | Sí |
| uq_reembolso_externo | proveedor | 1 | Sí |
| uq_reembolso_externo | referencia_externa | 2 | Sí |

## repertorio

Repertorio individual por servicio

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de cancion |
| id_servicio | bigint | NO | MUL |  | Servicio propietario |
| titulo | varchar(180) | NO |  |  | Titulo de la pieza |
| genero | varchar(80) | NO |  |  | Genero de la pieza |
| descripcion | varchar(1000) | YES |  |  | Descripcion opcional |
| duracion_segundos | int | YES |  |  | Duracion opcional positiva |
| audio_ruta | varchar(500) | YES |  |  | Ruta controlada de audio, no secreto ni binario |
| orden | int | NO |  | 0 | Orden de presentacion |
| activo | tinyint(1) | NO |  | 1 | Retiro logico |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_repertorio_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_repertorio_servicio | id_servicio | 1 | No |
| ix_repertorio_servicio | activo | 2 | No |
| ix_repertorio_servicio | orden | 3 | No |
| PRIMARY | id | 1 | Sí |

## reserva

Una reserva por contrato; fechas derivadas de snapshot contractual

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de reserva materializada |
| id_contrato | bigint | NO | UNI |  | Contrato unico que origina la reserva |
| id_servicio | bigint | NO |  |  | Servicio coherente con contrato |
| estado | varchar(12) | NO |  | CONFIRMADA | Estado operativo de reserva |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Primer abono aplicado UTC |
| cancelada_en | datetime(6) | YES |  |  | Liberacion de intervalo UTC segun politica |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_reserva_contrato | id_contrato | contrato.id | RESTRICT | RESTRICT |
| fk_reserva_contrato | id_servicio | contrato.id_servicio | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_reserva_contrato | id_contrato | 1 | No |
| fk_reserva_contrato | id_servicio | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_reserva_contrato | id_contrato | 1 | Sí |

## retencion_agenda

Soporte opcional de checkout asincrono; no obliga a habilitarlo

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de retencion tecnica |
| id_contrato | bigint | NO | MUL |  | Contrato que inicia checkout |
| id_servicio | bigint | NO |  |  | Servicio coherente con contrato |
| id_pago | bigint | NO | UNI |  | Intento asociado coherente con contrato |
| inicio_utc | datetime(6) | NO |  |  | Inicio con margenes UTC |
| fin_utc | datetime(6) | NO |  |  | Fin con margenes UTC |
| vence_en | datetime(6) | NO |  |  | Caducidad tecnica UTC, nunca reserva confirmada |
| estado | varchar(12) | NO | MUL | ACTIVA | Estado de retencion tecnica |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_retencion_contrato | id_contrato | contrato.id | RESTRICT | RESTRICT |
| fk_retencion_contrato | id_servicio | contrato.id_servicio | RESTRICT | RESTRICT |
| fk_retencion_pago | id_pago | pago.id | RESTRICT | RESTRICT |
| fk_retencion_pago | id_contrato | pago.id_contrato | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_retencion_contrato | id_contrato | 1 | No |
| fk_retencion_contrato | id_servicio | 2 | No |
| fk_retencion_pago | id_pago | 1 | No |
| fk_retencion_pago | id_contrato | 2 | No |
| ix_retencion_vencimiento | estado | 1 | No |
| ix_retencion_vencimiento | vence_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_retencion_pago | id_pago | 1 | Sí |

## rol

Cuatro roles; no crea cuentas de usuario

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | smallint | NO | PRI |  | Identificador estable del rol |
| codigo | varchar(24) | NO | UNI |  | Codigo de autorizacion del servidor |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id | 1 | Sí |
| uq_rol_codigo | codigo | 1 | Sí |

## servicio_genero

Lista de generos consultable por SQL

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id_servicio | bigint | NO | PRI |  | Servicio propietario |
| genero | varchar(80) | NO | PRI |  | Genero musical normalizado por backend |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_genero_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_genero_busqueda | genero | 1 | No |
| ix_genero_busqueda | id_servicio | 2 | No |
| PRIMARY | id_servicio | 1 | Sí |
| PRIMARY | genero | 2 | Sí |

## servicio_instrumento

Instrumentos publicos del servicio

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id_servicio | bigint | NO | PRI |  | Servicio propietario |
| instrumento | varchar(80) | NO | PRI |  | Instrumento o especialidad |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_instrumento_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_instrumento_busqueda | instrumento | 1 | No |
| ix_instrumento_busqueda | id_servicio | 2 | No |
| PRIMARY | id_servicio | 1 | Sí |
| PRIMARY | instrumento | 2 | Sí |

## servicio_musical

Banda o servicio individual de solista

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador publico del servicio |
| id_responsable | bigint | NO | MUL |  | Director o solista responsable; rol validado en Service |
| tipo | varchar(12) | NO |  |  | BANDA o SOLISTA; no son tablas duplicadas |
| id_solista_unico | bigint | YES | UNI | (case when (`tipo` = _ascii\'SOLISTA\') then `id_responsable` else NULL end) | Un solo servicio individual por solista |
| nombre | varchar(160) | NO |  |  | Nombre publico |
| descripcion_historica | text | YES |  |  | Resena historica opcional; no campo Presentacion del servicio |
| ciudad | varchar(100) | NO |  |  | Ubicacion publica sin direccion de eventos |
| experiencia | varchar(500) | YES |  |  | Experiencia publicada |
| incluye | text | YES |  |  | Contenido referencial sujeto a cotizacion |
| tarifa_centimos | bigint | YES |  |  | PEN por hora; NULL significa A cotizar |
| estado_publicacion | varchar(20) | NO | MUL | BORRADOR | Ciclo de revision administrativa |
| motivo_revision | varchar(1000) | YES |  |  | Motivo privado de observacion o retiro |
| version | bigint | NO |  | 0 | Version optimista |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC |
| actualizado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Modificacion UTC |
| agenda_version | bigint | NO |  | 0 |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_servicio_responsable | id_responsable | usuario.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_catalogo | estado_publicacion | 1 | No |
| ix_catalogo | tipo | 2 | No |
| ix_catalogo | ciudad | 3 | No |
| ix_catalogo | tarifa_centimos | 4 | No |
| ix_servicio_responsable | id_responsable | 1 | No |
| ix_servicio_responsable | estado_publicacion | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_servicio_solista | id_solista_unico | 1 | Sí |

## servicio_tipo_evento

Tipos de evento del perfil publico

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id_servicio | bigint | NO | PRI |  | Servicio propietario |
| tipo_evento | varchar(100) | NO | PRI |  | Tipo de evento atendido |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_tipo_evento_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id_servicio | 1 | Sí |
| PRIMARY | tipo_evento | 2 | Sí |

## sesion

Rotacion y revocacion futura de sesiones reales

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de sesion de usuario |
| id_usuario | bigint | NO | MUL |  | Titular autenticado |
| familia | char(36) | NO | MUL |  | Familia de rotacion para revocar reuso |
| refresh_hash | binary(32) | NO | UNI |  | Hash SHA256 de token aleatorio; nunca token en claro |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Emision UTC |
| vence_en | datetime(6) | NO |  |  | Caducidad UTC |
| consumida_en | datetime(6) | YES |  |  | Uso del refresh; conservar para detectar reuso |
| revocada_en | datetime(6) | YES |  |  | Revocacion UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_sesion_familia_usuario | familia | auth_familia.id | RESTRICT | RESTRICT |
| fk_sesion_familia_usuario | id_usuario | auth_familia.id_usuario | RESTRICT | RESTRICT |
| fk_sesion_usuario | id_usuario | usuario.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| fk_sesion_familia_usuario | familia | 1 | No |
| fk_sesion_familia_usuario | id_usuario | 2 | No |
| ix_sesion_familia | familia | 1 | No |
| ix_sesion_usuario | id_usuario | 1 | No |
| ix_sesion_usuario | revocada_en | 2 | No |
| ix_sesion_usuario | vence_en | 3 | No |
| PRIMARY | id | 1 | Sí |
| uq_sesion_refresh | refresh_hash | 1 | Sí |

## solicitud_cotizacion

Solicitud sin reserva de horario

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de solicitud |
| id_cliente | bigint | NO | MUL |  | Cliente solicitante |
| id_servicio | bigint | NO | MUL |  | Servicio elegido |
| id_proveedor | bigint | NO | MUL |  | Responsable al solicitar; se conserva historicamente |
| inicio_evento_utc | datetime(6) | NO |  |  | Inicio del evento UTC |
| fin_evento_utc | datetime(6) | NO |  |  | Fin exclusivo del evento UTC |
| lugar | varchar(500) | NO |  |  | Direccion privada del evento |
| ciudad | varchar(100) | NO |  |  | Ciudad del evento |
| tipo_evento | varchar(100) | NO |  |  | Tipo de evento solicitado |
| asistentes | int | YES |  |  | Asistentes previstos |
| preferencias | text | YES |  |  | Preferencias musicales |
| requerimientos | text | YES |  |  | Necesidades del evento |
| notas | text | YES |  |  | Notas privadas |
| estado | varchar(24) | NO |  | ENVIADA | Estado de la solicitud |
| clave_idempotencia | varchar(100) | NO |  |  | Clave de solicitud por cliente |
| version | bigint | NO |  | 0 | Version optimista |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Creacion UTC para indicadores de cohorte |
| primera_respuesta_en | datetime(6) | YES |  |  | Primera respuesta real UTC |
| huella_creacion | binary(32) | YES |  |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_solicitud_cliente | id_cliente | usuario.id | RESTRICT | RESTRICT |
| fk_solicitud_proveedor | id_proveedor | usuario.id | RESTRICT | RESTRICT |
| fk_solicitud_servicio | id_servicio | servicio_musical.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_solicitud_cliente | id_cliente | 1 | No |
| ix_solicitud_cliente | estado | 2 | No |
| ix_solicitud_cliente | creada_en | 3 | No |
| ix_solicitud_proveedor | id_proveedor | 1 | No |
| ix_solicitud_proveedor | estado | 2 | No |
| ix_solicitud_proveedor | creada_en | 3 | No |
| ix_solicitud_servicio | id_servicio | 1 | No |
| ix_solicitud_servicio | inicio_evento_utc | 2 | No |
| ix_solicitud_servicio | fin_evento_utc | 3 | No |
| PRIMARY | id | 1 | Sí |
| uq_solicitud_clave | id_cliente | 1 | Sí |
| uq_solicitud_clave | clave_idempotencia | 2 | Sí |
| uq_solicitud_contexto | id | 1 | Sí |
| uq_solicitud_contexto | id_servicio | 2 | Sí |
| uq_solicitud_contexto | id_cliente | 3 | Sí |
| uq_solicitud_contexto | id_proveedor | 4 | Sí |

## token_acceso

Solo huella del bearer corto; nunca el token en claro

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  |  |
| id_sesion | bigint | NO | MUL |  |  |
| token_hash | binary(32) | NO | UNI |  |  |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) |  |
| vence_en | datetime(6) | NO |  |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_token_acceso_sesion | id_sesion | sesion.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_token_acceso_sesion | id_sesion | 1 | No |
| ix_token_acceso_sesion | vence_en | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_token_acceso_hash | token_hash | 1 | Sí |

## usuario

Cuentas reales; baja logica preserva historia

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador del usuario |
| id_rol | smallint | NO | MUL |  | Rol validado por el servidor |
| nombres | varchar(100) | NO |  |  | Nombres de la persona |
| apellidos | varchar(120) | NO |  |  | Apellidos de la persona |
| correo | varchar(254) | NO |  |  | Correo capturado |
| correo_normalizado | varchar(254) | YES | UNI | lower(trim(`correo`)) | Identidad normalizada; conserva diferencias de acento |
| password_hash | varchar(255) | NO |  |  | Hash adaptativo generado por backend; nunca contrasena |
| telefono | varchar(24) | YES |  |  | Telefono autorizado del perfil |
| ciudad | varchar(100) | YES |  |  | Ciudad del perfil |
| biografia | varchar(1000) | YES |  |  | Texto opcional del perfil |
| estado | varchar(24) | NO |  | PENDIENTE_ACTIVACION | Estado administrativo de acceso |
| version | bigint | NO |  | 0 | Version para bloqueo optimista |
| creado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Instante UTC de creacion |
| actualizado_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Instante UTC de ultima modificacion |
| privacidad_version | varchar(80) | YES |  |  |  |
| privacidad_aceptada_en | datetime(6) | YES |  |  |  |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_usuario_rol | id_rol | rol.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| ix_usuario_rol_estado | id_rol | 1 | No |
| ix_usuario_rol_estado | estado | 2 | No |
| PRIMARY | id | 1 | Sí |
| uq_usuario_correo | correo_normalizado | 1 | Sí |

## valoracion

Valoracion; no se crea encuesta de RF-68 pendiente de conciliacion

| Columna | Tipo | NULL | Clave | Predeterminado o cálculo | Descripción |
|---|---|---|---|---|---|
| id | bigint | NO | PRI |  | Identificador de resena |
| id_contrato | bigint | NO | UNI |  | Unica valoracion por contrato finalizado |
| puntuacion | smallint | NO |  |  | Escala operativa 1-5; no instrumento de encuesta |
| comentario | varchar(2000) | YES |  |  | Comentario autorizado |
| consentimiento_publicacion | tinyint(1) | NO |  | 0 | Permiso expreso de publicacion |
| creada_en | datetime(6) | NO |  | CURRENT_TIMESTAMP(6) | Registro UTC |

### Relaciones

| Restricción | Columna | Referencia | Borrado | Actualización |
|---|---|---|---|---|
| fk_valoracion_contrato | id_contrato | contrato.id | RESTRICT | RESTRICT |

### Índices

| Índice | Columna | Orden | Único |
|---|---|---|---|
| PRIMARY | id | 1 | Sí |
| uq_valoracion_contrato | id_contrato | 1 | Sí |

## Restricciones CHECK

| Nombre | Expresión |
|---|---|
| ck_bloqueo_activo | (`activo` in (0,1)) |
| ck_bloqueo_intervalo | (`inicio_utc` < `fin_utc`) |
| ck_cobro_destino | ((((`yape` = 0) and (`plin` = 0)) or (`telefono` is not null)) and ((`transferencia` = 0) or (`numero_cuenta` is not null) or (`cci` is not null))) |
| ck_cobro_medios | ((`yape` = 1) or (`plin` = 1) or (`transferencia` = 1)) |
| ck_comprobante_origen | (((`id_pago` is not null) + (`id_reembolso` is not null)) = 1) |
| ck_contrato_aceptacion | ((`condiciones_aceptadas` = 1) and (json_type(`snapshot`) = _utf8mb4\'OBJECT\')) |
| ck_contrato_estado | (`estado` in (_utf8mb4\'PENDIENTE_PAGO\',_utf8mb4\'CONFIRMADO\',_utf8mb4\'FINALIZADO\',_utf8mb4\'CANCELADO\')) |
| ck_contrato_fechas | (((`estado` not in (_utf8mb4\'CONFIRMADO\',_utf8mb4\'FINALIZADO\')) or (`confirmado_en` is not null)) and ((`estado` <> _utf8mb4\'FINALIZADO\') or ((`finalizado_en` is not null) and (`finalizado_en` >= `fin_evento_utc`))) and ((`estado` <> _utf8mb4\'CANCELADO\') or ((`cancelado_en` is not null) and (char_length(trim(coalesce(`motivo_cancelacion`,_utf8mb4\'\'))) >= 5)))) |
| ck_contrato_intervalo | ((`inicio_evento_utc` < `fin_evento_utc`) and (`inicio_ocupacion_utc` <= `inicio_evento_utc`) and (`fin_ocupacion_utc` >= `fin_evento_utc`)) |
| ck_contrato_total | (`total_centimos` between 1 and 999999999999) |
| ck_cotizacion_estado | (`estado` in (_ascii\'BORRADOR\',_ascii\'ENVIADA\',_ascii\'ACEPTADA\',_ascii\'RECHAZADA\',_ascii\'CAMBIOS\',_ascii\'VENCIDA\',_ascii\'SUSTITUIDA\')) |
| ck_cotizacion_importes | ((`subtotal_centimos` between 1 and 999999999999) and (`descuento_centimos` >= 0) and (`descuento_centimos` < `subtotal_centimos`)) |
| ck_cotizacion_intervalo | ((`inicio_propuesto_utc` < `fin_propuesto_utc`) and (`fin_propuesto_utc` <= (`inicio_propuesto_utc` + interval 24 hour))) |
| ck_cotizacion_version | (`numero_version` > 0) |
| ck_cotizacion_vigencia | (`vigente_hasta_utc` > `creada_en`) |
| ck_detalle_cantidad | ((`cantidad` > 0) and (`cantidad` <= 1000000)) |
| ck_detalle_orden | (`orden` >= 0) |
| ck_detalle_precio | (`precio_unitario_centimos` between 0 and 999999999999) |
| ck_detalle_total | (`total_centimos` between 0 and 999999999999) |
| ck_evento_pago_estado | (`estado` in (_utf8mb4\'RECIBIDO\',_utf8mb4\'VERIFICANDO\',_utf8mb4\'APLICADO\',_utf8mb4\'IGNORADO\',_utf8mb4\'EN_REVISION\')) |
| ck_evento_pago_intentos | (`intentos` >= 0) |
| ck_horario_dia | (`dia_semana` between 1 and 7) |
| ck_horario_horas | ((`hora_inicio` >= _utf8mb4\'00:00:00\') and (`hora_inicio` < `hora_fin`) and (`hora_fin` <= _utf8mb4\'24:00:00\')) |
| ck_horario_margen | ((`margen_previo_min` between 0 and 1440) and (`margen_posterior_min` between 0 and 1440)) |
| ck_imagen_orden | (`orden` >= 0) |
| ck_imagen_portada | (`es_portada` in (0,1)) |
| ck_imagen_tamano | (`tamano_bytes` > 0) |
| ck_incidencia_estado | (`estado` in (_utf8mb4\'ABIERTA\',_utf8mb4\'EN_REVISION\',_utf8mb4\'RESUELTA\')) |
| ck_incidencia_resuelta | ((`estado` <> _utf8mb4\'RESUELTA\') or (`resuelta_en` is not null)) |
| ck_integrante_activo | (`activo` in (0,1)) |
| ck_integrante_vinculo | (((`id_solista_vinculado` is null) and (`estado_vinculo` = _utf8mb4\'SIN_VINCULO\')) or ((`id_solista_vinculado` is not null) and (`estado_vinculo` in (_utf8mb4\'INVITADO\',_utf8mb4\'ACEPTADO\',_utf8mb4\'RECHAZADO\',_utf8mb4\'RETIRADO\')))) |
| ck_medicion_accion | (`accion` in (_utf8mb4\'INICIO\',_utf8mb4\'PERFIL\',_utf8mb4\'FIN\')) |
| ck_medicion_perfil | ((`accion` <> _utf8mb4\'PERFIL\') or (`id_servicio` is not null)) |
| ck_movimiento_importe | (`importe_centimos` > 0) |
| ck_movimiento_origen | (((`id_pago` is not null) + (`id_reembolso` is not null)) = 1) |
| ck_ocupacion_activa | (`activa` in (0,1)) |
| ck_ocupacion_caducidad | (((`id_retencion` is null) and (`vence_en` is null)) or ((`id_retencion` is not null) and (`vence_en` is not null))) |
| ck_ocupacion_intervalo | (`inicio_utc` < `fin_utc`) |
| ck_ocupacion_origen | ((((`id_reserva` is not null) + (`id_bloqueo` is not null)) + (`id_retencion` is not null)) = 1) |
| ck_outbox_estado | (`estado` in (_utf8mb4\'PENDIENTE\',_utf8mb4\'PROCESANDO\',_utf8mb4\'ENVIADO\',_utf8mb4\'ERROR\')) |
| ck_outbox_intentos | (`intentos` >= 0) |
| ck_pago_estado | (`estado` in (_utf8mb4\'PENDIENTE\',_utf8mb4\'PROCESANDO\',_utf8mb4\'APROBADO\',_utf8mb4\'RECHAZADO\',_utf8mb4\'CANCELADO\',_utf8mb4\'EXPIRADO\',_utf8mb4\'REEMBOLSADO\',_utf8mb4\'REEMBOLSO_PENDIENTE\',_utf8mb4\'EN_REVISION\')) |
| ck_pago_importe | (`importe_centimos` between 1 and 999999999999) |
| ck_pago_modalidad | (`modalidad` in (_utf8mb4\'SALDO_COMPLETO\',_utf8mb4\'MITAD_SALDO\')) |
| ck_pago_moneda | (`moneda` = _utf8mb4\'PEN\') |
| ck_pago_verificacion | (((`cobrado_en` is null) or ((`verificado_en` is not null) and (`referencia_externa` is not null))) and ((`aplicado_en` is null) or (`cobrado_en` is not null)) and ((`estado` <> _utf8mb4\'APROBADO\') or (`aplicado_en` is not null))) |
| ck_recuperacion_vigencia | (`vence_en` > `creada_en`) |
| ck_recurso_origen | (((`id_servicio` is not null) + (`id_musico` is not null)) = 1) |
| ck_reembolso_confirmacion | ((`estado` <> _ascii\'APROBADO\') or ((`confirmado_en` is not null) and (`referencia_externa` is not null))) |
| ck_reembolso_estado | (`estado` in (_ascii\'PENDIENTE\',_ascii\'PROCESANDO\',_ascii\'APROBADO\',_ascii\'RECHAZADO\')) |
| ck_reembolso_importe | (`importe_centimos` between 1 and 999999999999) |
| ck_repertorio_activo | (`activo` in (0,1)) |
| ck_repertorio_duracion | ((`duracion_segundos` is null) or (`duracion_segundos` > 0)) |
| ck_repertorio_orden | (`orden` >= 0) |
| ck_reserva_cancelacion | ((`estado` <> _utf8mb4\'CANCELADA\') or (`cancelada_en` is not null)) |
| ck_reserva_estado | (`estado` in (_utf8mb4\'CONFIRMADA\',_utf8mb4\'CANCELADA\')) |
| ck_retencion_estado | (`estado` in (_utf8mb4\'ACTIVA\',_utf8mb4\'CONVERTIDA\',_utf8mb4\'EXPIRADA\',_utf8mb4\'CANCELADA\')) |
| ck_retencion_intervalo | ((`inicio_utc` < `fin_utc`) and (`vence_en` > `creada_en`)) |
| ck_rol_codigo | (`codigo` in (_utf8mb4\'ADMINISTRADOR\',_utf8mb4\'CLIENTE\',_utf8mb4\'DIRECTOR\',_utf8mb4\'SOLISTA\')) |
| ck_servicio_estado | (`estado_publicacion` in (_ascii\'BORRADOR\',_ascii\'EN_REVISION\',_ascii\'OBSERVADO\',_ascii\'PUBLICADO\',_ascii\'RETIRADO\')) |
| ck_servicio_nombre | (char_length(trim(`nombre`)) > 0) |
| ck_servicio_tarifa | ((`tarifa_centimos` is null) or (`tarifa_centimos` between 0 and 999999999999)) |
| ck_servicio_tipo | (`tipo` in (_ascii\'BANDA\',_ascii\'SOLISTA\')) |
| ck_servicio_version | (`version` >= 0) |
| ck_sesion_vigencia | (`vence_en` > `creada_en`) |
| ck_solicitud_asistentes | ((`asistentes` is null) or (`asistentes` > 0)) |
| ck_solicitud_estado | (`estado` in (_ascii\'ENVIADA\',_ascii\'EN_REVISION\',_ascii\'COTIZACION_RECIBIDA\',_ascii\'RECHAZADA\',_ascii\'CANCELADA\',_ascii\'CONTRATADA\',_ascii\'RESERVADA\')) |
| ck_solicitud_intervalo | ((`inicio_evento_utc` < `fin_evento_utc`) and (`fin_evento_utc` <= (`inicio_evento_utc` + interval 24 hour))) |
| ck_token_acceso_vigencia | (`vence_en` > `creado_en`) |
| ck_usuario_estado | (`estado` in (_ascii\'PENDIENTE_ACTIVACION\',_ascii\'ACTIVO\',_ascii\'SUSPENDIDO\',_ascii\'DESACTIVADO\')) |
| ck_usuario_nombre | ((char_length(trim(`nombres`)) > 0) and (char_length(trim(`apellidos`)) > 0)) |
| ck_usuario_privacidad | (((`privacidad_version` is null) and (`privacidad_aceptada_en` is null)) or ((`privacidad_version` is not null) and (`privacidad_aceptada_en` is not null))) |
| ck_usuario_version | (`version` >= 0) |
| ck_valoracion_consentimiento | (`consentimiento_publicacion` in (0,1)) |
| ck_valoracion_puntuacion | (`puntuacion` between 1 and 5) |

## Triggers

| Nombre | Tabla | Momento | Evento |
|---|---|---|---|
| tr_contrato_actualizar | contrato | BEFORE | UPDATE |
| tr_contrato_eliminar | contrato | BEFORE | DELETE |
| tr_contrato_insertar | contrato | BEFORE | INSERT |
| tr_cotizacion_actualizar | cotizacion | BEFORE | UPDATE |
| tr_cotizacion_eliminar | cotizacion | BEFORE | DELETE |
| tr_detalle_actualizar | detalle_cotizacion | BEFORE | UPDATE |
| tr_detalle_eliminar | detalle_cotizacion | BEFORE | DELETE |
| tr_detalle_insertar | detalle_cotizacion | BEFORE | INSERT |
| tr_valoracion_insertar | valoracion | BEFORE | INSERT |
