# Santa Cecilia — Android Studio

Para usarlo en este equipo: ejecuta **INICIAR_SANTA_CECILIA.cmd**, espera **SANTA CECILIA LISTO** y abre **backend/.local/desarrollo/acceso-administrador.txt** para obtener tu acceso inicial. La base MySQL persistente y sus tablas se preparan automáticamente. [Guía de arranque y administrador](docs/ARRANQUE_LOCAL.md).


Aplicación Java/XML MVC de contratación musical de la Asociación Musical Santa Cecilia, Ambo, Huánuco. Etapas 1–7 implementadas dentro de su alcance: base Android, esquema MySQL, backend Spring Boot e integración de acceso y catálogo. Room conserva el catálogo público; la comunicación utiliza HTTPS nativo de Java. Volley fue retirado.

La app permite consultar servicios y repertorio, registrar clientes con aceptación de privacidad, iniciar sesión, solicitar recuperación, consultar la cuenta y cerrar sesión. El acceso se guarda cifrado con Android Keystore y se renueva contra el servidor. La Etapa 5 añade dashboards por rol, gestión de cuentas y servicios, tarifas/repertorio, edición del perfil, búsqueda/filtros y comparación. La Etapa 6 conecta solicitudes, versiones de cotización, aceptación, contratos pendientes de pago y agenda con horarios/bloqueos. La Etapa 7 incorpora pagos directos por Yape, Plin y transferencia, comprobantes, conciliación administrativa, reserva por primer abono y devoluciones documentadas.

## Etapa 7 — pagos directos y comprobantes

[Guía de pagos](docs/pagos_arquitectura.md): administración configura las cuentas receptoras, el cliente registra el comprobante y administración contrasta el ingreso en su banca/billetera. La app registra esa verificación y confirma la reserva si corresponde. No necesita claves de pasarela comercial ni ejecuta transferencias.

## Etapa 6 — contrataciones y agenda

[Guía de uso y reglas](docs/contratacion_agenda.md): flujo cliente/proveedor, contrato histórico único, calendario, franjas, recursos compartidos, idempotencia y verificación local. Aceptar una cotización no confirma ni reserva sin pago.

## Etapa 5 — interfaz nativa por rol

[Guía de interfaz](docs/interfaz.md): navegación de cinco accesos y drawer, Inter/Poppins locales, pantallas administrativas y de proveedores conectadas y límites respecto a la referencia.

## Etapa 4 — Android conectado

[Guía de integración y arranque](docs/integracion_android.md): configuración HTTPS, sesión protegida, migración Room 1→2, manejo sin conexión y comprobaciones locales realizadas. Se verificó el mismo catálogo en dos emuladores y el cierre de sesión independiente por dispositivo. No hay cuentas, servicios ni credenciales de prueba precargados en la app.

Desde esta carpeta en PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\tools\Preparar-HttpsLocal.ps1
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:assembleRelease
```

APK instalable: [app-debug.apk](app/build/outputs/apk/debug/app-debug.apk). Abrir la raíz en Android Studio y ejecutar app en un dispositivo API 30+. Debug apunta a https://10.0.2.2:8443/api/v1/; necesita el backend del equipo encendido. En Android 17 hay que conceder el permiso de red local solicitado por debug. Release utiliza una URL HTTPS explícita del entorno y no incluye el certificado local.

## Backend y MySQL

Preparar el esquema y las cuentas MySQL, completar backend/.env y habilitar SMTP si se utilizará recuperación, siguiendo [backend/README.md](backend/README.md). Después:

```powershell
.\gradlew.bat -p backend bootJar
.\tools\Iniciar-Backend.ps1 -EnvironmentFile .\backend\.env -HttpsLocal
```

El arranque local usa TLS en loopback, puerto 8443. Ctrl+C detiene el backend. No se dejó un despliegue productivo ni se enviaron correos fuera de localhost durante las comprobaciones.

- [API implementada](backend/API.md): autenticación, roles, cuentas, servicios, solicitudes, cotizaciones, contratos y agenda.
- [Esquema MySQL](database/README.md), [modelo](database/modelo_logico.md) y [diccionario](database/diccionario_datos.md). Etapa 2 verificó V001–V004 (30 tablas); Etapa 3 agregó V005 de autenticación; Etapa 6 V006 de contratación; Etapa 7 V007 de pagos directos (36 tablas de negocio). Los resultados históricos corresponden a sus cortes.
- [Arquitectura actual](docs/arquitectura.md), [MVC y pantallas](docs/estructura_mvc.md), [árbol](docs/arbol_proyecto.md) y [dependencias](docs/dependencias.md).
- [Trazabilidad](docs/matriz_trazabilidad.md), [secuencias](docs/diagramas/secuencias_reales.md) y [orden de implementación](docs/orden_implementacion.md).
- [Entregables iniciales de Unidad III](ENTREGABLES_UNIDAD_III/README_ENTREGA.md) y [referencias originales](docs/referencias/inventario.json). Se conservan como evidencia de su etapa; el estado vigente está en esta guía.

La siguiente etapa es el flujo de contrataciones: solicitud, cotización, aceptación, contrato y agenda. No se declara terminado el sistema de contratación ni las metas RNF por compilar o por disponer de tablas. Esta entrega prioriza la construcción e integración y no añade otro paquete académico de pruebas.
