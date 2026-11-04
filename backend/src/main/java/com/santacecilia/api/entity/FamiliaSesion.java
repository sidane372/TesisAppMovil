package com.santacecilia.api.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="auth_familia")
public class FamiliaSesion {
    @Id @Column(columnDefinition="char(36)") private String id;
    @Column(name="id_usuario") private Long usuarioId;
    @Column(name="creada_en") private LocalDateTime creadaEn;
    @Column(name="revocada_en") private LocalDateTime revocadaEn;
    public String getId(){return id;} public void setId(String value){id=value;}
    public Long getUsuarioId(){return usuarioId;} public void setUsuarioId(Long value){usuarioId=value;}
    public LocalDateTime getCreadaEn(){return creadaEn;} public void setCreadaEn(LocalDateTime value){creadaEn=value;}
    public LocalDateTime getRevocadaEn(){return revocadaEn;} public void setRevocadaEn(LocalDateTime value){revocadaEn=value;}
}
