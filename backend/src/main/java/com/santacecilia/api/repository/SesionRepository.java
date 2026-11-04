package com.santacecilia.api.repository;
import com.santacecilia.api.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SesionRepository extends JpaRepository<Sesion,Long> {
    Optional<Sesion> findByRefreshHash(byte[] refreshHash);
}
