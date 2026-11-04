package com.santacecilia.api.controller;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.UsuarioDtos.*;
import com.santacecilia.api.security.AuthPrincipal;
import com.santacecilia.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController @RequestMapping("/api/v1/admin/usuarios")
public class AdminUsuarioController {
    private final UsuarioService users;
    public AdminUsuarioController(UsuarioService users){this.users=users;}
    @GetMapping public ApiResponse<Pagina<UsuarioResponse>> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="25") int tamano){return new ApiResponse<>(users.listar(pagina,tamano));}
    @GetMapping("/{id}") public ApiResponse<UsuarioResponse> obtener(@PathVariable long id){return new ApiResponse<>(users.obtener(id));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<UsuarioResponse> crear(@Valid @RequestBody CrearMusico body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(users.crearMusico(body,actor));}
    @PutMapping("/{id}") public ApiResponse<UsuarioResponse> editar(@PathVariable long id,@Valid @RequestBody Perfil body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(users.editarAdmin(id,body,actor));}
    @PatchMapping("/{id}/estado") public ApiResponse<UsuarioResponse> estado(@PathVariable long id,@Valid @RequestBody Estado body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(users.estado(id,body,actor));}
    @DeleteMapping("/{id}") public ApiResponse<UsuarioResponse> desactivar(@PathVariable long id,@Valid @RequestBody CambioConMotivo body,@AuthenticationPrincipal AuthPrincipal actor){
        return new ApiResponse<>(users.estado(id,new Estado(body.version(),"DESACTIVADO",body.motivo()),actor));}
}
