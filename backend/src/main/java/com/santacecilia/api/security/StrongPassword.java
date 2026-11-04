package com.santacecilia.api.security;
import jakarta.validation.*;
import java.lang.annotation.*;
@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default "Usa al menos 12 caracteres y como maximo 72 bytes UTF-8, sin caracteres de control";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
