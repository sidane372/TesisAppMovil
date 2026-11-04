# Migraciones de la base central

Orden: V001__modelo_central.sql → V002__roles_del_sistema.sql → V003__inmutabilidad_y_validaciones.sql → V004__consulta_saldos.sql.

Las cuatro migraciones se aplicaron en un esquema vacío de MySQL 8.0.30. Sus SHA-256 y resultados están en ../tests/evidence/resultado_mysql.json. Solo V002 inserta datos: los cuatro roles del sistema. Los datos sintéticos de prueba se crean aparte en tests/test_schema.py y no forman parte de una instalación.

Tras esta entrega las migraciones se consideran línea base versionada: las evoluciones deben usar V005 y siguientes, sin editar archivos ya aplicados en otros entornos. El identificador y checksum de cada script se incorporarán al historial de Flyway en el backend de Etapa 3. Las pruebas actuales aplican los scripts en orden mediante mysql; no se afirma que Flyway ya se haya ejecutado.

El esquema usa InnoDB, utf8mb4_0900_ai_ci, referencias explícitas y ON DELETE/UPDATE RESTRICT. Los identificadores financieros sensibles a mayúsculas tienen collation ascii_bin y el correo normalizado usa una collation sensible a acentos. Todas las conexiones deben establecer time_zone='+00:00'.

V003 contiene DELIMITER para definir triggers con el cliente mysql. Las consultas de ../queries/ con :parametros son plantillas para el backend y no son migraciones. Las tablas con historia contractual no se recrean ni eliminan con cascadas. ddl-auto=validate sustituirá cualquier actualización automática del esquema JPA.

No hay downgrade automático ni script de borrado de datos. Los cambios futuros que lo necesiten deberán acompañarse de respaldo, plan de recuperación y pruebas sobre una copia, conforme al entorno real.

## V005 — Etapa 3

V005__autenticacion_y_consentimiento.sql agrega auth_familia, token_acceso, FK de familia/titular en sesion y consentimiento en usuario. El backend ya incorpora las cinco migraciones al JAR; Flyway las aplicó sobre MySQL 8.0.30 y Hibernate validó sus entidades. No se editó V001–V004. La extensión completa está en ../autenticacion_etapa3.md.

## V006 — Etapa 6

V006__flujo_contratacion.sql agrega huellas de creación/envío, clave única de envío por solicitud, agenda_version y backfill de recursos. Flyway ejecutó V001–V006 en MySQL 8.0.30 durante la verificación del flujo HTTP. No crea servicios, horarios ni clientes. Ver ../../docs/contratacion_agenda.md.

## V007 — pagos directos, Etapa 7

Agrega cuenta_cobro, pago_directo, comprobante_directo y movimiento_cuenta, además de seguimiento idempotente/versionado en reembolso. El usuario eligió Yape/Plin/transferencia sin pasarela. Flyway aplicó las siete migraciones y Hibernate validó el esquema. No incluye cuentas receptoras, usuarios ni comprobantes de demostración. El total actual es 36 tablas de negocio. Ver ../../docs/pagos_arquitectura.md.
