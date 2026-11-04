package com.santacecilia.api.repository;
import com.santacecilia.api.entity.TokenAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TokenAccesoRepository extends JpaRepository<TokenAcceso,Long> {
    Optional<TokenAcceso> findByTokenHash(byte[] tokenHash);
}
