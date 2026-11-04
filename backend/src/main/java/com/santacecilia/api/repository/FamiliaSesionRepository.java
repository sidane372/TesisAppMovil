package com.santacecilia.api.repository;
import com.santacecilia.api.entity.FamiliaSesion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.Optional;
public interface FamiliaSesionRepository extends JpaRepository<FamiliaSesion,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select f from FamiliaSesion f where f.id=:id")
    Optional<FamiliaSesion> bloquear(String id);
    @Modifying @Query("update FamiliaSesion f set f.revocadaEn=:ahora where f.usuarioId=:usuarioId and f.revocadaEn is null")
    int revocarUsuario(long usuarioId, LocalDateTime ahora);
}
