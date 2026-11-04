package com.santacecilia.api.controller;

import com.santacecilia.api.dto.ApiResponse;
import com.santacecilia.api.dto.ContratacionDtos.*;
import com.santacecilia.api.security.AuthPrincipal;
import com.santacecilia.api.service.ContratacionService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ContratacionController {
    private final ContratacionService flow;

    public ContratacionController(ContratacionService flow) {
        this.flow = flow;
    }

    private ApiResponse<Map<String, Object>> ok(Map<String, Object> value) {
        return new ApiResponse<>(value);
    }

    @PostMapping("/solicitudes")
    public ApiResponse<?> solicitar(
            @Valid @RequestBody Solicitar b, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.solicitar(b, a));
    }

    @GetMapping("/solicitudes")
    public ApiResponse<?> solicitudes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.listado("solicitud", pagina, tamano, estado, q, a));
    }

    @GetMapping("/solicitudes/{id}")
    public ApiResponse<?> solicitud(
            @PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.detalleSolicitud(id, a));
    }

    @PostMapping("/solicitudes/{id}/estado")
    public ApiResponse<?> solicitudEstado(
            @PathVariable long id,
            @Valid @RequestBody Cambiar b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.cambiarSolicitud(id, b, a));
    }

    @PostMapping("/solicitudes/{id}/cotizaciones")
    public ApiResponse<?> cotizar(
            @PathVariable long id,
            @Valid @RequestBody Cotizar b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.cotizar(id, b, a));
    }

    @PostMapping("/solicitudes/{id}/cotizaciones/{quote}/respuesta")
    public ApiResponse<?> responder(
            @PathVariable long id,
            @PathVariable long quote,
            @Valid @RequestBody Responder b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.responder(id, quote, b, a));
    }

    @PostMapping("/solicitudes/{id}/cotizaciones/{quote}/aceptacion")
    public ApiResponse<?> aceptar(
            @PathVariable long id,
            @PathVariable long quote,
            @Valid @RequestBody Aceptar b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.aceptar(id, quote, b, a));
    }

    @GetMapping("/contratos")
    public ApiResponse<?> contratos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.listado("contrato", pagina, tamano, estado, q, a));
    }

    @GetMapping("/contratos/{id}")
    public ApiResponse<?> contrato(
            @PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.detalleContrato(id, a));
    }

    @PostMapping("/contratos/{id}/estado")
    public ApiResponse<?> contratoEstado(
            @PathVariable long id,
            @Valid @RequestBody Cambiar b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.cambiarContrato(id, b, a));
    }

    @GetMapping("/servicios/{id}/disponibilidad")
    public ApiResponse<?> disponible(
            @PathVariable long id,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.agenda(id, desde, hasta, a, false));
    }

    @GetMapping("/servicios/{id}/agenda")
    public ApiResponse<?> agenda(
            @PathVariable long id,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.agenda(id, desde, hasta, a, true));
    }

    @PutMapping("/servicios/{id}/horarios")
    public ApiResponse<?> horarios(
            @PathVariable long id,
            @Valid @RequestBody Horarios b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.horarios(id, b, a));
    }

    @PostMapping("/servicios/{id}/bloqueos")
    public ApiResponse<?> bloquear(
            @PathVariable long id,
            @Valid @RequestBody Bloquear b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.bloquear(id, b, a));
    }

    @DeleteMapping("/servicios/{id}/bloqueos/{block}")
    public void desbloquear(
            @PathVariable long id,
            @PathVariable long block,
            @AuthenticationPrincipal AuthPrincipal a) {
        flow.quitarBloqueo(id, block, a);
    }

    @GetMapping("/mi-resumen")
    public ApiResponse<?> resumen(@AuthenticationPrincipal AuthPrincipal a) {
        return ok(flow.resumen(a));
    }
}
