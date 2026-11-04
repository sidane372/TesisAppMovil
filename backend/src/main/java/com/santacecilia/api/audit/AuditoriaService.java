package com.santacecilia.api.audit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service
public class AuditoriaService {
    private final JdbcTemplate jdbc;
    public AuditoriaService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void registrar(Long actor,String entidad,Object id,String accion,String detalle){
        jdbc.update("INSERT INTO historial_operacion(id_actor,entidad,identificador_entidad,accion,detalle,correlation_id) VALUES(?,?,?,?,JSON_OBJECT('mensaje',?),?)",
            actor,entidad,String.valueOf(id),accion,detalle,UUID.randomUUID().toString());
    }
}
