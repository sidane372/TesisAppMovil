package com.santacecilia.api.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public final class ServicioDtos {
    private ServicioDtos() {}
    public record Guardar(@NotNull @Positive Long responsableId,@NotBlank @Pattern(regexp="BANDA|SOLISTA") String tipo,
        @NotBlank @Size(max=160) String nombre,@NotBlank @Size(max=100) String ciudad,
        @Size(max=10000) String descripcionHistorica,@Size(max=500) String experiencia,@Size(max=10000) String incluye,
        @PositiveOrZero @Max(999999999999L) Long tarifaCentimos,
        @NotNull @Size(max=30) List<@NotBlank @Size(max=80) String> generos,
        @NotNull @Size(max=40) List<@NotBlank @Size(max=80) String> instrumentos,
        @NotNull @Size(max=30) List<@NotBlank @Size(max=100) String> tiposEvento,
        @PositiveOrZero Long version) {}
    public record Publicacion(@NotNull @PositiveOrZero Long version,@NotBlank String estado,@Size(max=1000) String motivo) {}
    public record Tarifa(@NotNull @PositiveOrZero Long version,@PositiveOrZero @Max(999999999999L) Long tarifaCentimos) {}
    public record Cancion(@NotBlank @Size(max=180) String titulo,@NotBlank @Size(max=80) String genero,
        @Size(max=1000) String descripcion,@Positive Integer duracionSegundos,@PositiveOrZero Integer orden) {}
    public record Resumen(long id,String nombre,String tipo,String ciudad,Long tarifaCentimos,List<String> generos,List<String> instrumentos) {}
    public record Catalogo(boolean completo,List<Resumen> data) {}
    public record Detalle(long id,long responsableId,String tipo,String nombre,String ciudad,String descripcionHistorica,
        String experiencia,String incluye,Long tarifaCentimos,String estadoPublicacion,long version,
        List<String> generos,List<String> instrumentos,List<String> tiposEvento,List<CancionResponse> repertorio) {}
    public record CancionResponse(long id,String titulo,String genero,String descripcion,Integer duracionSegundos,int orden) {}
}
