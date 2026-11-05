-- Agregados separados para evitar multiplicar abonos al unir muchos reembolsos.
-- Un cobro EN_REVISION no aplicado no reduce el saldo contractual.
CREATE SQL SECURITY INVOKER VIEW v_saldo_contrato AS
SELECT c.id AS id_contrato,
       c.total_centimos,
       COALESCE(a.abonado_centimos,0) AS abonado_centimos,
       COALESCE(a.reembolsado_centimos,0) AS reembolsado_centimos,
       c.total_centimos - COALESCE(a.abonado_centimos,0) + COALESCE(a.reembolsado_centimos,0) AS saldo_centimos
FROM contrato c
LEFT JOIN (
    SELECT p.id_contrato, SUM(p.importe_centimos) AS abonado_centimos,
           SUM(COALESCE(r.importe_centimos,0)) AS reembolsado_centimos
    FROM pago p
    LEFT JOIN (
        SELECT id_pago,SUM(importe_centimos) AS importe_centimos
        FROM reembolso WHERE estado='APROBADO' GROUP BY id_pago
    ) r ON r.id_pago=p.id
    WHERE p.aplicado_en IS NOT NULL
    GROUP BY p.id_contrato
) a ON a.id_contrato=c.id;
