package com.santacecilia.api.controller;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.ServicioDtos.*;
import com.santacecilia.api.security.AuthPrincipal;
import com.santacecilia.api.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1")
public class ServicioController {
    private final ServicioService servicios;
    public ServicioController(ServicioService servicios){this.servicios=servicios;}
    @GetMapping("/servicios") public Catalogo catalogo(@RequestParam(required=false) String q,@RequestParam(required=false) String tipo,
        @RequestParam(required=false) String genero,@RequestParam(required=false) String ciudad,@RequestParam(required=false) Long tarifaMaxCentimos,@RequestParam java.util.Map<String,String> filtros){
        if(!java.util.Set.of("q","tipo","genero","ciudad","tarifaMaxCentimos").containsAll(filtros.keySet()))throw com.santacecilia.api.exception.ApiException.invalid("El filtro solicitado no esta disponible en esta etapa");
        return servicios.catalogo(q,tipo,genero,ciudad,tarifaMaxCentimos);}
    @GetMapping("/servicios/{id}") public ApiResponse<Detalle> detalle(@PathVariable long id,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.obtener(id,actor));}
    @GetMapping("/mis-servicios") public ApiResponse<List<Detalle>> propios(@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.propios(actor));}
    @PatchMapping("/servicios/{id}/tarifa") public ApiResponse<Detalle> tarifa(@PathVariable long id,@Valid @RequestBody Tarifa body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.tarifa(id,body,actor));}
    @PostMapping("/servicios/{id}/repertorio") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CancionResponse> agregar(@PathVariable long id,@Valid @RequestBody Cancion body,@AuthenticationPrincipal AuthPrincipal actor){return new ApiResponse<>(servicios.agregarCancion(id,body,actor));}
    @PutMapping("/servicios/{id}/repertorio/{cancion}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editar(@PathVariable long id,@PathVariable long cancion,@Valid @RequestBody Cancion body,@AuthenticationPrincipal AuthPrincipal actor){servicios.editarCancion(id,cancion,body,actor);}
    @DeleteMapping("/servicios/{id}/repertorio/{cancion}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable long id,@PathVariable long cancion,@AuthenticationPrincipal AuthPrincipal actor){servicios.retirarCancion(id,cancion,actor);}
}
