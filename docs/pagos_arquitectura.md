# Etapa 7 — pagos directos por Yape, Plin y transferencia

La aclaración del usuario en esta etapa cambia el diseño inicial: no dispone de pasarela comercial y desea cobrar por Yape, Plin y transferencia bancaria. Se implementó **registro de pagos directos con comprobante y conciliación administrativa**. El diseño de checkout externo, credenciales comerciales y webhook queda para una posible integración futura; no se presenta como módulo ejecutado ni se simulan respuestas bancarias.

## Uso

1. Administración abre **Cuentas receptoras** en el menú y agrega los datos oficiales de cuentas en soles (PEN): titular, entidad, celular para Yape/Plin y cuenta/CCI para transferencias. Si varios medios abonan a la misma cuenta real, se habilitan en el mismo registro. No se solicitan contraseñas ni PIN. No hay cuentas receptoras precargadas en el APK ni en las migraciones.
2. En el contrato, el cliente abre **Pagos y comprobantes**, elige cuenta, medio y saldo completo o mitad del saldo. El servidor calcula el importe en céntimos; la mitad de un saldo impar se redondea hacia arriba al céntimo. Solo hay un registro pendiente por contrato. Este paso no cobra ni reserva.
3. El cliente paga en su banca/billetera y adjunta un JPEG/PNG desde el selector de archivos de Android, con operación, fecha, importe transferido y titular de origen. El estado pasa a **Comprobante pendiente de revisión**. No se reduce el saldo ni se crea reserva.
4. Administración abre el comprobante y contrasta el ingreso en los movimientos de la cuenta receptora. Registra operación, fecha e importe verificados y marca su declaración de contraste. También puede rechazar con motivo. El proveedor consulta sus contratos, pero no aprueba pagos.
5. Al conciliar un ingreso coincidente, el backend vuelve a validar contrato, saldo, fecha y agenda. El primer abono aplicado confirma el contrato y crea una única reserva con sus recursos originales. Los siguientes reducen el saldo de ese mismo contrato.

La comprobación independiente sigue los criterios publicados por [Yape](https://www.yape.com.pe/preguntas-frecuentes/yape-negocios/como-puedo-verificar-que-he-recibido-un-pago-por-yape) y [BBVA sobre Plin](https://www.bbva.pe/personas/servicios-digitales/plin.html). La captura del pagador no se toma como confirmación automática del ingreso.

## Estados y consistencia

- PENDIENTE: instrucciones generadas, sin comprobante. Cancelar ese registro no revierte una transferencia externa.
- PROCESANDO: comprobante recibido, pendiente de revisión administrativa.
- APROBADO: ingreso contrastado y aplicado al contrato.
- RECHAZADO: comprobante rechazado con motivo, sin abono.
- EN_REVISION: ingreso contrastado con incidencia, conservado sin aplicación al saldo ni reserva.
- REEMBOLSO_PENDIENTE / REEMBOLSADO: seguimiento de devoluciones documentadas.

La clave de inicio y las huellas de reporte/revisión permiten reintentos sin duplicar efectos. El movimiento es único por **cuenta receptora, fecha y operación normalizada**. Una misma operación no puede acreditar dos contratos ni reutilizarse como devolución. Administración debe mantener una sola identidad por cuenta real; existen restricciones de duplicidad de número, CCI y celular por entidad.

Se aplica el [protocolo de agenda](../database/protocolo_agenda.md): recursos en orden ascendente, después contrato y pago, transacciones cortas READ COMMITTED y lecturas de conflicto con bloqueo. No hay retenciones con cronómetro ni llamadas bancarias dentro de la transacción.

Si el intervalo se ocupa antes de la revisión, el contrato se cancela, el evento empieza sin reserva previa, el saldo no admite el ingreso o el importe verificado difiere del previsto, se conserva el movimiento y se abre una incidencia. No se confirma una reserva incompatible. Un ingreso no aplicado no reduce el saldo. No se inicia otro pago mientras quede una incidencia pendiente.

Los datos receptores quedan copiados en el intento. Una cuenta con historial solo permite cambiar habilitación/medios; para otro destino se registra otra cuenta. Desactivar no impide conciliar un ingreso ya enviado a la cuenta histórica.

## Devoluciones y cierre

Administración solicita devoluciones de ingresos con incidencia o abonos de contratos cancelados, con motivo e importe exactos. El total pendiente/confirmado no supera el ingreso realmente verificado. Cancelar no genera una devolución automática ni modifica la política contractual histórica.

La devolución se realiza fuera de Santa Cecilia. Después se registra operación, fecha, imagen e importe y se confirma el contraste; solo entonces queda aprobada y se actualiza el saldo correspondiente. Una solicitud todavía no transferida puede anularse con motivo. Devolver totalmente un ingreso no aplicado resuelve su incidencia; una devolución parcial conserva la evidencia y el importe restante.

La app **no inicia transferencias ni consulta bancos**. Registra de forma auditable la responsabilidad del administrador de contrastar los movimientos. El comprobante no sustituye esa comprobación.

La cancelación contractual conserva pagos e historial y libera las ocupaciones originales. La finalización sigue limitada al proveedor/administrador después del fin del evento según el reloj del servidor. Estas transiciones no borran el dinero registrado.

## Implementación y privacidad

Backend: PagoDirectoController, PagoDirectoService, ComprobanteImagenService, PagosPrivadosFilter y PagoAvisosService. Flyway agrega V007 sin modificar V001–V006: cuentas receptoras, datos de pago directo, comprobantes privados, movimientos conciliados y seguimiento de devoluciones. Hay 36 tablas de negocio, además del historial de Flyway.

Android: PagosFragment, PagoDetalleFragment, PagoComprobanteFragment, CuentasCobroFragment y visor privado con zoom. Reutilizan controladores/repositorios HTTPS y controles nativos de fragment_flujo.xml. No hay captura de tarjeta, WebView bancaria ni secretos comerciales en el APK.

Los comprobantes requieren autorización por contrato. No tienen URL pública ni respuestas cacheables. El servidor valida JPEG/PNG, peso y dimensiones; recodifica a JPEG sin metadatos y limita la concurrencia del procesamiento. Android reduce la imagen antes del envío y no la copia a almacenamiento compartido; conserva solo la URI elegida en el estado del formulario. Los campos de céntimos rechazan valores JSON fraccionarios.

Los avisos se escriben en outbox con el negocio y se entregan a notificaciones internas en lotes de veinte cada cinco segundos. Aviso y confirmación de entrega se comprometen juntos. No se enviaron mensajes externos ni correos de cobro.

## Verificación y límites

Backend y APK debug/release compilados; Lint sin errores. En HTTPS y MySQL 8.0.30 aislados se completaron 28 comprobaciones agrupadas: permisos, mitad/saldo, privacidad/formato de comprobantes, idempotencia, revisión concurrente, reserva única, segundo abono, rechazo, operación reutilizada, conflictos, monto diferente, cancelación, devoluciones completas/parciales y avisos durables.

Los ingresos y comprobantes de verificación son **sintéticos**, identificados como tales y restringidos a auxiliares/instancias locales ignorados. No se realizó ninguna transferencia ni se presentó una prueba local como dinero recibido. Para operar con dinero real, administración debe configurar sus cuentas oficiales y contrastar cada ingreso en su propia banca/billetera. El [arranque habitual](integracion_android.md) sigue siendo necesario: el APK debug requiere backend HTTPS activo. No se desplegó producción ni se modificaron las bases habituales de Laragon.

Se verificó además el recorrido Android cliente → selector de archivo → comprobante pendiente → visor privado de administración → conciliación → contrato confirmado. Se completó una finalización después del fin real de un evento corto sintético, conservando saldo e historial, sin alterar directamente fechas SQL. Se comprobó el rechazo de céntimos fraccionarios y la entrega de avisos con motivos largos.

En el APK final se creó una cuenta receptora sintética desde administración y se verificó su persistencia mediante la API. El formulario conservó los datos y guardó correctamente después de recrearse con fuente al 150 %. Las cuentas, importes y comprobantes de estas verificaciones no se incorporan al APK ni a las migraciones.
