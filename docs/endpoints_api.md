Actualización de alcance Etapa 7: el contrato efectivo de solicitudes, agenda, contratos y pagos directos está en [backend/API.md](../backend/API.md). El inventario histórico inferior incluía una pasarela futura; la modalidad elegida por el usuario es comprobante y conciliación administrativa, descrita en [pagos_arquitectura.md](pagos_arquitectura.md).

# Contrato de API — diseño y estado de implementación

Actualización Etapa 3: el contrato efectivo de los endpoints implementados está en ../backend/API.md. El servidor ya es ejecutable y fue comprobado localmente. El inventario siguiente conserva el diseño de todos los módulos: no implica que solicitudes, agenda, contratos, pagos o reportes tengan Controllers implementados. El acceso local es loopback; HTTPS del entorno usa el perfil https o proxy. Etapas 4–5 conectan acceso, cuenta, catálogo/detalle, usuarios, servicios y contenido propio desde Android; ver [integracion_android.md](integracion_android.md).

Los endpoints de backend/API.md tienen contrato efectivo; el resto del inventario conserva nombres propuestos. La respuesta pública inicial de catálogo es un snapshot completo (sin paginación); completo=true se exige antes de reemplazar Room. Si se introduce paginación se deberá cambiar la estrategia de caché, no pasar una página a reemplazarCatalogo.

## Reglas compartidas

- Autorización: Bearer de corta duración en recursos privados. ADMIN=administrador, CLIENTE=dueño de su solicitud/contrato, PROVEEDOR=director/solista responsable del servicio. Roles y propiedad siempre se validan en servidor. Nunca aceptar un rol enviado por registro público.
- Mutaciones sensibles usan Idempotency-Key y validación de versión. Fechas ISO-8601 con offset; servidor conserva instantes UTC y aplica America/Lima a horario recurrente. Importes en céntimos PEN enteros, calculados y verificados en servidor.
- Respuesta de recurso: {"data":{...}}; colección privada paginada: {"data":[],"page":1,"total":0}; error: {"code":"CONFLICTO_HORARIO","message":"El horario ya no está disponible","fieldErrors":{},"correlationId":"..."}. Catálogo público es la excepción completa descrita abajo.
- Códigos: GET/PATCH/acciones 200, creación 201, logout/operación sin cuerpo 204; entrada inválida 400/422, sesión inválida 401, rol/propietario 403 (o 404 para no revelar registros), ausente 404, duplicado/versionado/agenda 409, límite de solicitudes 429, proveedor no disponible 502/503. No devolver stack traces ni secretos.
- Todas las escrituras relevantes registran HistorialOperacion y outbox en la misma transacción. No se borra el historial al suspender usuarios o cancelar.

## Inventario por recurso

En Entrada se enumeran campos JSON y parámetros de ruta/query; Salida contiene data con los campos indicados. Las restricciones anteriores aplican a todas las filas.

| Método y ruta relativa | Actor autorizado | Entrada / parámetros | Salida y validaciones específicas | MySQL previsto |
|---|---|---|---|---|
| POST /auth/register | Público, solo cliente | nombres, apellidos, correo, contraseña, aceptación de privacidad | 201 usuario{id,rol=CLIENTE,estado}; correo único, contraseña validada, hash seguro | Usuario, Rol, auditoría |
| POST /auth/login | Público | correo, contraseña | 200 accessToken, refreshToken, expiresIn, usuario; cuenta activa, límite intentos | Usuario, Sesion |
| POST /auth/refresh | Sesión válida | refreshToken | 200 tokens rotados; detectar reutilización y revocar familia | Sesion |
| POST /auth/logout | Usuario autenticado | refreshToken o sesión actual | 204; revocar sesión sin almacenar contraseña | Sesion |
| POST /auth/forgot-password | Público | correo | 202 mensaje uniforme; token seguro de un uso y TTL, no enumerar cuentas | RecuperacionAcceso |
| POST /auth/reset-password | Público con token de un uso | token, nuevaContraseña | 204; hash y revocación de sesiones, token consumido | Usuario, RecuperacionAcceso, Sesion |
| GET /me | Usuario autenticado | Sin cuerpo | perfil propio, nunca hashes/tokens de terceros | Usuario |
| PATCH /me | Usuario autenticado | nombres, apellidos, teléfono, ciudad, bio | perfil propio; no cambiar rol/estado administrativo | Usuario |
| GET /servicios | Público | Futuro: q, tipo, género, ciudad, tarifaMaxCentimos, fecha; base actual sin filtros | {completo:true,data:[resumen]}; solo publicados y cuentas activas; sin horas Desde/Hasta | ServicioMusical, Usuario, contenido público |
| GET /servicios/{id} | Público para publicados; propietario/admin para privado | id | perfil público, repertorio, galería y reseñas consentidas; nunca eventos privados | ServicioMusical, Repertorio, ImagenServicio, Valoracion |
| GET /servicios/{id}/disponibilidad | Público limitado / propietario | fechaDesde, fechaHasta; opcional inicio/fin del evento | intervalos y estados; márgenes, reservas/retenciones y recursos compartidos; sin identidad de clientes | HorarioAtencion, BloqueoAgenda, Reserva, RetencionAgenda |
| POST /admin/usuarios | ADMIN | datos de director/solista, rol autorizado, invitación de activación | 201 cuenta musical; correo único; sin contraseña en texto plano en respuesta | Usuario, Rol |
| GET /admin/usuarios | ADMIN | q, rol, estado, page | cuentas paginadas sin hashes | Usuario |
| PATCH /admin/usuarios/{id} | ADMIN | datos permitidos, estado, version, motivo | cuenta actualizada; proteger cambios de rol con historial, revocar acceso si se suspende | Usuario, Sesion |
| POST /admin/servicios | ADMIN | tipo, responsableId, nombre, ciudad, géneros, instrumentos | 201 servicio; director para banda, un servicio individual por solista; sin Presentación del servicio | ServicioMusical |
| PATCH /admin/servicios/{id} | ADMIN | campos administrativos, version | servicio; cambios de perfil publicado pasan a revisión sin perder contratos | ServicioMusical |
| POST /admin/servicios/{id}/publicacion | ADMIN | decisión, motivo, version | estado publicación; proveedor activo y repertorio mínimo al publicar | ServicioMusical |
| GET /mis-servicios | PROVEEDOR | page | servicios asignados por identidad autenticada | ServicioMusical |
| PATCH /servicios/{id}/tarifa | PROVEEDOR propio o ADMIN | tarifaCentimos nullable, version | tarifa referencial >=0; no altera snapshots de contrato | ServicioMusical |
| PUT /servicios/{id}/horario | PROVEEDOR propio o ADMIN | días, apertura, cierre, margenes, version | configuración válida, comprobar impacto sobre reservas existentes | HorarioAtencion |
| POST /servicios/{id}/bloqueos | PROVEEDOR propio o ADMIN | inicio, fin, motivo, idempotency key | 201 bloqueo; no contradice reserva/retención; bloquear recursos en transacción | BloqueoAgenda, Reserva |
| DELETE /servicios/{id}/bloqueos/{bloqueoId} | PROVEEDOR propio o ADMIN | idServicio/idBloqueo | 204 retiro auditado sin borrar historia | BloqueoAgenda |
| POST/PATCH /servicios/{id}/repertorio[/{cancionId}] | PROVEEDOR propio o ADMIN | título, género, descripción, duración/audio opcional, version al editar | canción; límites y propiedad; audio validado en servidor | Repertorio |
| DELETE /servicios/{id}/repertorio/{cancionId} | PROVEEDOR propio o ADMIN | IDs | 204; conservar referencias históricas | Repertorio |
| POST/PATCH /servicios/{id}/integrantes[/{integranteId}] | DIRECTOR propio o ADMIN | nombre, instrumento, función, solistaId opcional | integrante de BANDA; no integrantes de servicio individual, vínculo requiere consentimiento si corresponde | Integrante |
| DELETE /servicios/{id}/integrantes/{integranteId} | DIRECTOR propio o ADMIN | IDs | 204 retirar conservando snapshot histórico | Integrante |
| POST /servicios/{id}/imagenes | PROVEEDOR propio o ADMIN | multipart archivo, descripción, orden | 201 metadatos; MIME/tamaño validado, URL controlada; no claves en respuesta | ImagenServicio y almacén de objetos |
| DELETE /servicios/{id}/imagenes/{imagenId} | PROVEEDOR propio o ADMIN | IDs | 204 retirar; política de retención | ImagenServicio |
| POST /integrantes/{id}/respuesta-vinculo | Solista invitado | aceptar boolean, version | vínculo aceptado/rechazado; validar titularidad y conflictos compartidos | Integrante |
| POST /solicitudes | CLIENTE | servicioId, inicio, fin, lugar, ciudad, tipoEvento, asistentes, requerimientos | 201 solicitud; intervalo <=24h, fechas válidas, no duplicado abierto, agenda válida; no reserva | SolicitudCotizacion |
| GET /solicitudes | CLIENTE/PROVEEDOR propios, ADMIN consulta global | estado, q, page | solicitudes autorizadas paginadas | SolicitudCotizacion |
| GET /solicitudes/{id} | Dueño/PROVEEDOR responsable/ADMIN | id | detalle, versiones e historial limitado por actor | SolicitudCotizacion, Cotizacion, HistorialOperacion |
| POST /solicitudes/{id}/estado | CLIENTE cancelar propia; PROVEEDOR revisar/rechazar propia | acción, motivo, version | estado válido; rechazo/cancelación con motivo; no alterar si ya hay contrato | SolicitudCotizacion |
| POST /cotizaciones | PROVEEDOR responsable | solicitudId, conceptos, descuento, inicioPropuesto, finPropuesto, vigencia, condiciones, política | 201 versión consecutiva, total calculado, solicitud abierta sin contrato, disponibilidad | Cotizacion, DetalleCotizacion |
| GET /cotizaciones/{id} | CLIENTE/PROVEEDOR relacionados, ADMIN consulta | id | versión exacta, desglose, vigencia y condiciones | Cotizacion, DetalleCotizacion |
| POST /cotizaciones/{id}/respuesta | CLIENTE dueño | acción=rechazar/pedirCambios, motivo | respuesta; propuesta vigente y estado permitido | Cotizacion, SolicitudCotizacion |
| POST /cotizaciones/{id}/aceptacion | CLIENTE dueño | version, condicionesAceptadas=true, politicaVersion; Idempotency-Key | 201 contrato PENDIENTE_PAGO; disponibilidad revalidada, único por solicitud, snapshot inmutable | Contrato, Cotizacion, SolicitudCotizacion |
| GET /contratos[/{id}] | CLIENTE/PROVEEDOR propios, ADMIN global | filtros de fecha, estado, servicio, tipo, page | detalle/lista; fechas de evento distintas a fecha de confirmación | Contrato, Cotizacion, Pago |
| POST /contratos/{id}/cancelacion | Dueño/PROVEEDOR responsable/ADMIN | motivo, version; Idempotency-Key | contrato y resolución según política; devolución no instantánea si aún no verificada | Contrato, Reserva, Reembolso, historial |
| POST /contratos/{id}/finalizacion | PROVEEDOR responsable/ADMIN | version | contrato finalizado solo después del horario y estado autorizado | Contrato |
| GET /contratos/{id}/constancia | CLIENTE/PROVEEDOR propios/ADMIN | id | PDF privado con código/versión, no constancia ficticia | Contrato, snapshot |
| POST /contratos/{id}/pagos | CLIENTE dueño | modalidad=SALDO_COMPLETO/MITAD_SALDO; Idempotency-Key | 201 intento{id,estado,checkoutUrl}; servidor fija importe y moneda; sin estado aprobado de Android | Pago, RetencionAgenda condicional |
| GET /contratos/{id}/pagos | Actores relacionados autorizados/ADMIN | id | intentos/abonos/saldo oficial | Pago, Contrato |
| GET /pagos/{id} | CLIENTE/PROVEEDOR relacionados/ADMIN | id | estado verificado, importe, referencia pública y saldo; sin payload secreto | Pago, Contrato |
| POST /webhooks/pagos | Proveedor autenticado por firma oficial | evento oficial y headers de firma, sin asumir su formato antes de elegir proveedor | 2xx tras registrar evento; verificar por API, deduplicar; no confiar en un approved entrante sin validación | EventoPago, Pago, Reserva, IncidenciaPago, outbox |
| POST /pagos/{id}/reembolsos | ADMIN autorizado conforme política | motivo, importe autorizado; Idempotency-Key | 201 devolución pendiente; no declarar reembolsado antes de confirmación externa | Reembolso, Pago |
| POST /contratos/{id}/valoracion | CLIENTE dueño | puntuación, comentario, consentimientoPublicacion | 201 única por contrato finalizado; instrumento de encuesta pendiente | Valoracion |
| GET /notificaciones | Usuario destinatario | page, leida | notificaciones propias y destino autorizado | Notificacion |
| PATCH /notificaciones/{id} | Destinatario | leida=true | notificación actualizada | Notificacion |
| GET /admin/indicadores | ADMIN | desde, hasta, servicio, tipo | agregados reales y cohortes; diferenciar confirmación de evento | SolicitudCotizacion, Contrato, Pago, EventoMedicion |
| GET /admin/contratos/exportacion | ADMIN | mismos filtros | archivo de datos autorizado, sin secretos; no métricas inventadas | Contrato, Pago |
| GET /admin/auditoria | ADMIN | actor, entidad, desde, hasta, page | historial sin credenciales o datos financieros sensibles | HistorialOperacion |
| POST /mediciones/busqueda/eventos | Usuario con consentimiento | sesión, acción=inicio/perfil/fin, servicioId opcional, consentimiento | 201 evento con hora de servidor; no iniciar medición automáticamente | EventoMedicion |

Los grupos POST/PATCH se separarán en operaciones OpenAPI individuales al implementar el servidor. La matriz de roles está en ../ENTREGABLES_UNIDAD_III/08_Roles_Permisos_Seguridad.md.

## Contrato concreto consumido por el cliente base

```http
GET /api/v1/servicios
Accept: application/json
```

Respuesta vacía válida (ejemplo de contrato, no respuesta de un servidor ejecutado):

```json
{"completo":true,"data":[]}
```

Cada resumen incluye id entero positivo, nombre no vacío, tipo BANDA o SOLISTA, ciudad y tarifaCentimos entero no negativo o null. Etapa 5 añade listas públicas generos e instrumentos. No contiene estado privado, disponibilidad confirmada, dirección de eventos o usuario de clientes. IDs repetidos, céntimos fraccionarios, tipo desconocido o snapshots incompletos se rechazan sin sobrescribir la caché.

Debug apunta al backend HTTPS local del emulador; otros entornos se configuran con -PSANTA_CECILIA_API_URL. Android consume además registro/login/refresh/logout/forgot-password, GET /me y GET /servicios/{id}, con los cuerpos efectivos de ../backend/API.md. Las credenciales viajan solo por HTTPS; no se llaman endpoints financieros.
