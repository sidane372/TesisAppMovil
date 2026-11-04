package com.santacecilia.api.config;
import com.santacecilia.api.entity.*;
import com.santacecilia.api.repository.UsuarioRepository;
import com.santacecilia.api.audit.AuditoriaService;
import com.santacecilia.api.dto.UsuarioDtos.CrearMusico;
import jakarta.validation.Validator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Component @ConditionalOnProperty(name="app.bootstrap.enabled",havingValue="true")
public class AdministradorInicial implements CommandLineRunner {
    private final UsuarioRepository usuarios;private final PasswordEncoder passwords;private final AuditoriaService auditoria;
    private final Validator validator;private final Clock clock;private final String email,password,nombres,apellidos;
    public AdministradorInicial(UsuarioRepository usuarios,PasswordEncoder passwords,AuditoriaService auditoria,Validator validator,Clock clock,
        @Value("${app.bootstrap.email:}") String email,@Value("${app.bootstrap.password:}") String password,
        @Value("${app.bootstrap.nombres:Administrador}") String nombres,@Value("${app.bootstrap.apellidos:Santa Cecilia}") String apellidos){
        this.usuarios=usuarios;this.passwords=passwords;this.auditoria=auditoria;this.validator=validator;this.clock=clock;
        this.email=email;this.password=password;this.nombres=nombres;this.apellidos=apellidos;
    }
    @Override @Transactional public void run(String... args){
        if(usuarios.existsByRolId(Rol.ADMINISTRADOR.getId()))return;
        if(!validator.validate(new CrearMusico(nombres,apellidos,email,password,Rol.ADMINISTRADOR)).isEmpty())
            throw new IllegalStateException("Configura correo y contrasena seguros para el administrador inicial");
        if(usuarios.findByCorreoNormalizado(email.trim()).isPresent())throw new IllegalStateException("El correo inicial ya corresponde a otra cuenta; no se eleva su rol");
        LocalDateTime now=LocalDateTime.now(clock);Usuario u=new Usuario();u.setRolId(Rol.ADMINISTRADOR.getId());u.setNombres(nombres.trim());u.setApellidos(apellidos.trim());
        u.setCorreo(email.trim());u.setPasswordHash(passwords.encode(password));u.setEstado("ACTIVO");u.setCreadoEn(now);u.setActualizadoEn(now);
        usuarios.saveAndFlush(u);auditoria.registrar(u.getId(),"Usuario",u.getId(),"ADMINISTRADOR_INICIAL","Creado por configuracion explicita de operacion");
    }
}
