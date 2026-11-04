package com.santacecilia.api.controller;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.UsuarioDtos.*;
import com.santacecilia.api.security.AuthPrincipal;
import com.santacecilia.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me")
public class UsuarioController {
    private final UsuarioService users;
    public UsuarioController(UsuarioService users){this.users=users;}
    @GetMapping public ApiResponse<UsuarioResponse> me(@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(users.propio(actor));}
    @PatchMapping public ApiResponse<UsuarioResponse> perfil(@Valid @RequestBody Perfil body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(users.editarPropio(body,actor));}
}
