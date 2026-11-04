-- Ejecutar desde la raiz del repositorio con el cliente mysql, en esquema NUEVO.
-- Este instalador manual NO sustituye el historial/checksums de Flyway en el backend.
SOURCE database/bootstrap/crear_esquema.sql;
USE santa_cecilia;
SOURCE database/migrations/V001__modelo_central.sql;
SOURCE database/migrations/V002__roles_del_sistema.sql;
SOURCE database/migrations/V003__inmutabilidad_y_validaciones.sql;
SOURCE database/migrations/V004__consulta_saldos.sql;

-- Corte manual histórico V004 de Etapa 2. Para el sistema actual utilizar Flyway V001–V006
-- siguiendo backend/CONFIGURACION_MYSQL.md; no volver a aplicar V001 sobre una base existente.
