# Extensión de autenticación — V005

La migración V005__autenticacion_y_consentimiento.sql se aplicó mediante Flyway al arrancar el backend sobre MySQL 8.0.30. V001–V004 conservan sus archivos originales. El esquema de negocio pasa de 30 a 32 tablas; flyway_schema_history es una tabla técnica adicional del migrador.

## auth_familia

| Columna | Tipo / regla | Función |
|---|---|---|
| id | CHAR(36), PK, ascii_bin | Familia UUID de refresh, no token público |
| id_usuario | BIGINT NOT NULL, FK usuario | Titular único |
| creada_en | DATETIME(6) NOT NULL, UTC | Fecha de creación |
| revocada_en | DATETIME(6) NULL, UTC | Revoca todos los accesos/refresh de la familia |

UNIQUE(id,id_usuario), INDEX(id_usuario,revocada_en). La FK compuesta nueva de sesion(familia,id_usuario) exige que el titular coincida con esa familia. La rotación bloquea primero usuario y luego familia; reconsulta la sesión con bloqueo antes de consumirla para impedir dobles usos concurrentes. La reutilización confirma la revocación en la transacción y devuelve 401.

## token_acceso

| Columna | Tipo / regla | Función |
|---|---|---|
| id | BIGINT AUTO_INCREMENT, PK | Identificador interno |
| id_sesion | BIGINT NOT NULL, FK sesion | Sesión que lo emitió |
| token_hash | BINARY(32) NOT NULL, UNIQUE | SHA-256 del bearer aleatorio; no se guarda en claro |
| creado_en | DATETIME(6) NOT NULL, UTC | Emisión |
| vence_en | DATETIME(6) NOT NULL, UTC | Caducidad; CHECK mayor que creado_en |

INDEX(id_sesion,vence_en). Cada petición verifica token, vigencia, sesión, familia no revocada y cuenta activa. No utiliza JWT ni un secreto fijo en el APK. Cerrar sesión, suspender cuenta o restablecer contraseña invalida las familias correspondientes.

## Consentimiento agregado a usuario

privacidad_version VARCHAR(80) ascii_bin y privacidad_aceptada_en DATETIME(6), ambas NULL o ambas informadas por CHECK. El registro público exige aceptación expresa y la registra con la hora UTC del servidor. Las cuentas creadas por administración no reciben un consentimiento inventado.

El diccionario/diagrama identificados como Etapa 2 siguen siendo el inventario histórico de V004; esta extensión documenta todas las columnas nuevas de V005. El backend no cambia la caché Room v1. La clave de correo normalizado, bcrypt, hashes de recuperación y referencias financieras anteriores se conservan.
