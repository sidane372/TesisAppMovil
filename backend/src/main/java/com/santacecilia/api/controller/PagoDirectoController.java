package com.santacecilia.api.controller;

import com.santacecilia.api.dto.ApiResponse;
import com.santacecilia.api.dto.PagoDirectoDtos.*;
import com.santacecilia.api.security.AuthPrincipal;
import com.santacecilia.api.service.PagoDirectoService;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PagoDirectoController {
    private final PagoDirectoService pagos;

    public PagoDirectoController(PagoDirectoService pagos) {
        this.pagos = pagos;
    }

    private ApiResponse<?> ok(Object data) {
        return new ApiResponse<>(data);
    }

    @GetMapping("/admin/cuentas-cobro")
    public ApiResponse<?> cuentas(@AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.cuentas(a));
    }

    @PostMapping("/admin/cuentas-cobro")
    public ApiResponse<?> cuenta(
            @Valid @RequestBody Cuenta b, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.guardarCuenta(null, b, a));
    }

    @PutMapping("/admin/cuentas-cobro/{id}")
    public ApiResponse<?> cuenta(
            @PathVariable long id,
            @Valid @RequestBody Cuenta b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.guardarCuenta(id, b, a));
    }

    @GetMapping("/contratos/{id}/pagos")
    public ApiResponse<?> contrato(
            @PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.resumen(id, a));
    }

    @PostMapping("/contratos/{id}/pagos")
    public ApiResponse<?> iniciar(
            @PathVariable long id,
            @Valid @RequestBody Iniciar b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.iniciar(id, b, a));
    }

    @GetMapping("/pagos")
    public ApiResponse<?> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.listar(pagina, estado, a));
    }

    @GetMapping("/pagos/{id}")
    public ApiResponse<?> pago(@PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.detalle(id, a));
    }

    @PutMapping("/pagos/{id}/comprobante")
    public ApiResponse<?> reportar(
            @PathVariable long id,
            @Valid @RequestBody Comprobante b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.reportar(id, b, a));
    }

    @PostMapping("/pagos/{id}/cancelacion")
    public ApiResponse<?> cancelar(
            @PathVariable long id,
            @Valid @RequestBody Cancelar b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.cancelar(id, b, a));
    }

    @GetMapping("/pagos/{id}/comprobante")
    public ApiResponse<?> imagen(@PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.comprobante(id, false, a));
    }

    @PostMapping("/admin/pagos/{id}/revision")
    public ApiResponse<?> revision(
            @PathVariable long id,
            @Valid @RequestBody Revision b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.revisar(id, b, a));
    }

    @PostMapping("/admin/pagos/{id}/reembolsos")
    public ApiResponse<?> devolver(
            @PathVariable long id,
            @Valid @RequestBody Devolver b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.devolver(id, b, a));
    }

    @PostMapping("/admin/reembolsos/{id}/confirmacion")
    public ApiResponse<?> confirmar(
            @PathVariable long id,
            @Valid @RequestBody ConfirmarDevolucion b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.confirmarDevolucion(id, b, a));
    }

    @GetMapping("/reembolsos/{id}/comprobante")
    public ApiResponse<?> imagenDevolucion(
            @PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.comprobante(id, true, a));
    }

    @GetMapping("/reembolsos/{id}")
    public ApiResponse<?> reembolso(
            @PathVariable long id, @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.detalleReembolso(id, a));
    }

    @PostMapping("/admin/reembolsos/{id}/rechazo")
    public ApiResponse<?> rechazo(
            @PathVariable long id,
            @Valid @RequestBody Revision b,
            @AuthenticationPrincipal AuthPrincipal a) {
        return ok(pagos.rechazarDevolucion(id, b, a));
    }
}
