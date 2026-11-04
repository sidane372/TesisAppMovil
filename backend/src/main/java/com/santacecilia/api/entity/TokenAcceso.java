package com.santacecilia.api.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="token_acceso")
public class TokenAcceso {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="id_sesion") private Long sesionId;
    @Column(name="token_hash", columnDefinition="binary(32)") private byte[] tokenHash;
    @Column(name="creado_en") private LocalDateTime creadoEn;
    @Column(name="vence_en") private LocalDateTime venceEn;
    public Long getId() { return id; }
    public Long getSesionId() { return sesionId; }
    public void setSesionId(Long value) { this.sesionId=value; }
    public byte[] getTokenHash() { return tokenHash; }
    public void setTokenHash(byte[] value) { this.tokenHash=value; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime value) { this.creadoEn=value; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime value) { this.venceEn=value; }
}
