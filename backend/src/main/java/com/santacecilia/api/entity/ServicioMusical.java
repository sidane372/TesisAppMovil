package com.santacecilia.api.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="servicio_musical")
public class ServicioMusical {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version @Column(name="version") private long version;
    @Column(name="id_responsable") private Long responsableId;
    @Column(name="tipo", length=12) private String tipo;
    @Column(name="nombre", length=160) private String nombre;
    @Column(name="descripcion_historica", columnDefinition="text") private String descripcionHistorica;
    @Column(name="ciudad", length=100) private String ciudad;
    @Column(name="experiencia", length=500) private String experiencia;
    @Column(name="incluye", columnDefinition="text") private String incluye;
    @Column(name="tarifa_centimos") private Long tarifaCentimos;
    @Column(name="estado_publicacion", length=20) private String estadoPublicacion;
    @Column(name="motivo_revision", length=1000) private String motivoRevision;
    @Column(name="creado_en") private LocalDateTime creadoEn;
    @Column(name="actualizado_en") private LocalDateTime actualizadoEn;
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public Long getResponsableId() { return responsableId; }
    public void setResponsableId(Long value) { this.responsableId=value; }
    public String getTipo() { return tipo; }
    public void setTipo(String value) { this.tipo=value; }
    public String getNombre() { return nombre; }
    public void setNombre(String value) { this.nombre=value; }
    public String getDescripcionHistorica() { return descripcionHistorica; }
    public void setDescripcionHistorica(String value) { this.descripcionHistorica=value; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String value) { this.ciudad=value; }
    public String getExperiencia() { return experiencia; }
    public void setExperiencia(String value) { this.experiencia=value; }
    public String getIncluye() { return incluye; }
    public void setIncluye(String value) { this.incluye=value; }
    public Long getTarifaCentimos() { return tarifaCentimos; }
    public void setTarifaCentimos(Long value) { this.tarifaCentimos=value; }
    public String getEstadoPublicacion() { return estadoPublicacion; }
    public void setEstadoPublicacion(String value) { this.estadoPublicacion=value; }
    public String getMotivoRevision() { return motivoRevision; }
    public void setMotivoRevision(String value) { this.motivoRevision=value; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime value) { this.creadoEn=value; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime value) { this.actualizadoEn=value; }
}
