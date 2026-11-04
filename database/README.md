# Base central MySQL — Etapa 2

El esquema central está implementado en cuatro migraciones SQL y fue ejecutado en MySQL Community 8.0.30 usando una instancia de pruebas aislada. Contiene 30 tablas InnoDB y una vista de saldos. Los resultados verificables están en tests/evidence/resultado_mysql.json y ../docs/pruebas/verificacion_etapa2.md.

Esto no conecta todavía Android con MySQL: la API Spring Boot corresponde a la Etapa 3. Room conserva su esquema local v1 y su función de caché pública. No se crea una conexión JDBC desde Android ni se conectó una pasarela financiera.

| Archivo | Función |
|---|---|
| migrations/V001__modelo_central.sql | Tablas, PK/FK, índices, restricciones y comentarios de columnas |
| migrations/V002__roles_del_sistema.sql | Solo los cuatro roles; ninguna cuenta ni credencial |
| migrations/V003__inmutabilidad_y_validaciones.sql | Protección de cotizaciones enviadas, conceptos, snapshot contractual y valoración |
| migrations/V004__consulta_saldos.sql | Vista de importes exactos, agregación de devoluciones sin multiplicar pagos, SQL SECURITY INVOKER |
| modelo_logico.md | Decisiones de diseño y correspondencia con las 14 tablas originales |
| diccionario_datos.md | Todas las columnas, claves, índices y CHECK del esquema ejecutado |
| diagramas/modelo_relacional.mmd | Diagrama físico con relaciones reales |
| protocolo_agenda.md | Obligaciones transaccionales para evitar solapamientos y recursos compartidos |
| queries/ | Consultas base para el futuro backend; las que usan :parametros no se ejecutan directamente con mysql |
| instalar_desarrollo.sql | Orden manual de instalación en un esquema nuevo desde la raíz del proyecto |
| tests/test_schema.py | Pruebas de integración SQL mediante cliente MySQL; Python estándar, sin ORM ni librerías externas |
| ../tools/Test-Database.ps1 | Arranque, pruebas y parada de una instancia local independiente de Laragon |
| ../tools/export_database_docs.py | Regeneración del diccionario/diagrama a partir de la evidencia de MySQL |

## Reproducir la verificación

Desde la raíz del proyecto, con PowerShell y Python 3:

```powershell
.\tools\Test-Database.ps1 -MysqlHome 'C:\laragon\bin\mysql\mysql-8.0.30-winx64' -Python python
```

En el entorno de Codex se usó el Python disponible en C:\Users\sidan\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe; se puede pasar esa ruta al parámetro -Python. El puerto predeterminado es 33307 y se rechaza si ya está ocupado. Cada ejecución crea un datadir y un esquema sc_test_<identificador> nuevos bajo database/.local/. Usa --no-defaults para no heredar la configuración de Laragon, escucha solo en 127.0.0.1 y se detiene en finally. La contraseña aleatoria de esa instancia se transmite por el entorno del proceso y no se imprime ni se incorpora a los scripts.

Los datos de prueba se conservan en el datadir local ignorado por Git; son fixtures sintéticos, sin usuarios reales ni dinero. No se usa ni se modifica el datadir de Laragon. Los logs permanecen dentro de database/.local/run-<id>/. No se instala un servicio Windows. El esquema de pruebas no es una base de producción ni un servidor que quede atendiendo al terminar.

## Instalar una base de desarrollo persistente

1. Iniciar el servidor MySQL elegido y confirmar que santa_cecilia será un esquema nuevo de desarrollo.
2. Abrir el cliente mysql desde esta raíz con una cuenta de migraciones. Usar -p para que solicite su contraseña, sin escribirla en el comando.
3. Ejecutar SOURCE database/instalar_desarrollo.sql; una sola vez. No ejecutar el instalador sobre una versión anterior con datos.
4. Comprobar SHOW TABLES; SHOW CREATE TABLE contrato; y SELECT * FROM rol;.
5. Configurar las variables externas del futuro backend según ../backend/.env.example y ../backend/CONFIGURACION_MYSQL.md.

No se crearon ni se cambiaron cuentas del MySQL de Laragon. El instalador no contiene DROP DATABASE, TRUNCATE, desactivación de FK, usuarios precargados ni contraseñas. Si ya existe una base, se deberá incorporar el historial con Flyway y migraciones nuevas después de revisar su versión; no se debe ejecutar V001 otra vez ni ocultar errores con IF NOT EXISTS en todas las tablas.

MySQL hace commit implícito de sentencias DDL; un BEGIN no convierte toda la instalación en una migración reversible. Ante un fallo se revisa la sentencia/estado antes de continuar. [Manual de MySQL](https://dev.mysql.com/doc/refman/8.0/en/implicit-commit.html).

## Alcance de lo probado

Se validaron PK/FK/CHECK/UNIQUE, exactitud de importes, inmutabilidad y el protocolo de exclusión temporal mediante dos conexiones. Los registros SQL con estado de pago son fixtures identificados como FIXTURE_SQL_NO_PASARELA. No son sandbox de un proveedor ni demuestran autenticación, autorización HTTP, webhooks auténticos, transferencias bancarias o la integración completa.

La operación productiva, respaldo/restauración, permisos de cuenta de aplicación, TLS entre backend y base, y la ejecución de Flyway desde Spring Boot requieren el entorno de Etapa 3. No se validó MySQL 8.4 en este equipo; los resultados corresponden a 8.0.30.

## Actualización de Etapa 3

El backend ejecutable en ../backend/ ya aplica V001–V005 con Flyway y valida JPA. V005 agrega autenticación y consentimiento; ver autenticacion_etapa3.md. El esquema de negocio actual tiene 32 tablas, más el historial técnico de Flyway. Los informes/diccionario de Etapa 2 corresponden a su línea base V004 y se conservan como evidencia histórica.

Para una instalación nueva del backend se deja el esquema vacío y se permite migrar a Flyway. El instalador manual anterior llega hasta V004 y requiere baseline revisado para incorporarlo al backend; ver ../backend/CONFIGURACION_MYSQL.md. No se cambió el MySQL habitual de Laragon.

## Corte Etapa 7

El backend actual aplica V001–V007 con Flyway. V006 integra contratación y recursos; V007 agrega pago directo con comprobantes privados y conciliación administrativa por decisión del usuario. Hay 36 tablas de negocio, además de Flyway. La línea base y los resultados de Etapa 2 mantienen su alcance histórico. Guía actual: ../docs/pagos_arquitectura.md.
