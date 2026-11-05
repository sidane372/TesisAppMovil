# Base de datos implementada

El esquema vigente tiene **36 tablas de negocio y una tabla de historial Flyway**, con migraciones V001–V007 aplicadas en MySQL 8.0.30 local. Android utiliza Room v3 exclusivamente para caché pública; las operaciones de negocio se guardan en MySQL a través de la API.

| Archivo | Contenido |
|---|---|
| Santa_Cecilia_Tablas.sql | Archivo único importable con las 36 tablas, relaciones, restricciones, triggers, vista y cuatro roles |
| COMO_IMPORTAR_MYSQL.md | Pasos para abrir e importar la copia de consulta en MySQL |
| Diccionario_Datos_Actual.md | Columnas, PK, FK, índices, CHECK y triggers consultados en INFORMATION_SCHEMA |
| Modelo_Relacional_Actual.mmd | Relaciones del esquema aplicado |
| metadatos_esquema_actual.json | Metadatos verificables sin datos personales ni credenciales |
| resumen_esquema.json | Conteos del esquema vigente |
| Modelo_Logico_Actual.md | Modelo vigente y relación entre el negocio y la persistencia |
| modelo_logico.md | Modelo conceptual histórico con nota de evolución |
| protocolo_agenda.md | Reglas de exclusión y concurrencia |
| SQL/ | Copia de entrega de migraciones, consultas y creación de esquema |

Las copias SQL son instantáneas de `database/`, que sigue siendo la fuente de mantenimiento del proyecto. Sus hashes se incluyen en el manifiesto de entrega. No importar a una base con historial distinto ni editar migraciones ya aplicadas. En este equipo, INICIAR_SANTA_CECILIA.cmd prepara el entorno sin importación manual.

La evidencia histórica de 31 comprobaciones SQL de V001–V004 se conserva en el entregable 11. El esquema actual no se acredita con los conteos antiguos de 30 tablas. Las verificaciones de contratación y pagos se describen por separado y utilizaron datos sintéticos. No se incluye una copia de datos de personas ni de la base persistente.
