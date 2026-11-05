# Módulos integrados y evidencia disponible

El flujo principal ya está implementado entre Android, Spring Boot y MySQL. Su evidencia financiera corresponde a datos sintéticos de pruebas locales; no hubo transferencias reales ni uso de un sandbox bancario oficial.

| Flujo | Precondición | Resultado esperado | Resultado observado y evidencia |
|---|---|---|---|
| Registro y acceso | Servidor HTTPS, cliente con datos válidos | Cuenta persistente y transición estable al login | Cuenta observada activa; cierre de transición corregido; regresión instrumentada aprobada |
| Catálogo entre dispositivos | API común y servicios publicados de verificación | Consultas del estado central y caché pública | Comprobación histórica en dos emuladores; docs/integracion_android.md |
| Solicitud y cotización | Cliente, proveedor y franjas del servicio en entorno aislado | Solicitud sin reserva y nueva versión de propuesta | Capturas 07 y 08; contratacion_comprobaciones.json |
| Aceptación y contrato | Última propuesta vigente y consentimiento | Un contrato pendiente de pago con snapshot | Captura 09; comprobaciones de idempotencia y concurrencia |
| Comprobante y revisión | Cuenta receptora e ingreso sintéticos identificados | Comprobante privado, revisión solo por administrador | Capturas 10 y 12; pagos_comprobaciones_sinteticas.json |
| Abono y reserva | Ingreso sintético conciliado y agenda libre | Confirmación y reserva únicas en transacción | Captura 13; revisión concurrente y primer/segundo abono registrados |
| Conflicto y devolución | Recurso ocupado o importe distinto | Incidencia sin reserva incompatible y seguimiento de devolución | 28 comprobaciones agrupadas de pagos en instancia aislada |
| Persistencia habitual | Instancia local de desarrollo | Conservar esquema, administrador y sesión al reiniciar | verificacion_arranque.json; sin borrar datos |

Las evidencias JSON y de regresión están en 11_Evidencias_Iteraciones. Las capturas y sus vistas asociadas están en 06_Interfaces_Funcionales. Las fases anteriores se conservaron como evidencia histórica y no se volvieron a ejecutar para generar este paquete.

Pendientes: evaluación formal con usuarios, medición RNF de rendimiento y accesibilidad, infraestructura productiva y recorrido con los datos oficiales de la asociación. Las comprobaciones locales no acreditan recepción de dinero real ni cumplimiento total de los 90 requisitos formales.
