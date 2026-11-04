-- Ejecutar DESPUES de bloquear todos los recursos, en la MISMA transaccion.
-- Lectura actual (FOR UPDATE), no snapshot antiguo de REPEATABLE READ.
-- Intervalos [inicio, fin); limites contiguos no se solapan.
SELECT id, id_reserva, id_bloqueo, id_retencion
FROM ocupacion_agenda
WHERE id_recurso = :id_recurso AND activa = 1
  AND inicio_utc < :fin_utc AND fin_utc > :inicio_utc
  AND (vence_en IS NULL OR vence_en > UTC_TIMESTAMP(6))
FOR UPDATE;
