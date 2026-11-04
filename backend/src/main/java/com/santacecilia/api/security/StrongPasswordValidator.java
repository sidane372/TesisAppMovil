package com.santacecilia.api.security;
import jakarta.validation.*;
import java.nio.charset.StandardCharsets;
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword,String> {
    public boolean isValid(String value,ConstraintValidatorContext context) {
        return value!=null && value.codePointCount(0,value.length())>=12
            && value.getBytes(StandardCharsets.UTF_8).length<=72
            && value.codePoints().noneMatch(Character::isISOControl);
    }
}
