package com.santacecilia.api.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.*;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ResponseEntity<ApiError> error(HttpStatus status,String code,String message,Map<String,String> fields,HttpServletRequest request){
        return ResponseEntity.status(status).body(new ApiError(code,message,fields,(String)request.getAttribute("correlationId")));
    }
    @ExceptionHandler(ApiException.class) ResponseEntity<ApiError> api(ApiException e,HttpServletRequest r){return error(e.getStatus(),e.getCode(),e.getMessage(),Map.of(),r);}
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiError> validation(MethodArgumentNotValidException e,HttpServletRequest r){
        Map<String,String> fields=new LinkedHashMap<>();e.getBindingResult().getFieldErrors().forEach(f->fields.putIfAbsent(f.getField(),f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST,"VALIDACION","Revisa los campos indicados",fields,r);
    }
    @ExceptionHandler({ConstraintViolationException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> invalid(Exception e,HttpServletRequest r){return error(HttpStatus.BAD_REQUEST,"DATOS_INVALIDOS","El formato o los campos enviados no son validos",Map.of(),r);}
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ApiError> integrity(Exception e,HttpServletRequest r){return error(HttpStatus.CONFLICT,"DATOS_EN_CONFLICTO","La operacion entra en conflicto con un registro o restriccion existente",Map.of(),r);}
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class) ResponseEntity<ApiError> stale(Exception e,HttpServletRequest r){return error(HttpStatus.CONFLICT,"VERSION_DESACTUALIZADA","La informacion cambio. Actualiza antes de guardar",Map.of(),r);}
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ApiError> denied(Exception e,HttpServletRequest r){return error(HttpStatus.FORBIDDEN,"ACCESO_DENEGADO","No tienes permiso para esta operacion",Map.of(),r);}
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<ApiError> missing(Exception e,HttpServletRequest r){return error(HttpStatus.NOT_FOUND,"NO_ENCONTRADO","No se encontro el recurso solicitado",Map.of(),r);}
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(Exception e,HttpServletRequest r){return error(HttpStatus.METHOD_NOT_ALLOWED,"METODO_NO_PERMITIDO","Metodo no disponible para esta ruta",Map.of(),r);}
    @ExceptionHandler(Exception.class) ResponseEntity<ApiError> unexpected(Exception e,HttpServletRequest r){
        LOG.error("Error interno {}: {}",r.getAttribute("correlationId"),e.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR,"ERROR_INTERNO","No pudimos completar la operacion",Map.of(),r);
    }
}
