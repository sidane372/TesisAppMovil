package com.santacecilia.api.exception;
import org.springframework.http.HttpStatus;
/** Esta excepcion permite confirmar la revocacion al detectar reuso de refresh. */
public class TokenRejectedException extends ApiException {
    public TokenRejectedException(){super(HttpStatus.UNAUTHORIZED,"SESION_INVALIDA","Inicia sesion nuevamente");}
}
