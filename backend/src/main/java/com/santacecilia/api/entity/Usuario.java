package com.santacecilia.api.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="usuario")
public class Usuario {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version @Column(name="version") private long version;
    @Column(name="id_rol") private short rolId;
    @Column(name="nombres", length=100) private String nombres;
    @Column(name="apellidos", length=120) private String apellidos;
    @Column(name="correo", length=254) private String correo;
    @Column(name="correo_normalizado", insertable=false, updatable=false, length=254) private String correoNormalizado;
    @Column(name="password_hash", length=255) private String passwordHash;
    @Column(name="telefono", length=24) private String telefono;
    @Column(name="ciudad", length=100) private String ciudad;
    @Column(name="biografia", length=1000) private String biografia;
    @Column(name="estado", length=24) private String estado;
    @Column(name="creado_en") private LocalDateTime creadoEn;
    @Column(name="actualizado_en") private LocalDateTime actualizadoEn;
    @Column(name="privacidad_version", length=80) private String privacidadVersion;
    @Column(name="privacidad_aceptada_en") private LocalDateTime privacidadAceptadaEn;
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public short getRolId() { return rolId; }
    public void setRolId(short value) { this.rolId=value; }
    public String getNombres() { return nombres; }
    public void setNombres(String value) { this.nombres=value; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String value) { this.apellidos=value; }
    public String getCorreo() { return correo; }
    public void setCorreo(String value) { this.correo=value; }
    public String getCorreoNormalizado() { return correoNormalizado; }
    public void setCorreoNormalizado(String value) { this.correoNormalizado=value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { this.passwordHash=value; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String value) { this.telefono=value; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String value) { this.ciudad=value; }
    public String getBiografia() { return biografia; }
    public void setBiografia(String value) { this.biografia=value; }
    public String getEstado() { return estado; }
    public void setEstado(String value) { this.estado=value; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime value) { this.creadoEn=value; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime value) { this.actualizadoEn=value; }
    public String getPrivacidadVersion() { return privacidadVersion; }
    public void setPrivacidadVersion(String value) { this.privacidadVersion=value; }
    public LocalDateTime getPrivacidadAceptadaEn() { return privacidadAceptadaEn; }
    public void setPrivacidadAceptadaEn(LocalDateTime value) { this.privacidadAceptadaEn=value; }
}
