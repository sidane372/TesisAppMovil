package com.santacecilia.api.security;
import com.santacecilia.api.entity.Rol;
public record AuthPrincipal(long usuarioId,Rol rol,String familia) {}
