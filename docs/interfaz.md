# Interfaz Android — Etapa 5

Actualización Etapa 6: [contrataciones y agenda](contratacion_agenda.md) incorpora solicitudes, cotizaciones, contratos y calendario. Las limitaciones de negocio descritas más abajo corresponden al corte de Etapa 5.

La aplicación conserva Java, XML y MVC. La interfaz incorpora el shell móvil de la referencia: cinco accesos inferiores según el rol, drawer con la cuenta activa, barra contextual, tarjetas nativas y pantallas desplazables. Mantiene azul #3158A8, marino #14253D y dorado #E5B65D; el acceso usa el verde #153D35 y fondo #FCFAF7.

[Procedencia y hashes de recursos](referencias/recursos_interfaz.json). Fuentes oficiales: [Inter](https://github.com/google/fonts/tree/main/ofl/inter), [Poppins](https://github.com/google/fonts/tree/main/ofl/poppins).

Inter y Poppins están incluidas en res/font con sus licencias SIL OFL en assets/licenses, consultables desde Guía. La frase destacada del hero utiliza la serif del sistema en cursiva, porque Georgia no se distribuye con el proyecto. Las imágenes originales band.jpg/guitar.jpg devolvieron HTTP 403; el hero usa un dibujo musical vectorial local. Las portadas mantienen el placeholder musical de la referencia, sin sustituirlas por fotografías de ejemplo.

## Navegación y pantallas

| Espacio | Pantallas y operaciones reales |
|---|---|
| Público/cliente | Inicio, Explorar músicos, comparación de hasta tres servicios, perfil público, Guía, acceso/registro/recuperación y Mi cuenta |
| Administrador | Inicio, cuentas paginadas, creación/edición de músicos, detalle y cambios de estado con motivo; servicios paginados, creación de borradores, edición de información y revisión/publicación |
| Director | Inicio, Mis bandas asignadas, administración de tarifa y repertorio propio, catálogo/comparación y perfil personal |
| Solista | Inicio, Mi servicio asignado, administración de tarifa/repertorio propio, catálogo/comparación y perfil personal |
| Formularios comunes | Edición de perfil propio, cuenta musical, información de servicio, tarifa y canción; guardado con carga, errores de campo y confirmación del servidor |

Las pantallas comparten layouts/componentes cuando los campos coinciden. Cada listado utiliza RecyclerView con Adapter y ViewHolder independientes. Las vistas delegan las consultas y escrituras en controladores; los repositorios usan HTTPS en segundo plano. Las pantallas privadas muestran únicamente datos obtenidos con autorización del servidor, sin persistir listados privados en Room.

## Comportamiento

El registro público crea exclusivamente clientes. El administrador crea director/solista con contraseña inicial real; no existe un selector de cuentas de demostración. Correo y rol de una cuenta existente se conservan. El administrador puede editar perfiles y activar/suspender clientes, conforme a RF-12; la desactivación del CRUD musical se ofrece a director/solista y no al administrador o cliente. En servicio nuevo el tipo se deriva del responsable activo; al editar se conserva responsable/tipo y se omite el campo retirado Presentación del servicio. Teléfono, ciudad y biografía se completan mediante edición del perfil.

Los cambios de estado piden motivo y confirmación. El servidor conserva historial, comprueba roles/propiedad y exige repertorio antes de publicar. La tarifa usa céntimos enteros y puede quedar vacía (A cotizar). Una tarifa operativa no cambia los importes contractuales anteriores. Cuenta, identidad de servicio, tarifa y publicación envían la versión leída; un conflicto obliga a recargar y revisar, sin reenvío automático. Canciones siguen el contrato específico de repertorio del backend.

El catálogo busca por nombre, ciudad, géneros e instrumentos y filtra por tipo, género y tarifa máxima. Trabaja sobre el snapshot público completo; filtrar nunca sustituye la caché por un subconjunto. El backend añade generos/instrumentos a su resumen público de forma compatible; Room v3 guarda ambos con migración explícita 2→3 y conserva las migraciones anteriores. La información guardada se identifica como anterior si no puede actualizarse. El perfil ampliado y comparación necesitan conexión.

Los listados administrativos tienen paginación; su buscador y estado filtran la página actual y así se informa en pantalla. El selector de responsables recorre las páginas autorizadas para incluir todos los músicos activos. El contador de cuentas del inicio excluye DESACTIVADO. Las estadísticas de solicitudes/contratos sin endpoint muestran — (sin datos), no ceros inventados.

Con fuente ampliada se mantienen cinco iconos inferiores y la etiqueta del acceso seleccionado; las estadísticas usan etiquetas breves con descripciones completas para accesibilidad. Las pantallas privadas restauradas se descartan si no hay sesión o el rol ya no permite su ruta.

Los formularios conservan borradores al recrearse cuando la versión del registro no cambió. No guardan contraseñas en el estado. La recarga que reemplaza lo escrito pide confirmación. Mientras se guarda se impiden envíos duplicados; los POST no se reintentan automáticamente. Ante un resultado de red incierto se debe volver a consultar el estado oficial antes de repetir una creación.

## Alcance respecto a la referencia

La autenticación real sustituye el acceso de demostración. Se adaptan los cinco accesos a módulos disponibles: administrador (Inicio/Usuarios/Servicios/Músicos/Perfil), proveedores (Inicio/Mis bandas o Mi servicio/Músicos/Comparar/Perfil), cliente/público (Inicio/Músicos/Comparar/Guía/Perfil o Ingresar). Guía también está en el drawer.

El CTA administrativo abre Revisar servicios. Las solicitudes, cotizaciones, contratos, agenda operativa y pagos se implementan en las etapas 6–7. Galería, integrantes, reseñas y mediciones voluntarias requieren sus endpoints; no se muestran pestañas o acciones que aparenten estar conectadas. El perfil actual contiene Información y Repertorio. No se simula disponibilidad por fecha ni se muestran resultados académicos inventados.

## Ejecutar y comprobar

Seguir [integracion_android.md](integracion_android.md) para HTTPS local, MySQL y arranque. Desde la raíz:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\tools\Preparar-HttpsLocal.ps1
.\gradlew.bat -p backend bootJar
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:assembleRelease
.\tools\Iniciar-Backend.ps1 -EnvironmentFile .\backend\.env -HttpsLocal
```

La APK debug está en app/build/outputs/apk/debug/app-debug.apk. Release usa una URL HTTPS del entorno y firmado propio, según la guía de integración.

La comprobación local usa datos creados mediante la API en una instancia MySQL aislada y un emulador temporal; no están precargados en el código ni representan clientes de la asociación. Se compilaron debug/release y Android Lint terminó sin errores. Se revisaron inicio, catálogo/filtros a 360×800, 393×851 y 412×892 dp; a 360 dp también hero, acceso/registro, controles y navegación con fuente 150 %. Se verificó la migración real Room 2→3 conservando el catálogo, creación de cuenta/servicio/canción y publicación, edición de tarifa/repertorio por director, perfil propio del solista, menús de los cuatro roles y revocación de acceso tras suspensión. La comparación admitió tres servicios y rechazó el cuarto; búsqueda por instrumento y filtro de tipo/tarifa produjeron los resultados del servidor. Un borrador sobrevivió a la recreación sin guardarse automáticamente. Se confirmó el cambio de estado administrativo con fuente 150 % y la restauración del espacio público al recrear una tarea privada con el archivo de sesión ilegible. Con el backend detenido el catálogo guardado siguió disponible, marcado como anterior. Los procesos temporales de API/MySQL/SMTP y el emulador se detuvieron al finalizar. Estas comprobaciones técnicas no son una validación con ciudadanos ni un nuevo paquete académico de pruebas.

## Capturas de referencia implementada

Capturas de los cuatro roles a 393 dp, obtenidas del emulador y datos del entorno aislado (sin cuentas precargadas en la aplicación):

- [Administrador](interfaz/capturas/administrador.png).
- [Director](interfaz/capturas/director.png).
- [Solista](interfaz/capturas/solista.png).
- [Cliente](interfaz/capturas/cliente.png).

Etapa 7: PagosFragment/PagoDetalleFragment conectan saldo, instrucciones, conciliación e incidencias; PagoComprobanteFragment usa el selector del sistema y comprime imágenes en segundo plano; CuentasCobroFragment gestiona cuentas oficiales y habilitación. El menú agrega Pagos y comprobantes a los roles autenticados y Cuentas receptoras a administración. El cliente no aprueba ingresos ni mueve dinero desde la app. Ver pagos_arquitectura.md.
