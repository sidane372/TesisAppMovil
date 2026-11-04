package com.santacecilia.api.dto;
import com.santacecilia.api.entity.Rol;
import com.santacecilia.api.security.StrongPassword;
import jakarta.validation.constraints.*;
public final class UsuarioDtos {
    private UsuarioDtos() {}
    public record CrearMusico(@NotBlank @Size(max=100) String nombres,@NotBlank @Size(max=120) String apellidos,
        @NotBlank @Email @Size(max=254) String correo,@StrongPassword String password,@NotNull Rol rol) {}
    public record Perfil(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=100) String nombres,
        @NotBlank @Size(max=120) String apellidos,@Size(max=24) String telefono,@Size(max=100) String ciudad,
        @Size(max=1000) String biografia) {}
    public record Estado(@NotNull @PositiveOrZero Long version,@NotBlank String estado,@NotBlank @Size(min=5,max=1000) String motivo) {}
}
