# Importar las tablas en MySQL

Archivo: Santa_Cecilia_Tablas.sql.

1. Conectarse a MySQL 8.0.30 o superior desde MySQL Workbench u otro cliente SQL.
2. Abrir Santa_Cecilia_Tablas.sql y ejecutar el script completo.
3. Actualizar la lista de esquemas y abrir santa_cecilia_tablas para consultar las tablas.

El archivo crea una base nueva llamada santa_cecilia_tablas. Si ya existe, utiliza otra base nueva modificando todas las apariciones de ese nombre antes de importar. No importarlo encima de una base con información.

Contiene 36 tablas de negocio, 55 claves foráneas, 75 CHECK, 9 triggers, una vista de saldos y los cuatro roles de referencia. No incluye usuarios ni contraseñas. Flyway mantiene una tabla técnica adicional en la base operativa de la app, que no se copia aquí.

La base habitual de Android sigue siendo santa_cecilia en el puerto 33306 de este equipo. Esta copia sirve para revisar las tablas o entregarlas. Para ejecutar el backend, utilizar el arranque gestionado y sus migraciones; importar esta copia no configura las cuentas MySQL ni el historial de Flyway del sistema.

Tablas:

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
