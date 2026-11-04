-- Parametros enlazados por el futuro Repository. Ejecutar dentro de START TRANSACTION.
-- Repetir en ORDEN ASCENDENTE de id para TODOS los recursos afectados.
SELECT id FROM recurso_agenda WHERE id = :id_recurso FOR UPDATE;
