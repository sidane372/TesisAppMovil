# Modelo lógico actual de Santa Cecilia

La base central representa identidad y roles, servicios musicales y su contenido, solicitudes y propuestas versionadas, snapshots contractuales, recursos compartidos de agenda y movimientos de pago conciliados. Las relaciones se materializan mediante PK, FK y restricciones consultadas en MySQL. Los importes se almacenan en céntimos PEN; los instantes de operaciones utilizan UTC y la presentación horaria usa America/Lima.

La consulta del catálogo es pública solo para servicios publicados. La solicitud no reserva. La aceptación expresa genera un contrato pendiente de pago, que conserva sus recursos y condiciones. El primer ingreso contrastado y aplicable confirma el contrato y crea la reserva bajo la misma transacción. Se conservan movimientos no aplicados con incidencia y seguimiento de devoluciones. El sistema no opera cuentas bancarias ni usa retenciones con una cuenta regresiva.

Tablas de negocio del esquema aplicado:

- auth_familia
- bloqueo_agenda
- comprobante_directo
- contrato
- cotizacion
- cuenta_cobro
- detalle_cotizacion
- evento_medicion
- evento_outbox
- evento_pago
- historial_operacion
- horario_atencion
- imagen_servicio
- incidencia_pago
- integrante
- movimiento_cuenta
- notificacion
- ocupacion_agenda
- pago
- pago_directo
- recuperacion_acceso
- recurso_agenda
- reembolso
- repertorio
- reserva
- retencion_agenda
- rol
- servicio_genero
- servicio_instrumento
- servicio_musical
- servicio_tipo_evento
- sesion
- solicitud_cotizacion
- token_acceso
- usuario
- valoracion

La existencia de estructuras como valoraciones, auditoría o retenciones no acredita una interfaz completa de ese requisito. El backlog conserva las funciones parciales y pendientes. Room v3 pertenece a Android y almacena únicamente caché pública, no el esquema central ni datos financieros.

La tabla flyway_schema_history registra las siete migraciones aplicadas. El diccionario físico incluye todas las columnas, claves e índices. El diagrama actual representa nulabilidad y unicidad según la definición real de las claves foráneas; no es una muestra de datos ni una exportación de personas.
