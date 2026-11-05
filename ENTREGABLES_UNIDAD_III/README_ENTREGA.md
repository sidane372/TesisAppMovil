# Entrega de la Unidad III de Santa Cecilia

Autores: Galvez Andre Jherson y Briceño Contreras Sidanne Luis.

Este paquete acompaña la versión funcional integrada de Android, Spring Boot y MySQL. El sistema tiene implementación local y evidencia del flujo principal con datos sintéticos. La entrega documental no equivale a cumplimiento total del backlog ni a despliegue de producción.

Repositorio: https://github.com/sidane372/TesisAppMovil

| Nº | Archivo o carpeta | Estado | Evidencia | Pendiente |
|---|---|---|---|---|
| 01 | 01_Plan_Desarrollo.docx | Documento generado | Objetivo, equipo, etapas, productos, criterios, riesgos y cronograma editable. | Completar datos académicos y acordar fechas. |
| 02 | 02_Backlog_Priorizado.xlsx | Libro generado | 68 historias vinculadas a RF y 90 requisitos originales conservados. | Revisar prioridades y cerrar solo lo validado. |
| 03 | 03_Control_Versiones.md | Repositorio publicado | main y primer commit verificable en GitHub. | Publicar el paquete académico cuando se acuerde. |
| 04 | 04_Base_Datos/ | Esquema local aplicado | 36 tablas de negocio, Flyway V001–V007, metadatos, SQL y diccionario actuales. | Respaldos y despliegue de producción. |
| 05 | 05_Logica_Negocio_Servicios.md | Informe actualizado | Controllers, Services, persistencia y reglas centrales. | Funciones parciales detalladas en el backlog. |
| 06 | 06_Interfaces_Funcionales/ | Capturas reales reunidas | Login, cuatro roles, catálogo, solicitud, cotización, contrato, pagos y calendario. | Preparar datos oficiales y ensayar la exposición. |
| 07 | 07_Modulos_Integrados.md | Integración local documentada | Flujo principal y comprobaciones sintéticas; sin transferencias reales. | Evaluación formal y recorrido con datos oficiales. |
| 08 | 08_Roles_Permisos_Seguridad.md | Informe actualizado | Matriz, bcrypt, sesiones, HTTPS y privacidad. | Revisión formal de seguridad y producción. |
| 09 | 09_Registro_Incidencias.xlsx | Libro generado | Incidencias observadas, solución, responsable de equipo y evidencia. | Advertencias de lint registradas como abiertas. |
| 10 | 10_Documentacion_Tecnica.docx | Documento generado | Arquitectura, MVC, datos, HTTPS, API, pagos, roles, pruebas y límites. | Completar portada y revisión académica. |
| 11 | 11_Evidencias_Iteraciones/ | Evidencias reunidas | Iteraciones, resultados JSON, regresión y análisis estático. | No sustituye validación con usuarios ni pruebas productivas. |

## Orden de revisión

Leer primero el plan y el backlog; revisar después el repositorio, esquema y módulos; utilizar las capturas y evidencias para la demostración. La guía GUIA_DEMOSTRACION.md explica el arranque y los puntos que deben presentarse.

## Cambios de alcance que deben explicarse

Volley fue reemplazado por HTTPS nativo. Los pagos simulados con tarjeta del documento formal se adaptaron a pagos externos por Yape, Plin y transferencia con comprobante y contraste administrativo. La confirmación no se basa solo en una imagen del pagador. El flujo privado completo no funciona offline; la caché Room conserva únicamente información pública. Las metas RNF todavía requieren validación formal.

Los datos académicos distintos de los nombres de los autores quedan en blanco por indicación del equipo. Las fechas de planificación no se presentan como ejecución histórica. Los resultados y capturas financieras incluidos son sintéticos y no corresponden a dinero recibido ni a un sandbox bancario oficial.

El manifiesto SHA-256 permite identificar los archivos de este corte. El paquete excluye secretos, base persistente y datos privados. Las referencias al repositorio usan sus rutas relativas desde la raíz; las copias SQL del entregable 04 forman una instantánea de entrega.
