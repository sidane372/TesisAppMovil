package com.santacecilia.api.dto;
import com.santacecilia.api.entity.*;
public record UsuarioResponse(Long id,String nombres,String apellidos,String correo,String telefono,String ciudad,String biografia,Rol rol,String estado,long version) {
    public static UsuarioResponse of(Usuario u) {
        return new UsuarioResponse(u.getId(),u.getNombres(),u.getApellidos(),u.getCorreo(),u.getTelefono(),u.getCiudad(),u.getBiografia(),Rol.fromId(u.getRolId()),u.getEstado(),u.getVersion());
    }
}
