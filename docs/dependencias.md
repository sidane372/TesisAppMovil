# Dependencias y compatibilidad

| Dependencia | Versión | Uso y decisión |
|---|---|---|
| AGP / wrapper Gradle | 9.4.1 / 9.6.0 | Se conservan las versiones del proyecto encontrado |
| SDK / minSdk / targetSdk | 37 / 30 / 37 | Se conserva configuración existente |
| JDK daemon / fuente Java | 25 / 11 | JBR de Android Studio; se conserva toolchain |
| AndroidX core / activity | 1.19.1 / 1.13.0 | Insets y Activity; dependencias base sin extensiones KTX directas |
| AppCompat | 1.8.0 | Activities y compatibilidad de vistas |
| Material Components | 1.14.0 | Tarjetas, botones y estados de carga XML |
| ConstraintLayout | 2.2.2 | Contenedor principal XML |
| RecyclerView | 1.4.0 | Listados, Adapter y ViewHolder |
| Room runtime / compiler | 2.8.5 / 2.8.5 | Caché y DAO; annotationProcessor Java, sin KSP/Room 3 |
| HTTPS nativo Java | Plataforma Android | HttpsURLConnection, ExecutorService y cancelación; Volley retirado en Etapa 4 |
| Inter / Poppins | Archivos locales, SIL OFL 1.1 | Cuerpo y controles / títulos; licencias incluidas en assets/licenses, sin descarga al abrir la app |
| JUnit | 4.13.2 | Configuración HTTPS y exactitud monetaria |
| AndroidX test.ext JUnit / Espresso | 1.3.0 / 3.7.0 | Runner y pruebas instrumentadas de Room/parser |
| Room testing | 2.8.5 | Preparación de pruebas de almacenamiento/migraciones |

Las versiones son explícitas en gradle/libs.versions.toml; el resultado real de resolución se registra en pruebas/verificacion_etapa1.md. No se incorpora Kotlin de aplicación, Compose, Retrofit, Firebase ni conexión JDBC Android–MySQL. El backend tiene dependencias independientes documentadas en ../backend/README.md.

Fuentes técnicas consultadas: [Room y configuración Java](https://developer.android.com/jetpack/androidx/releases/room), [confianza TLS por variante](https://developer.android.com/privacy-and-security/security-config) y [permiso de red local Android 17](https://developer.android.com/privacy-and-security/local-network-permission). La interfaz y los recursos de Etapa 5 se detallan en interfaz.md. Las pruebas de compilación locales son la evidencia de compatibilidad con esta plantilla.
