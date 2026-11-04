# Verificación MySQL — Etapa 2

Resultado final: **31 pruebas aprobadas, 0 fallos y 0 errores**, MySQL Community 8.0.30. Finalizó el 03/11/2026 14:52:55, America/Lima.

La ejecución utilizó los binarios instalados en C:\laragon\bin\mysql\mysql-8.0.30-winx64, con datadir propio bajo database/.local/ y puerto 33307 limitado a 127.0.0.1. No se usaron las bases de Laragon ni se instaló un servicio. La instancia se detuvo al terminar. El esquema de prueba fue sc_test_4be2f1458c6e.

## Evidencias

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Migraciones | Cuatro scripts aplicados desde esquema vacío | database/tests/evidence/resultado_mysql.json, lista migrations con SHA-256 |
| Tablas / columnas | 30 tablas InnoDB, 276 columnas físicas | schema_before_fixtures y metadata.columns |
| Integridad | 40 FK y 68 CHECK aplicados; índices PK/UNIQUE/consulta | metadata.foreign_keys, constraints, checks e indexes |
| Protección de historia | 9 triggers creados | metadata.triggers |
| Saldos | Vista exacta con SQL SECURITY INVOKER | metadata.views y prueba 18 |
| Migración de referencia | Solo cuatro roles; cero usuarios antes de fixtures | Prueba 01 |
| Concurrencia | Segunda conexión espera el bloqueo y detecta conflicto | Pruebas 24 y 25 |
| SQL reutilizable | Consultas de recurso/conflicto son leídas por el runner | database/queries/ y tests/test_schema.py |

## Casos ejecutados

| Prueba | Resultado | Duración observada (s) |
|---|---|---|
| test_01_reference_roles_without_demo_accounts | Aprobada | 0.094 |
| test_02_innodb_utf8_and_enforced_checks | Aprobada | 0.093 |
| test_03_email_normalization_unique | Aprobada | 0.157 |
| test_04_orphan_foreign_key_rejected | Aprobada | 0.031 |
| test_05_one_solo_service_per_musician | Aprobada | 0.14 |
| test_06_negative_tariff_rejected | Aprobada | 0.094 |
| test_07_reversed_interval_rejected | Aprobada | 0.063 |
| test_08_quote_version_unique_within_request | Aprobada | 0.109 |
| test_09_sending_quote_requires_exact_detail_sum | Aprobada | 0.156 |
| test_10_decimal_quantity_rounding_is_exact | Aprobada | 0.094 |
| test_11_sent_quote_and_details_are_immutable | Aprobada | 0.156 |
| test_12_one_contract_even_across_quote_versions | Aprobada | 0.125 |
| test_13_contract_cannot_mix_request_and_quote | Aprobada | 0.203 |
| test_14_contract_snapshot_cannot_change_or_delete | Aprobada | 0.125 |
| test_15_payment_cannot_name_another_client | Aprobada | 0.094 |
| test_16_approval_needs_verification_and_application | Aprobada | 0.188 |
| test_17_payment_and_webhook_deduplication | Aprobada | 0.25 |
| test_18_exact_balance_does_not_multiply_refunds | Aprobada | 0.297 |
| test_19_payment_transaction_rolls_back_on_invalid_reservation | Aprobada | 0.125 |
| test_20_one_reservation_per_contract | Aprobada | 0.093 |
| test_21_occupation_requires_one_origin | Aprobada | 0.094 |
| test_22_expired_hold_does_not_block | Aprobada | 0.156 |
| test_23_adjacent_intervals_are_allowed | Aprobada | 0.11 |
| test_24_two_connections_cannot_occupy_same_resource | Aprobada | 1.14 |
| test_25_shared_musician_blocks_different_bands | Aprobada | 1.157 |
| test_26_pending_contract_cannot_be_reviewed_or_finished_early | Aprobada | 0.125 |
| test_27_historical_foreign_keys_restrict_deletion | Aprobada | 0.093 |
| test_28_active_hold_blocks_same_interval | Aprobada | 0.157 |
| test_29_refund_reference_is_unique_across_payments | Aprobada | 0.187 |
| test_30_only_one_cover_image_per_service | Aprobada | 0.109 |
| test_31_session_hash_unique_and_expiry_ordered | Aprobada | 0.172 |

## Naturaleza de los datos y límites

Los nombres, correos example.invalid, intervalos 2030 e importes se crearon únicamente como fixtures SQL. Las filas financieras se identifican como FIXTURE_SQL_NO_PASARELA. No son cuentas reales, transacciones bancarias, resultados de investigación ni pruebas sandbox de una pasarela. Las claves foráneas y timestamps prueban estructura; no acreditan identidad o firma de un proveedor.

El caso 18 verifica total 10001 céntimos, abonos aplicados de 5001 y devoluciones de 1000: saldo esperado y obtenido 6000. Los intentos rechazados/cobros en revisión no aplicados no reducen ese saldo. La agregación evita multiplicar pagos al unir varias devoluciones.

Las pruebas 24/25 ejecutan dos conexiones y el protocolo recursos ordenados → lectura actual → escritura → commit. Verifican exclusión para el mismo servicio y para un músico compartido por bandas distintas. No demuestran que una inserción SQL que omita el protocolo quede automáticamente bloqueada por una restricción temporal. Eso se debe exigir a todos los repositorios del backend.

La instalación se probó con mysql, no con Flyway, JPA ni Spring Boot. No se probó MySQL 8.4, backup/restauración, TLS de un servidor remoto, grants productivos, API/autorización por HTTP o pasarela. Las seis pruebas instrumentadas Android de Etapa 1 siguen pendientes de un dispositivo conectado. No fue necesario repetir la compilación Android porque esta etapa no cambió sus fuentes/Gradle.

Incidencia de la herramienta: la primera comprobación del datadir aislado leyó la ruta con escapes del cliente mysql; se corrigió con --raw antes de aplicar migraciones. No se modificó una base equivocada. El reporte final solo contiene la ejecución aprobada y sus scripts efectivos.

## Reproducción

```powershell
.\tools\Test-Database.ps1 -MysqlHome 'C:\laragon\bin\mysql\mysql-8.0.30-winx64' -Python python
python .\tools\export_database_docs.py
```

El primer comando crea una instancia y esquema nuevos y siempre intenta detenerlos en finally; el segundo exige un reporte exitoso y SHA-256 coincidentes antes de generar diccionario/diagrama. Los logs de servidor son locales e ignorados por Git; el JSON de evidencia contiene metadatos y resultados, sin contraseñas.
