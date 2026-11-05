# Entregable 3 Repositorio con control de versiones

**Asignatura:** Seminario de Tesis II.

**Proyecto:** Santa Cecilia, aplicación Android y backend de contratación musical.

**Autores del trabajo académico:** Galvez Andre Jherson y Briceño Contreras Sidanne Luis.

**Fecha de revisión:** 2026-11-04T22:02:35-05:00 (America/Lima, UTC−05:00).

## 1 Objetivo y alcance

Documentar el control de versiones mediante el historial real del repositorio, identificar sus ramas, describir todos los commits alcanzables desde las referencias locales observadas y relacionar sus cambios con evidencias de integración. La revisión utiliza consultas de Git y archivos existentes; no reconstruye artificialmente la cronología del desarrollo.

La información siguiente sustituye el estado anterior de este entregable, que describía únicamente el primer commit. Se conservaron las salidas completas de los comandos en [03_Evidencias_Git](03_Evidencias_Git/README.md).

## 2 Identificación del repositorio

| Elemento | Resultado comprobado |
|---|---|
| URL de consulta | https://github.com/sidane372/TesisAppMovil |
| URL Git de origin | https://github.com/sidane372/TesisAppMovil.git |
| Remoto configurado | origin, con las direcciones fetch y push indicadas por git remote -v |
| Rama principal y rama activa | main |
| HEAD al comenzar esta revisión | `526df8eff10021079d39c5f459e9fd4bed95dd8c` |
| Número de commits alcanzables | 9 |
| Commits con varios padres | 0 |
| Versión de Git | git version 2.56.0.windows.3 |

La dirección configurada como push identifica un destino; no demuestra por sí sola que se haya publicado un cambio. La publicación del HEAD observado se contrastó mediante una consulta de lectura al servidor remoto.

## 3 Ramas existentes y relación con GitHub

| Referencia | Tipo | Identificador observado | Interpretación |
|---|---|---|---|
| main | Rama local | 526df8eff10021079d39c5f459e9fd4bed95dd8c | Rama de trabajo y principal |
| origin/main | Referencia local de seguimiento remoto | 526df8eff10021079d39c5f459e9fd4bed95dd8c | Copia local de la posición conocida del remoto; no es otra rama de desarrollo local |
| refs/heads/main | Rama anunciada por GitHub | 526df8eff10021079d39c5f459e9fd4bed95dd8c | Resultado de git ls-remote, sin hacer fetch ni cambiar referencias locales |

La consulta remota identifica `main` como rama predeterminada. El HEAD local y `refs/heads/main` de GitHub coinciden en el hash completo indicado.

No se encontraron ramas locales adicionales de desarrollo ni otras ramas anunciadas por GitHub en esta consulta.
El historial observado contiene 0 commits de merge. La secuencia de este corte es lineal; no se atribuyen pull requests, revisiones o fusiones que no estén acreditados.

Evidencias: [git branch](03_Evidencias_Git/03_git_branch.txt), [referencias y upstream](03_Evidencias_Git/10_git_referencias.txt) y [consulta al servidor](03_Evidencias_Git/09_git_ls_remote.txt).

## 4 Método de obtención de evidencias

Se usaron `git log --all` para recorrer el historial, `git branch --all --verbose --verbose` para las ramas, `git show` para los metadatos y archivos de cada commit y `git status` para diferenciar el contenido confirmado de los cambios del directorio de trabajo. Las fechas de autoría y confirmación se extrajeron con formato ISO 8601 y su desplazamiento horario.

Las capturas son transcripciones textuales de la salida real, no imágenes simuladas de una terminal. Cada archivo registra el comando exacto, instante de consulta, código de salida, salida estándar y salida de error. Se desactivaron color y paginación para facilitar su lectura y `GIT_OPTIONAL_LOCKS=0` para evitar actualizaciones opcionales del índice durante las consultas.

No se ejecutaron `git add`, `git commit`, `git push`, `git merge` ni `git fetch`. Tampoco se crearon ramas o etiquetas, se modificaron referencias, se eliminaron archivos previos o se alteró código de la aplicación. La integridad del índice, HEAD y archivos versionados ajenos a este informe se comprobó al finalizar.

## 5 Registro completo de commits reales

Se presentan todos los commits alcanzables en orden cronológico del historial, del más antiguo al más reciente. Las fechas son las registradas por Git y no fechas estimadas de ejecución de cada etapa.

| Nº | Identificador completo | Fecha de autoría | Fecha de confirmación | Mensaje exacto | Archivos afectados |
|---|---|---|---|---|---|
| 1 | `3bcb57c7e7af2307ad91ed7c5f3e51f1c3a4908c` | 2026-11-03T23:53:32-05:00 | 2026-11-03T23:53:32-05:00 | feat: incorpora Santa Cecilia Android y backend | 328 |
| 2 | `3276c815deb28c73ebbe9511a997bb7f172a64b7` | 2026-11-04T21:45:19-05:00 | 2026-11-04T21:45:19-05:00 | docs: agrega plan de desarrollo | 1 |
| 3 | `6e0e528b88fc0233d9111b2f8ec641d6cfe47c88` | 2026-11-04T21:45:25-05:00 | 2026-11-04T21:45:25-05:00 | docs: agrega backlog priorizado | 1 |
| 4 | `cc5ad2193878190a980f57f1dceccc933792ab4c` | 2026-11-04T21:45:49-05:00 | 2026-11-04T21:45:49-05:00 | docs: incorpora modelo y scripts MySQL | 21 |
| 5 | `0ae47f7f341192731b29e6958638b675c3d0b2aa` | 2026-11-04T21:45:55-05:00 | 2026-11-04T21:45:55-05:00 | docs: documenta servicios y permisos | 2 |
| 6 | `32d19efd7db950183772fea8bbfcfd5684150dec` | 2026-11-04T21:46:01-05:00 | 2026-11-04T21:46:01-05:00 | docs: agrega evidencias de interfaces y modulos | 15 |
| 7 | `f1cd040ce62697c86ad31266b8ab1cd944b54e4b` | 2026-11-04T21:46:06-05:00 | 2026-11-04T21:46:06-05:00 | docs: incorpora incidencias y documentacion tecnica | 2 |
| 8 | `e5d35b97a99f460f211927b4a835672bed9a5acd` | 2026-11-04T21:46:10-05:00 | 2026-11-04T21:46:10-05:00 | docs: incorpora evidencias y resultados de pruebas | 16 |
| 9 | `526df8eff10021079d39c5f459e9fd4bed95dd8c` | 2026-11-04T21:46:15-05:00 | 2026-11-04T21:46:15-05:00 | docs: actualiza control de versiones y entregables | 2 |

La autoría y confirmación registradas por Git corresponden a Sidane Contreras. Es metadato de la cuenta que confirmó los cambios y no demuestra la distribución individual del trabajo entre los dos autores académicos.

### 5.1 Cambios y evidencia de cada commit

#### 01 3bcb57c feat: incorpora Santa Cecilia Android y backend

**Identificador:** `3bcb57c7e7af2307ad91ed7c5f3e51f1c3a4908c`.

**Padre:** Sin padre; commit raíz.

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-03T23:53:32-05:00. **Fecha de confirmación:** 2026-11-03T23:53:32-05:00.

Corte inicial de la solución integrada: Android Java/XML, controladores y repositorios, API Spring Boot, seguridad, contratación, agenda, pagos directos, Flyway V001–V007, scripts de arranque y documentación base. Incluye la corrección de la transición de registro y su prueba instrumentada. Es una incorporación acumulada; no un commit por cada etapa previa.

**Estadística literal de Git:** `328 files changed, 30922 insertions(+)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Estados registrados: A: 328. `A` indica incorporación y `M` modificación. La lista completa se conserva en el inventario, incluidos los archivos binarios.

Evidencia: [git show de 3bcb57c](03_Evidencias_Git/commits/01_3bcb57c_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/01_3bcb57c_archivos.txt).

#### 02 3276c81 docs: agrega plan de desarrollo

**Identificador:** `3276c815deb28c73ebbe9511a997bb7f172a64b7`.

**Padre:** `3bcb57c7e7af2307ad91ed7c5f3e51f1c3a4908c`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:45:19-05:00. **Fecha de confirmación:** 2026-11-04T21:45:19-05:00.

Añade el plan de desarrollo en Word. Este commit registra la incorporación del archivo académico; no un cambio funcional en Android o backend.

**Estadística literal de Git:** `1 file changed, 0 insertions(+), 0 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Archivos afectados:

- `A` `ENTREGABLES_UNIDAD_III/01_Plan_Desarrollo.docx`.

Evidencia: [git show de 3276c81](03_Evidencias_Git/commits/02_3276c81_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/02_3276c81_archivos.txt).

#### 03 6e0e528 docs: agrega backlog priorizado

**Identificador:** `6e0e528b88fc0233d9111b2f8ec641d6cfe47c88`.

**Padre:** `3276c815deb28c73ebbe9511a997bb7f172a64b7`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:45:25-05:00. **Fecha de confirmación:** 2026-11-04T21:45:25-05:00.

Añade el backlog priorizado en Excel, con trazabilidad a los requisitos. No introduce código de aplicación.

**Estadística literal de Git:** `1 file changed, 0 insertions(+), 0 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Archivos afectados:

- `A` `ENTREGABLES_UNIDAD_III/02_Backlog_Priorizado.xlsx`.

Evidencia: [git show de 6e0e528](03_Evidencias_Git/commits/03_6e0e528_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/03_6e0e528_archivos.txt).

#### 04 cc5ad21 docs: incorpora modelo y scripts MySQL

**Identificador:** `cc5ad2193878190a980f57f1dceccc933792ab4c`.

**Padre:** `6e0e528b88fc0233d9111b2f8ec641d6cfe47c88`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:45:49-05:00. **Fecha de confirmación:** 2026-11-04T21:45:49-05:00.

Incorpora el diccionario, modelos lógico y relacional, metadatos, guía de importación, archivo SQL único y copias de las siete migraciones y consultas del entregable 04. Los scripts operativos del esquema ya existían en el corte inicial.

**Estadística literal de Git:** `21 files changed, 11212 insertions(+), 15 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Estados registrados: A: 20, M: 1. `A` indica incorporación y `M` modificación. La lista completa se conserva en el inventario, incluidos los archivos binarios.

Evidencia: [git show de cc5ad21](03_Evidencias_Git/commits/04_cc5ad21_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/04_cc5ad21_archivos.txt).

#### 05 0ae47f7 docs: documenta servicios y permisos

**Identificador:** `0ae47f7f341192731b29e6958638b675c3d0b2aa`.

**Padre:** `cc5ad2193878190a980f57f1dceccc933792ab4c`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:45:55-05:00. **Fecha de confirmación:** 2026-11-04T21:45:55-05:00.

Actualiza los informes de lógica de negocio y roles, permisos y seguridad. Documenta la implementación existente.

**Estadística literal de Git:** `2 files changed, 32 insertions(+), 37 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Archivos afectados:

- `M` `ENTREGABLES_UNIDAD_III/05_Logica_Negocio_Servicios.md`.
- `M` `ENTREGABLES_UNIDAD_III/08_Roles_Permisos_Seguridad.md`.

Evidencia: [git show de 0ae47f7](03_Evidencias_Git/commits/05_0ae47f7_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/05_0ae47f7_archivos.txt).

#### 06 32d19ef docs: agrega evidencias de interfaces y modulos

**Identificador:** `32d19efd7db950183772fea8bbfcfd5684150dec`.

**Padre:** `0ae47f7f341192731b29e6958638b675c3d0b2aa`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:46:01-05:00. **Fecha de confirmación:** 2026-11-04T21:46:01-05:00.

Actualiza los índices de interfaces y módulos integrados e incorpora 13 capturas reales de verificaciones locales. Las capturas de pagos identifican datos sintéticos.

**Estadística literal de Git:** `15 files changed, 33 insertions(+), 22 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Estados registrados: A: 13, M: 2. `A` indica incorporación y `M` modificación. La lista completa se conserva en el inventario, incluidos los archivos binarios.

Evidencia: [git show de 32d19ef](03_Evidencias_Git/commits/06_32d19ef_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/06_32d19ef_archivos.txt).

#### 07 f1cd040 docs: incorpora incidencias y documentacion tecnica

**Identificador:** `f1cd040ce62697c86ad31266b8ab1cd944b54e4b`.

**Padre:** `32d19efd7db950183772fea8bbfcfd5684150dec`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:46:06-05:00. **Fecha de confirmación:** 2026-11-04T21:46:06-05:00.

Añade el registro de incidencias en Excel y la documentación técnica preliminar en Word.

**Estadística literal de Git:** `2 files changed, 0 insertions(+), 0 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Archivos afectados:

- `A` `ENTREGABLES_UNIDAD_III/09_Registro_Incidencias.xlsx`.
- `A` `ENTREGABLES_UNIDAD_III/10_Documentacion_Tecnica.docx`.

Evidencia: [git show de f1cd040](03_Evidencias_Git/commits/07_f1cd040_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/07_f1cd040_archivos.txt).

#### 08 e5d35b9 docs: incorpora evidencias y resultados de pruebas

**Identificador:** `e5d35b97a99f460f211927b4a835672bed9a5acd`.

**Padre:** `f1cd040ce62697c86ad31266b8ab1cd944b54e4b`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:46:10-05:00. **Fecha de confirmación:** 2026-11-04T21:46:10-05:00.

Incorpora informes de iteraciones 03–08, resultados JSON, resumen de lint, regresión de registro, verificación de persistencia, guía de demostración y Estado_Actual_Proyecto.md. Registrar un informe no equivale a volver a ejecutar las pruebas en la fecha del commit.

**Estadística literal de Git:** `16 files changed, 7312 insertions(+)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Estados registrados: A: 16. `A` indica incorporación y `M` modificación. La lista completa se conserva en el inventario, incluidos los archivos binarios.

Evidencia: [git show de e5d35b9](03_Evidencias_Git/commits/08_e5d35b9_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/08_e5d35b9_archivos.txt).

#### 09 526df8e docs: actualiza control de versiones y entregables

**Identificador:** `526df8eff10021079d39c5f459e9fd4bed95dd8c`.

**Padre:** `e5d35b97a99f460f211927b4a835672bed9a5acd`

**Autor:** Sidane Contreras. **Confirmador:** Sidane Contreras.

**Fecha de autoría:** 2026-11-04T21:46:15-05:00. **Fecha de confirmación:** 2026-11-04T21:46:15-05:00.

Modifica el informe de control de versiones y el índice general de entregables. El contenido anterior del informe conserva referencias al primer corte; esta revisión lo sustituye por el historial completo observado.

**Estadística literal de Git:** `2 files changed, 41 insertions(+), 30 deletions(-)`. Los archivos binarios cuentan como archivos afectados; su tamaño no se interpreta como líneas de código.

Archivos afectados:

- `M` `ENTREGABLES_UNIDAD_III/03_Control_Versiones.md`.
- `M` `ENTREGABLES_UNIDAD_III/README_ENTREGA.md`.

Evidencia: [git show de 526df8e](03_Evidencias_Git/commits/09_526df8e_git_show.txt) y [todos sus archivos y estados](03_Evidencias_Git/commits/09_526df8e_archivos.txt).

### 5.2 Interpretación académica de la secuencia

El primer commit incorpora la solución existente de forma acumulada. Los ocho commits posteriores observados incorporan y actualizan documentos, capturas y resultados. No constituyen ocho nuevas implementaciones funcionales del sistema ni evidencian que las pruebas se ejecutaran al momento de cada commit.

Las confirmaciones documentales del 04/11/2026 se concentran entre las 21:45:19 y 21:46:15, según Git. Esta proximidad describe el momento de confirmación de archivos, no el tiempo empleado en desarrollarlos. No se inventan commits por cada etapa previa, fechas de inicio, jornadas de trabajo ni contribuciones individuales.

## 6 Evidencias de integración vinculadas al historial

Git demuestra qué archivos forman parte de una revisión y cómo cambian entre commits. Un mensaje de commit, una captura o la existencia de una clase no equivalen por sí solos a una prueba de integración aprobada. Las evidencias siguientes se distinguen según su alcance.

| Integración o producto | Commit que lo incorpora o documenta | Evidencia verificable | Alcance acreditado y límite |
|---|---|---|---|
| Android, API y persistencia central | 3bcb57c | [AppContainer](../app/src/main/java/com/example/santaceciliatesis/data/AppContainer.java), [API](../backend/API.md), [migraciones](../database/migrations/) y [guía HTTPS](../docs/integracion_android.md) | Código integrado en el primer corte e informes locales. La compilación no acredita todos los RF/RNF. |
| Contratación y agenda | Código en 3bcb57c; evidencias en e5d35b9 | [Comprobaciones de contratación](11_Evidencias_Iteraciones/contratacion_comprobaciones.json), [concurrencia](11_Evidencias_Iteraciones/contratacion_concurrencia.json) e [informe](../docs/contratacion_agenda.md) | 15 comprobaciones enumeradas y registro de 11 de concurrencia; el informe describe 26 agrupadas. La aceptación genera contrato pendiente de pago, no reserva. |
| Pagos directos y reserva | Código en 3bcb57c; evidencias en e5d35b9 | [Resultados de pagos](11_Evidencias_Iteraciones/pagos_comprobaciones_sinteticas.json) e [informe de pagos](../docs/pagos_arquitectura.md) | 28 comprobaciones agrupadas locales con datos sintéticos. No hubo transferencias reales ni sandbox bancario oficial. |
| Acceso y persistencia local | Código en 3bcb57c; resúmenes en e5d35b9 | [Regresión del registro](11_Evidencias_Iteraciones/regresion_registro.txt) y [arranque persistente](11_Evidencias_Iteraciones/verificacion_arranque.json) | Una regresión instrumentada aprobada y comprobaciones de persistencia documentadas; no una suite instrumentada completa. |
| Análisis Android | Resumen incorporado en e5d35b9 | [Reporte de lint conservado](11_Evidencias_Iteraciones/resumen_lint.txt) | 0 errores y 30 advertencias en ese reporte. No acredita ausencia de defectos. |
| Interfaz por rol y flujo conectado | 32d19ef | [Índice de interfaces y 13 capturas](06_Interfaces_Funcionales/README.md) | Capturas reales de emulador, con origen y límites. Los datos de negocio usados en las verificaciones son sintéticos. |
| Esquema y material SQL | Migraciones en 3bcb57c; paquete de base de datos en cc5ad21 | [Resumen del esquema](04_Base_Datos/resumen_esquema.json), [SQL único](04_Base_Datos/Santa_Cecilia_Tablas.sql) y [metadatos](04_Base_Datos/metadatos_esquema_actual.json) | 36 tablas de negocio y una tabla técnica Flyway en la base operativa. La exportación de consulta no incluye datos privados ni historial Flyway. |
| Paquete académico | 3276c81, 6e0e528, cc5ad21, 0ae47f7, 32d19ef, f1cd040, e5d35b9 y 526df8e | [Índice de entrega](README_ENTREGA.md) y [verificación del paquete](11_Evidencias_Iteraciones/Verificacion_Paquete.json) | Versionamiento de documentos y comprobaciones de sus archivos. Los Word se revisaron con composición local, sin verificación de su composición nativa en Microsoft Word. |

Se verificó mediante `git ls-tree` que los archivos de evidencia citados existen en los commits que los incorporan. No se volvieron a ejecutar pruebas Android, de backend, SQL o de rendimiento durante esta actualización documental.

La consulta del árbol `.github` en HEAD no devolvió archivos. No se acredita un workflow de GitHub Actions versionado ni se presenta el análisis local como una ejecución de CI.

## 7 Estado del directorio de trabajo

### 7.1 Estado antes de redactar esta actualización

La salida inicial de `git status` fue:

```text
## main...origin/main
 M .gitignore
?? ENTREGABLES_UNIDAD_III/SHA256SUMS.txt
?? Santa_Cecilia_Unidad_III.zip
?? tatus
```

La modificación de `.gitignore` y los archivos sin seguimiento ya existían antes de esta revisión. No se incorporaron, eliminaron ni confirmaron. En particular, no se atribuye al archivo `tatus` una función o causa de creación: Git únicamente lo identifica como archivo sin seguimiento.

`git diff --cached --name-status` no devolvió archivos: no había cambios preparados en el índice.

### 7.2 Estado producido por esta actualización

Se modifica únicamente este informe y se crean los archivos de su carpeta `03_Evidencias_Git`. Esos cambios quedan sin preparar y sin confirmar. El historial de nueve commits y el hash de HEAD se mantienen. La [salida final de git status](03_Evidencias_Git/12_git_status_final.txt) diferencia estos archivos de los cambios que ya existían.

La consulta remota acredita el HEAD publicado al momento indicado en su transcripción; no publica esta nueva redacción. Tampoco se actualizan el ZIP académico, su manifiesto anterior ni otros informes del proyecto, que conservan su propio corte.

## 8 Índice de evidencias y reproducibilidad

| Archivo o carpeta | Contenido |
|---|---|
| [00_Documento_Anterior.md](03_Evidencias_Git/00_Documento_Anterior.md) | Copia literal del informe antes de actualizarlo; se conserva como antecedente, no como estado vigente. |
| [01_git_version.txt](03_Evidencias_Git/01_git_version.txt) | Versión del cliente Git utilizado. |
| [02_git_remote.txt](03_Evidencias_Git/02_git_remote.txt) | URL configurada de origin para consulta y envío. |
| [03_git_branch.txt](03_Evidencias_Git/03_git_branch.txt) | Ramas locales y referencias de seguimiento remoto. |
| [04_git_log_grafo.txt](03_Evidencias_Git/04_git_log_grafo.txt) | Grafo y posición de HEAD y ramas. |
| [05_git_log_completo.txt](03_Evidencias_Git/05_git_log_completo.txt) | Historial completo con autor, confirmador, fechas y mensajes. |
| [06_git_status_inicial.txt](03_Evidencias_Git/06_git_status_inicial.txt) | Estado anterior a las escrituras de esta actualización. |
| [07_git_diff_sin_preparar.txt](03_Evidencias_Git/07_git_diff_sin_preparar.txt) y [08_git_diff_preparado.txt](03_Evidencias_Git/08_git_diff_preparado.txt) | Diferencia entre cambios del directorio de trabajo e índice. |
| [09_git_ls_remote.txt](03_Evidencias_Git/09_git_ls_remote.txt) | Rama predeterminada y ramas anunciadas directamente por GitHub. |
| [10_git_referencias.txt](03_Evidencias_Git/10_git_referencias.txt) | Hashes completos y relación de upstream. |
| [11_git_arbol_github.txt](03_Evidencias_Git/11_git_arbol_github.txt) | Consulta de archivos .github en el commit HEAD. |
| [12_git_status_final.txt](03_Evidencias_Git/12_git_status_final.txt) | Estado después de crear el informe y las evidencias. |
| [commits/](03_Evidencias_Git/commits/) | Dos transcripciones git show por cada uno de los nueve commits: resumen y todos los archivos afectados. |
| [Historial_Real.json](03_Evidencias_Git/Historial_Real.json) | Metadatos y cambios estructurados obtenidos de Git. |
| [Verificacion_Integridad.json](03_Evidencias_Git/Verificacion_Integridad.json) | Comparación de HEAD, índice y archivos versionados ajenos al informe antes y después. |
| [SHA256SUMS.txt](03_Evidencias_Git/SHA256SUMS.txt) | Hashes del informe y de sus evidencias; el manifiesto no se incluye a sí mismo. |

Las transcripciones preservan los metadatos de Git tal como fueron obtenidos. No contienen contraseñas, credenciales de MySQL, archivos `.env` privados ni una copia de datos personales de la base. No se generaron capturas ficticias ni fechas retroactivas.

## 9 Conclusiones y pendientes

El repositorio presenta una rama principal `main` y 9 commits reales alcanzables. El primer commit contiene la integración acumulada de la solución y los ocho siguientes organizan su documentación y evidencias. La rama principal publicada coincide con el HEAD observado durante la consulta.

Esta revisión mejora la trazabilidad académica sin alterar la historia, el índice de preparación ni el código de la aplicación. La existencia del historial no implica que el sistema esté desplegado en producción o que todos sus requisitos estén aprobados.

Queda pendiente revisar los cambios locales y decidir su posterior incorporación al control de versiones por parte del equipo. Por instrucción expresa del usuario, esta tarea termina con el documento y las evidencias guardados localmente, sin preparar, confirmar, enviar ni fusionar cambios.
