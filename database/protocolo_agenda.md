# Protocolo transaccional de agenda

El esquema incluye recurso_agenda, ocupacion_agenda y las fuentes bloqueo_agenda, retencion_agenda y reserva. Este protocolo está verificado con conexiones simultáneas en los tests 24 y 25; el futuro backend debe aplicarlo a toda escritura de agenda. No hay una restricción UNIQUE que por sí sola excluya intervalos solapados.

## Recursos

Cada servicio tiene una fila estable en recurso_agenda(id_servicio). Cada músico identificado que pueda compartirse tiene recurso_agenda(id_musico). Un servicio individual utiliza su recurso y el del solista. Una banda utiliza su recurso y los de músicos con vínculos aceptados/activos, además de los recursos musicales que su política determine. Una fila tiene exactamente un origen; los dos tipos no se confunden.

El conjunto se toma al aceptar la propuesta y se conserva en el snapshot del contrato y en las ocupaciones. Retirar un integrante después no libera un compromiso histórico; primero se debe resolver la sustitución y validar todos los recursos implicados. Un integrante sin usuario vinculado no puede identificarse automáticamente como el mismo músico de otra banda. Esa limitación debe resolverse mediante vínculo consentido o gestión explícita de recursos, sin inventar identidades.

Los recursos deben crearse cuando se registra el servicio/músico. Si falta alguno al confirmar, el backend falla la operación; no se continúa con un bloqueo vacío. El proceso de alta y los cambios de vínculos también deberán coordinarse al implementar el backend.

## Algoritmo común

1. Abrir una transacción InnoDB. Resolver el conjunto completo de recursos de la operación/snapshot y ordenar sus IDs de menor a mayor.
2. Bloquear cada fila mediante queries/bloquear_recurso.sql. Todas las operaciones (reserva, bloqueo, retención, cambio de horario y sustitución) deben respetar ese mismo orden. Si además se bloquea el contrato, adoptar siempre recursos → contrato y evitar invertir el orden en otra ruta.
3. Con el mutex de recursos adquirido, consultar las ocupaciones actuales mediante queries/ocupaciones_en_conflicto.sql. FOR UPDATE hace una lectura actual, evitando usar una vista antigua de REPEATABLE READ. Las lecturas con bloqueo se mantienen hasta commit/rollback. [Manual de MySQL](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html).
4. Aplicar márgenes sobre el intervalo aceptado. Existe cruce si inicioExistente < finNuevo AND finExistente > inicioNuevo. Por tanto [14:30,16:30) y [16:30,18:00) son contiguos y compatibles.
5. Rechazar si algún recurso tiene ocupación activa no vencida. Para convertir una retención propia en reserva, identificarla de manera explícita, validarla y desactivar sus ocupaciones antes de registrar las definitivas en la misma transacción; no ignorar todas las ocupaciones de un contrato sin verificar su origen.
6. Crear/actualizar la fuente (bloqueo, retención o reserva) y una ocupación por cada recurso. Mantener intervalos/caducidad coherentes entre fuente, snapshot y ocupaciones. La tabla exige un solo origen y un registro por recurso/origen, pero las coincidencias de fechas entre tablas se validan en el Service.
7. Al confirmar el primer pago verificado: aplicar abono, confirmar contrato, crear reserva única, ocupaciones, historial y outbox dentro de la misma transacción. Los abonos posteriores conservan la reserva. Cancelar/liberar/desactivar fuentes y ocupaciones también debe ser atómico y seguir la política aceptada.
8. Commit; ante conflicto, rollback. Reintentar deadlocks/timeouts de forma acotada usando la misma clave de idempotencia y reevaluando datos, nunca asumiendo que la primera lectura sigue vigente.

Las comprobaciones de días/horario de atención, fechas válidas y permisos también son obligatorias. Las consultas públicas de calendario no toman decisiones de confirmación.

## Pagos y retenciones

No se mantienen bloqueos MySQL durante el checkout ni al consultar una API financiera. Registrar primero intento/retención en una transacción corta; después llamar al proveedor. Al llegar el resultado, verificarlo fuera de la transacción y abrir una nueva para aplicar efectos internos. Antes de confirmar, revalidar recurso, contrato, saldo y retención.

Una retención vencida deja de excluir por vence_en aunque su fila siga marcada ACTIVA hasta que un conciliador la cierre. Un cobro tardío que encuentra el intervalo ocupado permanece como evidencia externa sin aplicar al contrato y origina una incidencia/devolución; no confirma una reserva superpuesta. La devolución requiere confirmación del proveedor, no rollback SQL.

## Límite de la garantía

La base rechaza orígenes/FK/intervalos inválidos y duplicados por contrato/recurso. MySQL no ejecuta este protocolo automáticamente cuando alguien inserta ocupaciones a mano. Una cuenta que omita el bloqueo y escriba SQL arbitrario puede crear cruces. La cuenta de aplicación será privada del servidor y todos sus repositorios deberán usar este protocolo, con pruebas de integración HTTP en Etapa 3. Las pruebas actuales verifican la serialización SQL del protocolo, no todos los futuros puntos de entrada del sistema.
