package com.santacecilia.api.controller;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.ServicioDtos.*;
import com.santacecilia.api.security.AuthPrincipal;
import com.santacecilia.api.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/servicios")
public class AdminServicioController {
    private final ServicioService servicios;
    public AdminServicioController(ServicioService servicios){this.servicios=servicios;}
    @GetMapping public ApiResponse<Pagina<Detalle>> listar(@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="25") int tamano){return new ApiResponse<>(servicios.listar(pagina,tamano));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Detalle> crear(@Valid @RequestBody Guardar body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.crear(body,actor));}
    @PutMapping("/{id}") public ApiResponse<Detalle> editar(@PathVariable long id,@Valid @RequestBody Guardar body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.editar(id,body,actor));}
    @PostMapping("/{id}/publicacion") public ApiResponse<Detalle> publicar(@PathVariable long id,@Valid @RequestBody Publicacion body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.publicacion(id,body,actor));}
    @DeleteMapping("/{id}") public ApiResponse<Detalle> retirar(@PathVariable long id,@Valid @RequestBody CambioConMotivo body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.publicacion(id,new Publicacion(body.version(),"RETIRADO",body.motivo()),actor));}
}
