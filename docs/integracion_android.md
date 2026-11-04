Actualización de arranque local: [ARRANQUE_LOCAL.md](ARRANQUE_LOCAL.md) ofrece los comandos INICIAR/DETENER/REINICIAR/ESTADO, MySQL persistente en 33306 y acceso inicial protegido. Para este equipo sustituye la preparación manual descrita en el corte histórico inferior.

# Integración Android — Etapa 4

Corte de integración: 03/11/2026. La Etapa 5 amplía la interfaz y evoluciona Room a v3; ver [interfaz.md](interfaz.md). Los resultados de v2 siguientes conservan el corte de Etapa 4. Implementación Java/XML MVC conectada al backend real de Etapa 3. Volley se retiró del código y de Gradle; ApiClient utiliza HttpsURLConnection en ExecutorService, con entrega al hilo principal. No se incorporan Retrofit, Compose ni JDBC en Android.

## Funciones integradas

| Pantalla/acción | Contrato real |
|---|---|
| Catálogo público | GET /servicios; snapshot completo=true, importes enteros en céntimos |
| Perfil del servicio y repertorio | GET /servicios/{id} |
| Registro de cliente | POST /auth/register, privacidad v1 y rol fijado por servidor |
| Ingreso | POST /auth/login |
| Renovación automática | POST /auth/refresh, rotación de tokens opacos |
| Recuperación | POST /auth/forgot-password; enlace enviado por SMTP y cambio en la página HTTPS del backend |
| Mi cuenta | GET /me; muestra el rol devuelto por servidor |
| Cierre | POST /auth/logout y eliminación local del acceso |

Las pantallas de acceso y cuenta son funcionales para esta integración. Los dashboards y formularios administrativos/musicales están implementados en Etapa 5; ver interfaz.md. El cliente no dispone de selector de rol ni autoriza operaciones mediante sus etiquetas de rol.

## Arranque en Android Studio

1. Preparar MySQL y backend/.env según [la guía del backend](../backend/README.md). Usar credenciales propias; las cuentas runtime y migración son diferentes. Los datos oficiales se crean mediante las operaciones autorizadas del backend. No se siembran ejemplos en la app.
2. Desde la raíz del proyecto, generar/reutilizar el certificado local y compilar:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\tools\Preparar-HttpsLocal.ps1
.\gradlew.bat -p backend bootJar
.\gradlew.bat :app:assembleDebug
.\tools\Iniciar-Backend.ps1 -EnvironmentFile .\backend\.env -HttpsLocal
```

3. Dejar esa consola del servidor abierta. Ejecutar app desde Android Studio en un emulador API 30+. Debug usa https://10.0.2.2:8443/api/v1/; 10.0.2.2 enlaza al loopback del equipo anfitrión. Health local: https://localhost:8443/api/v1/health, confiando explícitamente en el certificado público local. Ctrl+C detiene el backend.
4. En Android 17 (API 37), aceptar el permiso de acceso a red local. Si se deniega, el catálogo informa el motivo; Actualizar catálogo vuelve a solicitarlo. Si Android ya bloqueó la solicitud, habilitarlo en los permisos de la app. Esta declaración y solicitud son exclusivas de debug para un endpoint local.

Preparar-HttpsLocal.ps1 guarda la clave privada y contraseña en backend/.local/tls, ignorado por Git; exporta solo el certificado público a app/src/debug/res/raw/desarrollo_ca.pem. Este certificado se confía únicamente en debug y para localhost, 127.0.0.1 y 10.0.2.2. No se instala una CA global, no se desactiva la validación TLS ni se permite HTTP. Si se regenera el certificado, recompilar/reinstalar debug. Al compartir el proyecto, regenerarlo en el nuevo equipo, sin compartir la clave privada.

Un dispositivo físico necesita un servidor HTTPS alcanzable con un certificado válido para su dominio. El certificado local de emulador no cubre la IP LAN del equipo. Para otro entorno:

```powershell
.\gradlew.bat :app:assembleDebug -PSANTA_CECILIA_API_URL=https://dominio-real/api/v1/
.\gradlew.bat :app:assembleRelease -PSANTA_CECILIA_API_URL=https://dominio-real/api/v1/
```

Reemplazar el dominio por el del entorno. La URL no acepta HTTP, credenciales, query ni fragmento. Release no trae URL operativa predeterminada, permiso de red local ni CA de desarrollo. Su APK de esta compilación es unsigned; el firmado de distribución y el despliegue productivo siguen pendientes.

## Sesión y errores

SessionStore cifra acceso, refresh, vencimiento y perfil mínimo propio con AES-256-GCM; la clave no exportable pertenece a Android Keystore. El archivo privado no_backup/sesion.enc tiene escritura atómica y autenticación ligada al paquete y URL del backend. No se guarda contraseña, tokens en Room ni registros de credenciales. Copias de seguridad y transferencia de datos de la app están excluidas.

SessionManager restaura en segundo plano y usa una revisión de sesión para rechazar respuestas de cuentas anteriores o de una sesión cerrada. Una única renovación compartida evita rotar simultáneamente el mismo refresh. Se renueva antes del vencimiento y se reintenta una sola vez el recurso tras un 401. Un refresh con resultado incierto no se reenvía automáticamente: se pide nuevo ingreso para evitar reutilizarlo.

Las peticiones tienen tiempos límite, cancelación por ciclo de vida y límite de respuesta de 2 MiB. Los formularios muestran errores seguros y de campo; las credenciales y tokens nunca se imprimen. El cierre intenta revocar la familia del dispositivo en el servidor y elimina el acceso local aunque no exista conexión. Si no se pudo contactar al servidor, se informa expresamente; la credencial remota permanece sujeta a expiración/revocación en servidor, sin una cola local de refresh pendientes.

Sin conexión se puede presentar el perfil propio guardado, marcado como no verificado; no constituye autorización. No se crean usuarios, reservas, contratos ni pagos offline. Recuperación requiere SMTP configurado; una respuesta de solicitud aceptada no certifica entrega del correo. El aviso de privacidad operativo v1 necesita revisión de la asociación antes de producción.

## Caché Room

Versión 2 con migración explícita MIGRATION_1_2; esquemas 1.json y 2.json conservados. cache_scope liga el catálogo a la URL del backend. Al cambiar de origen o migrar una caché antigua sin origen se descarta ese contenido público y se vuelve a consultar; no se mezcla con otro entorno. No hay datos privados por usuario en Room: el perfil propio está dentro de la sesión cifrada.

La app muestra primero el resumen público anterior y después consulta la fuente oficial. Un snapshot válido se guarda en una transacción; uno vacío elimina servicios retirados. Errores o respuestas incompletas conservan el resumen anterior y se avisa que no se pudo actualizar. La caché no acredita disponibilidad ni precio contractual. El detalle completo y repertorio requieren conexión y no se conservan todavía en Room.

## Verificación local realizada

Se compilaron debug y release y se ejecutó Android Lint: 0 errores; una advertencia sobre una versión AGP más nueva, conservando la configuración del proyecto. Se usaron dos emuladores API 37 con overlays temporales, un backend HTTPS real, certificado verificado y una instancia MySQL 8.0.30 aislada de las bases habituales de Laragon.

Se comprobó que la navegación inferior reaparece después de cerrar el teclado y completar el acceso, actualización de una instalación Room v1 a v2, catálogo compartido en ambos emuladores, detalle/repertorio, registro, ingreso y solicitud de recuperación desde Android con SMTP localhost, archivo de sesión cifrado sin correo/contraseña/token en claro, restauración al reiniciar y renovación real. El vencimiento se acortó únicamente en la instancia temporal para observar la renovación; el valor predeterminado del backend no se alteró.

Se detuvo el backend temporal: el resumen público persistió incluso después de reiniciar la app, el perfil propio no se presentó como verificado y el cierre offline eliminó el archivo cifrado; tras otro reinicio se volvió a mostrar acceso. El cierre online revocó solo la familia de ese dispositivo y la otra sesión siguió consultando GET /me. Release se inspeccionó sin CA privada/pública de desarrollo ni permiso de red local. Los servidores temporales de API/MySQL/SMTP y los dos emuladores de comprobación se detuvieron al finalizar. Las verificaciones son técnicas de la integración, no una validación completa de seguridad, accesibilidad o rendimiento ni un nuevo paquete académico de pruebas.
