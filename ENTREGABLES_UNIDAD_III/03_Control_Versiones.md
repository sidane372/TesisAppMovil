# Control de versiones — corte 03/11/2026

El proyecto se incorpora a Git después de la construcción de las etapas 1–7 y las correcciones de conexión y acceso. Este primer corte reúne el trabajo existente; no se reconstruyen commits históricos para las etapas anteriores.

| Elemento | Evidencia / estado |
|---|---|
| URL remota | https://github.com/sidane372/TesisAppMovil.git |
| Remoto local | origin |
| Rama principal | main |
| Historial | Consultable con `git log --oneline --decorate` |
| Archivos versionados | Android Java/XML, backend, migraciones, documentación y herramientas de arranque |
| Archivos excluidos | Credenciales, datos locales de MySQL, certificados privados, compilaciones y preferencias del equipo |

El administrador y la base persistente se preparan localmente mediante INICIAR_SANTA_CECILIA.cmd; sus contraseñas y datos no se distribuyen por GitHub. Se incluye backend/.env.example con marcadores de configuración.

Las evidencias de las etapas previas describen comprobaciones locales, no versiones históricas de Git. Para registrar cambios posteriores, revisar `git status` y `git diff` antes de crear un commit y enviarlo a origin.
