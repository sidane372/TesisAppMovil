# Roles permisos y mecanismos de seguridad

La autorización se aplica en el servidor por rol, relación con el registro y estado de negocio. Los menús Android ayudan a navegar, pero no conceden permisos por sí mismos.

| Operación | Administrador | Cliente | Director | Solista |
|---|---|---|---|---|
| Consultar catálogo publicado | Sí | Sí | Sí | Sí |
| Crear músicos y servicios | Sí | No | No | No |
| Publicar o retirar servicios | Sí | No | No | No |
| Editar tarifa y repertorio | Global autorizado | No | Bandas asignadas | Servicio propio |
| Configurar franjas y bloqueos | Global autorizado | No | Bandas asignadas | Servicio propio |
| Enviar solicitud y aceptar propuesta | No por el cliente | Propia | No | No |
| Cotizar o rechazar solicitudes | Consulta global | No | Bandas asignadas | Servicio propio |
| Consultar contratos y pagos | Global autorizado | Propios | Bandas asignadas | Servicio propio |
| Adjuntar comprobante de ingreso | No por el cliente | Contrato propio | No | No |
| Configurar cuentas y conciliar | Sí | No | No | No |
| Registrar seguimiento de devolución | Sí | Consulta propia | Consulta propia | Consulta propia |
| Cancelar contrato confirmado | Según operación autorizada | Propio con motivo | Según rol autorizado | Según rol autorizado |
| Finalizar después del evento | Sí | No | Banda asignada | Servicio propio |

Registro público fijo CLIENTE y alta inicial de administrador mediante configuración privada. Contraseñas bcrypt, longitud mínima de 12 caracteres y máximo de 72 bytes UTF-8. Tokens aleatorios con hashes en servidor, acceso corto, refresh rotatorio, revocación por familia y control de reutilización. Suspender una cuenta revoca su acceso.

Android cifra la sesión mediante AES-GCM y Android Keystore. No guarda contraseñas en Room, preferencias o el repositorio. Las peticiones usan HTTPS con validación del certificado; la CA de desarrollo solo está en debug. Release requiere URL, TLS y firma del entorno de distribución.

Los comprobantes son privados y autorizados por contrato, con validación de imagen y recodificación sin metadatos. Los errores no publican trazas ni secretos. El calendario público oculta direcciones, identidad y motivos privados. Las cuentas MySQL de aplicación y migración tienen funciones separadas.

Evidencias locales: rechazo entre actores ajenos, comprobante privado, revisión restringida, idempotencia, concurrencia y revocación descritos en las comprobaciones de los entregables 07 y 11. Pendientes de producción: revisión de privacidad, SMTP externo, backups y restauración probados, análisis formal de seguridad, límites distribuidos y gestión de certificados. No se certifica seguridad integral por haber compilado el APK.
