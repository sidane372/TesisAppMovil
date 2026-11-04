# Árbol de fuentes — Etapa 5

Inventario actual; excluye build, caches, .local y credenciales. Backend es un build independiente con el wrapper raíz y -p backend. Los módulos futuros están en estructura_mvc.md.

```text
SantaCeciliaTesis/
├── app/
│   ├── schemas/
│   │   └── com.example.santaceciliatesis.data.local.AppDatabase/
│   │       ├── 1.json
│   │       ├── 2.json
│   │       └── 3.json
│   ├── src/
│   │   ├── androidTest/
│   │   │   └── java/
│   │   │       └── com/
│   │   │           └── example/
│   │   │               └── santaceciliatesis/
│   │   │                   └── data/
│   │   │                       ├── local/
│   │   │                       │   └── AppDatabaseTest.java
│   │   │                       └── remote/
│   │   │                           └── ServicioJsonMapperTest.java
│   │   ├── debug/
│   │   │   ├── res/
│   │   │   │   ├── raw/
│   │   │   │   │   └── desarrollo_ca.pem
│   │   │   │   └── xml/
│   │   │   │       └── network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── licenses/
│   │   │   │       ├── Inter-OFL.txt
│   │   │   │       └── Poppins-OFL.txt
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           └── santaceciliatesis/
│   │   │   │               ├── controller/
│   │   │   │               │   ├── AuthController.java
│   │   │   │               │   ├── CatalogoController.java
│   │   │   │               │   ├── ComparacionController.java
│   │   │   │               │   ├── ConsultaController.java
│   │   │   │               │   ├── CuentaController.java
│   │   │   │               │   ├── DetalleServicioController.java
│   │   │   │               │   └── FormularioController.java
│   │   │   │               ├── data/
│   │   │   │               │   ├── local/
│   │   │   │               │   │   ├── dao/
│   │   │   │               │   │   │   └── ServicioDao.java
│   │   │   │               │   │   ├── entity/
│   │   │   │               │   │   │   ├── CacheScopeEntity.java
│   │   │   │               │   │   │   └── ServicioCacheEntity.java
│   │   │   │               │   │   ├── mapper/
│   │   │   │               │   │   │   └── ServicioCacheMapper.java
│   │   │   │               │   │   ├── migration/
│   │   │   │               │   │   │   └── DatabaseMigrations.java
│   │   │   │               │   │   └── AppDatabase.java
│   │   │   │               │   ├── remote/
│   │   │   │               │   │   ├── dto/
│   │   │   │               │   │   │   └── ServicioResumenDto.java
│   │   │   │               │   │   ├── mapper/
│   │   │   │               │   │   │   ├── AuthJsonMapper.java
│   │   │   │               │   │   │   ├── ServicioDetalleMapper.java
│   │   │   │               │   │   │   └── ServicioJsonMapper.java
│   │   │   │               │   │   ├── ApiClient.java
│   │   │   │               │   │   ├── ApiConfig.java
│   │   │   │               │   │   ├── ApiEndpoints.java
│   │   │   │               │   │   ├── ApiFailure.java
│   │   │   │               │   │   ├── AuthenticatedApiClient.java
│   │   │   │               │   │   └── RequestHandle.java
│   │   │   │               │   ├── repository/
│   │   │   │               │   │   ├── AuthRepository.java
│   │   │   │               │   │   ├── Callback.java
│   │   │   │               │   │   ├── ComunidadRepository.java
│   │   │   │               │   │   └── ServicioRepository.java
│   │   │   │               │   └── AppContainer.java
│   │   │   │               ├── model/
│   │   │   │               │   ├── Cancion.java
│   │   │   │               │   ├── Pagina.java
│   │   │   │               │   ├── Rol.java
│   │   │   │               │   ├── ServicioDetalle.java
│   │   │   │               │   ├── ServicioMusical.java
│   │   │   │               │   ├── Sesion.java
│   │   │   │               │   ├── TipoServicio.java
│   │   │   │               │   └── Usuario.java
│   │   │   │               ├── service/
│   │   │   │               │   ├── ComparacionStore.java
│   │   │   │               │   ├── SessionManager.java
│   │   │   │               │   └── SessionStore.java
│   │   │   │               ├── util/
│   │   │   │               │   ├── MoneyUtils.java
│   │   │   │               │   ├── Presentacion.java
│   │   │   │               │   └── Resource.java
│   │   │   │               ├── view/
│   │   │   │               │   ├── activity/
│   │   │   │               │   │   ├── MainActivity.java
│   │   │   │               │   │   └── NavigationHost.java
│   │   │   │               │   ├── adapter/
│   │   │   │               │   │   ├── CancionAdapter.java
│   │   │   │               │   │   ├── GestionServicioAdapter.java
│   │   │   │               │   │   ├── ServicioAdapter.java
│   │   │   │               │   │   └── UsuarioAdapter.java
│   │   │   │               │   ├── dialog/
│   │   │   │               │   │   └── EstadoDialog.java
│   │   │   │               │   ├── fragment/
│   │   │   │               │   │   ├── administrador/
│   │   │   │               │   │   │   ├── InicioAdministradorFragment.java
│   │   │   │               │   │   │   ├── UsuarioDetalleFragment.java
│   │   │   │               │   │   │   └── UsuariosFragment.java
│   │   │   │               │   │   ├── cliente/
│   │   │   │               │   │   │   └── InicioClienteFragment.java
│   │   │   │               │   │   ├── common/
│   │   │   │               │   │   │   ├── AccesoFragment.java
│   │   │   │               │   │   │   ├── BaseFragment.java
│   │   │   │               │   │   │   ├── CatalogoFragment.java
│   │   │   │               │   │   │   ├── CompararFragment.java
│   │   │   │               │   │   │   ├── CuentaFragment.java
│   │   │   │               │   │   │   ├── DetalleServicioFragment.java
│   │   │   │               │   │   │   ├── FormularioFragment.java
│   │   │   │               │   │   │   ├── GestionServicioFragment.java
│   │   │   │               │   │   │   ├── GuiaFragment.java
│   │   │   │               │   │   │   ├── InicioBaseFragment.java
│   │   │   │               │   │   │   ├── PrivateScreen.java
│   │   │   │               │   │   │   ├── Refreshable.java
│   │   │   │               │   │   │   └── ServiciosGestionFragment.java
│   │   │   │               │   │   ├── director/
│   │   │   │               │   │   │   └── InicioDirectorFragment.java
│   │   │   │               │   │   └── solista/
│   │   │   │               │   │       └── InicioSolistaFragment.java
│   │   │   │               │   └── viewholder/
│   │   │   │               │       ├── CancionViewHolder.java
│   │   │   │               │       ├── GestionServicioViewHolder.java
│   │   │   │               │       ├── ServicioViewHolder.java
│   │   │   │               │       └── UsuarioViewHolder.java
│   │   │   │               └── SantaCeciliaApplication.java
│   │   │   ├── keepRules/
│   │   │   ├── res/
│   │   │   │   ├── color/
│   │   │   │   │   ├── input_access_outline.xml
│   │   │   │   │   ├── input_outline.xml
│   │   │   │   │   └── navigation_item_color.xml
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── bg_avatar.xml
│   │   │   │   │   ├── bg_badge.xml
│   │   │   │   │   ├── bg_card.xml
│   │   │   │   │   ├── bg_hero.xml
│   │   │   │   │   ├── bg_logo.xml
│   │   │   │   │   ├── bg_notice.xml
│   │   │   │   │   ├── bg_soft.xml
│   │   │   │   │   ├── bg_success.xml
│   │   │   │   │   ├── hero_music.xml
│   │   │   │   │   ├── ic_arrow.xml
│   │   │   │   │   ├── ic_back.xml
│   │   │   │   │   ├── ic_catalogo.xml
│   │   │   │   │   ├── ic_compare.xml
│   │   │   │   │   ├── ic_cuenta.xml
│   │   │   │   │   ├── ic_edit.xml
│   │   │   │   │   ├── ic_help.xml
│   │   │   │   │   ├── ic_inicio.xml
│   │   │   │   │   ├── ic_launcher_background.xml
│   │   │   │   │   ├── ic_launcher_foreground.xml
│   │   │   │   │   ├── ic_location.xml
│   │   │   │   │   ├── ic_logout.xml
│   │   │   │   │   ├── ic_menu.xml
│   │   │   │   │   ├── ic_music.xml
│   │   │   │   │   ├── ic_plus.xml
│   │   │   │   │   ├── ic_refresh.xml
│   │   │   │   │   ├── ic_search.xml
│   │   │   │   │   └── ic_users.xml
│   │   │   │   ├── drawable-nodpi/
│   │   │   │   ├── font/
│   │   │   │   │   ├── inter.xml
│   │   │   │   │   ├── inter_variable.ttf
│   │   │   │   │   ├── poppins.xml
│   │   │   │   │   ├── poppins_bold.ttf
│   │   │   │   │   ├── poppins_medium.ttf
│   │   │   │   │   ├── poppins_regular.ttf
│   │   │   │   │   └── poppins_semibold.ttf
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── dialog_estado.xml
│   │   │   │   │   ├── form_cancion.xml
│   │   │   │   │   ├── form_servicio.xml
│   │   │   │   │   ├── form_tarifa.xml
│   │   │   │   │   ├── form_usuario.xml
│   │   │   │   │   ├── fragment_acceso.xml
│   │   │   │   │   ├── fragment_catalogo.xml
│   │   │   │   │   ├── fragment_comparar.xml
│   │   │   │   │   ├── fragment_cuenta.xml
│   │   │   │   │   ├── fragment_detalle_servicio.xml
│   │   │   │   │   ├── fragment_formulario.xml
│   │   │   │   │   ├── fragment_gestion_servicio.xml
│   │   │   │   │   ├── fragment_guia.xml
│   │   │   │   │   ├── fragment_inicio.xml
│   │   │   │   │   ├── fragment_lista_gestion.xml
│   │   │   │   │   ├── header_navigation.xml
│   │   │   │   │   ├── item_cancion.xml
│   │   │   │   │   ├── item_comparacion.xml
│   │   │   │   │   ├── item_gestion_servicio.xml
│   │   │   │   │   ├── item_selector.xml
│   │   │   │   │   ├── item_servicio.xml
│   │   │   │   │   └── item_usuario.xml
│   │   │   │   ├── menu/
│   │   │   │   ├── mipmap-anydpi/
│   │   │   │   │   ├── ic_launcher.xml
│   │   │   │   │   └── ic_launcher_round.xml
│   │   │   │   ├── mipmap-hdpi/
│   │   │   │   ├── mipmap-mdpi/
│   │   │   │   ├── mipmap-xhdpi/
│   │   │   │   ├── mipmap-xxhdpi/
│   │   │   │   ├── mipmap-xxxhdpi/
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── ids.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── strings_etapa5.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── values-night/
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       ├── data_extraction_rules.xml
│   │   │   │       └── network_security_config.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── example/
│   │                   └── santaceciliatesis/
│   │                       ├── data/
│   │                       │   └── remote/
│   │                       │       └── ApiConfigTest.java
│   │                       └── util/
│   │                           └── MoneyUtilsTest.java
│   ├── .gitignore
│   └── build.gradle.kts
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── santacecilia/
│   │       │           └── api/
│   │       │               ├── audit/
│   │       │               │   └── AuditoriaService.java
│   │       │               ├── config/
│   │       │               │   ├── AdministradorInicial.java
│   │       │               │   ├── CorrelationFilter.java
│   │       │               │   ├── CorreoConfig.java
│   │       │               │   └── SecurityConfig.java
│   │       │               ├── controller/
│   │       │               │   ├── AdminServicioController.java
│   │       │               │   ├── AdminUsuarioController.java
│   │       │               │   ├── AuthController.java
│   │       │               │   ├── EstadoController.java
│   │       │               │   ├── ServicioController.java
│   │       │               │   └── UsuarioController.java
│   │       │               ├── dto/
│   │       │               │   ├── ApiResponse.java
│   │       │               │   ├── AuthDtos.java
│   │       │               │   ├── CambioConMotivo.java
│   │       │               │   ├── Pagina.java
│   │       │               │   ├── ServicioDtos.java
│   │       │               │   ├── UsuarioDtos.java
│   │       │               │   └── UsuarioResponse.java
│   │       │               ├── entity/
│   │       │               │   ├── FamiliaSesion.java
│   │       │               │   ├── RecuperacionAcceso.java
│   │       │               │   ├── Rol.java
│   │       │               │   ├── ServicioMusical.java
│   │       │               │   ├── Sesion.java
│   │       │               │   ├── TokenAcceso.java
│   │       │               │   └── Usuario.java
│   │       │               ├── exception/
│   │       │               │   ├── ApiError.java
│   │       │               │   ├── ApiException.java
│   │       │               │   ├── GlobalExceptionHandler.java
│   │       │               │   └── TokenRejectedException.java
│   │       │               ├── repository/
│   │       │               │   ├── ContenidoServicioRepository.java
│   │       │               │   ├── FamiliaSesionRepository.java
│   │       │               │   ├── RecuperacionRepository.java
│   │       │               │   ├── ServicioRepository.java
│   │       │               │   ├── SesionRepository.java
│   │       │               │   ├── TokenAccesoRepository.java
│   │       │               │   └── UsuarioRepository.java
│   │       │               ├── security/
│   │       │               │   ├── AuthPrincipal.java
│   │       │               │   ├── BearerAuthenticationFilter.java
│   │       │               │   ├── RequestThrottle.java
│   │       │               │   ├── StrongPassword.java
│   │       │               │   ├── StrongPasswordValidator.java
│   │       │               │   └── TokenCodec.java
│   │       │               ├── service/
│   │       │               │   ├── AuthService.java
│   │       │               │   ├── RecoveryMailSender.java
│   │       │               │   ├── RecoveryService.java
│   │       │               │   ├── ServicioService.java
│   │       │               │   └── UsuarioService.java
│   │       │               └── SantaCeciliaApiApplication.java
│   │       └── resources/
│   │           └── static/
│   │               ├── restablecer.css
│   │               ├── restablecer.html
│   │               └── restablecer.js
│   ├── .env.example
│   ├── API.md
│   ├── build.gradle.kts
│   ├── CONFIGURACION_MYSQL.md
│   ├── README.md
│   └── settings.gradle.kts
├── database/
│   ├── bootstrap/
│   │   └── crear_esquema.sql
│   ├── diagramas/
│   ├── migrations/
│   │   ├── README.md
│   │   ├── V001__modelo_central.sql
│   │   ├── V002__roles_del_sistema.sql
│   │   ├── V003__inmutabilidad_y_validaciones.sql
│   │   ├── V004__consulta_saldos.sql
│   │   └── V005__autenticacion_y_consentimiento.sql
│   ├── queries/
│   │   ├── bloquear_recurso.sql
│   │   ├── catalogo_publico.sql
│   │   └── ocupaciones_en_conflicto.sql
│   ├── tests/
│   │   ├── evidence/
│   │   │   ├── manifesto_archivos.json
│   │   │   └── resultado_mysql.json
│   │   └── test_schema.py
│   ├── autenticacion_etapa3.md
│   ├── diccionario_datos.md
│   ├── instalar_desarrollo.sql
│   ├── modelo_logico.md
│   ├── protocolo_agenda.md
│   └── README.md
├── docs/
│   ├── diagramas/
│   │   └── secuencias_reales.md
│   ├── interfaz/
│   │   └── capturas/
│   │       ├── administrador.png
│   │       ├── cliente.png
│   │       ├── director.png
│   │       └── solista.png
│   ├── pruebas/
│   │   ├── local/
│   │   │   └── requerimientos_reextraidos.json
│   │   ├── resultado_etapa1.json
│   │   ├── verificacion_etapa1.md
│   │   └── verificacion_etapa2.md
│   ├── referencias/
│   │   ├── inventario.json
│   │   ├── recursos_interfaz.json
│   │   └── requerimientos_extraidos.json
│   ├── arbol_proyecto.md
│   ├── arquitectura.md
│   ├── cambios_requerimientos_v2.md
│   ├── dependencias.md
│   ├── diagnostico_inicial.md
│   ├── endpoints_api.md
│   ├── estructura_mvc.md
│   ├── integracion_android.md
│   ├── interfaz.md
│   ├── matriz_trazabilidad.md
│   ├── orden_implementacion.md
│   └── pagos_arquitectura.md
├── ENTREGABLES_UNIDAD_III/
│   ├── 04_Base_Datos/
│   │   └── README.md
│   ├── 06_Interfaces_Funcionales/
│   │   └── README.md
│   ├── 11_Evidencias_Iteraciones/
│   │   ├── Iteracion_01_Arquitectura.md
│   │   └── Iteracion_02_MySQL.md
│   ├── 03_Control_Versiones.md
│   ├── 05_Logica_Negocio_Servicios.md
│   ├── 07_Modulos_Integrados.md
│   ├── 08_Roles_Permisos_Seguridad.md
│   └── README_ENTREGA.md
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   ├── gradle-daemon-jvm.properties
│   └── libs.versions.toml
├── tools/
│   ├── export_database_docs.py
│   ├── extract_requirements.py
│   ├── Iniciar-Backend.ps1
│   ├── Preparar-HttpsLocal.ps1
│   └── Test-Database.ps1
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts
```

Etapa 6 agrega controller/FlujoController, data/repository/ContratacionRepository y las vistas comunes FlujoBaseFragment, BandejaFragment, SolicitudDetalleFragment, ContratoDetalleFragment, ContratacionFormFragment, AgendaFragment y HorariosFragment. Comparten fragment_flujo.xml y estilos nativos. Backend incorpora ContratacionDtos, ContratacionController, ContratacionService y ContratacionRepository; la evolución SQL es V006__flujo_contratacion.sql. Guía actual en docs/contratacion_agenda.md.

Etapa 7 agrega las vistas comunes PagosFragment, PagoDetalleFragment, PagoComprobanteFragment, CuentasCobroFragment y ComprobanteDialog; strings_etapa7.xml; PagoDirectoController/Dtos/Service, ComprobanteImagenService, PagoAvisosService y PagosPrivadosFilter en backend; y V007__pagos_directos_conciliados.sql. Guía actual: docs/pagos_arquitectura.md.
