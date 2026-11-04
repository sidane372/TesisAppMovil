package com.santacecilia.api.repository;
import com.santacecilia.api.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    @Query("select u from Usuario u where u.correoNormalizado=lower(trim(:correo))")
    Optional<Usuario> findByCorreoNormalizado(String correo);
    boolean existsByRolId(short rolId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from Usuario u where u.id=:id")
    Optional<Usuario> bloquear(long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from Usuario u where u.correoNormalizado=lower(trim(:correo))")
    Optional<Usuario> bloquearCorreo(String correo);
}
