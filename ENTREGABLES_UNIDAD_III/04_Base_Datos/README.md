# Base de datos — Etapa 2 implementada y verificada localmente

Se crearon 30 tablas MySQL InnoDB, 40 FK, 68 CHECK, índices, 9 triggers y una vista de saldos. Las cuatro migraciones se aplicaron en MySQL Community 8.0.30 y pasaron 31 pruebas SQL, incluidas dos conexiones simultáneas para el mismo recurso/músico compartido.

| Entregable | Fuente única |
|---|---|
| Guía de instalación y reproducción | ../../database/README.md |
| Modelo lógico y decisiones | ../../database/modelo_logico.md |
| Diccionario físico completo de 276 columnas | ../../database/diccionario_datos.md |
| Diagrama de relaciones | ../../database/diagramas/modelo_relacional.mmd |
| Scripts versionados | ../../database/migrations/V001__modelo_central.sql a V004__consulta_saldos.sql |
| PK, FK, índices y restricciones | Diccionario generado desde INFORMATION_SCHEMA; metadata del reporte |
| Protocolo de agenda y recursos compartidos | ../../database/protocolo_agenda.md |
| Pruebas de consultas/rollback/concurrencia | ../../database/tests/test_schema.py |
| Evidencia real y hashes | ../../database/tests/evidence/resultado_mysql.json |
| Informe de resultados y límites | ../../docs/pruebas/verificacion_etapa2.md |
| Configuración prevista para backend | ../../backend/CONFIGURACION_MYSQL.md y .env.example |

Se enlazan las fuentes para evitar mantener copias SQL divergentes. La instancia de pruebas fue detenida y no modifica Laragon. No hay aún un servidor MySQL productivo desplegado, backend/API conectado, migración Flyway ejecutada ni pasarela financiera. La base local Room v1 sigue separada del esquema central.
