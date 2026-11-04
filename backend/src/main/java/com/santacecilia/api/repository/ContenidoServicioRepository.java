package com.santacecilia.api.repository;
import com.santacecilia.api.dto.ServicioDtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class ContenidoServicioRepository {
    private final JdbcTemplate jdbc;
    public ContenidoServicioRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public List<String> generos(long id){return jdbc.queryForList("SELECT genero FROM servicio_genero WHERE id_servicio=? ORDER BY genero",String.class,id);}
    public List<String> instrumentos(long id){return jdbc.queryForList("SELECT instrumento FROM servicio_instrumento WHERE id_servicio=? ORDER BY instrumento",String.class,id);}
    public List<String> tiposEvento(long id){return jdbc.queryForList("SELECT tipo_evento FROM servicio_tipo_evento WHERE id_servicio=? ORDER BY tipo_evento",String.class,id);}
    public void reemplazarListas(long id,Guardar input){
        jdbc.update("DELETE FROM servicio_genero WHERE id_servicio=?",id);
        for(String value:new LinkedHashSet<>(input.generos()))jdbc.update("INSERT INTO servicio_genero(id_servicio,genero) VALUES(?,?)",id,value.trim());
        jdbc.update("DELETE FROM servicio_instrumento WHERE id_servicio=?",id);
        for(String value:new LinkedHashSet<>(input.instrumentos()))jdbc.update("INSERT INTO servicio_instrumento(id_servicio,instrumento) VALUES(?,?)",id,value.trim());
        jdbc.update("DELETE FROM servicio_tipo_evento WHERE id_servicio=?",id);
        for(String value:new LinkedHashSet<>(input.tiposEvento()))jdbc.update("INSERT INTO servicio_tipo_evento(id_servicio,tipo_evento) VALUES(?,?)",id,value.trim());
    }
    public List<CancionResponse> repertorio(long id){
        return jdbc.query("SELECT id,titulo,genero,descripcion,duracion_segundos,orden FROM repertorio WHERE id_servicio=? AND activo=1 ORDER BY orden,id",
            (rs,n)->new CancionResponse(rs.getLong("id"),rs.getString("titulo"),rs.getString("genero"),rs.getString("descripcion"),(Integer)rs.getObject("duracion_segundos"),rs.getInt("orden")),id);
    }
    public long agregarCancion(long id,Cancion input){
        var holder=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO repertorio(id_servicio,titulo,genero,descripcion,duracion_segundos,orden) VALUES(?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1,id);statement.setString(2,input.titulo().trim());statement.setString(3,input.genero().trim());statement.setString(4,input.descripcion());
            statement.setObject(5,input.duracionSegundos());statement.setInt(6,input.orden()==null?0:input.orden());return statement;},holder);
        return Objects.requireNonNull(holder.getKey()).longValue();
    }
    public boolean editarCancion(long servicio,long cancion,Cancion input){return jdbc.update("UPDATE repertorio SET titulo=?,genero=?,descripcion=?,duracion_segundos=?,orden=? WHERE id=? AND id_servicio=? AND activo=1",input.titulo().trim(),input.genero().trim(),input.descripcion(),input.duracionSegundos(),input.orden()==null?0:input.orden(),cancion,servicio)==1;}
    public boolean retirarCancion(long servicio,long cancion){return jdbc.update("UPDATE repertorio SET activo=0 WHERE id=? AND id_servicio=? AND activo=1",cancion,servicio)==1;}
    public boolean tieneHistoria(long servicio){return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM solicitud_cotizacion WHERE id_servicio=?) + (SELECT COUNT(*) FROM bloqueo_agenda WHERE id_servicio=?)",Long.class,servicio,servicio)>0;}
    public void crearRecursoServicio(long servicio){jdbc.update("INSERT INTO recurso_agenda(id_servicio) VALUES(?)",servicio);}
    public void asegurarRecursoMusico(long usuario){jdbc.update("INSERT INTO recurso_agenda(id_musico) VALUES(?) ON DUPLICATE KEY UPDATE id_musico=VALUES(id_musico)",usuario);}
}
