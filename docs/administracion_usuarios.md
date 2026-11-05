# Administración de usuarios

## Alta de cuentas y roles

Con una sesión administrativa, abrir **Usuarios → Crear usuario**, elegir el rol y guardar. El selector ofrece Director de banda, Solista, Administrador y Cliente. La selección se conserva al recrear la pantalla. La petición se envía a `POST /api/v1/admin/usuarios` y el backend guarda el código del rol recibido, protegido por autorización administrativa.

El registro público continúa creando exclusivamente clientes. No permite seleccionar ni inyectar permisos administrativos. Las cuentas previamente creadas conservan su rol; este cambio no reclasifica usuarios existentes.

## Eliminación

En el detalle de un usuario no administrador se ofrece **Eliminar usuario**. El diálogo explica la conservación de contratos y pagos y exige un motivo. Se utiliza `DELETE /api/v1/admin/usuarios/{id}` con versión y motivo. La operación realiza una baja lógica (`DESACTIVADO`), registra auditoría y revoca las familias de sesiones. No elimina filas con historia contractual o financiera. La cuenta sigue visible al filtrar desactivadas y puede reactivarse mediante Estado de cuenta.

Las cuentas administradoras conservan la protección existente contra suspensión y eliminación desde la aplicación.

## Presentación

La gestión incorpora cabecera con degradado, tarjetas con avatar y estado, jerarquía visual para los roles y acción de eliminación diferenciada. El formulario explica el alcance de la selección de rol.

## Verificación

- Backend: `bootJar --offline` completado correctamente.
- Android: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline` completados correctamente.
- API y MySQL aislados: 28 comprobaciones HTTP aprobadas, con cuentas sintéticas, verificando creación/persistencia/login de los cuatro roles, permisos administrativos, protección de administradores, baja lógica, rechazo de sesiones y login tras la baja, y registro público sin elevación de permisos.
- Resultado conservado en [resultado_roles_usuarios.json](pruebas/resultado_roles_usuarios.json). Esta ejecución usa una instancia API temporal y un esquema independiente; no modifica usuarios de la base habitual.

El ZIP de fuentes del Entregable 5 generado anteriormente corresponde al corte anterior a esta modificación.
