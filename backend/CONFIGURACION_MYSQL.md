# Configuración MySQL del backend ejecutable

El servidor usa JPA con ddl-auto=validate y Flyway con scripts empacados en classpath:db/migration. El JAR incluye V001–V007. No depende del directorio desde el que se ejecute para localizar SQL y no actualiza tablas mediante Hibernate.

## Esquema nuevo

Crear una base vacía santa_cecilia y cuentas distintas para aplicación/migraciones, con contraseñas propias. Configurar DB_URL, DB_USERNAME, DB_PASSWORD, DB_MIGRATION_USERNAME y DB_MIGRATION_PASSWORD según .env.example. El script tools/Iniciar-Backend.ps1 carga el archivo de variables sin ejecutar shell embebido y rechaza los marcadores REEMPLAZAR.

La cuenta de migración necesita privilegios de esquema para tablas, índices, referencias, triggers, vistas e historial Flyway; nunca requiere GRANT OPTION. La cuenta de aplicación necesita SELECT/INSERT/UPDATE en el esquema y DELETE limitado a servicio_genero, servicio_instrumento, servicio_tipo_evento, detalle_cotizacion, imagen_servicio y horario_atencion, según los módulos habilitados. No utiliza root ni permisos DDL. En un MySQL con binlog se deben revisar los requisitos de creación de triggers con su administrador antes de migrar.

El arranque y las operaciones de esta etapa se comprobaron con cuentas separadas y esos permisos en una instancia MySQL 8.0.30 aislada. No se crearon cuentas o bases en la instalación habitual de Laragon.

## Base con V001–V004 ya instalada manualmente

Si la base proviene del instalador manual de Etapa 2 y no tiene flyway_schema_history, el backend rechazará migrar automáticamente un esquema no vacío. Antes de integrar Flyway, revisar que corresponde exactamente a V004 y registrar un baseline explícito en versión 4 con las herramientas de operación elegidas; después se aplicarán V005, V006 y V007. No activar baseline-on-migrate ni clean para ocultar diferencias o borrar información. Los cuatro scripts anteriores no se editaron.

V005 incorpora auth_familia y token_acceso, enlaza sesiones a su familia/titular y agrega consentimiento de privacidad. Hace backfill de familias existentes; si una misma familia contiene titulares mezclados, la FK falla para que se revise la inconsistencia. No se inventan ni migran contraseñas de usuarios del prototipo.

## Variables de entorno

- DB_URL: JDBC del servidor elegido; connectionTimeZone=UTC y forceConnectionTimeZoneToSession=true. Hikari también establece la sesión UTC. Configurar TLS con verificación de identidad para una base remota.
- DB_USERNAME/DB_PASSWORD: identidad privada de aplicación.
- DB_MIGRATION_USERNAME/DB_MIGRATION_PASSWORD: identidad exclusiva para migraciones.
- SERVER_ADDRESS/SERVER_PORT: por defecto 127.0.0.1:8080 para desarrollo.
- APP_BOOTSTRAP_*: creación inicial explícita de administrador; deshabilitar después. No se cambia una cuenta existente ni se elevan clientes.
- SMTP_* y APP_PUBLIC_URL: envío real de recuperación y URL pública del formulario; sin esos valores la recuperación devuelve 503.

El backend no carga .env por sí solo: se usa Iniciar-Backend.ps1 o se configuran variables del proceso/IDE. No se guardan contraseñas de MySQL, SMTP o personas en el repositorio ni en Android. Los tokens de acceso/refresh/recuperación se generan aleatoriamente y solo sus hashes quedan en MySQL; la contraseña humana usa bcrypt.

Para despliegue todavía se deben configurar el servidor persistente, HTTPS, certificados, respaldo/restauración, gateway y SMTP del entorno. La comprobación local no acredita ese despliegue.

V006 agrega idempotencia de solicitudes/cotizaciones y versión de agenda, y completa los recursos históricos faltantes. No incluye datos de demostración ni habilita atención automáticamente. El proveedor configura las franjas desde Android.

V007 incorpora cuentas receptoras, comprobantes privados, movimientos conciliados y seguimiento de devoluciones. Utiliza los mismos permisos SELECT/INSERT/UPDATE de aplicación; no requiere DELETE adicional ni acceso a bancos. La configuración de cuentas receptoras se hace desde Android, nunca con claves de banca en .env.
