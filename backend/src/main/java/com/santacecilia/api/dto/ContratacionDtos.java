package com.santacecilia.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.List;

public final class ContratacionDtos {
    private ContratacionDtos() {}

    public record Solicitar(
            @Positive long servicioId,
            @NotNull Instant inicio,
            @NotNull Instant fin,
            @NotBlank @Size(min = 5, max = 250) String lugar,
            @NotBlank @Size(min = 2, max = 100) String ciudad,
            @NotBlank @Size(min = 2, max = 100) String tipoEvento,
            @Min(1) @Max(100000) Integer asistentes,
            @Size(max = 1000) String preferencias,
            @Size(max = 1000) String requerimientos,
            @Size(max = 1500) String notas,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,100}") String clave) {}

    public record Linea(
            @NotBlank @Size(min = 2, max = 150) String concepto,
            @Min(1) @Max(1000) int cantidad,
            @Min(0) @Max(100000000) long precioCentimos) {}

    public record Cotizar(
            @NotNull @Min(0) Long version,
            @NotNull Instant inicio,
            @NotNull Instant fin,
            @NotEmpty @Size(max = 15) List<@Valid Linea> conceptos,
            @Min(0) @Max(100000000) long descuentoCentimos,
            @NotBlank @Size(min = 10, max = 2000) String condiciones,
            @AssertTrue boolean incluyePolitica,
            @NotNull Instant vigenteHasta,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,100}") String clave) {}

    public record Responder(
            @NotNull @Min(0) Long version,
            @NotBlank String accion,
            @Size(max = 1000) String motivo) {}

    public record Aceptar(
            @NotNull @Min(0) Long version,
            @AssertTrue boolean aceptaCondiciones,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,100}") String clave) {}

    public record Cambiar(
            @NotNull @Min(0) Long version,
            @NotBlank String accion,
            @Size(max = 1000) String motivo) {}

    public record Franja(
            @Min(1) @Max(7) int dia,
            @NotNull LocalTime desde,
            @NotBlank
                    @Pattern(
                            regexp =
                                    "(?:[01][0-9]|2[0-3]):[0-5][0-9](?::[0-5][0-9])?|24:00(?::00)?")
                    String hasta,
            @Min(0) @Max(1440) int previoMin,
            @Min(0) @Max(1440) int posteriorMin) {}

    public record Horarios(
            @NotNull @Min(0) Long version, @NotNull @Size(max = 28) List<@Valid Franja> franjas) {}

    public record Bloquear(
            @NotNull Instant inicio,
            @NotNull Instant fin,
            @NotBlank @Size(min = 5, max = 1000) String motivo) {}
}
