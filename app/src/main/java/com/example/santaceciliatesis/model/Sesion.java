package com.example.santaceciliatesis.model;

/** Credenciales en memoria; no sobrescribe toString para evitar exponerlas en logs. */
public final class Sesion {
    private final String accessToken, refreshToken;
    private final long venceEn;
    private final Usuario usuario;

    public Sesion(String accessToken, String refreshToken, long venceEn, Usuario usuario) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.venceEn = venceEn;
        this.usuario = usuario;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getVenceEn() {
        return venceEn;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Sesion conUsuario(Usuario value) {
        return new Sesion(accessToken, refreshToken, venceEn, value);
    }
}
