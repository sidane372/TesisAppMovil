package com.santacecilia.api.exception;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public ApiException(HttpStatus status,String code,String message){super(message);this.status=status;this.code=code;}
    public HttpStatus getStatus(){return status;} public String getCode(){return code;}
    public static ApiException notFound(){return new ApiException(HttpStatus.NOT_FOUND,"NO_ENCONTRADO","No se encontro el recurso solicitado");}
    public static ApiException conflict(String message){return new ApiException(HttpStatus.CONFLICT,"CONFLICTO",message);}
    public static ApiException invalid(String message){return new ApiException(HttpStatus.BAD_REQUEST,"DATOS_INVALIDOS",message);}
    public static ApiException unauthenticated(){return new ApiException(HttpStatus.UNAUTHORIZED,"CREDENCIALES_INVALIDAS","Correo, contrasena o sesion no validos");}
}
