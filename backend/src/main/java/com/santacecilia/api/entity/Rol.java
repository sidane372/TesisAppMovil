package com.santacecilia.api.entity;
public enum Rol {
    ADMINISTRADOR(1), CLIENTE(2), DIRECTOR(3), SOLISTA(4);
    private final short id;
    Rol(int id) { this.id=(short)id; }
    public short getId() { return id; }
    public static Rol fromId(short id) {
        for (Rol rol:values()) if(rol.id==id) return rol;
        throw new IllegalStateException("Rol de base de datos desconocido");
    }
}
