# Módulos integrados — registro inicial

Actualización Etapa 4: Android conecta autenticación/cuenta y catálogo/detalle con el backend real por HTTPS nativo. Volley fue retirado; sesión cifrada con Keystore y Room v2. Se verificaron dos emuladores, renovación, caché al reiniciar sin conexión y logout independiente/local. [Guía y alcance](../docs/integracion_android.md). El resto del registro conserva el corte inicial; el flujo completo de contratación/pagos sigue pendiente.

| Flujo | Precondición / datos | Resultado esperado | Resultado obtenido / estado |
|---|---|---|---|
| Base de compilación | SDK y dependencias locales/descargadas | APK compilable, pruebas JVM y lint | Ver ../docs/pruebas/verificacion_etapa1.md |
| Consulta real de catálogo | API HTTPS con snapshot completo, servicios publicados | Catálogo y caché sincronizados | Verificado localmente en Etapa 4: HTTPS, catálogo y Room; sin despliegue productivo |
| Cliente → Solicitud → Director/Solista → Cotización → Aceptación → Contrato → Pago → Reserva | Cuentas reales de prueba, backend/MySQL, catálogo y sandbox oficial | Contrato y reserva únicos tras pago verificado | No ejecutado: módulos no implementados |
| Consulta entre dispositivos | Dos sesiones/dispositivos y API común | Mismo estado central autorizado | Verificado para catálogo público y cuenta propia en dos emuladores (Etapa 4) |

No hay datos de clientes reales ni cobros de pruebas registrados. No se presenta la compilación como prueba de extremo a extremo del negocio. No se generaron capturas de flujos inexistentes.


## Verificación local añadida en Etapa 2

El esquema MySQL central ya existe como migraciones ejecutadas y verificadas: 31 pruebas SQL aprobadas, incluido rollback interno y agenda concurrente. Esto acredita integridad/protocolo de la capa de datos; no el flujo de extremo a extremo ni comunicación Android–backend–MySQL. Las fixtures de pagos no corresponden a una pasarela o sandbox oficial. Evidencia: ../docs/pruebas/verificacion_etapa2.md.

## Interfaz añadida en Etapa 5

Dashboards por rol, drawer/cinco accesos, usuarios/servicios administrativos, tarifas/repertorio propios, perfil editable y catálogo con filtros/comparación. Se verificaron operaciones Android contra MySQL/HTTPS y revisión de tamaños/fuente ampliada. [Guía y límites](../docs/interfaz.md). Contrataciones, agenda y pagos siguen en sus etapas; no se generó otro paquete académico de pruebas.
