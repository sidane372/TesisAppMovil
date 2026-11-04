# Lógica de negocio y servicios — estado inicial

Implementado en Android: CatalogoController coordina estados/ciclo de vida; ServicioRepository lee caché y coordina refresco; ApiClient/VolleySingleton encapsulan GET HTTPS; ServicioJsonMapper valida snapshot público; ServicioDao reemplaza caché de manera transaccional. Los modelos, mappers, DTO y vistas están separados. MoneyUtils formatea céntimos sin float/double.

No están implementados Controllers REST, Services Spring Boot, Repositories JPA ni reglas ejecutables de contratación, agenda, contratos, pagos o reservas. El diseño de esas responsabilidades está en ../backend/README.md, ../docs/endpoints_api.md y ../docs/pagos_arquitectura.md.

La ausencia de API se representa como catálogo no disponible. Un resultado remoto inválido no borra la caché; una respuesta completa vacía sí elimina servicios retirados. Room no aprueba transacciones ni confirma horarios. Las validaciones/errores y operaciones futuras se documentan, no se presentan como funcionales.


## Actualización de Etapa 2

Se implementaron protecciones SQL ejecutables: restricciones de integridad, triggers de inmutabilidad/suma/aceptación y vista de saldos. El protocolo de agenda fue probado con dos conexiones, incluidos recursos compartidos. Ver ../database/README.md y ../docs/pruebas/verificacion_etapa2.md. Estas protecciones no sustituyen los Services de autorización, transacciones del negocio y verificación financiera del backend, que siguen pendientes.

## Implementación real de Etapa 3

Existen AuthService/RecoveryService/UsuarioService/ServicioService, Controllers REST, entidades, Repositories JPA y ContenidoServicioRepository parametrizado. SecurityConfig aplica autenticación bearer y roles; los Services comprueban propiedad, estados y versión. Las operaciones registran historial transaccional. El JAR se compiló y arrancó con MySQL/Flyway/Hibernate. Ver ../backend/README.md y API.md.

Registro de cliente, acceso/sesiones, cuentas musicales, perfil, servicio/repertorio/tarifa/publicación están implementados. Recuperación requiere SMTP configurado; se comprobó con un receptor local, sin envío externo. Solicitudes, agenda operativa, cotizaciones, contratos y pagos de pasarela siguen pendientes. No se creó un paquete nuevo de pruebas académicas.
