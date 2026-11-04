package com.santacecilia.api.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="recuperacion_acceso")
public class RecuperacionAcceso {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="id_usuario") private Long usuarioId;
    @Column(name="token_hash", columnDefinition="binary(32)") private byte[] tokenHash;
    @Column(name="creada_en") private LocalDateTime creadaEn;
    @Column(name="vence_en") private LocalDateTime venceEn;
    @Column(name="utilizada_en") private LocalDateTime utilizadaEn;
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long value) { this.usuarioId=value; }
    public byte[] getTokenHash() { return tokenHash; }
    public void setTokenHash(byte[] value) { this.tokenHash=value; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime value) { this.creadaEn=value; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime value) { this.venceEn=value; }
    public LocalDateTime getUtilizadaEn() { return utilizadaEn; }
    public void setUtilizadaEn(LocalDateTime value) { this.utilizadaEn=value; }
}
