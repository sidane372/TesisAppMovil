# Verificación de la base — Etapa 1

Corte: 03/11/2026, America/Lima. Resultado observado final: **BUILD SUCCESSFUL**.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain
```

| Comprobación | Resultado observado | Evidencia local |
|---|---|---|
| APK de aplicación | Generado | app/build/outputs/apk/debug/app-debug.apk |
| Pruebas JVM | 11 ejecutadas, 0 fallos, 0 errores | app/build/test-results/testDebugUnitTest/ y app/build/reports/tests/testDebugUnitTest/index.html |
| ApiConfigTest | 8 pruebas: URL vacía, normalización HTTPS, rechazo de HTTP, credenciales, query, fragmento, endpoint ajeno y petición sin API | TEST-com.example.santaceciliatesis.data.remote.ApiConfigTest.xml |
| MoneyUtilsTest | 3 pruebas: precisión de céntimos, dos decimales e importe de un céntimo | TEST-com.example.santaceciliatesis.util.MoneyUtilsTest.xml |
| Android lint | 0 errores, 1 advertencia | app/build/reports/lint-results-debug.html y .txt |
| APK de pruebas instrumentadas | Compilado; 6 casos preparados | app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk |
| Room | Esquema v1 generado mediante annotationProcessor | app/schemas/com.example.santaceciliatesis.data.local.AppDatabase/1.json |
| Dispositivo/emulador | adb devices devolvió lista vacía | No hay dispositivo conectado; no se ejecutó connectedDebugAndroidTest |
| Pruebas Android de Room | 2 compiladas, no ejecutadas | AppDatabaseTest: reemplazo del snapshot y rollback con IDs duplicados |
| Pruebas Android de JSON | 4 compiladas, no ejecutadas | ServicioJsonMapperTest: tarifa null, snapshot parcial, céntimo fraccionario, ID duplicado |
| Ejecución visual/arranque | No verificados en dispositivo | No se aportan capturas de funcionamiento |
| Backend/MySQL/pagos | No implementados ni ejecutados | Arquitectura propuesta, sin API desplegada o proveedor financiero |

La advertencia restante informa de AGP 9.5.0 disponible frente a 9.4.1 del proyecto. Se conserva 9.4.1 para no introducir una actualización de herramientas sin necesidad. No se suprimieron errores ni se creó baseline para ocultarlos.

Durante la verificación aparecieron cuatro advertencias iniciales de lint: notifyDataSetChanged, fondo redundante y dos colores sin uso. Se sustituyó el Adapter por ListAdapter/DiffUtil y se retiraron fondo/colores redundantes. Una ejecución posterior falló por DiffUtilEquals al usar == entre enums dentro de areContentsTheSame; se cambió a equals y la verificación final pasó. Esta incidencia es de análisis estático; la comparación original de enums era válida en Java. No se declara como defecto financiero ni funcional del negocio.

El primer intento aislado de Gradle no pudo abrir el lock de la caché global por permisos del sandbox. La ejecución verificada utilizó el JBR/caché de Gradle habituales, con acceso permitido. No fue un error de código Android.

Log final completo de este equipo: docs/pruebas/local/gradle-verificacion.log (ignorado por Git por ser evidencia local generada). Resumen estructurado y hashes de reportes/APKs/esquema: resultado_etapa1.json. Los hashes identifican estos binarios, no sustituyen una prueba en dispositivo.

## Validaciones pendientes

Ejecutar las 6 pruebas instrumentadas en API 30+; verificar arranque, rotación, reintentos y vuelta a la pantalla; revisar 360/393/412dp y fuente 150 %. Probar API HTTPS real, fallos de red, caché tras reinicio y catálogo retirado. Validar migraciones al evolucionar Room. Probar autenticación/propiedad, flujo de contratación, concurrencia de reservas y sandbox oficial cuando esos módulos existan. No se validaron metas de rendimiento o satisfacción de la tesis.
