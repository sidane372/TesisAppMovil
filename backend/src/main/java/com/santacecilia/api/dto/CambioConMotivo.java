package com.santacecilia.api.dto;
import jakarta.validation.constraints.*;
public record CambioConMotivo(@NotNull @PositiveOrZero Long version,@NotBlank @Size(min=5,max=1000) String motivo) {}
