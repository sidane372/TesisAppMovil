package com.santacecilia.api.repository;
import com.santacecilia.api.entity.RecuperacionAcceso;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.Optional;
public interface RecuperacionRepository extends JpaRepository<RecuperacionAcceso,Long> {
    Optional<RecuperacionAcceso> findByTokenHash(byte[] tokenHash);
    @Modifying @Query("update RecuperacionAcceso r set r.utilizadaEn=:ahora where r.usuarioId=:usuarioId and r.utilizadaEn is null")
    int invalidarUsuario(long usuarioId,LocalDateTime ahora);
}
