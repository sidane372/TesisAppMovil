# Cómo preparar y presentar la Unidad III

## Antes de entregar

1. Abrir README_ENTREGA.md y revisar los once puntos.
2. Completar los datos académicos en blanco de los dos Word; los autores ya están incluidos.
3. Revisar prioridades, responsables y fechas con el equipo y la docente. El cronograma no contiene fechas históricas inventadas.
4. Presentar el repositorio GitHub junto con el paquete. El código, las migraciones y la documentación de referencia están allí.
5. Informar que los pagos se realizan fuera de la app y se concilian manualmente, y que las evidencias de pruebas usan datos sintéticos.

## Antes de demostrar la aplicación

1. En el equipo preparado, ejecutar INICIAR_SANTA_CECILIA.cmd y esperar SANTA CECILIA LISTO.
2. Ejecutar ESTADO_SANTA_CECILIA.cmd y comprobar MySQL y API activos.
3. Abrir la app debug en el emulador con permiso de red local.
4. Usar el acceso privado del administrador sin mostrar ni entregar la contraseña. Crear las cuentas y servicios oficiales, publicar y configurar horarios si todavía no existen.
5. Preparar sesiones de cliente y proveedor y explicar solicitud, cotización, aceptación y contrato pendiente de pago.
6. Para mostrar pagos sin mover dinero, usar las capturas históricas identificadas como sintéticas. Un comprobante real solo puede aprobarse tras contrastar el movimiento recibido en la cuenta oficial.

## Qué explicar a la ingeniera

La versión local integra Android, API y MySQL. La aceptación genera contrato pendiente de pago; el primer ingreso conciliado crea la reserva. Room conserva únicamente el catálogo público. Las exportaciones, constancia y encuestas están pendientes y no se presentan como funcionales. Producción y las mediciones RNF requieren trabajo posterior.

El paquete no contiene .env, contraseña de administrador, datos persistentes de MySQL ni comprobantes privados de personas. Para ejecutar en otro equipo se necesita preparar su entorno y sus credenciales con el código del repositorio.
