# Interfaces funcionales

Todas las vistas utilizan MainActivity. Las imágenes son capturas reales de Android, seleccionadas de verificaciones previas. Las pantallas de negocio muestran datos sintéticos de instancias aisladas; no son cuentas o ingresos oficiales. El login corresponde a la corrección actual de acceso.

| Captura | Fragment | Layout | Alcance |
|---|---|---|---|
| [Acceso](capturas/01_login.png) | AccesoFragment | fragment_acceso.xml | Estado actual con fuente 150 por ciento; sin credenciales. |
| [Dashboard administrador](capturas/02_administrador.png) | InicioAdministradorFragment | fragment_inicio.xml | Captura histórica de revisión con datos sintéticos. |
| [Dashboard cliente](capturas/03_cliente.png) | InicioClienteFragment | fragment_inicio.xml | Captura histórica con datos sintéticos. |
| [Dashboard director](capturas/04_director.png) | InicioDirectorFragment | fragment_inicio.xml | Captura histórica con datos sintéticos. |
| [Dashboard solista](capturas/05_solista.png) | InicioSolistaFragment | fragment_inicio.xml | Captura histórica con datos sintéticos. |
| [Catálogo y filtros](capturas/06_catalogo.png) | CatalogoFragment | fragment_catalogo.xml | Servicios de verificación sintéticos. |
| [Solicitud enviada](capturas/07_solicitud.png) | SolicitudDetalleFragment | fragment_flujo.xml | Evento futuro sintético; la solicitud no reserva. |
| [Cotización enviada](capturas/08_cotizacion.png) | SolicitudDetalleFragment | fragment_flujo.xml | Versiones e importes sintéticos. |
| [Contrato pendiente de pago](capturas/09_contrato.png) | ContratoDetalleFragment | fragment_flujo.xml | Captura histórica de Etapa 6; su texto de pagos pendientes corresponde a ese corte. |
| [Pagos y comprobantes](capturas/10_pagos.png) | PagosFragment | fragment_flujo.xml | Importes e instrucciones sintéticos; no acredita transferencia. |
| [Calendario público](capturas/11_calendario.png) | AgendaFragment | fragment_flujo.xml | Franjas y eventos de verificación. |
| [Conciliación administrativa](capturas/12_revision_pago.png) | PagoDetalleFragment | fragment_flujo.xml | Resultado sintético registrado para comprobar las reglas. |
| [Reserva confirmada](capturas/13_reserva.png) | ContratoDetalleFragment | fragment_flujo.xml | Confirmación por ingreso sintético, no por movimiento bancario real. |

Las capturas históricas conservan el estado de la prueba en que se obtuvieron. No demuestran que la base habitual tenga esos servicios cargados. El uso cotidiano empieza con el arranque y la configuración administrativa descritos en la guía de demostración.
