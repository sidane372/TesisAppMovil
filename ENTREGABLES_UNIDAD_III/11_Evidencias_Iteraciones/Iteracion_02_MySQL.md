# Iteración 02 — esquema MySQL

Verificación final: 03/11/2026 14:52:55, America/Lima. Objetivo: materializar el modelo central de la arquitectura y validar integridad/agenda en MySQL real.

Actividades verificables: cuatro migraciones SQL, 30 tablas InnoDB, PK/FK/UNIQUE/CHECK e índices, triggers de historia, vista exacta de saldos, protocolo de recursos y consultas del futuro Repository. Se generaron diccionario y diagrama desde metadatos del esquema creado y se preparó configuración externa para Spring Boot.

Pruebas: 31 aprobadas en MySQL 8.0.30; reporte ../../database/tests/evidence/resultado_mysql.json e informe ../../docs/pruebas/verificacion_etapa2.md. Incluyen cruces concurrentes del mismo servicio/músico compartido, retenciones activas/vencidas, límites contiguos, snapshot/versiones, FK compuestas, duplicados, redondeo exacto, saldos, reembolsos y rollback interno.

Datos: fixtures sintéticos SQL, sin cuentas reales ni transacciones de pasarela. Capturas: no se fabrican imágenes de un administrador de base de datos; la evidencia es ejecución SQL y metadatos. Commits: no disponibles, carpeta todavía sin Git. Incidencia de herramientas: ruta del datadir escapada, corregida con --raw antes de migrar. No se tocaron las bases de Laragon.

Se localizó e inspeccionó LaboratorioROOM (1).zip en Descargas; diagnóstico actualizado en ../../docs/diagnostico_inicial.md. Pendientes: backend real/autenticación/roles, ejecución de Flyway/JPA, conexión API Android, despliegue/grants/TLS, backup y pasarela oficial. RF-68 conserva la discrepancia de encuesta pendiente, sin tabla sustitutiva.
