# Diagnóstico de partida y reutilización

Inspección: 03/11/2026, carpeta SantaCeciliaTesis de Android Studio.

Estado encontrado: app Android mínima, MainActivity.kt, activity_main.xml con Hello World, dos pruebas Kotlin de plantilla, applicationId com.example.santaceciliatesis; sin Room, Volley, reglas de negocio ni backend. git status y git log informaron que no es un repositorio Git. No hay commits, ramas o remoto verificables.

Se conservan raíz Android, wrapper Gradle 9.6.0, AGP 9.4.1, SDK 37, minSdk 30, targetSdk 37, JDK de daemon 25 y compatibilidad fuente Java 11. No se traslada el proyecto a android-app/: rompería la ruta ya abierta sin aportar una ventaja a esta etapa. app/, gradle/ y archivos Gradle continúan en la raíz; backend/, database/ y docs/ son hermanos.

Se sustituye únicamente el código de plantilla Kotlin por Java. Los scripts Gradle .kts son configuración, no código Kotlin de la aplicación. Las dependencias pueden incorporar componentes Kotlin transitivos sin que se agreguen fuentes .kt.

## LaboratorioROOM

En la inspección de Etapa 1 no se encontró el ZIP dentro de TESIS_II y por eso no se atribuyó código reutilizado a ese archivo. En Etapa 2 se localizó C:\Users\sidan\Downloads\LaboratorioROOM (1).zip y se inspeccionaron sus clases principales y archivos Gradle, sin extraerlo sobre el proyecto ni modificar el laboratorio.

| Concepto descrito en el prompt | Aplicación en la base | Estado |
|---|---|---|
| Entidad Room y PK | ServicioCacheEntity | Implementado desde cero, no copiado del ZIP |
| DAO | ServicioDao, snapshot transaccional | Implementado desde cero |
| Singleton Room | AppDatabase | Implementado desde cero |
| ExecutorService | AppContainer/ServicioRepository, trabajo local fuera del hilo principal | Implementado desde cero |
| RecyclerView/Adapter | ServicioAdapter | Implementado desde cero |
| ViewHolder | ServicioViewHolder independiente | Implementado desde cero |
| Producto y formulario de laboratorio | No representan servicios musicales ni contratación | No trasladados |

El diagnóstico específico del ZIP se completó en Etapa 2 mediante lectura de entradas del archivo. Se registraron su SHA-256 y hashes de fuentes en referencias/inventario.json. La implementación de Santa Cecilia ya se había creado de forma independiente; no se copió posteriormente Producto ni se sustituyeron archivos con el laboratorio.

### Hallazgos reales del laboratorio

- Java/XML, namespace com.udh.laboratorioroom, SDK 37/minSdk 30, fuente Java 11, AGP 9.3.3, Room 2.8.5 con annotationProcessor y RecyclerView 1.4.0.
- Producto usa @Entity y @PrimaryKey; ProductoDao tiene @Insert/@Update/@Delete y consultas SQLite. Se reutilizan esos conceptos mediante ServicioCacheEntity y ServicioDao, no su dominio de productos/stock.
- AppDatabase usa singleton con Context de aplicación. El laboratorio contiene exportSchema=false y fallbackToDestructiveMigration(); Santa Cecilia exporta esquema y no borra caché automáticamente al migrar.
- MainActivity realiza consultas DAO y coordina ExecutorService/runOnUiThread. Santa Cecilia conserva el trabajo fuera del hilo principal pero lo separa en repositorio/controlador; su Activity solo contiene la vista/navegación.
- ProductoAdapter declara ViewHolder como clase interna y usa notifyDataSetChanged. Santa Cecilia mantiene ServicioViewHolder en paquete propio y ListAdapter/DiffUtil.
- El laboratorio usa double para precio; Santa Cecilia usa céntimos enteros y formato decimal exacto. La concatenación || del DAO corresponde a SQLite y no se traslada literalmente a MySQL.
- Revisión estática, sin ejecutar el laboratorio: tvEmptyState se usa sin asignación visible en onCreate y el formato de precio no contiene marcador de valor. Son observaciones del archivo de referencia, no incidencias verificadas en ejecución de Santa Cecilia.

No se compiló ni ejecutó el ZIP; los resultados de compilación Android documentados corresponden a SantaCeciliaTesis.

Se leyeron el diagrama de clases, las secuencias, las secciones funcionales/visuales del Artifact y las 90 filas de requisitos del DOCX. Inventario y hashes: referencias/inventario.json. Extracción de requisitos: referencias/requerimientos_extraidos.json.
