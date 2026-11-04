# Iteración 01 — arquitectura y base

Fecha de preparación/verificación: 03/11/2026. No se atribuye una fecha de inicio histórica no documentada.

Objetivo: transformar la plantilla inicial en una base Java/XML/MVC y preparar la arquitectura real. Requisitos relacionados parcialmente: RF-02/RF-51, RNF-07/16/20/21; no se declaran aprobados integralmente.

Actividades verificables: inspección de archivos, extracción de 90 requisitos, documentación de diferencias, sustitución de fuentes Kotlin de plantilla, Room v1 con esquema exportado, GET Volley configurable, repositorio/controlador, Adapter/ViewHolder separados y recursos XML de identidad Santa Cecilia.

Commits: no disponibles; carpeta sin Git. Pruebas y resultado comprobable: ../../docs/pruebas/verificacion_etapa1.md. Capturas: ninguna ejecución visual documentada. Advertencias reales de lint inicial: notificación global del Adapter, fondo redundante y dos colores sin uso; corregidas en código. El primer intento de Gradle dentro del sandbox no pudo abrir el lock de la caché local; la verificación se ejecutó con acceso a la caché habitual.

Pendientes: inspeccionar ZIP LaboratorioROOM, formalizar discrepancia de encuesta, elegir políticas/proveedor financiero, diseñar e implementar MySQL/backend y completar módulos Android progresivamente. No se probó el flujo financiero ni integración entre dispositivos.
