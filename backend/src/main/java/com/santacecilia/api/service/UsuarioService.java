package com.santacecilia.api.service;
import com.santacecilia.api.audit.AuditoriaService;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.UsuarioDtos.*;
import com.santacecilia.api.entity.*;
import com.santacecilia.api.exception.ApiException;
import com.santacecilia.api.repository.*;
import com.santacecilia.api.security.AuthPrincipal;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Set;
@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;private final FamiliaSesionRepository familias;
    private final PasswordEncoder passwords;private final AuditoriaService auditoria;private final Clock clock;
    public UsuarioService(UsuarioRepository usuarios,FamiliaSesionRepository familias,PasswordEncoder passwords,AuditoriaService auditoria,Clock clock){
        this.usuarios=usuarios;this.familias=familias;this.passwords=passwords;this.auditoria=auditoria;this.clock=clock;
    }
    @Transactional(readOnly=true) public UsuarioResponse propio(AuthPrincipal actor){return UsuarioResponse.of(usuarios.findById(actor.usuarioId()).orElseThrow(ApiException::notFound));}
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional(readOnly=true)
    public Pagina<UsuarioResponse> listar(int pagina,int tamano){
        if(pagina<0||tamano<1||tamano>100)throw ApiException.invalid("Usa pagina >= 0 y tamano entre 1 y 100");
        var result=usuarios.findAll(PageRequest.of(pagina,tamano,Sort.by("id")));
        return new Pagina<>(result.getContent().stream().map(UsuarioResponse::of).toList(),pagina,tamano,result.getTotalElements());
    }
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional(readOnly=true)
    public UsuarioResponse obtener(long id){return UsuarioResponse.of(usuarios.findById(id).orElseThrow(ApiException::notFound));}
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional
    public UsuarioResponse crearMusico(CrearMusico input,AuthPrincipal actor){
        if(input.rol()!=Rol.DIRECTOR&&input.rol()!=Rol.SOLISTA)throw ApiException.invalid("Solo se crean cuentas de director o solista en esta operacion");
        if(usuarios.findByCorreoNormalizado(input.correo().trim()).isPresent())throw ApiException.conflict("El correo ya esta registrado");
        LocalDateTime now=LocalDateTime.now(clock);Usuario u=new Usuario();u.setRolId(input.rol().getId());u.setNombres(input.nombres().trim());
        u.setApellidos(input.apellidos().trim());u.setCorreo(input.correo().trim());u.setPasswordHash(passwords.encode(input.password()));
        u.setEstado("ACTIVO");u.setCreadoEn(now);u.setActualizadoEn(now);usuarios.saveAndFlush(u);
        auditoria.registrar(actor.usuarioId(),"Usuario",u.getId(),"CREACION_CUENTA_MUSICAL","Cuenta creada por administrador");return UsuarioResponse.of(u);
    }
    @Transactional public UsuarioResponse editarPropio(Perfil input,AuthPrincipal actor){return editar(actor.usuarioId(),input,actor);}
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional
    public UsuarioResponse editarAdmin(long id,Perfil input,AuthPrincipal actor){return editar(id,input,actor);}
    private UsuarioResponse editar(long id,Perfil input,AuthPrincipal actor){
        Usuario u=usuarios.bloquear(id).orElseThrow(ApiException::notFound);version(u,input.version());
        u.setNombres(input.nombres().trim());u.setApellidos(input.apellidos().trim());u.setTelefono(input.telefono());
        u.setCiudad(input.ciudad());u.setBiografia(input.biografia());u.setActualizadoEn(LocalDateTime.now(clock));
        usuarios.saveAndFlush(u);auditoria.registrar(actor.usuarioId(),"Usuario",u.getId(),"PERFIL_ACTUALIZADO","Datos de perfil actualizados");return UsuarioResponse.of(u);
    }
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional
    public UsuarioResponse estado(long id,Estado input,AuthPrincipal actor){
        if(!Set.of("ACTIVO","SUSPENDIDO","DESACTIVADO").contains(input.estado()))throw ApiException.invalid("Estado de cuenta no permitido");
        Usuario u=usuarios.bloquear(id).orElseThrow(ApiException::notFound);version(u,input.version());
        if(u.getRolId()==Rol.ADMINISTRADOR.getId()&&!"ACTIVO".equals(input.estado()))throw ApiException.conflict("El acceso administrativo se conserva; usa el procedimiento de operacion del servidor");
        u.setEstado(input.estado());u.setActualizadoEn(LocalDateTime.now(clock));
        if(!"ACTIVO".equals(input.estado()))familias.revocarUsuario(id,LocalDateTime.now(clock));
        usuarios.saveAndFlush(u);auditoria.registrar(actor.usuarioId(),"Usuario",id,"ESTADO_CUENTA",input.motivo().trim());return UsuarioResponse.of(u);
    }
    private void version(Usuario u,long version){if(u.getVersion()!=version)throw ApiException.conflict("La cuenta cambio; actualiza la informacion antes de guardar");}
}
