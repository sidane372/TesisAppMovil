package com.santacecilia.api.service;
import com.santacecilia.api.audit.AuditoriaService;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.AuthDtos.*;
import com.santacecilia.api.entity.*;
import com.santacecilia.api.exception.*;
import com.santacecilia.api.repository.*;
import com.santacecilia.api.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final SesionRepository sesiones;
    private final FamiliaSesionRepository familias;
    private final TokenAccesoRepository accesos;
    private final PasswordEncoder passwords;
    private final TokenCodec tokens;
    private final AuditoriaService auditoria;
    private final RequestThrottle throttle;
    private final Clock clock;
    private final Duration accessTtl,refreshTtl;
    private final String dummyHash;
    public AuthService(UsuarioRepository usuarios,SesionRepository sesiones,FamiliaSesionRepository familias,
        TokenAccesoRepository accesos,PasswordEncoder passwords,TokenCodec tokens,AuditoriaService auditoria,
        RequestThrottle throttle,Clock clock,@Value("${app.access-ttl:PT15M}") Duration accessTtl,
        @Value("${app.refresh-ttl:P30D}") Duration refreshTtl){
        this.usuarios=usuarios;this.sesiones=sesiones;this.familias=familias;this.accesos=accesos;
        this.passwords=passwords;this.tokens=tokens;this.auditoria=auditoria;this.throttle=throttle;this.clock=clock;
        if(accessTtl.isNegative()||accessTtl.isZero()||accessTtl.compareTo(Duration.ofHours(1))>0
           ||refreshTtl.compareTo(accessTtl)<=0||refreshTtl.compareTo(Duration.ofDays(90))>0)
            throw new IllegalArgumentException("Configura tiempos de sesion validos");
        this.accessTtl=accessTtl;this.refreshTtl=refreshTtl;
        this.dummyHash=passwords.encode(tokens.nuevo("dummy"));
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private LocalDateTime now(){return LocalDateTime.now(clock);}
    @Transactional
    public UsuarioResponse registrar(Registro input){
        if(usuarios.findByCorreoNormalizado(input.correo().trim()).isPresent())throw ApiException.conflict("El correo ya esta registrado");
        Usuario u=new Usuario();u.setRolId(Rol.CLIENTE.getId());u.setNombres(input.nombres().trim());u.setApellidos(input.apellidos().trim());
        u.setCorreo(input.correo().trim());u.setPasswordHash(passwords.encode(input.password()));u.setEstado("ACTIVO");
        u.setCreadoEn(now());u.setActualizadoEn(now());u.setPrivacidadVersion(input.privacidadVersion());u.setPrivacidadAceptadaEn(now());
        usuarios.saveAndFlush(u);auditoria.registrar(u.getId(),"Usuario",u.getId(),"REGISTRO_CLIENTE","Registro con aceptacion de privacidad");
        return UsuarioResponse.of(u);
    }
    @Transactional
    public Tokens login(Login input){
        String email=input.correo().trim();
        throttle.check("cuenta:"+HexFormat.of().formatHex(tokens.hash(email.toLowerCase(Locale.ROOT))),10,Duration.ofMinutes(15));
        Usuario u=usuarios.bloquearCorreo(email).orElse(null);
        boolean validLength=input.password().getBytes(StandardCharsets.UTF_8).length<=72;
        boolean valid=validLength&&passwords.matches(input.password(),u==null?dummyHash:u.getPasswordHash());
        if(!valid||u==null||!"ACTIVO".equals(u.getEstado()))throw ApiException.unauthenticated();
        FamiliaSesion f=new FamiliaSesion();f.setId(UUID.randomUUID().toString());f.setUsuarioId(u.getId());f.setCreadaEn(now());
        familias.saveAndFlush(f);Tokens result=emitir(u,f.getId(),now().plus(refreshTtl));
        auditoria.registrar(u.getId(),"Sesion",f.getId(),"LOGIN","Inicio de sesion");return result;
    }
    private Tokens emitir(Usuario u,String familia,LocalDateTime limiteRefresh){
        LocalDateTime ahora=now();String refresh=tokens.nuevo("r1"),access=tokens.nuevo("a1");
        Sesion s=new Sesion();s.setUsuarioId(u.getId());s.setFamilia(familia);s.setRefreshHash(tokens.hash(refresh));s.setCreadaEn(ahora);s.setVenceEn(limiteRefresh);
        sesiones.saveAndFlush(s);
        LocalDateTime expiry=ahora.plus(accessTtl);if(expiry.isAfter(limiteRefresh))expiry=limiteRefresh;
        TokenAcceso a=new TokenAcceso();a.setSesionId(s.getId());a.setTokenHash(tokens.hash(access));a.setCreadoEn(ahora);a.setVenceEn(expiry);accesos.saveAndFlush(a);
        return new Tokens("Bearer",access,refresh,Duration.between(ahora,expiry).toSeconds(),UsuarioResponse.of(u));
    }
    @Transactional(noRollbackFor=TokenRejectedException.class)
    public Tokens refresh(Refresh input){
        if(!tokens.formato(input.refreshToken(),"r1"))throw new TokenRejectedException();
        Sesion old=sesiones.findByRefreshHash(tokens.hash(input.refreshToken())).orElseThrow(TokenRejectedException::new);
        // Todas las mutaciones de autenticacion bloquean usuario antes de familia.
        Usuario u=usuarios.bloquear(old.getUsuarioId()).orElseThrow(TokenRejectedException::new);
        FamiliaSesion f=familias.bloquear(old.getFamilia()).orElseThrow(TokenRejectedException::new);
        entityManager.refresh(old,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(f.getRevocadaEn()!=null||old.getRevocadaEn()!=null||!old.getVenceEn().isAfter(now())||!"ACTIVO".equals(u.getEstado()))throw new TokenRejectedException();
        if(old.getConsumidaEn()!=null){
            f.setRevocadaEn(now());auditoria.registrar(u.getId(),"Sesion",f.getId(),"REUSO_REFRESH","Familia revocada por reutilizacion");
            throw new TokenRejectedException();
        }
        old.setConsumidaEn(now());return emitir(u,f.getId(),old.getVenceEn());
    }
    @Transactional(readOnly=true)
    public Optional<AuthPrincipal> autenticar(String access){
        if(!tokens.formato(access,"a1"))return Optional.empty();
        TokenAcceso a=accesos.findByTokenHash(tokens.hash(access)).orElse(null);
        if(a==null||!a.getVenceEn().isAfter(now()))return Optional.empty();
        Sesion s=sesiones.findById(a.getSesionId()).orElse(null);
        if(s==null||s.getRevocadaEn()!=null||!s.getVenceEn().isAfter(now()))return Optional.empty();
        FamiliaSesion f=familias.findById(s.getFamilia()).orElse(null);
        Usuario u=usuarios.findById(s.getUsuarioId()).orElse(null);
        if(f==null||f.getRevocadaEn()!=null||u==null||!"ACTIVO".equals(u.getEstado()))return Optional.empty();
        return Optional.of(new AuthPrincipal(u.getId(),Rol.fromId(u.getRolId()),f.getId()));
    }
    @Transactional
    public void logout(AuthPrincipal actor){
        usuarios.bloquear(actor.usuarioId()).orElseThrow(ApiException::unauthenticated);
        FamiliaSesion f=familias.bloquear(actor.familia()).orElseThrow(ApiException::unauthenticated);
        f.setRevocadaEn(now());auditoria.registrar(actor.usuarioId(),"Sesion",f.getId(),"LOGOUT","Sesion cerrada");
    }
}
