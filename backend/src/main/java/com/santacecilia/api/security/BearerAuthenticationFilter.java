package com.santacecilia.api.security;
import com.santacecilia.api.service.AuthService;
import com.santacecilia.api.exception.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
public class BearerAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService auth;private final ObjectMapper json;
    public BearerAuthenticationFilter(AuthService auth,ObjectMapper json){this.auth=auth;this.json=json;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        String header=request.getHeader("Authorization");
        if(header!=null){
            Optional<AuthPrincipal> actor=header.startsWith("Bearer ")?auth.autenticar(header.substring(7)):Optional.empty();
            if(actor.isEmpty()){
                response.setStatus(401);response.setContentType("application/json");
                json.writeValue(response.getOutputStream(),new ApiError("SESION_INVALIDA","Inicia sesion nuevamente",Map.of(),(String)request.getAttribute("correlationId")));return;
            }
            AuthPrincipal principal=actor.get();
            var authentication=new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_"+principal.rol().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request,response);
    }
}
