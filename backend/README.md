Arranque local preparado: ejecuta INICIAR_SANTA_CECILIA.cmd en la raíz. Se configura una instancia MySQL persistente del proyecto y se crea el administrador inicial; consulta ../docs/ARRANQUE_LOCAL.md. No hace falta importar SQL a mano.

# Santa Cecilia API — Etapa 3

Backend Java con Spring Boot 4.1.2, Spring Security, JPA/Hibernate, MySQL y Flyway. El JAR ejecutable está en build/libs/santa-cecilia-api.jar después de compilar. No se requieren Maven ni un Gradle diferente: se utiliza el wrapper de la raíz con -p backend.

Implementado: registro público exclusivo de clientes con aceptación de privacidad; contraseñas bcrypt; login; tokens bearer opacos con expiración; refresh rotatorio y revocación de familia al detectar reutilización; logout; recuperación SMTP y enlace de un uso; perfil propio; administración de cuentas musicales; activación/suspensión/baja lógica; CRUD de servicios; publicación del catálogo; tarifas y repertorio según propietario; validaciones, errores seguros y auditoría.

El backend se arrancó con MySQL 8.0.30, cuentas de aplicación/migración separadas, Flyway V001–V005 e Hibernate ddl-auto=validate. Se comprobó su funcionamiento por HTTP, incluida recuperación con un receptor SMTP local. Ningún correo salió de localhost. Los procesos usados en esa comprobación se detuvieron. No se dejó un servidor productivo desplegado ni cuentas de ejemplo en el código.

## Compilar

Desde C:\Users\sidan\AndroidStudioProjects\SantaCeciliaTesis:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat -p backend bootJar
```

Se compiló con JDK 25 y release Java 21; el JAR requiere Java 21+. El módulo Android conserva su configuración independiente. [Requisitos del plugin Spring Boot](https://docs.spring.io/spring-boot/gradle-plugin/index.html).

## Configurar y ejecutar

1. Preparar un esquema MySQL vacío y cuentas separadas según CONFIGURACION_MYSQL.md. El backend aplica sus migraciones empaquetadas con Flyway; no ejecutar antes el instalador manual en ese esquema nuevo.
2. Copiar .env.example a .env y completar las cinco variables DB requeridas con las credenciales de ese entorno. .env está ignorado por Git. El script no interpreta instrucciones de shell dentro de ese archivo.
3. Para crear el primer administrador, habilitar APP_BOOTSTRAP_ENABLED=true y establecer su correo/contraseña propios. Se exige la misma política de contraseña de las cuentas normales. No existe registro público de administrador. Si ya hay un administrador, el arranque no cambia sus credenciales ni eleva otras cuentas.
4. Arrancar desde la raíz:

```powershell
.\tools\Iniciar-Backend.ps1 -EnvironmentFile .\backend\.env
```

5. Deshabilitar el bootstrap después del primer arranque. Consultar GET http://127.0.0.1:8080/api/v1/health para verificar la disponibilidad local. Ctrl+C detiene el servidor.

El enlace local usa HTTP solo en loopback. Para HTTPS directo existe el perfil https: establecer SPRING_PROFILES_ACTIVE=https, TLS_KEY_STORE (ruta/URI del PKCS12), TLS_KEY_STORE_PASSWORD y SERVER_PORT=8443. También se puede terminar HTTPS en un proxy del entorno. Para el emulador, Etapa 4 añade el modo explícito `-HttpsLocal` a Iniciar-Backend.ps1. Genera/reutiliza el PKCS12 privado ignorado, activa TLS en 8443/loopback y usa la CA pública exclusiva de debug. Preparar antes el certificado y recompilar Android según [integracion_android.md](../docs/integracion_android.md). No instala certificados globales. Android ya consume la API por HTTPS nativo; Volley fue retirado.

## Recuperación de acceso

Configurar SMTP_HOST/PORT/USERNAME/PASSWORD/FROM, SMTP_AUTH, SMTP_STARTTLS y APP_PUBLIC_URL (HTTPS en despliegue; se admite HTTP para localhost). La API nunca devuelve el token de recuperación. Se envía por correo después de confirmar la transacción, en una cola local acotada; en MySQL solo queda su hash. /restablecer.html recibe el token en el fragmento de URL, lo retira de la barra y permite elegir otra contraseña. Al usarlo se revocan las sesiones anteriores.

Sin SMTP la operación responde 503 para cualquier correo y no afirma haber enviado un mensaje. Con SMTP devuelve un 202 uniforme; las fallas de entrega se registran sin el token o la dirección. La cola actual está en memoria: no garantiza entrega ante caída del proceso; para reintentar se solicita otro enlace. No existe integración con un proveedor de correo ni una cuenta SMTP precargada. [Configuración SMTP de Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html).

## Organización

| Paquete | Responsabilidad |
|---|---|
| entity / repository | Entidades del módulo de identidad/servicios y acceso a MySQL mediante JPA o consultas parametrizadas |
| service | Reglas de roles/propiedad, cambios transaccionales, autenticación y recuperación |
| security / config | Bearer, bcrypt, validación, límites de intentos, correlación y administrador inicial explícito |
| controller / dto | Contrato REST validado; nunca serializa entidades o hashes |
| exception | Respuestas 400/401/403/404/409/429/503 con código y correlación, sin secretos |
| audit | Historial de operaciones dentro de la transacción |

La definición efectiva de endpoints y cuerpos está en API.md. Las Etapas 6–7 integran solicitudes, versiones, contratos, agenda y pagos directos con comprobante/conciliación administrativa. Galería e integrantes completos conservan sus alcances pendientes. La modalidad de pago actual no utiliza pasarela comercial; ver ../docs/pagos_arquitectura.md. No se generó otro paquete de pruebas académicas: esta etapa prioriza el servidor ejecutable.

Estado actual: el JAR incorpora Flyway V001–V007 (36 tablas de negocio) y las pantallas Android de pagos se conectan a API.md. Configura las cuentas receptoras desde la cuenta ADMINISTRADOR; no agregues contraseñas bancarias al backend.
