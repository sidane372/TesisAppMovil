package com.santacecilia.api.controller;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.AuthDtos.*;
import com.santacecilia.api.security.*;
import com.santacecilia.api.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;private final RecoveryService recovery;private final RequestThrottle throttle;
    public AuthController(AuthService auth,RecoveryService recovery,RequestThrottle throttle){this.auth=auth;this.recovery=recovery;this.throttle=throttle;}
    private void limitar(HttpServletRequest request){throttle.check("auth-ip:"+request.getRemoteAddr(),60,Duration.ofMinutes(1));}
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UsuarioResponse> register(@Valid @RequestBody Registro body,HttpServletRequest request){limitar(request);return new ApiResponse<>(auth.registrar(body));}
    @PostMapping("/login") public ApiResponse<Tokens> login(@Valid @RequestBody Login body,HttpServletRequest request){limitar(request);return new ApiResponse<>(auth.login(body));}
    @PostMapping("/refresh") public ApiResponse<Tokens> refresh(@Valid @RequestBody Refresh body,HttpServletRequest request){limitar(request);return new ApiResponse<>(auth.refresh(body));}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthPrincipal actor){auth.logout(actor);}
    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Mensaje> forgot(@Valid @RequestBody Forgot body,HttpServletRequest request){limitar(request);recovery.solicitar(body);return new ApiResponse<>(new Mensaje("Si existe una cuenta habilitada, recibiras instrucciones de recuperacion"));}
    @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody Reset body,HttpServletRequest request){limitar(request);recovery.restablecer(body);}
}
