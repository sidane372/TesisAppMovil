package com.santacecilia.api.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="sesion")
public class Sesion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="id_usuario") private Long usuarioId;
    @Column(name="familia", length=36, columnDefinition="char(36)") private String familia;
    @Column(name="refresh_hash", columnDefinition="binary(32)") private byte[] refreshHash;
    @Column(name="creada_en") private LocalDateTime creadaEn;
    @Column(name="vence_en") private LocalDateTime venceEn;
    @Column(name="consumida_en") private LocalDateTime consumidaEn;
    @Column(name="revocada_en") private LocalDateTime revocadaEn;
    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long value) { this.usuarioId=value; }
    public String getFamilia() { return familia; }
    public void setFamilia(String value) { this.familia=value; }
    public byte[] getRefreshHash() { return refreshHash; }
    public void setRefreshHash(byte[] value) { this.refreshHash=value; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime value) { this.creadaEn=value; }
    public LocalDateTime getVenceEn() { return venceEn; }
    public void setVenceEn(LocalDateTime value) { this.venceEn=value; }
    public LocalDateTime getConsumidaEn() { return consumidaEn; }
    public void setConsumidaEn(LocalDateTime value) { this.consumidaEn=value; }
    public LocalDateTime getRevocadaEn() { return revocadaEn; }
    public void setRevocadaEn(LocalDateTime value) { this.revocadaEn=value; }
}
