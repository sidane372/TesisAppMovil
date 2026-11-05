# Lógica de negocio y servicios implementados

Android Java/XML coordina vistas, controladores y repositorios HTTPS. Spring Boot aplica las reglas centrales y MySQL conserva el estado compartido. Room v3 almacena solamente el resumen público del catálogo.

| Módulo | Controllers REST | Services | Persistencia | Reglas |
|---|---|---|---|---|
| Identidad | AuthController y UsuarioController | AuthService, RecoveryService y UsuarioService | UsuarioRepository y repositorios de sesión | Registro CLIENTE, bcrypt, rotación, revocación y permisos por rol |
| Catálogo | ServicioController y AdminServicioController | ServicioService | ServicioRepository y ContenidoServicioRepository | Publicación, propiedad, tarifa en céntimos, repertorio y versión |
| Contratación | ContratacionController | ContratacionService | Repositorios de contratación y acceso JDBC parametrizado | Desglose, vigencia, consentimiento, idempotencia y snapshot contractual |
| Agenda | ContratacionController | ContratacionService | ContratacionRepository, recurso_agenda y ocupaciones | Franjas locales, márgenes, exclusión y recursos compartidos |
| Pagos directos | PagoDirectoController | PagoDirectoService y ComprobanteImagenService | Movimientos, comprobantes y devoluciones en MySQL | Contraste administrativo, saldo exacto, reserva atómica y privacidad |
| Avisos | Operaciones de negocio | PagoAvisosService | Outbox y notificaciones internas | Persistencia transaccional y entrega interna; bandeja de usuario pendiente |

La solicitud no ocupa la agenda. La aceptación expresa de la última cotización vigente crea un contrato PENDIENTE_PAGO con datos congelados. El cliente paga fuera de la app y adjunta un comprobante. Solo el administrador puede conciliar el ingreso después de contrastarlo en su cuenta. La primera conciliación aplicable vuelve a validar la agenda y crea una reserva única dentro de la transacción del abono. Reintentos e ingresos repetidos no duplican efectos.

Los importes se manejan en céntimos enteros. Un ingreso con conflicto o importe distinto se conserva con incidencia sin abonar ni reservar. Las devoluciones se realizan fuera del sistema y su confirmación requiere evidencia y contraste. La finalización se permite después del fin del evento; la cancelación conserva el historial y libera los recursos originales.

Las respuestas distinguen errores de validación, sesión vencida, permiso insuficiente, conflicto y fallo de conexión. Las consultas SQL son parametrizadas. Android cancela solicitudes de vistas detenidas y conserva el catálogo anterior cuando falla una actualización. No hay escritura financiera offline.

Referencias del proyecto: backend/API.md, docs/endpoints_api.md, docs/contratacion_agenda.md y docs/pagos_arquitectura.md. El alcance incompleto de integrantes, imágenes, indicadores, avisos visibles y exportaciones se conserva en el backlog; la existencia de una tabla no acredita una interfaz completa.
