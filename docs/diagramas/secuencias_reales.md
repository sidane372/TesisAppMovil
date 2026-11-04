# Adaptación de secuencias — propuesta Etapa 1

Se conservan los códigos DS del documento original. DS-02…DS-07 pasan de Datos locales a Controller REST → Service autorizado → MySQL con transacción y auditoría. DS-08 permite caché pública pero consulta disponibilidad real por API. DS-09…DS-12 escriben solicitudes/versiones en MySQL y generan notificaciones por outbox. DS-16 requiere política de cancelación/reembolso; DS-17 verifica hora final en servidor; DS-18 conserva valoración y discrepancia de encuesta pendiente; DS-19/20 consultan datos centrales con permisos y fechas reales. No son flujos implementados.

## DS-01: acceso real propuesto

```mermaid
sequenceDiagram
  actor U as Usuario
  participant V as Vista Android
  participant C as AuthController y AuthRepository previstos
  participant API as Spring Boot
  participant DB as MySQL
  U->>V: Correo y contraseña
  V->>C: Iniciar sesión
  C->>API: POST /auth/login HTTPS
  API->>DB: Usuario activo y hash
  API->>API: Verificar contraseña y permisos
  API-->>C: Acceso breve y refresh rotatorio
  C-->>V: Identidad validada y navegación por rol
  Note over C,DB: Registro público solo CLIENTE; administrador crea músicos
```

## DS-13: aceptación y contrato propuestos

```mermaid
sequenceDiagram
  actor C as Cliente
  participant A as Android
  participant S as API y Service
  participant DB as MySQL
  C->>A: Aceptar propuesta y políticas
  A->>S: POST /cotizaciones/id/aceptacion + Idempotency-Key
  S->>DB: Bloquear solicitud, validar versión vigente y agenda
  S->>DB: Crear contrato único con snapshot PENDIENTE_PAGO
  S->>DB: Guardar auditoría y outbox; commit
  S-->>A: Contrato pendiente de pago
  Note over A,DB: Aceptar no confirma una reserva
```

## DS-14/15: pago verificado y reserva propuestos

```mermaid
sequenceDiagram
  actor C as Cliente
  participant A as Android
  participant S as Backend
  participant DB as MySQL
  participant P as Proveedor financiero pendiente
  C->>A: Elegir modalidad de abono
  A->>S: Crear intento idempotente
  S->>DB: Validar saldo/agenda y registrar intento
  S->>P: Crear checkout con referencia estable
  P-->>A: Checkout alojado
  P->>S: Webhook autenticado
  S->>DB: Registrar evento sin duplicarlo
  S->>P: Consultar y verificar pago
  P-->>S: Referencia, moneda, importe y estado
  S->>DB: Bloquear recursos/contrato y validar intervalo
  alt Cobro válido y agenda compatible
    S->>DB: Abono y primera reserva/confirmación + outbox; commit
  else Cobro tardío y conflicto
    S->>DB: Incidencia/reembolso pendiente; conservar cobro externo
  end
  A->>S: Consultar estado oficial del intento
  S-->>A: Estado verificado
```

Una transacción SQL no se mantiene abierta durante checkout/verificación de red. Los eventos repetidos no repiten abonos. No se conecta un simulador a la ruta de producción.
