# API implementada — Etapas 3–7

Prefijo /api/v1. JSON UTF-8. Recursos privados: Authorization: Bearer <accessToken>. Contraseñas: al menos 12 caracteres y como máximo 72 bytes UTF-8, sin controles. Los tokens son opacos, no JWT. Cada acceso se verifica contra cuenta, sesión y familia revocada en MySQL; no se confía en un rol suministrado por Android.

Salvo el catálogo, las respuestas con datos usan {"data":...}. Los errores usan {"code":...,"message":...,"fieldErrors":{},"correlationId":...}. No incluyen hashes, contraseñas, SQL ni tokens de terceros. Los cuerpos rechazan propiedades desconocidas: agregar rol=ADMINISTRADOR al registro público no eleva permisos.

## Acceso y perfil

| Método / ruta | Autorización | Cuerpo o resultado |
|---|---|---|
| GET /health | Público | data.estado; consulta MySQL |
| POST /auth/register | Público | nombres, apellidos, correo, password, aceptaPrivacidad=true, privacidadVersion; 201 cliente |
| POST /auth/login | Público | correo, password; data con tokenType=Bearer, accessToken, refreshToken, expiresIn y usuario |
| POST /auth/refresh | Público con refresh válido | refreshToken; emite ambos tokens nuevos. No enviar un accessToken vencido en Authorization |
| POST /auth/logout | Bearer válido | Sin cuerpo; 204, revoca toda la familia |
| POST /auth/forgot-password | Público y limitado por IP | correo; 202 uniforme con SMTP disponible; 503 si no está configurado |
| POST /auth/reset-password | Token de correo de un uso | token, nuevaPassword; 204 y revoca sesiones |
| GET /me | Usuario activo | Perfil propio y rol oficial |
| PATCH /me | Usuario activo | version, nombres, apellidos, telefono opcional, ciudad opcional, biografia opcional |

Access token: 15 minutos; refresh: máximo 30 días desde login, sin prolongación ilimitada al rotar; recuperación: 30 minutos. MySQL almacena hashes de los tokens aleatorios. Al reutilizar un refresh consumido se confirma la revocación de la familia aunque se responda 401. El límite local es 60 llamadas de acceso por minuto/IP y 10 intentos de login por cuenta cada 15 minutos; un despliegue con varias réplicas debe coordinar el límite en su gateway.

## Usuarios administrativos

Solo ADMINISTRADOR:

| Método / ruta | Cuerpo / resultado |
|---|---|
| GET /admin/usuarios?pagina=0&tamano=25 | Página de perfiles sin hashes; tamaño máximo 100 |
| GET /admin/usuarios/{id} | Perfil administrativo |
| POST /admin/usuarios | nombres, apellidos, correo, password, rol=DIRECTOR o SOLISTA; 201 |
| PUT /admin/usuarios/{id} | version y campos de Perfil, como PATCH /me |
| PATCH /admin/usuarios/{id}/estado | version, estado=ACTIVO/SUSPENDIDO/DESACTIVADO, motivo (5–1000 caracteres) |
| DELETE /admin/usuarios/{id} | version, motivo; baja lógica DESACTIVADO |

El correo y rol no se modifican mediante los cuerpos de perfil. La creación musical recibe la contraseña inicial elegida por el administrador, la guarda como bcrypt y nunca la devuelve. No se suspende/desactiva un administrador desde este CRUD para conservar el acceso de operación. Suspender/desactivar revoca familias y oculta del catálogo los servicios de esa cuenta.

## Servicios y repertorio

| Método / ruta | Actor | Cuerpo / resultado |
|---|---|---|
| GET /servicios | Público | Snapshot {completo:true,data:[{id,nombre,tipo,ciudad,tarifaCentimos,generos,instrumentos}]} de publicados con responsable activo |
| GET /servicios/{id} | Público si publicado; propietario/admin en otro estado | Perfil, géneros, instrumentos, tipos de evento y repertorio; sin datos privados de clientes |
| GET /mis-servicios | DIRECTOR/SOLISTA | Servicios asignados al actor |
| GET /admin/servicios?pagina=0&tamano=25 | ADMINISTRADOR | Página administrativa |
| POST /admin/servicios | ADMINISTRADOR | Cuerpo Guardar de abajo; 201 BORRADOR |
| PUT /admin/servicios/{id} | ADMINISTRADOR | Cuerpo Guardar completo con version; cambios de perfil publicado pasan a EN_REVISION |
| POST /admin/servicios/{id}/publicacion | ADMINISTRADOR | version, estado, motivo si OBSERVADO/RETIRADO |
| DELETE /admin/servicios/{id} | ADMINISTRADOR | version, motivo; retiro lógico RETIRADO |
| PATCH /servicios/{id}/tarifa | ADMINISTRADOR o proveedor propio | version, tarifaCentimos entero no negativo o null (A cotizar) |
| POST /servicios/{id}/repertorio | ADMINISTRADOR o proveedor propio | titulo, genero, descripcion opcional, duracionSegundos opcional, orden opcional; 201 |
| PUT /servicios/{id}/repertorio/{cancion} | ADMINISTRADOR o proveedor propio | Mismo cuerpo de canción; 204 |
| DELETE /servicios/{id}/repertorio/{cancion} | ADMINISTRADOR o proveedor propio | Retiro lógico; 204 |

Cuerpo Guardar:

```json
{
  "responsableId": 123,
  "tipo": "BANDA",
  "nombre": "Nombre del servicio",
  "ciudad": "Ambo",
  "descripcionHistorica": null,
  "experiencia": null,
  "incluye": null,
  "tarifaCentimos": null,
  "generos": [],
  "instrumentos": [],
  "tiposEvento": [],
  "version": 0
}
```

El ID anterior es un marcador: debe pertenecer a una cuenta real registrada. version se omite al crear y se exige al editar. No hay campo Presentación del servicio. Una banda requiere director activo, un servicio individual requiere solista activo y no se duplica ese servicio por solista. El tipo no se cambia y no se reasigna un servicio con historial mediante este CRUD. Para publicar o enviar a revisión debe existir al menos una canción. Las tarifas y cambios operativos de repertorio no alteran contratos históricos.

Filtros actuales del catálogo: q (nombre, ciudad, géneros, instrumentos), tipo=BANDA/SOLISTA, genero, ciudad y tarifaMaxCentimos. No se agregaron Desde/Hasta. Los filtros desconocidos, incluida fecha, se rechazan hasta implementar disponibilidad real; no se simula que una fecha está libre.

Los ejemplos son contratos de API; no crean cuentas o servicios al arrancar. Disponibilidad, solicitudes, cotizaciones, contratos, pagos y notificaciones todavía no tienen Controllers REST ejecutables en esta etapa.

## Ampliación de interfaz — Etapa 5

El resumen público incorpora generos e instrumentos como listas de texto, sin datos privados ni cambios de esquema MySQL. Permite los filtros del catálogo completo en la caché Android. Los demás cuerpos y permisos se conservan; la interfaz usa paginación administrativa, CRUD, PATCH /me, tarifa y repertorio sobre los endpoints anteriores.
# Etapa 6 — solicitudes, propuestas, contratos y agenda

Se añade al contrato anterior; prefijo `/api/v1`. Respuesta `{data:...}`. Los campos de las entradas son camelCase y los registros históricos de este módulo conservan nombres SQL snake_case. Las fechas de entrada/salida son instantes ISO-8601 UTC (`...Z`); Android convierte desde/hacia `America/Lima`. La precisión de inicio/fin es de segundos, sin fracciones. Importes enteros en céntimos PEN.

| Método/ruta | Actores y entrada |
|---|---|
| POST `/solicitudes` | CLIENTE. `{servicioId,inicio,fin,lugar,ciudad,tipoEvento,asistentes?,preferencias?,requerimientos?,notas?,clave}` |
| GET `/solicitudes` | Listado del cliente/proveedor; administrador global. `pagina=0&tamano=20`, `estado?`, `q?` |
| GET `/solicitudes/{id}` | Solo actores de esa solicitud o administrador. Incluye `cotizaciones`, conceptos por versión, `historial`, `contratoId` nullable y `ahora` |
| POST `/solicitudes/{id}/estado` | `{version,accion,motivo?}`. Cliente: CANCELAR; proveedor: REVISION/RECHAZAR. Cancelar/rechazar requiere motivo de 5–1000 caracteres. Administrador no responde por otros actores |
| POST `/solicitudes/{id}/cotizaciones` | Proveedor original. `{version,inicio,fin,conceptos:[{concepto,cantidad,precioCentimos}],descuentoCentimos,condiciones,incluyePolitica:true,vigenteHasta,clave}` |
| POST `/solicitudes/{id}/cotizaciones/{quote}/respuesta` | Cliente original. `{version,accion:CAMBIOS/RECHAZAR,motivo?}`. CAMBIOS requiere al menos cinco caracteres |
| POST `/solicitudes/{id}/cotizaciones/{quote}/aceptacion` | Cliente original. `{version,aceptaCondiciones:true,clave}`. Devuelve el contrato, sin reserva ni retención |
| GET `/contratos` | Paginación/filtros como solicitudes, limitados por actor |
| GET `/contratos/{id}` | Actores o administrador. Incluye snapshot JSON histórico, horas del evento y de ocupación, estado/version, historial y reloj `ahora` |
| POST `/contratos/{id}/estado` | `{version,accion:CANCELAR/FINALIZAR,motivo?}`. Solo CONFIRMADO. CANCELAR: actores/admin, motivo obligatorio. FINALIZAR: proveedor/admin, después del fin según reloj del servidor |
| GET `/servicios/{id}/disponibilidad?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Público si servicio publicado y responsable activo; intervalo máximo de 63 días. Sin motivos, actores, contratos ni IDs de ocupaciones |
| GET `/servicios/{id}/agenda?desde=...&hasta=...` | Propietario/admin; agrega bloqueos propios con motivo e ID |
| PUT `/servicios/{id}/horarios` | Propietario/admin. `{version,franjas:[{dia:1..7,desde:HH:mm,hasta:HH:mm,previoMin:0..1440,posteriorMin:0..1440}]}`. Hasta 28 franjas; `hasta` admite `24:00`; sin superposición. Vacío deshabilita atención futura |
| POST `/servicios/{id}/bloqueos` | Propietario/admin. `{inicio,fin,motivo}`. Intervalo futuro de hasta 24 horas, motivo de 5–1000 caracteres. Una ocupación por recurso original |
| DELETE `/servicios/{id}/bloqueos/{block}` | Propietario/admin. Retiro lógico; libera los recursos originales en la misma transacción |
| GET `/mi-resumen` | Conteos autorizados: `solicitudes` abiertas, `contratos` únicos y `eventos` futuros CONFIRMADO |

Los listados devuelven `{items,pagina,tamano,total}`. Las agendas devuelven `{version,zonaHoraria,ahora,horarios,ocupaciones,bloqueos?}`. Las ocupaciones públicas contienen exclusivamente `inicio_utc`, `fin_utc`, `estado:RESERVADO/OCUPADO`.

`clave` debe tener 16–100 caracteres ASCII `[A-Za-z0-9_-]`; un UUID es válido. Solicitudes, cotizaciones y aceptaciones conservan una huella canónica: el mismo reintento devuelve el registro existente, otra entrada con igual clave devuelve 409. Las versiones de solicitud se incrementan también al enviar/responder cotizaciones. La versión de agenda es independiente de la versión del perfil musical. No se borra el historial de propuestas ni contratos.

Los conceptos son de 2–150 caracteres, cantidad entera 1–1000, hasta 15 líneas y total positivo máximo 100 000 000 céntimos. Condiciones de 10–2000 caracteres. Vigencia futura hasta el inicio propuesto; siempre se comprueba la última propuesta ENVIADA. La política `cancelacion-cliente-v1` se incorpora en el servidor y se copia al contrato. La entrada nunca puede imponer otra política ni un total calculado por el cliente.

El cierre contractual tras un pago real y la confirmación/reserva financiera se completan y verifican con Etapa 7. No existe un endpoint manual para confirmar contratos ni aprobar pagos en esta etapa. Véase [guía y alcance](../docs/contratacion_agenda.md).


## Etapa 7 — pagos directos (decisión actual del usuario)

No se integró pasarela comercial: Yape, Plin y transferencia se realizan fuera de la app y se concilian por administración. Respuestas 200 con `{data:...}`; imágenes privadas con Cache-Control no-store. Los campos desconocidos y céntimos JSON fraccionarios se rechazan.

| Método / ruta | Entrada y permisos |
|---|---|
| GET /admin/cuentas-cobro | ADMIN; lista de cuentas receptoras |
| POST /admin/cuentas-cobro | ADMIN; `{version:0,titular,banco,telefono?,numeroCuenta?,cci?,yape,plin,transferencia,activo}`; enviar todos los booleanos |
| PUT /admin/cuentas-cobro/{id} | ADMIN; mismo cuerpo con versión actual. Los datos receptores con historial no se modifican |
| GET /contratos/{id}/pagos | Actores del contrato / ADMIN; total, abonado, devuelto, saldo, cuentas habilitadas e intentos |
| POST /contratos/{id}/pagos | CLIENTE dueño; `{cuentaId,metodo:YAPE/PLIN/TRANSFERENCIA,modalidad:SALDO_COMPLETO/MITAD_SALDO,clave}`; servidor fija importe |
| GET /pagos?pagina=0&estado=... | Bandeja paginada de 20, delimitada por cliente/proveedor; ADMIN global |
| GET /pagos/{id} | Actores / ADMIN; intento, destino histórico, reporte, movimientos verificados, incidencias y devoluciones |
| PUT /pagos/{id}/comprobante | CLIENTE dueño; `{version,operacion,fecha:YYYY-MM-DD,importeCentimos,titularOrigen,imagenBase64}`; pasa a PROCESANDO sin abonar ni reservar |
| GET /pagos/{id}/comprobante | Actores / ADMIN; `{imagenBase64,tipo:image/jpeg}`; no hay archivo público |
| POST /pagos/{id}/cancelacion | CLIENTE dueño; `{version}`; solo PENDIENTE sin comprobante. No revierte transferencias |
| POST /admin/pagos/{id}/revision | ADMIN; rechazo `{version,accion:RECHAZAR,motivo}` o contraste `{version,accion:VERIFICAR,contrasteEnCuenta:true,operacion,fecha,importeCentimos,motivo?}` |
| POST /admin/pagos/{id}/reembolsos | ADMIN; `{importeCentimos,motivo,clave}`; solo ingreso contrastado y según estado/política; no mueve dinero |
| GET /reembolsos/{id} | Actores / ADMIN; detalle de devolución con pago relacionado |
| POST /admin/reembolsos/{id}/confirmacion | ADMIN; `{version,operacion,fecha,importeCentimos,contrasteEnCuenta:true,imagenBase64}`; documenta devolución ya realizada |
| POST /admin/reembolsos/{id}/rechazo | ADMIN; `{version,accion:RECHAZAR,motivo}`; anula solicitud aún no confirmada |
| GET /reembolsos/{id}/comprobante | Actores / ADMIN; imagen privada de devolución confirmada |

`clave` es ASCII `[A-Za-z0-9_-]`, 16–100 caracteres. Operación: letras/números/guion, 3–100 caracteres, normalizada a mayúsculas. Motivos obligatorios: 5–1000 caracteres. Imágenes: JPEG/PNG hasta 2 MB de entrada al servidor, hasta cuatro millones de píxeles y 4096 por lado; recodificadas a JPEG de hasta 1 MB. El cuerpo completo de upload se limita a 3 MB; Android reduce archivos seleccionados de hasta 10 MB antes del envío.

Solo se aplica un ingreso verificado que coincida con el intento y pueda abonarse sin conflicto. La referencia DIRECTO-n identifica el registro conciliado interno; la operación bancaria se conserva en movimientos. Un ingreso incompatible se conserva como EN_REVISION sin aplicación al saldo. La unicidad de movimiento por cuenta/fecha/operación y las huellas evitan doble aplicación. Reintentos de revisión iguales devuelven el resultado previo; distintas entradas dan 409.

Las devoluciones se confirman con su propio comprobante y operación, sin exceder el ingreso verificado ni reutilizar movimientos. No hay endpoint para que un cliente declare un pago aprobado, ni se confía solo en su captura. Los avisos se entregan mediante outbox a notificaciones internas. Véase ../docs/pagos_arquitectura.md para operación y límites de comprobación humana.
