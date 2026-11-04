# Diferencias y decisiones de arquitectura

Corte: 03/11/2026. Se conserva RF-01…RF-68 y RNF-01…RNF-22. Esta propuesta no sustituye la aprobación documental de la tesis.

| Fuente anterior | Diferencia | Decisión de Etapa 1 | Trazabilidad |
|---|---|---|---|
| Artifact, DS-01 y requisitos §1.3 | Selector de cinco cuentas; no hay autenticación real | Correo/contraseña, autorregistro solo de cliente; admin crea músicos; permisos en servidor. No crear selector de rol público. | RF-01, RF-11, RF-12, RNF-13; nuevas funciones propuestas sin código RF aprobado |
| RF-21, RF-60…RF-62, DS-14/15 | Pagos simulados y resultados elegidos localmente | Intentos reales/sandbox oficial, checkout externo, verificación del proveedor y webhooks idempotentes | RF-21, RF-60…RF-63; RNF-09/10/15/16 |
| RNF-22 | Flujo principal completo offline | Offline solo consulta previamente sincronizada; no aceptar definitivamente, pagar o confirmar horarios offline | RNF-08, RNF-11/12/22 |
| RNF-09 y regla de pago anterior | Pago y reserva en una única operación local | Atomicidad solo de efectos internos MySQL; cobro externo requiere conciliación y compensaciones | RF-06/63, RNF-09 |
| Diagrama de 14 tablas | Integrantes/repertorio/imágenes como listas TEXT | Propuesta normalizada en tablas hijas por CRUD, referencias y vínculos de músicos compartidos | RF-15/16/53, modelo lógico propuesto |
| Diagrama vs Artifact | Contenido administrado solo por admin vs mantenimiento propio del proveedor | Admin controla identidad/publicación; director mantiene contenido de sus bandas; solista repertorio/galería de su servicio | RF-15/16/27/39, ampliación pendiente de aprobación |
| RF-68 y DS-18 vs diagrama | Encuesta voluntaria incluida vs eliminada del modelo | Conservar discrepancia pendiente. Valoración única por contrato se mantiene. No crear encuesta ni sustituir el instrumento | RF-68 |
| RF-66 | Constancia de muestra | Documento generado por servidor con versión/identificador, acceso autorizado; no afirmar validez legal sin definición | RF-66 |
| Diagrama Cotizacion | Etiqueta numero_version UNIQUE puede interpretarse globalmente | UNIQUE(id_solicitud, numero_version); una solicitud no genera dos contratos aunque acepte versiones distintas | RF-09/34/46/59, RNF-10 |
| Catálogo anterior | Filtros de hora Desde/Hasta retirados | No reintroducirlos; horas se capturan en solicitud. Reportes sí conservan filtros de fechas | RF-20/52/55 |
| Formulario de servicio | Campo Presentación del servicio retirado | No incorporarlo. La descripción histórica opcional del perfil no justifica reintroducir el campo | RF-15/53 |

Funciones adicionales propuestas, sin inventar RF-69 ni renumerar: registro de cliente, login, recuperación de acceso, rotación/revocación de sesión, verificación de webhook, conciliación, gestión de reembolsos e incidencias. Deben pasar a revisión formal y a sus casos de prueba.

Proveedor de pagos, comisiones, soporte específico de Yape/Plin, política de cancelación y vinculación contractual con la asociación quedan pendientes de selección y verificación oficial. No se presenta ningún proveedor como conectado.


## Decisiones materializadas en Etapa 2

Las 14 entidades se conservan y se amplían a 30 tablas físicas; ver ../database/modelo_logico.md y diccionario_datos.md. Se normalizan contenido/listas y se agregan recursos/ocupaciones para el protocolo de agenda compartida, además de sesiones, eventos de pago, devoluciones, incidencias, medición y outbox.

En SQL el retiro/desactivación de publicación usa estado_publicacion=RETIRADO, equivalente operativo al servicio desactivado/retirado de las referencias; no elimina el servicio ni la historia. La revisión usa EN_REVISION, la observación OBSERVADO y el borrador BORRADOR. El backend deberá mapear estos códigos a las etiquetas de interfaz de la referencia, sin usar el estado de cuenta del músico como sustituto del estado de publicación.

La encuesta RF-68 sigue pendiente de conciliación. No se crea una tabla de encuesta ni un instrumento sustituto y no se renumera el catálogo aprobado.

## Decisión expresa de Etapa 7

El usuario aclara que no dispone de pasarela y utilizará Yape, Plin y transferencia bancaria. Se adapta la construcción a comprobantes y conciliación administrativa de ingresos externos. Esta instrucción reemplaza la exigencia anterior de checkout/webhook exclusivo: no son pagos simulados del cliente ni una API bancaria inexistente. La reserva se confirma por el servidor después del contraste administrativo y la revalidación de agenda. La guía actual es pagos_arquitectura.md.
