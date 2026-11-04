# Cómo ejecutar Santa Cecilia y entrar como administrador

El APK se conecta a una API Spring Boot y esta API utiliza MySQL. **Compilar Android no inicia los servidores.** El mensaje de las capturas aparecía porque no había una API funcionando y faltaba `backend/.env`.

## Uso diario

1. Ejecuta **INICIAR_SANTA_CECILIA.cmd**, en la raíz del proyecto. Espera el mensaje **SANTA CECILIA LISTO**.
2. Abre [el archivo de acceso inicial](../backend/.local/desarrollo/acceso-administrador.txt). Contiene el correo y la contraseña del administrador de este equipo.
3. Ejecuta `app` desde Android Studio e inicia sesión con esos datos. El correo inicial es `administrador@santacecilia.local`; su contraseña es aleatoria y está en ese archivo, no en el APK.

Los servidores quedan en segundo plano al terminar el comando. Después de reiniciar Windows, vuelve a ejecutar INICIAR. Para revisar el estado, usa **ESTADO_SANTA_CECILIA.cmd**. Para detenerlos de forma controlada, **DETENER_SANTA_CECILIA.cmd**. Para cargar un JAR nuevo del backend, **REINICIAR_SANTA_CECILIA.cmd**.

**No tienes que copiar ni importar los SQL a mano.** La preparación crea la base vacía y Flyway aplica automáticamente V001–V007: 36 tablas de negocio y su tabla de historial. El administrador se crea una sola vez; iniciar o reiniciar no borra datos ni cambia su contraseña.

## Dónde está la base

- Motor: MySQL 8.0.30, utilizando los binarios de Laragon instalados en este equipo.
- Instancia del proyecto: `127.0.0.1:33306`, base `santa_cecilia`.
- Archivos persistentes: `backend/.local/desarrollo/mysql/data`. **Conserva esta carpeta: contiene tus datos.**
- API: `https://localhost:8443/api/v1/`; desde el emulador, `https://10.0.2.2:8443/api/v1/`.
- Configuración privada: `backend/.env`. Contiene las cuentas técnicas de MySQL; son distintas del administrador de la app.

Esta instancia está separada del servidor habitual de Laragon en 3306. Si quieres inspeccionarla con un gestor SQL, conecta al puerto **33306** y utiliza la configuración de `backend/.env`. No se modificaron ni importaron datos en las otras bases de Laragon.

El arranque comprueba el directorio real de MySQL antes de administrarlo y el PID/ruta del JAR antes de detenerlo. No incluye borrado, reinicialización de bases existentes ni restablecimiento automático de contraseñas. El backend ejecuta una copia del JAR en la carpeta local para que una compilación posterior no sobrescriba el archivo que está usando Java.

## Primeros datos de la aplicación

Desde el administrador, registra las cuentas de directores/solistas y sus servicios. Agrega repertorio, publica los servicios y configura sus franjas de atención. Así aparecerán en el catálogo y podrán recibir solicitudes. El registro público de la app crea clientes; no concede el rol de administrador.

Las cuentas receptoras de Yape, Plin y transferencia se configuran desde **Cuentas receptoras**. La recuperación por correo necesita SMTP configurado; el acceso inicial del administrador ya está disponible en el archivo local.

## Si vuelve a aparecer un error de conexión

Ejecuta ESTADO y confirma MySQL y API en `True`. Si están detenidos, ejecuta INICIAR. Mantén el permiso de red local concedido a la app debug en Android 17. El certificado de desarrollo corresponde a este equipo y al emulador; si se genera otro certificado, recompila/reinstala debug.

Registros: `backend/.local/desarrollo/api/servidor.log` y `backend/.local/desarrollo/mysql/servidor-error.log`. Las contraseñas y los datos persistentes son locales e ignorados por Git; no se incluyen en el APK.

## Corrección del acceso

Login, registro y recuperación usan toda la pantalla, con botón Volver, acceso a Iniciar sesión desde registro y margen de desplazamiento al final. La navegación inferior se oculta en estas pantallas y respeta el teclado. Los campos de contraseña no se guardan en el estado de la vista.
