# Unidad III — índice inicial de evidencias

Corte: 03/11/2026. El proyecto comprende Etapas 1–5: arquitectura, esquema MySQL, backend, integración Android e interfaz por rol. No acredita el flujo completo de contratación/pagos; el estado vigente y los comandos están en [la guía de integración](../docs/integracion_android.md). Los formatos académicos DOCX/XLSX aún no se generaron. Los documentos de estado siguientes se basan en archivos y resultados disponibles.

| Nº | Archivo o carpeta solicitada | Estado | Evidencia disponible | Pendientes |
|---|---|---|---|---|
| 01 | 01_Plan_Desarrollo.docx | Pendiente de formato académico | ../docs/orden_implementacion.md y ../README.md | Cronograma/fechas y responsables confirmados; generar DOCX revisado |
| 02 | 02_Backlog_Priorizado.xlsx | Pendiente | ../docs/matriz_trazabilidad.md contiene los 68 RF; ../docs/referencias/requerimientos_extraidos.json | Historias, criterios actualizados, priorización MoSCoW, iteraciones propuestas y XLSX |
| 03 | 03_Control_Versiones.md | Diagnóstico inicial creado | Inspección: no hay Git en la carpeta | Configurar Git y registrar historial real |
| 04 | 04_Base_Datos/ | Implementado y verificado localmente | 4 migraciones, 30 tablas, diccionario físico y 31 pruebas MySQL aprobadas | Despliegue, grants/TLS, Flyway/JPA y backend real |
| 05 | 05_Logica_Negocio_Servicios.md | Backend de identidad/servicios implementado | JAR Spring Boot, Flyway, JPA, seguridad y endpoints funcionales | Contrataciones/pagos y módulos de contenido posteriores |
| 06 | 06_Interfaces_Funcionales/ | Interfaz nativa implementada | Interfaz por rol y formularios conectados; ../docs/interfaz.md | Completar los módulos de negocio posteriores y formatos académicos |
| 07 | 07_Modulos_Integrados.md | Registro inicial creado | Compilación/pruebas locales; E2E no ejecutado | Flujo multiusuario completo y sandbox oficial |
| 08 | 08_Roles_Permisos_Seguridad.md | Autenticación y permisos implementados en backend | bcrypt, sesiones revocables, roles/propiedad y recuperación SMTP | Configuración HTTPS/SMTP del despliegue y módulos posteriores |
| 09 | 09_Registro_Incidencias.xlsx | Pendiente de formato académico | Advertencias de lint y corrección en 11_Evidencias_Iteraciones/Iteracion_01_Arquitectura.md | Registro XLSX y evidencia de resolución; no inventar errores |
| 10 | 10_Documentacion_Tecnica.docx | Pendiente de formato académico | ../docs/arquitectura.md, estructura_mvc.md, endpoints_api.md, pagos_arquitectura.md y database/ | Consolidar DOCX y verificar su renderizado |
| 11 | 11_Evidencias_Iteraciones/ | Iteraciones 01 y 02 documentadas | Código, esquema y resultados reales Android/MySQL | Commits, dispositivo, capturas y próximas iteraciones |

No se inventan responsables, fechas históricas, commits, mediciones, encuestas, capturas o transacciones. Se continúa la documentación académica junto con cada etapa implementada; la etapa actual prioriza una base de arquitectura verificable.

En Etapa 3 se priorizó la construcción del servidor por instrucción del usuario. No se creó un paquete nuevo de pruebas académicas. Los informes anteriores se conservan como evidencia histórica; el alcance actual y el arranque están en ../backend/README.md.
