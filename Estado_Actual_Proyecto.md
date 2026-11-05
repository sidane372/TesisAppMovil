# Estado actual del proyecto Santa Cecilia

**Fecha de corte:** 04/11/2026, zona horaria America/Lima.  
**Autores:** Galvez Andre Jherson y Briceño Contreras Sidanne Luis.  
**Repositorio:** <https://github.com/sidane372/TesisAppMovil>.  
**Rama local:** `main`. **Último commit local observado:** `3bcb57c`, del 03/11/2026, «feat: incorpora Santa Cecilia Android y backend».

## 1 Resumen del estado

Santa Cecilia dispone de una versión funcional **en el entorno local de desarrollo**, integrada mediante Android Java/XML, una API Spring Boot y MySQL. El recorrido implementado comprende catálogo, identidad, solicitudes, cotizaciones versionadas, aceptación expresa, contrato, comprobantes, conciliación administrativa y reserva. La existencia de este recorrido no equivale al cumplimiento integral de los 68 requerimientos funcionales y 22 no funcionales del documento formal.

En esta revisión se confirmó que MySQL y la API estaban activos y que `GET https://localhost:8443/api/v1/health` respondió **HTTP 200**, con estado `DISPONIBLE` y validación del certificado HTTPS. Es una comprobación puntual de disponibilidad; no sustituye una nueva ejecución completa del flujo de negocio.

Los ensayos anteriores de contratación y pagos utilizaron **datos sintéticos en instancias locales aisladas**. No se realizaron transferencias bancarias reales ni pruebas de un sandbox oficial de Yape o Plin. La aplicación registra ingresos que administración contrasta fuera del sistema; no mueve dinero ni consulta movimientos bancarios automáticamente.

Hay cambios y documentos nuevos locales que todavía no forman parte del último commit observado. El código publicado en el primer commit y el paquete académico generado posteriormente deben distinguirse al presentar evidencias.

## 2 Módulos que funcionan dentro de su alcance local

En esta tabla, «funciona» significa que existe implementación integrada y evidencia de las comprobaciones descritas. No significa validación productiva, certificación de seguridad ni aprobación de todos los requisitos asociados.

| Módulo | Funcionalidad operativa | Evidencia disponible | Límite o condición |
|---|---|---|---|
| Registro y acceso | Autorregistro de clientes con aceptación de privacidad, inicio y cierre de sesión, renovación y restauración del acceso cifrado. | [Integración Android](docs/integracion_android.md), [regresión del registro](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/regresion_registro.txt) y [AccesoFragmentTest](app/src/androidTest/java/com/example/santaceciliatesis/view/fragment/common/AccesoFragmentTest.java). | El servidor debe estar activo. La recuperación por correo se trata como módulo condicionado en la sección 3. |
| Usuarios y permisos | Administración crea músicos, consulta y modifica cuentas, cambia estados y revoca accesos. El servidor limita operaciones por rol y propietario. | [API implementada](backend/API.md) y [matriz de permisos](ENTREGABLES_UNIDAD_III/08_Roles_Permisos_Seguridad.md). | Los controles locales no constituyen una auditoría completa de seguridad. |
| Catálogo y servicios | Consulta de servicios publicados, detalle, tarifa, repertorio, búsqueda, filtros implementados y comparación. Administración crea y publica servicios. | [Interfaz](docs/interfaz.md), [capturas](ENTREGABLES_UNIDAD_III/06_Interfaces_Funcionales/README.md) y `ServicioController`/`ServicioService`. | Imágenes, integrantes y todos los filtros del documento formal no están completos. |
| Gestión del proveedor | Director consulta sus bandas y solista su servicio; mantenimiento de tarifa y repertorio, consulta de solicitudes y contratos propios. | [Interfaz por rol](docs/interfaz.md) y capturas de director y solista del entregable 06. | No se acredita una gestión integral de integrantes, invitaciones o sustituciones. |
| Perfil y navegación por rol | Perfil propio editable, dashboards de los cuatro roles, menú lateral y navegación adaptada al rol. Login y registro usan la pantalla sin la barra inferior superpuesta. | [Capturas reales](ENTREGABLES_UNIDAD_III/06_Interfaces_Funcionales/README.md) y [guía de arranque y correcciones](docs/ARRANQUE_LOCAL.md). | Los contadores básicos no equivalen a un módulo completo de indicadores analíticos. |
| Solicitudes y cotizaciones | Solicitud futura sin reserva, revisión/rechazo con motivo, cotizaciones con desglose exacto, descuento, vigencia, condiciones y versiones. | [Contratación y agenda](docs/contratacion_agenda.md), [comprobaciones de contratación](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/contratacion_comprobaciones.json) y capturas 07 y 08. | Las operaciones requieren conexión, permisos y configuración de franjas del servicio. |
| Aceptación y contratos | Aceptación expresa de la propuesta vigente, contrato único `PENDIENTE_PAGO`, snapshot histórico, consulta de versiones y reintentos idempotentes. | [Contratación y agenda](docs/contratacion_agenda.md), [concurrencia](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/contratacion_concurrencia.json) y captura 09. | Aceptar no reserva ni retiene el horario. La constancia descargable sigue pendiente. |
| Disponibilidad y agenda | Consulta mensual y por día, franjas, márgenes de preparación, bloqueos y exclusión por recursos compartidos. | [Protocolo de agenda](database/protocolo_agenda.md), [informe de contratación](docs/contratacion_agenda.md) y captura 11. | La disponibilidad se vuelve a validar en servidor; la caché del dispositivo no la acredita. |
| Pagos directos | Cuentas receptoras, instrucciones por saldo completo o mitad, adjunto de comprobante, visor privado, rechazo y conciliación restringida a administración. | [Arquitectura de pagos](docs/pagos_arquitectura.md), [28 comprobaciones agrupadas](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/pagos_comprobaciones_sinteticas.json) y capturas 10 y 12. | Yape, Plin y transferencia se ejecutan fuera de la app. Una imagen no prueba por sí sola que se recibió dinero. |
| Abonos, reserva y devoluciones | Primer ingreso aplicable confirma contrato y reserva en transacción; abonos posteriores ajustan saldo; ingresos incompatibles generan incidencia; seguimiento de devoluciones. | [Comprobaciones sintéticas de pagos](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/pagos_comprobaciones_sinteticas.json) y captura 13. | No hay reembolso bancario automático. La devolución externa exige evidencia y contraste administrativo. |
| Cancelación y finalización | Cancelación de contratos confirmados con motivo e historial, liberación de ocupaciones y finalización autorizada después del evento. | [Reglas de contratación](docs/contratacion_agenda.md) y [verificación de pagos y cierre](docs/pagos_arquitectura.md). | No cubre toda posible política comercial. No se permite tratar un contrato pendiente de pago como confirmado. |
| Persistencia y entorno local | MySQL persistente, Flyway V001–V007, Room v3 para catálogo público y scripts de inicio, estado, reinicio y detención. | [Arranque local](docs/ARRANQUE_LOCAL.md), [verificación de persistencia](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/verificacion_arranque.json) y [esquema actual](ENTREGABLES_UNIDAD_III/04_Base_Datos/resumen_esquema.json). | Compilar Android no inicia los servidores. El entorno local no es un despliegue productivo. |

## 3 Módulos incompletos y condiciones pendientes

| Área | Lo que ya existe | Trabajo pendiente |
|---|---|---|
| Recuperación de contraseña | Endpoints, token de recuperación y flujo Android. Hay comprobación histórica con receptor SMTP local. | Configurar y verificar SMTP externo y la URL pública. No se acredita entrega de correos reales a usuarios. |
| Imágenes e integrantes | Estructuras de datos, contenido parcial del servicio y repertorio operativo. | Completar galería y gestión integral de integrantes, invitaciones y sustituciones desde la interfaz y API. RF-15/RF-16 siguen parciales. |
| Notificaciones | Outbox y escritura/entrega durable a notificaciones internas. | Completar la bandeja o consulta visible de avisos para cada usuario. Persistir avisos no acredita su recorrido completo en pantalla. |
| Filtros y reportes | Búsqueda, estado, filtros de catálogo implementados y contadores básicos. | Completar filtros analíticos por periodo, evento y proveedor; agrupaciones diarias/semanales/mensuales y todos los indicadores de RF-20/RF-23/RF-24. |
| Auditoría consultable | Registro de operaciones con actor, fecha y referencias. | Interfaz general de consulta y filtros de auditoría, RF-26. |
| Exportaciones | Datos centrales e historial de operaciones. | Exportar registros e indicadores para análisis, RF-25. |
| Constancia contractual | Contrato y snapshot consultables. | Generación y descarga de constancia identificada y autorizada, RF-66. |
| Valoración y encuesta | Parte de la estructura del dominio y discrepancias documentadas. | Flujo de valoración única y encuesta voluntaria tras finalizar; definición y validación del instrumento, RF-68. Tener una tabla no completa el requisito. |
| Validación no funcional | Revisión local de tamaños, fuente ampliada, precisión, permisos y concurrencia. | Medir metas de rendimiento, usabilidad, accesibilidad y satisfacción con protocolo y participantes autorizados. No existen resultados de campo acreditados. |
| Producción y distribución | APK debug y JAR disponibles, configuración de release y esquema versionado. | Servidor accesible fuera del equipo, TLS del entorno, firma de Android, URL de release, SMTP, monitoreo y respaldo/restauración verificados. |
| Documentación académica | Dos Word, dos Excel, informes Markdown, SQL y capturas organizados. | Completar campos académicos y cronograma, revisar con la docente las adaptaciones de alcance y publicar los documentos locales cuando corresponda. |

### Estado de los binarios conservados

- El [APK debug](app/build/outputs/apk/debug/app-debug.apk) fue generado después de la corrección del cierre de registro. La clase compilada debug conserva el navegador y el texto antes de retirar `AccesoFragment`.
- El [APK release unsigned conservado](app/build/outputs/apk/release/app-release-unsigned.apk) es **anterior a esa corrección**. La salida de `javap` de la compilación release todavía muestra la llamada a `host()` después de `mostrarAcceso("LOGIN")`. Debe reconstruirse desde el código actual y verificarse antes de distribuirse; además requiere firmado y configuración de su entorno.
- Existe el [JAR del backend](backend/build/libs/santa-cecilia-api.jar). Su presencia no incluye la configuración privada ni inicia automáticamente MySQL.
- La fecha de modificación del fuente puede ser posterior a debug por ajustes de formato. No se interpreta esa fecha como prueba de que se haya ejecutado una nueva suite.

## 4 Pruebas y comprobaciones ejecutadas

Los resultados siguientes corresponden a ejecuciones anteriores conservadas o documentadas, salvo las filas identificadas como comprobación de esta revisión. **No se volvió a ejecutar toda la suite para redactar este archivo.** Los números corresponden a unidades distintas y no deben sumarse como una sola cobertura ni transformarse en porcentaje de requisitos aprobados.

| Tipo | Ejecución y resultado observado | Evidencia | Alcance y limitación |
|---|---|---|---|
| Compilación | Backend JAR y Android debug/release compilados en sus respectivos cortes; APK de instrumentación generado. | [Informes de integración](docs/integracion_android.md), [contratación](docs/contratacion_agenda.md), [pagos](docs/pagos_arquitectura.md) y binarios locales. | Release conservado requiere actualización por la corrección posterior del registro. Compilar no demuestra el flujo completo. |
| Pruebas JVM Android | **11 pruebas, 0 fallos, 0 errores:** 8 de `ApiConfigTest` y 3 de `MoneyUtilsTest`. | [Resultados XML locales](app/build/test-results/testDebugUnitTest/) e [informe histórico](docs/pruebas/verificacion_etapa1.md). | Configuración HTTPS y precisión monetaria; no equivalen a pruebas de los Services del backend. Los reportes de `build/` son locales y regenerables. |
| Android Lint | Último reporte conservado: **0 errores y 30 advertencias**. | [Reporte local](app/build/reports/lint-results-debug.txt) y [copia de entrega](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/resumen_lint.txt). | Quedan advertencias. El informe antiguo de una sola advertencia corresponde a otra etapa. |
| Regresión instrumentada del registro | **1 prueba aprobada**, `registrationSuccessShowsNoticeAfterRegistrationFragmentIsDetached`, en Pixel 7a emulado API 37. | [Fuente de la prueba](app/src/androidTest/java/com/example/santaceciliatesis/view/fragment/common/AccesoFragmentTest.java) y [registro del resultado observado](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/regresion_registro.txt). | Reproduce el éxito y retiro real del fragmento, conserva el aviso y comprueba la pila. No crea cuentas ni prueba por sí sola una petición de registro real. |
| Instrumentación Room y JSON | Se compilaron **2 casos Room y 4 casos JSON** en la base del proyecto. | [AppDatabaseTest](app/src/androidTest/java/com/example/santaceciliatesis/data/local/AppDatabaseTest.java), [ServicioJsonMapperTest](app/src/androidTest/java/com/example/santaceciliatesis/data/remote/ServicioJsonMapperTest.java) e [informe de Etapa 1](docs/pruebas/verificacion_etapa1.md). | **No hay evidencia aportada de ejecución de esos seis casos.** No se declaran aprobados por haberse compilado. |
| Esquema MySQL histórico | **31 comprobaciones SQL** aprobadas sobre V001–V004, con rollback y concurrencia. | [Reporte JSON histórico](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/mysql_verificacion_historica_v004.json) e [informe de Etapa 2](docs/pruebas/verificacion_etapa2.md). | Es un corte de 30 tablas. No debe presentarse como una suite nueva sobre las 36 tablas actuales. |
| Contratación y agenda | El informe registra **26 comprobaciones agrupadas**. Los auxiliares conservan 15 comprobaciones enumeradas y un resultado de 11 comprobaciones de concurrencia. | [Informe](docs/contratacion_agenda.md), [lista de comprobaciones](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/contratacion_comprobaciones.json) y [resultado de concurrencia](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/contratacion_concurrencia.json). | Permisos, disponibilidad, versiones, consentimiento, snapshot e idempotencia en HTTPS/MySQL local. La fecha `2026-11-11` del JSON identifica un evento sintético, no la fecha de ejecución de la prueba. |
| Pagos directos | **28 comprobaciones agrupadas** documentadas sobre permisos, importe, comprobante privado, reintentos, revisión concurrente, reserva, incidencias y devoluciones. | [Resultados JSON](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/pagos_comprobaciones_sinteticas.json) y [informe de pagos](docs/pagos_arquitectura.md). | Ingresos y comprobantes sintéticos. No validan recepción de dinero real ni integración bancaria automática. |
| Recorridos Android conectados | Se documentaron catálogo en dos emuladores, sesión/renovación/logout, acciones por rol, solicitud, cotización, aceptación y recorrido de comprobante a conciliación/reserva. | [Integración](docs/integracion_android.md), [interfaz](docs/interfaz.md), [contratación](docs/contratacion_agenda.md), [pagos](docs/pagos_arquitectura.md) y las 13 capturas del entregable 06. | Verificación técnica local e histórica. No es estudio de satisfacción, prueba de producción ni ejecución acreditada en teléfonos físicos. |
| Arranque y persistencia | Reinicio conservando esquema, administrador, credenciales y sesión; el resumen registra API, administrador y persistencia en `true`. | [Resumen JSON](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/verificacion_arranque.json) y [guía de arranque](docs/ARRANQUE_LOCAL.md). | El valor `37:1:007` refleja el esquema y usuario del momento de esa comprobación. No es un conteo actual de usuarios. |
| Importación del SQL de tablas | En la sesión anterior se importó la exportación en un esquema temporal: **36 tablas, 55 FK, 75 CHECK, 9 triggers, 1 vista y 4 roles**, sin registros personales. El esquema temporal se eliminó al terminar. | [SQL entregado](ENTREGABLES_UNIDAD_III/04_Base_Datos/Santa_Cecilia_Tablas.sql) y [guía de importación](ENTREGABLES_UNIDAD_III/04_Base_Datos/COMO_IMPORTAR_MYSQL.md). | Resultado observado en la sesión; no se conserva un log independiente de importación en el paquete. El SQL no incluye el historial Flyway ni sustituye el arranque del backend. |
| Archivos académicos | Dos libros exportados con filtros, paneles fijos, validaciones y conteos comprobados; revisión local de 13 páginas y coincidencia del texto de los dos Word. | [Verificación del paquete](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/Verificacion_Paquete.json). | No hubo automatización disponible de Microsoft Word; la composición nativa de Word no quedó verificada. Estas comprobaciones no son pruebas funcionales del negocio. |
| Estado del entorno hoy | **Comprobación de esta revisión:** MySQL 33306 y API 8443 activos; endpoint de salud HTTPS con certificado validado, HTTP 200 y `DISPONIBLE`. | `tools/Sistema-Local.ps1 -Accion Estado` y consulta HTTPS de salud observados al redactar este archivo. | Lectura puntual del entorno. No se reinició ni se alteró la base para redactar el informe. |
| Revisión de binarios hoy | **Comprobación estática de esta revisión:** comparación de `onRegistered()` mediante `javap` en las clases debug y release conservadas. | Clases locales en `app/build/intermediates/javac/` y [fuente actual](app/src/main/java/com/example/santaceciliatesis/view/fragment/common/AccesoFragment.java). | Acredita la diferencia entre los cortes compilados; no se generó un release nuevo durante esta revisión. |

No se aporta una suite JUnit ejecutada de los Services Spring Boot ni un informe de cobertura del backend. Las comprobaciones HTTP/MySQL descritas son distintas de una suite unitaria formal. Tampoco se acredita ejecución completa de todos los tests instrumentados existentes.

## 5 Evidencias y archivos disponibles

### Código y documentación técnica

- [Código Android](app/src/main/java/com/example/santaceciliatesis/) y [servidor](backend/src/main/java/com/santacecilia/api/).
- [API implementada](backend/API.md), [arquitectura](docs/arquitectura.md), [contratación](docs/contratacion_agenda.md) y [pagos directos](docs/pagos_arquitectura.md).
- [Esquemas Room exportados](app/schemas/) y [migraciones centrales](database/migrations/), V001–V007.
- [Índice de Unidad III](ENTREGABLES_UNIDAD_III/README_ENTREGA.md) y [guía de demostración](ENTREGABLES_UNIDAD_III/GUIA_DEMOSTRACION.md).

### Base de datos vigente

- [Resumen del esquema](ENTREGABLES_UNIDAD_III/04_Base_Datos/resumen_esquema.json): **36 tablas de negocio**, una tabla técnica Flyway, **337 columnas de negocio, 55 FK, 75 CHECK, 9 triggers y 1 vista**. Los conteos describen el corte de metadatos exportado.
- [Diccionario físico actual](ENTREGABLES_UNIDAD_III/04_Base_Datos/Diccionario_Datos_Actual.md), [modelo lógico actual](ENTREGABLES_UNIDAD_III/04_Base_Datos/Modelo_Logico_Actual.md), [diagrama relacional](ENTREGABLES_UNIDAD_III/04_Base_Datos/Modelo_Relacional_Actual.mmd) y [metadatos sin datos de personas](ENTREGABLES_UNIDAD_III/04_Base_Datos/metadatos_esquema_actual.json).
- [Archivo SQL único](ENTREGABLES_UNIDAD_III/04_Base_Datos/Santa_Cecilia_Tablas.sql): crea `santa_cecilia_tablas` para consulta o entrega. No copia usuarios, contraseñas, sesiones, contratos, pagos ni comprobantes de la base operativa.

### Capturas e iteraciones

Existen **13 capturas reales** enumeradas en el [índice de interfaces](ENTREGABLES_UNIDAD_III/06_Interfaces_Funcionales/README.md): login; cuatro dashboards por rol; catálogo; solicitud; cotización; contrato pendiente de pago; pagos; calendario; conciliación administrativa y contrato confirmado. El índice identifica Fragment, layout y alcance de cada captura. Las capturas de negocio muestran datos sintéticos y algunas conservan textos de su etapa histórica; no describen necesariamente el contenido actual de la base habitual.

La carpeta [11_Evidencias_Iteraciones](ENTREGABLES_UNIDAD_III/11_Evidencias_Iteraciones/) conserva los informes de arquitectura, MySQL, backend, integración, interfaz, contratación, pagos y correcciones, además de los resultados JSON y resúmenes anteriores. No existe un commit independiente para cada iteración histórica: el primer commit reúne el trabajo acumulado.

### Documentos de entrega

- [Plan de desarrollo](ENTREGABLES_UNIDAD_III/01_Plan_Desarrollo.docx).
- [Backlog priorizado](ENTREGABLES_UNIDAD_III/02_Backlog_Priorizado.xlsx), con 68 historias y una hoja que conserva los 90 requisitos originales.
- [Registro de incidencias](ENTREGABLES_UNIDAD_III/09_Registro_Incidencias.xlsx), con 7 incidencias documentadas; las advertencias de lint siguen abiertas.
- [Documentación técnica](ENTREGABLES_UNIDAD_III/10_Documentacion_Tecnica.docx).
- [Paquete ZIP local](Santa_Cecilia_Unidad_III.zip) y [manifiesto SHA-256 del paquete](ENTREGABLES_UNIDAD_III/SHA256SUMS.txt).

Este informe se crea en la raíz después del paquete anterior. No se afirma que ya esté incorporado en ese ZIP ni publicado en GitHub.

## 6 Incidencias y limitaciones relevantes

1. **Conexión local:** se corrigió la ausencia de configuración y servidores persistentes mediante el arranque gestionado. Después de reiniciar el equipo hay que iniciar el entorno; una APK por sí sola no lo hace.
2. **Pantallas de acceso:** se corrigieron el solapamiento de la navegación y el acceso a botones con fuente ampliada.
3. **Cierre después del registro:** la cuenta se guardaba y luego se accedía a un Fragment retirado. La corrección conserva el navegador y el mensaje antes de navegar. Hay regresión instrumentada aprobada en debug; el release conservado sigue pendiente de reconstrucción.
4. **Advertencias de calidad:** el último lint conserva 30 advertencias. Cero errores de lint no significa ausencia de defectos funcionales.
5. **Documentos históricos:** algunos informes iniciales aún mencionan Volley, Room v1/v2, 30 tablas o contratación pendiente. Deben leerse como evidencia de su etapa, no como descripción vigente. Este archivo distingue esos cortes.
6. **Alcance financiero y offline:** las instrucciones anteriores de pasarela comercial, pago simulado, tarjeta o recorrido privado completo offline no describen la solución actual. Se usa HTTPS nativo; los pagos son externos con conciliación administrativa y Room conserva solo caché pública. La adaptación técnica requiere su conciliación con la documentación formal de tesis.
7. **Evaluación formal pendiente:** no se han acreditado metas de rendimiento, accesibilidad integral, satisfacción, seguridad productiva ni restauración de respaldos mediante resultados de campo.

## 7 Siguientes acciones

1. Reconstruir y verificar release desde el código corregido antes de preparar su distribución firmada.
2. Preparar los datos y sesiones autorizados para la demostración, incluidos servicios publicados, repertorio, franjas y cuentas receptoras. No confundir las capturas sintéticas con datos oficiales cargados.
3. Completar los módulos parciales y pendientes según el backlog: avisos visibles, imágenes/integrantes, filtros e indicadores, auditoría, exportaciones, constancia y RF-68.
4. Ejecutar las pruebas instrumentadas que no tienen evidencia de ejecución y establecer una suite repetible del backend con resultados conservados.
5. Realizar la validación RNF y preparar despliegue, SMTP, firma, privacidad y respaldos antes de producción.
6. Revisar los documentos académicos y registrar en nuevos commits los cambios locales que se decida publicar. No se hizo commit ni push al redactar este informe.
