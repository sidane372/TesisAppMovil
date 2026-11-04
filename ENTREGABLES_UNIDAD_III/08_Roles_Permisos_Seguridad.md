# Roles, permisos y seguridad — diseño y estado inicial

La tabla reúne permisos de todo el sistema. Etapa 3 implementó autenticación y permisos de identidad/servicios en backend; Etapa 4 conectó acceso y cuenta desde Android. Contratación, agenda y pagos de esta matriz siguen previstos. Ver [API efectiva](../backend/API.md) y [sesión Android](../docs/integracion_android.md).

| Operación | Administrador | Cliente | Director | Solista |
|---|---|---|---|---|
| Crear músicos y servicios | Sí | No | No | No |
| Cambiar identidad/publicación de servicios | Sí | No | No | No |
| Precio, agenda, repertorio y galería | Sí | No | Bandas asignadas | Servicio propio |
| Integrantes | Bandas | No | Bandas asignadas | No en servicio individual |
| Solicitar cotización | No en nombre del cliente | Propia | No | No |
| Responder/cotizar | Consulta global | No | Solicitudes de sus bandas | Solicitudes de su servicio |
| Aceptar y pagar | No en nombre del cliente | Contrato propio | No | No |
| Consultar contratos/pagos | Global autorizado | Propios | Bandas asignadas | Servicio propio |
| Cancelar según política | Sí | Propio | Propio | Propio |
| Finalizar después del horario | Sí | No | Propio | Propio |
| Valorar contrato finalizado | Consulta autorizada | Propio y único | No | No |
| Auditoría/indicadores/exportación | Sí | No | No | No |
| Perfil personal | Propio | Propio | Propio | Propio |

Estado implementado: URL HTTPS validada, tráfico cleartext deshabilitado, caché solo pública, sin contraseñas/tokens/datos de tarjeta. Las pruebas JVM de configuración se informan en ../docs/pruebas/verificacion_etapa1.md. El enum Rol no concede permisos y la pantalla base no representa una sesión autenticada.

Pendientes: hash de contraseñas seguro en servidor, login/recuperación, tokens breves/rotación/revocación, Keystore para credenciales de sesión en Android, límites de intentos, autorización por propietario en cada endpoint, pruebas 401/403 y acceso directo a contratos ajenos, limpieza de caché privada al cambiar cuenta, privacidad y seguridad de backups, despliegue HTTPS y revisión del proveedor financiero.


## Estructura de seguridad añadida en Etapa 2

MySQL contiene los cuatro roles de referencia, cuentas sin usuarios sembrados, hash de contraseña, sesiones con hashes/rotación/revocación y recuperación con token de un uso. FK compuestas impiden asociar pagos a otro cliente contractual. Estas restricciones estructurales se probaron por SQL; todavía no existe login, verificación de contraseña ni autorización HTTP. La cuenta usada por las pruebas pertenece a una instancia aislada detenida al finalizar. El acceso privado del backend, grants, TLS y respaldo/restauración siguen pendientes del entorno de servidor.

## Controles implementados en Etapa 3

Registro público fijo CLIENTE; creación de músicos solo ADMINISTRADOR; bcrypt; contraseñas de 12 caracteres mínimo y 72 bytes UTF-8 máximo; acceso corto y refresh aleatorios con hashes; rotación con revocación al reutilizar; cierre de familia; suspensión/desactivación con revocación; propietario validado para tarifa y repertorio; rechazo de versión antigua; errores 401/403/404 según contexto. La API no devuelve entidades, hashes ni tokens de recuperación.

Se comprobaron accesos HTTP y recuperación con correo local, incluida revocación posterior. Para operación real se deben configurar HTTPS, SMTP, credenciales privadas y límites coordinados si hay varias réplicas. Los controles de contratos/pagos todavía no están implementados. Ver ../backend/API.md y CONFIGURACION_MYSQL.md.
