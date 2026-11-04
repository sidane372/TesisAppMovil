package com.santacecilia.api.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class PagoDirectoDtos {
    private PagoDirectoDtos() {}

    public record Cuenta(
            @NotNull @Min(0) Long version,
            @NotBlank @Size(min = 2, max = 180) String titular,
            @NotBlank @Size(min = 2, max = 100) String banco,
            @Pattern(regexp = "9[0-9]{8}") String telefono,
            @Pattern(regexp = "[0-9]{6,40}") String numeroCuenta,
            @Pattern(regexp = "[0-9]{20}") String cci,
            boolean yape,
            boolean plin,
            boolean transferencia,
            boolean activo) {
        public Cuenta {
            titular = trim(titular);
            banco = trim(banco);
        }
    }

    public record Iniciar(
            @Positive long cuentaId,
            @NotBlank String metodo,
            @NotBlank String modalidad,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,100}") String clave) {}

    public record Comprobante(
            @NotNull @Min(0) Long version,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{3,100}") String operacion,
            @NotNull LocalDate fecha,
            @Min(1) @Max(100000000) long importeCentimos,
            @NotBlank @Size(min = 2, max = 180) String titularOrigen,
            @NotBlank @Size(max = 2800000) String imagenBase64) {
        public Comprobante {
            titularOrigen = trim(titularOrigen);
        }
    }

    public record Revision(
            @NotNull @Min(0) Long version,
            @NotBlank String accion,
            @Size(max = 1000) String motivo,
            Boolean contrasteEnCuenta,
            @Pattern(regexp = "[A-Za-z0-9-]{3,100}") String operacion,
            LocalDate fecha,
            @Min(0) @Max(100000000) Long importeCentimos) {
        public Revision {
            motivo = trim(motivo);
        }
    }

    public record Cancelar(@NotNull @Min(0) Long version) {}

    public record Devolver(
            @Min(1) @Max(100000000) long importeCentimos,
            @NotBlank @Size(min = 5, max = 1000) String motivo,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,100}") String clave) {
        public Devolver {
            motivo = trim(motivo);
        }
    }

    public record ConfirmarDevolucion(
            @NotNull @Min(0) Long version,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{3,100}") String operacion,
            @NotNull LocalDate fecha,
            @Min(1) @Max(100000000) long importeCentimos,
            @AssertTrue boolean contrasteEnCuenta,
            @NotBlank @Size(max = 2800000) String imagenBase64) {}

    private static String trim(String text) {
        return text == null ? null : text.trim();
    }
}
