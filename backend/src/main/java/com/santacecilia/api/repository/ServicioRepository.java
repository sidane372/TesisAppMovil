package com.santacecilia.api.repository;
import com.santacecilia.api.entity.ServicioMusical;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.List;
import java.util.Optional;
public interface ServicioRepository extends JpaRepository<ServicioMusical,Long> {
    List<ServicioMusical> findByResponsableIdOrderByNombreAsc(long responsableId);
    @Query("select s from ServicioMusical s, Usuario u where u.id=s.responsableId and u.estado='ACTIVO' and s.estadoPublicacion='PUBLICADO' order by s.nombre,s.id")
    List<ServicioMusical> publicados();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from ServicioMusical s where s.id=:id")
    Optional<ServicioMusical> bloquear(long id);
}
