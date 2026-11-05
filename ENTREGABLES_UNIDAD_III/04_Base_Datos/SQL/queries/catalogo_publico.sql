-- Consulta de snapshot publico completo; sin identidades ni eventos de clientes.
SELECT s.id, s.nombre, s.tipo, s.ciudad, s.tarifa_centimos AS tarifaCentimos
FROM servicio_musical s
JOIN usuario u ON u.id=s.id_responsable
WHERE s.estado_publicacion='PUBLICADO' AND u.estado='ACTIVO'
ORDER BY s.nombre, s.id;
