package com.santacecilia.api.service;
import com.santacecilia.api.audit.AuditoriaService;
import com.santacecilia.api.dto.*;
import com.santacecilia.api.dto.ServicioDtos.*;
import com.santacecilia.api.entity.*;
import com.santacecilia.api.exception.ApiException;
import com.santacecilia.api.repository.*;
import com.santacecilia.api.security.AuthPrincipal;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class ServicioService {
    private final ServicioRepository servicios;private final UsuarioRepository usuarios;private final ContenidoServicioRepository contenido;
    private final AuditoriaService auditoria;private final Clock clock;
    public ServicioService(ServicioRepository servicios,UsuarioRepository usuarios,ContenidoServicioRepository contenido,AuditoriaService auditoria,Clock clock){
        this.servicios=servicios;this.usuarios=usuarios;this.contenido=contenido;this.auditoria=auditoria;this.clock=clock;
    }
    @Transactional(readOnly=true)
    public Catalogo catalogo(String q,String tipo,String genero,String ciudad,Long tarifaMax){
        if(tipo!=null&&!Set.of("BANDA","SOLISTA").contains(tipo))throw ApiException.invalid("Tipo de servicio no valido");
        if(tarifaMax!=null&&tarifaMax<0)throw ApiException.invalid("La tarifa maxima debe ser no negativa");
        var result=servicios.publicados().stream().filter(s->tipo==null||s.getTipo().equals(tipo))
            .filter(s->ciudad==null||s.getCiudad().equalsIgnoreCase(ciudad))
            .filter(s->tarifaMax==null||(s.getTarifaCentimos()!=null&&s.getTarifaCentimos()<=tarifaMax))
            .filter(s->genero==null||contenido.generos(s.getId()).stream().anyMatch(g->g.equalsIgnoreCase(genero)))
            .filter(s->q==null||q.isBlank()||coincide(s,q))
            .map(this::resumen).toList();return new Catalogo(true,result);
    }
    private boolean coincide(ServicioMusical s,String q){
        String query=q.toLowerCase(Locale.ROOT).trim();List<String> values=new ArrayList<>(List.of(s.getNombre(),s.getCiudad()));
        values.addAll(contenido.generos(s.getId()));values.addAll(contenido.instrumentos(s.getId()));
        return values.stream().anyMatch(value->value.toLowerCase(Locale.ROOT).contains(query));
    }
    private Resumen resumen(ServicioMusical s){return new Resumen(s.getId(),s.getNombre(),s.getTipo(),s.getCiudad(),s.getTarifaCentimos(),contenido.generos(s.getId()),contenido.instrumentos(s.getId()));}
    @Transactional(readOnly=true)
    public Detalle obtener(long id,AuthPrincipal actor){
        ServicioMusical s=servicios.findById(id).orElseThrow(ApiException::notFound);
        boolean publicado="PUBLICADO".equals(s.getEstadoPublicacion())&&usuarios.findById(s.getResponsableId()).map(u->"ACTIVO".equals(u.getEstado())).orElse(false);
        if(!publicado)propietario(s,actor);return detalle(s);
    }
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional(readOnly=true)
    public Pagina<Detalle> listar(int pagina,int tamano){
        if(pagina<0||tamano<1||tamano>100)throw ApiException.invalid("Paginacion no valida");
        var page=servicios.findAll(PageRequest.of(pagina,tamano,Sort.by("id")));
        return new Pagina<>(page.getContent().stream().map(this::detalle).toList(),pagina,tamano,page.getTotalElements());
    }
    @PreAuthorize("hasAnyRole('DIRECTOR','SOLISTA')") @Transactional(readOnly=true)
    public List<Detalle> propios(AuthPrincipal actor){return servicios.findByResponsableIdOrderByNombreAsc(actor.usuarioId()).stream().map(this::detalle).toList();}
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional
    public Detalle crear(Guardar input,AuthPrincipal actor){
        validarResponsable(input);ServicioMusical s=new ServicioMusical();asignar(s,input);s.setEstadoPublicacion("BORRADOR");
        s.setCreadoEn(LocalDateTime.now(clock));s.setActualizadoEn(LocalDateTime.now(clock));servicios.saveAndFlush(s);
        contenido.reemplazarListas(s.getId(),input);contenido.crearRecursoServicio(s.getId());
        contenido.asegurarRecursoMusico(s.getResponsableId());
        auditoria.registrar(actor.usuarioId(),"ServicioMusical",s.getId(),"SERVICIO_CREADO","Servicio registrado por administrador");return detalle(s);
    }
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional
    public Detalle editar(long id,Guardar input,AuthPrincipal actor){
        if(input.version()==null)throw ApiException.invalid("Se requiere la version del servicio");
        validarResponsable(input);ServicioMusical s=servicios.bloquear(id).orElseThrow(ApiException::notFound);version(s,input.version());
        if(!s.getTipo().equals(input.tipo()))throw ApiException.conflict("El tipo no se cambia: registra el servicio correcto sin eliminar su historia");
        if(!s.getResponsableId().equals(input.responsableId())&&contenido.tieneHistoria(id))throw ApiException.conflict("No se reasigna un servicio con historial mediante esta operacion");
        asignar(s,input);if("PUBLICADO".equals(s.getEstadoPublicacion()))s.setEstadoPublicacion("EN_REVISION");
        s.setActualizadoEn(LocalDateTime.now(clock));servicios.saveAndFlush(s);contenido.reemplazarListas(id,input);
        contenido.asegurarRecursoMusico(s.getResponsableId());
        auditoria.registrar(actor.usuarioId(),"ServicioMusical",id,"SERVICIO_ACTUALIZADO","Perfil enviado a revision cuando corresponde");return detalle(s);
    }
    @PreAuthorize("hasRole('ADMINISTRADOR')") @Transactional
    public Detalle publicacion(long id,Publicacion input,AuthPrincipal actor){
        ServicioMusical s=servicios.bloquear(id).orElseThrow(ApiException::notFound);version(s,input.version());
        if(!Set.of("BORRADOR","EN_REVISION","OBSERVADO","PUBLICADO","RETIRADO").contains(input.estado()))throw ApiException.invalid("Estado de publicacion no valido");
        if(Set.of("OBSERVADO","RETIRADO").contains(input.estado())&&(input.motivo()==null||input.motivo().trim().length()<5))throw ApiException.invalid("Indica un motivo de al menos cinco caracteres");
        if(Set.of("PUBLICADO","EN_REVISION").contains(input.estado())&&contenido.repertorio(id).isEmpty())throw ApiException.conflict("Agrega al menos una cancion antes de enviar a revision o publicar");
        if("PUBLICADO".equals(input.estado())&&!usuarios.findById(s.getResponsableId()).map(u->"ACTIVO".equals(u.getEstado())).orElse(false))throw ApiException.conflict("La cuenta responsable debe estar activa");
        s.setEstadoPublicacion(input.estado());s.setMotivoRevision(input.motivo());s.setActualizadoEn(LocalDateTime.now(clock));servicios.saveAndFlush(s);
        auditoria.registrar(actor.usuarioId(),"ServicioMusical",id,"PUBLICACION_"+input.estado(),input.motivo()==null?"Cambio autorizado":input.motivo());return detalle(s);
    }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','SOLISTA')") @Transactional
    public Detalle tarifa(long id,Tarifa input,AuthPrincipal actor){
        ServicioMusical s=servicios.bloquear(id).orElseThrow(ApiException::notFound);propietario(s,actor);version(s,input.version());
        s.setTarifaCentimos(input.tarifaCentimos());s.setActualizadoEn(LocalDateTime.now(clock));servicios.saveAndFlush(s);
        auditoria.registrar(actor.usuarioId(),"ServicioMusical",id,"TARIFA_ACTUALIZADA","Tarifa referencial; no modifica contratos previos");return detalle(s);
    }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','SOLISTA')") @Transactional
    public CancionResponse agregarCancion(long id,Cancion input,AuthPrincipal actor){
        ServicioMusical s=servicios.bloquear(id).orElseThrow(ApiException::notFound);propietario(s,actor);long cancion=contenido.agregarCancion(id,input);
        auditoria.registrar(actor.usuarioId(),"Repertorio",cancion,"CANCION_CREADA","Contenido propio del servicio");
        return contenido.repertorio(id).stream().filter(c->c.id()==cancion).findFirst().orElseThrow(ApiException::notFound);
    }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','SOLISTA')") @Transactional
    public void editarCancion(long id,long cancion,Cancion input,AuthPrincipal actor){
        propietario(servicios.bloquear(id).orElseThrow(ApiException::notFound),actor);
        if(!contenido.editarCancion(id,cancion,input))throw ApiException.notFound();
        auditoria.registrar(actor.usuarioId(),"Repertorio",cancion,"CANCION_ACTUALIZADA","Contenido propio del servicio");
    }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','DIRECTOR','SOLISTA')") @Transactional
    public void retirarCancion(long id,long cancion,AuthPrincipal actor){
        ServicioMusical s=servicios.bloquear(id).orElseThrow(ApiException::notFound);propietario(s,actor);
        if("PUBLICADO".equals(s.getEstadoPublicacion())&&contenido.repertorio(id).size()<=1)throw ApiException.conflict("Un servicio publicado debe conservar al menos una cancion");
        if(!contenido.retirarCancion(id,cancion))throw ApiException.notFound();
        auditoria.registrar(actor.usuarioId(),"Repertorio",cancion,"CANCION_RETIRADA","Retiro logico de contenido propio");
    }
    private void validarResponsable(Guardar input){
        Usuario u=usuarios.bloquear(input.responsableId()).orElseThrow(ApiException::notFound);
        Rol esperado="BANDA".equals(input.tipo())?Rol.DIRECTOR:Rol.SOLISTA;
        if(u.getRolId()!=esperado.getId()||!"ACTIVO".equals(u.getEstado()))throw ApiException.invalid("El responsable debe tener el rol musical correspondiente y una cuenta activa");
    }
    private void propietario(ServicioMusical s,AuthPrincipal actor){if(actor==null||(actor.rol()!=Rol.ADMINISTRADOR&&(!s.getResponsableId().equals(actor.usuarioId())||!Set.of(Rol.DIRECTOR,Rol.SOLISTA).contains(actor.rol()))))throw ApiException.notFound();}
    private void version(ServicioMusical s,long v){if(s.getVersion()!=v)throw ApiException.conflict("El servicio cambio; actualiza antes de guardar");}
    private void asignar(ServicioMusical s,Guardar i){s.setResponsableId(i.responsableId());s.setTipo(i.tipo());s.setNombre(i.nombre().trim());s.setCiudad(i.ciudad().trim());s.setDescripcionHistorica(i.descripcionHistorica());s.setExperiencia(i.experiencia());s.setIncluye(i.incluye());s.setTarifaCentimos(i.tarifaCentimos());}
    private Detalle detalle(ServicioMusical s){return new Detalle(s.getId(),s.getResponsableId(),s.getTipo(),s.getNombre(),s.getCiudad(),s.getDescripcionHistorica(),s.getExperiencia(),s.getIncluye(),s.getTarifaCentimos(),s.getEstadoPublicacion(),s.getVersion(),contenido.generos(s.getId()),contenido.instrumentos(s.getId()),contenido.tiposEvento(s.getId()),contenido.repertorio(s.getId()));}
}
