# Etapa 6 — contrataciones y agenda

Implementación Java/XML MVC conectada al backend Spring Boot y MySQL. Se mantienen las decisiones de autenticación real, HTTPS y ausencia de pagos simulados. Las instrucciones de demostración de los documentos de referencia no crean cuentas ni cobros en esta entrega.

## Uso

1. El proveedor abre **Calendario**, selecciona su servicio y configura las franjas de atención. Sin franjas, el servicio no recibe solicitudes. Se pueden agregar hasta 28 intervalos semanales y márgenes antes/después. La hora es de Perú; `24:00` sirve como cierre del día. Un cruce de medianoche requiere una franja por cada día.
2. El cliente abre un perfil público y **Disponibilidad y solicitud**. Consulta el mes, selecciona un día habilitado e inicia el asistente de tres pasos: horario, evento y revisión. La solicitud no reserva el intervalo.
3. El proveedor consulta **Solicitudes**, marca revisión, rechaza con motivo o elabora una cotización. El compositor admite hasta 15 conceptos, cantidades enteras, precios exactos, descuento, vigencia, condiciones y política de cancelación obligatoria. Cada envío conserva una versión nueva.
4. El cliente revisa el desglose, las condiciones y las versiones. Puede pedir cambios, rechazar la propuesta o aceptar expresamente la última versión vigente. El administrador consulta el conjunto global y no acepta en nombre del cliente.
5. La aceptación crea un único contrato **PENDIENTE_PAGO**, sin confirmación, reserva ni retención. El detalle conserva los actores, servicio, evento, conceptos, integrantes, recursos y política aceptados. Cambiar después la tarifa del servicio no modifica ese snapshot.
6. El proveedor puede bloquear intervalos futuros y retirar sus bloqueos. El calendario público muestra únicamente horas y clasificación; los motivos quedan en la agenda privada.

Las bandejas tienen búsqueda, estado y paginación de 20 elementos. El menú del cliente incorpora solicitudes/contratos; el del proveedor solicitudes/calendario, con contratos en el drawer; el administrador consulta solicitudes, contratos y agenda desde su navegación. Los contadores de solicitudes abiertas y próximos eventos confirmados consultan datos reales del servidor.

## Reglas implementadas

- Eventos futuros, fin posterior al inicio y duración máxima de 24 horas. El backend valida la cobertura completa por franjas locales y agrega los márgenes a la comprobación de ocupación.
- Intervalos semiabiertos: el fin de uno puede coincidir con el inicio del siguiente, considerando preparación.
- Bloqueo transaccional de filas estables de `recurso_agenda` en orden ascendente. Se considera el servicio, su responsable y los solistas con vínculo activo **ACEPTADO** existente. No se deducen identidades a partir de nombres descriptivos.
- Las ocupaciones de un bloqueo conservan su conjunto original. El snapshot contractual también congela sus recursos. No hay una interfaz nueva de invitaciones o sustitución de integrantes en esta etapa.
- Las escrituras del flujo utilizan `READ COMMITTED`, mutexes de recurso y lecturas de conflicto `FOR UPDATE`. Se vuelve a comprobar el conjunto de recursos tras bloquear y se reintenta hasta tres veces ante errores de bloqueo transitorios. Véase [protocolo de agenda](../database/protocolo_agenda.md).
- Claves de idempotencia y huellas SHA-256 para solicitud, envío de cotización y aceptación. Reutilizar una clave con otra entrada da conflicto. Las restricciones únicas protegen un contrato por solicitud/propuesta incluso con pulsaciones simultáneas.
- Versiones optimistas para cambios de solicitud/contrato y configuración de horarios. Una propuesta enviada no se edita: otra versión sustituye a la anterior. Propuestas vencidas, respondidas o sustituidas no se aceptan.
- Historial y avisos internos se escriben en la misma transacción que el cambio de solicitud/cotización/contrato. No se enviaron correos de contratación ni mensajes externos.
- Las direcciones, notas, condiciones e historial solo se consultan por cliente, proveedor responsable o administrador. Los datos contractuales no se guardan en Room ni existe una cola de mutaciones sin conexión.
- Los borradores de asistente/compositor, paso y clave de envío se conservan al recrear la pantalla. Ante respuesta incierta, consultar el detalle antes de cambiar y reenviar la operación.

## Cancelación y cierre

Antes del contrato, el cliente cancela la solicitud con motivo; el proveedor puede rechazarla. Una solicitud con contrato se gestiona desde ese contrato.

La cancelación contractual y el registro de finalización requieren estado **CONFIRMADO**, que solo podrá provenir de un pago verificado en Etapa 7. El backend ya implementa las transiciones y liberación atómica de ocupaciones bajo los recursos originales. La finalización corresponde al proveedor o administrador y usa la hora del servidor, después del fin del evento. La cancelación conserva snapshot, motivo, fecha, pagos e historial; no ejecuta un reembolso automático. No se ofrece cancelación de un contrato pendiente de pago.

La app actual no contiene checkout, aprobación manual, botón de pago simulado ni una vía para confirmar contratos sin pago. No se generó una constancia jurídica ni una exportación administrativa; esos entregables no son el criterio de salida de esta etapa.

## Base y arranque

Flyway empaqueta **V001–V006**. V006 agrega huellas/claves de envío, una versión independiente de agenda y completa los recursos de servicios, responsables e integrantes aceptados existentes. No altera las migraciones aplicadas ni crea franjas, clientes o servicios de muestra. El esquema mantiene 32 tablas de negocio, además del historial de Flyway.

Se conservan las cuentas MySQL separadas y los permisos documentados en [configuración](../backend/CONFIGURACION_MYSQL.md), incluido `DELETE` sobre `horario_atencion`. Para iniciar, seguir [integración Android](integracion_android.md). El APK debug requiere el backend HTTPS local activo.

## Verificación realizada

Compilaciones de backend y Android, Lint y ejecución en un emulador temporal. Sobre HTTPS real y una instancia MySQL 8.0.30 aislada se completaron 26 comprobaciones agrupadas: permisos entre actores, idempotencia, consentimiento, desglose, versiones, snapshot histórico, cruces de medianoche, márgenes, bloqueo compartido, retiro y aceptación concurrente. Los reintentos simultáneos con la misma clave devuelven el mismo contrato; dos claves distintas producen un ganador y un conflicto. Una propuesta que vence según la hora real del servidor se rechaza.

Se verificó que **aceptar no ocupa la agenda** y que un contrato pendiente no se finaliza ni cancela como si ya estuviera confirmado. Las ramas posteriores a un primer pago real deben verificarse con la integración financiera de Etapa 7; no se fabricaron pagos para acreditarlas. Las comprobaciones anteriores de Etapa 2 conservan su alcance histórico V001–V004.

Los datos sintéticos y auxiliares de ejecución están fuera de los artefactos de aplicación, en carpetas locales ignoradas. No se modificaron las bases habituales de Laragon ni los documentos originales de la tesis.

En Android se completaron el asistente con retorno al detalle, el envío de cotización desde proveedor y la aceptación desde cliente. El APK final se revisó también con fuente al 150 %: borrador de horarios conservado al recrear la pantalla y botón de aceptación deshabilitado hasta marcar las condiciones. Se corrigió el retorno del asistente para conservar la referencia al navegador antes de retirar su fragmento. Backend y APK debug/release compilados; Lint terminó sin errores (quedan advertencias de estilo/recursos).

Actualización Etapa 7: [pagos directos y conciliación](pagos_arquitectura.md) integra el primer abono y la confirmación de reserva, los saldos/devoluciones y la comprobación del cierre tras un abono sintético. Las indicaciones anteriores de pagos pendientes de integrar corresponden al corte histórico de Etapa 6.
