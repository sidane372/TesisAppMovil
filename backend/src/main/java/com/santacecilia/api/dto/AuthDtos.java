package com.santacecilia.api.dto;
import com.santacecilia.api.security.StrongPassword;
import jakarta.validation.constraints.*;
public final class AuthDtos {
    private AuthDtos() {}
    public record Registro(@NotBlank @Size(max=100) String nombres,@NotBlank @Size(max=120) String apellidos,
        @NotBlank @Email @Size(max=254) String correo,@StrongPassword String password,
        @NotNull @AssertTrue Boolean aceptaPrivacidad,@NotBlank @Size(max=80) @Pattern(regexp="[A-Za-z0-9._-]+") String privacidadVersion) {}
    public record Login(@NotBlank @Email @Size(max=254) String correo,@NotBlank @Size(max=200) String password) {}
    public record Refresh(@NotBlank @Size(max=160) String refreshToken) {}
    public record Forgot(@NotBlank @Email @Size(max=254) String correo) {}
    public record Reset(@NotBlank @Size(max=160) String token,@StrongPassword String nuevaPassword) {}
    public record Tokens(String tokenType,String accessToken,String refreshToken,long expiresIn,UsuarioResponse usuario) {}
    public record Mensaje(String mensaje) {}
}
