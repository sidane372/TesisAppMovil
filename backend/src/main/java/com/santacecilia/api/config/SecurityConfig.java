package com.santacecilia.api.config;
import com.santacecilia.api.exception.ApiError;
import com.santacecilia.api.security.BearerAuthenticationFilter;
import com.santacecilia.api.service.AuthService;
import com.santacecilia.api.repository.UsuarioRepository;
import com.santacecilia.api.entity.Rol;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.*;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain chain(HttpSecurity http,AuthService auth,ObjectMapper json)throws Exception {
        http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c->c.disable()).formLogin(f->f.disable()).httpBasic(b->b.disable()).logout(l->l.disable())
            .authorizeHttpRequests(a->a
                .requestMatchers("/error","/restablecer.html","/restablecer.js","/restablecer.css").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/forgot-password","/api/v1/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/health","/api/v1/servicios","/api/v1/servicios/*","/api/v1/servicios/*/disponibilidad").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,error)->{res.setStatus(401);res.setContentType("application/json");json.writeValue(res.getOutputStream(),new ApiError("AUTENTICACION_REQUERIDA","Inicia sesion",Map.of(),(String)req.getAttribute("correlationId")));})
                .accessDeniedHandler((req,res,error)->{res.setStatus(403);res.setContentType("application/json");json.writeValue(res.getOutputStream(),new ApiError("ACCESO_DENEGADO","No tienes permiso para esta operacion",Map.of(),(String)req.getAttribute("correlationId")));}))
            .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; frame-ancestors 'none'; base-uri 'none'")))
            .addFilterBefore(new BearerAuthenticationFilter(auth,json),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean UserDetailsService userDetails(UsuarioRepository users){
        return email->{var u=users.findByCorreoNormalizado(email.trim()).orElseThrow(()->new UsernameNotFoundException("Credenciales invalidas"));
            return User.withUsername(u.getCorreo()).password(u.getPasswordHash()).roles(Rol.fromId(u.getRolId()).name()).disabled(!"ACTIVO".equals(u.getEstado())).build();};
    }
}
