package com.santacecilia.api.service;
import com.santacecilia.api.audit.AuditoriaService;
import com.santacecilia.api.dto.AuthDtos.*;
import com.santacecilia.api.entity.*;
import com.santacecilia.api.exception.ApiException;
import com.santacecilia.api.repository.*;
import com.santacecilia.api.security.TokenCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service
public class RecoveryService {
    public record Solicitada(String email,String token){@Override public String toString(){return "Recuperacion[REDACTADO]";}}
    private final UsuarioRepository usuarios;private final RecuperacionRepository recuperaciones;
    private final FamiliaSesionRepository familias;private final PasswordEncoder passwords;private final TokenCodec tokens;
    private final AuditoriaService auditoria;private final Clock clock;private final ApplicationEventPublisher events;
    private final RecoveryMailSender correo;private final Duration ttl;
    public RecoveryService(UsuarioRepository usuarios,RecuperacionRepository recuperaciones,FamiliaSesionRepository familias,
        PasswordEncoder passwords,TokenCodec tokens,AuditoriaService auditoria,Clock clock,ApplicationEventPublisher events,
        RecoveryMailSender correo,@Value("${app.reset-ttl:PT30M}") Duration ttl){
        this.usuarios=usuarios;this.recuperaciones=recuperaciones;this.familias=familias;this.passwords=passwords;
        this.tokens=tokens;this.auditoria=auditoria;this.clock=clock;this.events=events;this.correo=correo;this.ttl=ttl;
        if(ttl.isZero()||ttl.isNegative()||ttl.compareTo(Duration.ofHours(2))>0)throw new IllegalArgumentException("Vigencia de recuperacion invalida");
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private LocalDateTime now(){return LocalDateTime.now(clock);}
    @Transactional
    public void solicitar(Forgot request){
        if(!correo.disponible())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"CORREO_NO_CONFIGURADO","La recuperacion de acceso no esta disponible temporalmente");
        Usuario u=usuarios.bloquearCorreo(request.correo().trim()).orElse(null);
        if(u==null||!"ACTIVO".equals(u.getEstado()))return;
        recuperaciones.invalidarUsuario(u.getId(),now());
        String raw=tokens.nuevo("p1");RecuperacionAcceso r=new RecuperacionAcceso();r.setUsuarioId(u.getId());r.setTokenHash(tokens.hash(raw));
        r.setCreadaEn(now());r.setVenceEn(now().plus(ttl));recuperaciones.saveAndFlush(r);
        auditoria.registrar(u.getId(),"Usuario",u.getId(),"RECUPERACION_SOLICITADA","Solicitud de enlace de un solo uso");
        events.publishEvent(new Solicitada(u.getCorreo(),raw));
    }
    @Transactional
    public void restablecer(Reset request){
        if(!tokens.formato(request.token(),"p1"))throw invalido();
        RecuperacionAcceso r=recuperaciones.findByTokenHash(tokens.hash(request.token())).orElseThrow(this::invalido);
        Usuario u=usuarios.bloquear(r.getUsuarioId()).orElseThrow(this::invalido);
        // Refrescar despues de obtener el mutex de usuario para impedir consumo concurrente.
        r=recuperaciones.findById(r.getId()).orElseThrow(this::invalido);
        entityManager.refresh(r,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(r.getUtilizadaEn()!=null||!r.getVenceEn().isAfter(now())||!"ACTIVO".equals(u.getEstado()))throw invalido();
        r.setUtilizadaEn(now());u.setPasswordHash(passwords.encode(request.nuevaPassword()));u.setActualizadoEn(now());
        familias.revocarUsuario(u.getId(),now());recuperaciones.invalidarUsuario(u.getId(),now());
        auditoria.registrar(u.getId(),"Usuario",u.getId(),"PASSWORD_RESTABLECIDA","Sesiones anteriores revocadas");
    }
    private ApiException invalido(){return new ApiException(HttpStatus.BAD_REQUEST,"ENLACE_INVALIDO","El enlace no es valido, ya fue usado o ha vencido");}
}
