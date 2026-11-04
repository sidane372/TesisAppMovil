# Paquetes, clases y pantallas

La raíz sigue siendo el proyecto Android abierto. El código base implementado se separa del inventario futuro; los paquetes previstos se documentan aquí y se crean cuando haya una responsabilidad ejecutable. Los recursos Android están bajo app/src/main/res/, nunca dentro de java/.

## Responsabilidad de paquetes Android

| Paquete bajo com.example.santaceciliatesis | Responsabilidad | Estado |
|---|---|---|
| model | Dominio sin anotaciones Room/JSON | ServicioMusical, ServicioDetalle, Usuario, Sesion, TipoServicio, Rol |
| view.activity | Contenedor y navegación entre pantallas | MainActivity |
| view.fragment.common | Pantallas compartidas nativas | InicioBaseFragment, CatalogoFragment, AccesoFragment, CuentaFragment, DetalleServicioFragment, CompararFragment, GuiaFragment, ServiciosGestionFragment, GestionServicioFragment, FormularioFragment |
| view.fragment.cliente / administrador / director / solista | Dashboards por rol y administración | InicioClienteFragment, InicioAdministradorFragment, InicioDirectorFragment, InicioSolistaFragment; UsuariosFragment, UsuarioDetalleFragment |
| view.adapter | Enlace de listas y callbacks | ServicioAdapter, UsuarioAdapter, GestionServicioAdapter, CancionAdapter |
| view.viewholder | Presentación de filas independientes | ServicioViewHolder, UsuarioViewHolder, GestionServicioViewHolder, CancionViewHolder |
| view.dialog | Cambios de estado con selección/motivo | EstadoDialog |
| controller | Coordinar vista/repositorios y ciclo de vida | CatalogoController, AuthController, CuentaController, ConsultaController, FormularioController, ComparacionController |
| data | Composición de dependencias con alcance Application | AppContainer |
| data.local | Singleton Room y esquema SQLite | AppDatabase |
| data.local.entity / dao / mapper | Esquema local, consultas y transformación | Caché de catálogo |
| data.local.migration | Migraciones explícitas Room | DatabaseMigrations.MIGRATION_1_2 y MIGRATION_2_3 |
| data.remote | HTTPS nativo, cancelación, errores y renovación | ApiConfig, ApiClient, ApiEndpoints, RequestHandle, ApiFailure, AuthenticatedApiClient |
| data.remote.dto / mapper | Contrato y validación JSON | ServicioResumenDto, ServicioJsonMapper, ServicioDetalleMapper, AuthJsonMapper |
| data.repository | Coordinar fuente oficial/caché y acceso | ServicioRepository, AuthRepository, ComunidadRepository |
| service | Sesión real cifrada y revisión de cuenta | SessionStore, SessionManager |
| util | Formato monetario exacto y resultado de consultas | MoneyUtils, Resource |

## Clases principales

| Clase o grupo | Responsabilidad | Capa | Estado |
|---|---|---|---|
| SantaCeciliaApplication, AppContainer | Construir dependencias sin retener Activities | Infraestructura | Base implementada |
| ServicioMusical, TipoServicio, Rol | Resumen público e identificadores del dominio | Modelo | Implementado; enum de rol no autoriza operaciones |
| AppDatabase, ServicioCacheEntity, CacheScopeEntity, ServicioDao | Room v3, origen y metadatos públicos | Modelo/datos | Implementado, migraciones 1→2→3 |
| ServicioResumenDto, ServicioJsonMapper, ServicioCacheMapper | Separación entre transporte, dominio y caché | Modelo/datos | Implementado |
| ApiConfig, ApiEndpoints, ApiClient, AuthenticatedApiClient | HTTPS público/autenticado y rotación | Modelo/datos | Integrado con backend real |
| SessionStore, SessionManager, Usuario, Sesion | Persistencia cifrada y restauración de acceso propio | Modelo/servicio | Implementado |
| AuthController, CuentaController, AuthRepository | Registro/login/recuperación/perfil/logout | Controlador/datos | Integrado |
| ServicioRepository | Consulta de caché, refresco remoto y errores | Modelo/datos | Implementado |
| CatalogoController | Estados y cancelación al detener vista | Controlador | Implementado |
| MainActivity, CatalogoFragment | Contenedor y presentación de consulta | Vista | Base implementada |
| ServicioAdapter, ServicioViewHolder | Fila del catálogo y callback independiente | Vista | Implementado |
| HorarioAtencion, BloqueoAgenda | Agenda | Modelo | Previsto |
| SolicitudCotizacion, Cotizacion, DetalleCotizacion | Solicitudes y versiones de propuesta | Modelo | Previsto |
| Contrato, Pago, Reserva | Contratación y seguimiento de estados oficiales | Modelo | Previsto; sin escrituras offline |
| Integrante, Repertorio, ImagenServicio | Contenido individual de servicio | Modelo | Previsto |
| Notificacion, Valoracion, HistorialOperacion | Seguimiento y evidencia | Modelo | Previsto |
| ServicioController, SolicitudController, CotizacionController | Coordinación por módulo | Controlador | Previsto |
| ContratoController, PagoController, CalendarioController, AdministradorController | Coordinación de consultas y acciones autorizadas | Controlador | Previsto |
| UsuarioRepository, SolicitudRepository, CotizacionRepository, ContratoRepository, PagoRepository, CalendarioRepository | Acceso centralizado por módulo | Modelo/datos | Previsto |

## Relación de pantalla y archivos

Prefijos de las tablas: A=Activity, F=Fragment. Cada nombre previsto se crea en su etapa con su XML; no se incluyen pantallas vacías para aparentar cobertura. Los listados comparten Adapter/ViewHolder solo cuando comparten el mismo modelo de fila.

| Actor / pantalla | Activity o Fragment | Layout | Adapter → ViewHolder → fila | Estado |
|---|---|---|---|---|
| Común / contenedor | A MainActivity | activity_main.xml | No corresponde | Implementado |
| Común / catálogo | F CatalogoFragment | fragment_catalogo.xml | ServicioAdapter → ServicioViewHolder → item_servicio.xml | HTTPS real y caché Room |
| Acceso | F AccesoFragment (LOGIN) | fragment_acceso.xml | No corresponde | Integrado |
| Registro cliente | F AccesoFragment (REGISTRO) | fragment_acceso.xml | No corresponde | Integrado, privacidad obligatoria |
| Recuperación | F AccesoFragment (RECUPERAR) | fragment_acceso.xml | No corresponde | Solicitud SMTP integrada |
| Mi cuenta | F CuentaFragment | fragment_cuenta.xml | No corresponde | Perfil propio y cierre integrados |
| Cliente / inicio | F cliente.InicioClienteFragment | fragment_inicio.xml | ServicioAdapter → ServicioViewHolder → item_servicio.xml | Implementado |
| Admin / inicio | F administrador.InicioAdministradorFragment | fragment_inicio.xml | ServicioAdapter → ServicioViewHolder → item_servicio.xml | Implementado |
| Director / inicio | F director.InicioDirectorFragment | fragment_inicio.xml | GestionServicioAdapter → GestionServicioViewHolder → item_gestion_servicio.xml | Implementado |
| Solista / inicio | F solista.InicioSolistaFragment | fragment_inicio.xml | GestionServicioAdapter → GestionServicioViewHolder → item_gestion_servicio.xml | Implementado |
| Admin / usuarios | F administrador.UsuariosFragment | fragment_lista_gestion.xml | UsuarioAdapter → UsuarioViewHolder → item_usuario.xml | Integrado con paginación |
| Admin / servicios | F common.ServiciosGestionFragment (admin) | fragment_lista_gestion.xml | GestionServicioAdapter → GestionServicioViewHolder → item_gestion_servicio.xml | Integrado |
| Admin / edición servicio | F common.FormularioFragment (SERVICIO) | fragment_formulario.xml + form_servicio.xml | No corresponde | Integrado |
| Director / mis bandas | F common.ServiciosGestionFragment (propios) | fragment_lista_gestion.xml | GestionServicioAdapter → GestionServicioViewHolder → item_gestion_servicio.xml | Integrado |
| Solista / mi servicio | F common.ServiciosGestionFragment (propios) | fragment_lista_gestion.xml | GestionServicioAdapter → GestionServicioViewHolder → item_gestion_servicio.xml | Integrado |
| Común / perfil público | F common.DetalleServicioFragment | fragment_detalle_servicio.xml | Información/repertorio en vista común | Consulta integrada; calendario integrado en Etapa 6; galería/reseñas pendientes |
| Proveedor / tarifa | F common.FormularioFragment (TARIFA) | fragment_formulario.xml + form_tarifa.xml | No corresponde | Tarifa integrada; horario integrado en AgendaFragment/HorariosFragment |
| Proveedor / repertorio | F common.GestionServicioFragment + FormularioFragment (CANCION) | fragment_gestion_servicio.xml + form_cancion.xml | CancionAdapter → CancionViewHolder → item_cancion.xml | CRUD de texto/duración integrado; audio pendiente |
| Banda / integrantes | F common.IntegrantesFragment | fragment_integrantes.xml | IntegranteAdapter → IntegranteViewHolder → item_integrante.xml | Previsto |
| Servicio / galería | F common.GaleriaFragment | fragment_galeria.xml | ImagenAdapter → ImagenViewHolder → item_imagen.xml | Previsto |
| Común / calendario | F common.AgendaFragment + HorariosFragment | fragment_flujo.xml | Controles nativos de calendario y franjas variables | Integrado en Etapa 6: público/privado, horarios y bloqueos |
| Cliente / solicitar en tres pasos | F common.ContratacionFormFragment (solicitar) | fragment_flujo.xml | Asistente nativo con borrador/paso/clave | Integrado en Etapa 6 |
| Roles autorizados / solicitudes | F common.BandejaFragment (solicitudes) | fragment_flujo.xml | Tarjetas nativas y paginación | Integrado por rol/propiedad |
| Común / detalle solicitud | F common.SolicitudDetalleFragment | fragment_flujo.xml | Versiones, desglose, historial y respuestas | Integrado en Etapa 6 |
| Proveedor / elaborar cotización | F common.ContratacionFormFragment (cotizar) | fragment_flujo.xml | Conceptos variables, consentimiento y total | Integrado en Etapa 6 |
| Cliente / revisar propuesta | F common.SolicitudDetalleFragment | fragment_flujo.xml | Aceptación expresa, cambios/rechazo | Integrado en Etapa 6 |
| Común / contratos | F common.BandejaFragment (contratos) | fragment_flujo.xml | Búsqueda, estado y paginación | Integrado por rol/propiedad |
| Común / detalle contrato | F common.ContratoDetalleFragment | fragment_flujo.xml | Snapshot y cierre autorizado | Integrado; pago/confirmación real en Etapa 7 |
| Común / pagos y saldos | F common.PagosFragment + PagoDetalleFragment | fragment_flujo.xml | Controles nativos, bandejas y formularios variables | Integrado en Etapa 7: saldo, reporte, conciliación y devoluciones |
| Cliente / comprobante | F common.PagoComprobanteFragment | fragment_flujo.xml | Selector de documentos del sistema y preparación de imagen en repositorio | Integrado en Etapa 7 para pagos directos; sin checkout comercial |
| Común / notificaciones | F common.NotificacionesFragment | fragment_notificaciones.xml | NotificacionAdapter → NotificacionViewHolder → item_notificacion.xml | Previsto |
| Cliente / valoración | Diálogo ValoracionDialog | dialog_valoracion.xml | No corresponde | Previsto |
| Común / perfil personal | F common.CuentaFragment + FormularioFragment (PERFIL) | fragment_cuenta.xml + form_usuario.xml | No corresponde | Consulta y edición integradas |
| Común / guía | F common.GuiaFragment | fragment_guia.xml | No corresponde | Implementado, privacidad/licencias locales |
| Admin / indicadores | F administrador.IndicadoresFragment | fragment_indicadores.xml | No corresponde | Previsto |
| Admin / auditoría | F administrador.AuditoriaFragment | fragment_auditoria.xml | HistorialAdapter → HistorialViewHolder → item_historial.xml | Previsto |

La vista recibe datos del controlador; no invoca DAO/SQL ni pasarela. El acceso a AppContainer en onViewCreated solo construye el controlador con su repositorio. Los callbacks de clic regresan a la pantalla/controlador. Los permisos y el filtrado de datos se exigirán en el servidor.

Etapa 5: el shell MainActivity incorpora drawer y cinco accesos por rol. CompararFragment utiliza ComparacionController y selección pública de hasta tres IDs; FormularioFragment infla campos XML por operación y delega reglas/JSON a FormularioController. ComunidadRepository no persiste listados privados. Usuario incorpora contacto opcional; Cancion y Pagina tipan repertorio y paginación. Ver interfaz.md para alcance y adaptación de rutas.

Etapa 6: FlujoController controla operaciones en curso y callbacks de ciclo de vida; ContratacionRepository usa HTTPS y JSON privado exclusivamente en memoria. Los formularios validan la presentación y componen la entrada; las reglas definitivas de disponibilidad, estados, dinero, versión e identidad pertenecen a ContratacionService del backend. No hay DAO ni SQL en las vistas. Las listas variables usan controles nativos sobre fragment_flujo.xml, sin WebView.

Etapa 7 reutiliza FlujoController y ContratacionRepository para pagos privados. CuentasCobroFragment administra destinos; ComprobanteDialog muestra imágenes autenticadas con zoom. PagoComprobanteFragment conserva campos/URI sin guardar bytes en Bundle; el repositorio prepara imágenes en el executor. Las reglas monetarias y la aplicación de abonos pertenecen a PagoDirectoService, no a Room. Los recursos/franjas/contratos continúan en sus clases de Etapa 6.
